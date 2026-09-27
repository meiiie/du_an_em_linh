"use server";

import { createHash } from "crypto";
import { eq } from "drizzle-orm";
import { mkdir, writeFile } from "fs/promises";
import path from "path";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { requireRole } from "../auth";
import { parseProvider } from "../ai-catalog";
import { probeProvider } from "../ai-harness";
import { db, sql } from "../db";
import {
  auditLogs,
  classSettings,
  contentReviews,
  documents,
  formulaSheets,
  formulas,
  hintLevels,
  problems,
  solutions,
  verificationRuns,
  verificationTierResults,
} from "../db/schema";
import { mathJob } from "../math";

async function audit(actor: string, action: string, entity: string, entityId: string, reason?: string) {
  await db.insert(auditLogs).values({
    id: crypto.randomUUID(),
    actorUserId: actor,
    action,
    entity,
    entityId,
    at: new Date(),
    reason: reason || null,
  });
}

function hashContent(data: unknown) {
  return createHash("sha256").update(JSON.stringify(data)).digest("hex");
}

async function currentCorpus() {
  const docs = await db.select().from(documents);
  const sheet = await db.select().from(formulaSheets);
  const latest = [...sheet].sort((a, b) => b.version - a.version)[0];
  const cts = latest ? await db.select().from(formulas).where(eq(formulas.formulaSheetId, latest.id)) : [];
  return {
    tai_lieu: docs.map((d) => ({
      id: d.id,
      text: d.textContent,
      license_status: d.licenseStatus,
      phien_ban: d.version,
    })),
    cong_thuc: cts.map((c) => ({ id: c.id, latex: c.latex, noi_dung: c.noiDung, ten: c.title })),
  };
}

export async function taiTaiLieu(form: FormData) {
  const user = await requireRole("GV");
  const title = String(form.get("title") || "").trim();
  const kind = String(form.get("kind") || "tu_soan");
  const license = String(form.get("license") || "tu_soan");
  let text = String(form.get("text") || "");
  const file = form.get("file");
  let storage: string | null = null;
  if (file instanceof File && file.size > 0) {
    const buf = Buffer.from(await file.arrayBuffer());
    const dir = path.join(process.cwd(), "..", "..", "data", "uploads");
    await mkdir(dir, { recursive: true });
    const name = `${crypto.randomUUID()}-${file.name.replace(/[^\w.]+/g, "_")}`;
    storage = path.join(dir, name);
    await writeFile(storage, buf);
    if (file.name.toLowerCase().endsWith(".pdf")) {
      try {
        const extracted = await mathJob<{ text?: string; trang_thai?: string }>("extract_pdf", { path: storage });
        if (extracted.text) text = `${text}\n${extracted.text}`.trim();
      } catch {
        text = text || "";
      }
    } else if (file.type.startsWith("text") || file.name.endsWith(".txt") || file.name.endsWith(".md")) {
      text = `${text}\n${buf.toString("utf8")}`.trim();
    }
  }
  if (!title) return;
  await db.insert(documents).values({
    id: crypto.randomUUID(),
    title,
    kind,
    source: "giáo viên nạp",
    licenseStatus: license,
    textContent: text,
    version: 1,
    uploadedBy: user.id,
    createdAt: new Date(),
  });
  await audit(user.id, "NAP_TAI_LIEU", "document", title);
  revalidatePath("/gv/tai-lieu");
  revalidatePath("/hs/kho");
  revalidatePath("/gv/ket-noi-ai");
  revalidatePath("/gv");
}

export async function themCongThuc(form: FormData) {
  const user = await requireRole("GV");
  const title = String(form.get("title") || "").trim();
  const latex = String(form.get("latex") || "").trim();
  const noiDung = String(form.get("noi_dung") || "").trim();
  if (!title || !noiDung) return;
  const sheets = await db.select().from(formulaSheets);
  const latest = [...sheets].sort((a, b) => b.version - a.version)[0];
  const oldFormulas = latest ? await db.select().from(formulas).where(eq(formulas.formulaSheetId, latest.id)) : [];
  const sheetId = crypto.randomUUID();
  const version = (latest?.version || 0) + 1;
  await db.insert(formulaSheets).values({
    id: sheetId,
    classId: latest?.classId || null,
    ownerTeacherId: user.id,
    version,
    status: "locked",
    lockedAt: new Date(),
    note: "Thêm công thức, phiên bản mới",
  });
  for (const f of oldFormulas) {
    await db.insert(formulas).values({
      id: crypto.randomUUID(),
      formulaSheetId: sheetId,
      skillCode: f.skillCode,
      title: f.title,
      latex: f.latex,
      noiDung: f.noiDung,
    });
  }
  await db.insert(formulas).values({
    id: crypto.randomUUID(),
    formulaSheetId: sheetId,
    skillCode: "T12.DH.03",
    title,
    latex: latex || title,
    noiDung,
  });
  await sql`update verification_runs set stale = true where stale = false`;
  await audit(user.id, "SUA_BANG_CONG_THUC", "formula_sheet", sheetId, `version ${version}`);
  revalidatePath("/gv/cong-thuc");
  revalidatePath("/hs/kho");
  revalidatePath("/gv/ket-noi-ai");
  revalidatePath("/gv");
}

export async function duyetBai(problemId: string, note: string) {
  const user = await requireRole("GV");
  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  if (!prob[0] || prob[0].status !== "CHO_GIAO_VIEN_DUYET") return;
  await db.insert(contentReviews).values({
    id: crypto.randomUUID(),
    problemId,
    contentHash: prob[0].contentHash,
    reviewerId: user.id,
    decision: "duyet",
    note: note || "GV duyệt các tầng không kiểm được",
    at: new Date(),
  });
  const runs = await db.select().from(verificationRuns).where(eq(verificationRuns.problemId, problemId));
  const run = [...runs].sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())[0];
  if (run) {
    const tiers = await db.select().from(verificationTierResults).where(eq(verificationTierResults.runId, run.id));
    for (const t of tiers) {
      if (t.status === "KHONG_KIEM_DUOC") {
        await db
          .update(verificationTierResults)
          .set({ status: "GV_DUYET", reasonText: `GV_DUYET bởi ${user.displayName}: ${note}` })
          .where(eq(verificationTierResults.id, t.id));
      }
    }
  }
  await db.update(problems).set({ status: "DA_PHAT_HANH" }).where(eq(problems.id, problemId));
  await audit(user.id, "DUYET_NOI_DUNG", "problem", problemId, note);
  revalidatePath("/gv/duyet");
}

export async function bacBai(problemId: string, note: string) {
  const user = await requireRole("GV");
  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  if (!prob[0]) return;
  await db.insert(contentReviews).values({
    id: crypto.randomUUID(),
    problemId,
    contentHash: prob[0].contentHash,
    reviewerId: user.id,
    decision: "bac",
    note: note || "GV bác",
    at: new Date(),
  });
  await db.update(problems).set({ status: "BI_CHAN" }).where(eq(problems.id, problemId));
  await audit(user.id, "BAC_NOI_DUNG", "problem", problemId, note);
  revalidatePath("/gv/duyet");
}

type Gen = {
  ham: string;
  latex: string;
  de_bai: string;
  muc_do_4: string;
  muc_do_bo_3: string;
  muc_bloom: string;
  bai_lam: unknown;
  payload_cham: unknown;
  thang_goi_y: { ma_buoc: string; cac_cap: { cap: number; noi_dung: string }[] }[];
  su_kien: unknown;
  ky_nang_chinh: string;
  loi?: string;
};

export async function sinhBienThe(form: FormData) {
  const user = await requireRole("GV");
  const dang = String(form.get("dang") || "bac_ba");
  const seed = Number(form.get("seed") || Date.now() % 10000);
  const gen = await mathJob<Gen>("generate", { dang, seed });
  if (gen.loi || !gen.ham) {
    redirect(`/gv/sinh-bai?loi=${encodeURIComponent(gen.loi || "Không sinh được")}`);
  }
  const corpus = await currentCorpus();
  const verified = await mathJob<{
    trang_thai_phat_hanh: string;
    trang_thai_tong: string;
    tang: { tang: number; trang_thai: string; ly_do?: string; loai_ket_qua?: string; buoc_sai?: unknown; trich_dan?: unknown; cong_thuc?: unknown }[];
  }>("verify", { ham: gen.ham, bai_lam: gen.bai_lam, ...corpus });
  const id = crypto.randomUUID();
  const code = `GEN-${dang}-${seed}-${id.slice(0, 8)}`;
  const contentHash = hashContent({ de: gen.de_bai, bl: gen.bai_lam, hints: gen.thang_goi_y });
  await db.insert(problems).values({
    id,
    code,
    skillCode: gen.ky_nang_chinh,
    skillCodesPhu: [],
    mucDo4: gen.muc_do_4,
    mucDoBo3: gen.muc_do_bo_3,
    bloomLevel: gen.muc_bloom,
    difficulty: 0.5,
    statementText: gen.de_bai.replace(/\$/g, ""),
    statementLatex: gen.latex,
    hamSympy: gen.ham,
    origin: "THAM_SO_HOA",
    status: verified.trang_thai_phat_hanh,
    contentHash,
    createdBy: user.id,
  });
  await db.insert(solutions).values({
    problemId: id,
    baiLam: gen.bai_lam,
    protectedFacts: gen.su_kien,
    finalAnswer: null,
  });
  for (const block of gen.thang_goi_y) {
    for (const cap of block.cac_cap) {
      await db.insert(hintLevels).values({ problemId: id, maBuoc: block.ma_buoc, cap: cap.cap, noiDung: cap.noi_dung });
    }
  }
  const runId = crypto.randomUUID();
  await db.insert(verificationRuns).values({
    id: runId,
    problemId: id,
    contentHash,
    overallStatus: verified.trang_thai_tong,
    publishStatus: verified.trang_thai_phat_hanh,
    stale: false,
    createdAt: new Date(),
  });
  for (const t of verified.tang) {
    await db.insert(verificationTierResults).values({
      id: crypto.randomUUID(),
      runId,
      tier: t.tang,
      status: t.trang_thai,
      loaiKetQua: t.loai_ket_qua || null,
      buocSai: t.buoc_sai || null,
      reasonText: t.ly_do || null,
      citation: t.trich_dan || t.cong_thuc || null,
      rawJson: t,
    });
  }
  await audit(user.id, "SINH_BIEN_THE", "problem", id, verified.trang_thai_phat_hanh);
  revalidatePath("/gv/ngan-hang");
  revalidatePath("/gv/duyet");
  redirect(`/gv/sinh-bai?ma=${encodeURIComponent(code)}&trang=${encodeURIComponent(verified.trang_thai_phat_hanh)}`);
}

export async function luuCaiDatLop(form: FormData) {
  const user = await requireRole("GV");
  const mo = form.get("mo_loi_giai") === "on";
  const provider = parseProvider(form.get("ai_provider"));
  const model = String(form.get("ai_model") || "").trim() || null;
  const allowLocal = form.get("ai_allow_local") === "on";
  const keyRaw = String(form.get("ai_api_key") || "").trim();
  const xoaKey = form.get("xoa_ai_api_key") === "on";
  const rows = await db.select().from(classSettings);
  if (!rows[0]) return;
  const patch: {
    moLoiGiaiSauKhiNop: boolean;
    aiProvider: string;
    aiModel: string | null;
    aiAllowLocal: boolean;
    aiApiKey?: string | null;
  } = {
    moLoiGiaiSauKhiNop: mo,
    aiProvider: provider,
    aiModel: model,
    aiAllowLocal: allowLocal,
  };
  if (xoaKey) patch.aiApiKey = null;
  else if (keyRaw && keyRaw !== "********") patch.aiApiKey = keyRaw;
  await db.update(classSettings).set(patch).where(eq(classSettings.classId, rows[0].classId));
  await audit(user.id, "LUU_CAI_DAT_AI", "class_settings", rows[0].classId, provider);
  revalidatePath("/gv/cai-dat");
  revalidatePath("/hs");
}

export async function kiemTraNhaCungCap(providerRaw: string) {
  await requireRole("GV");
  const provider = parseProvider(providerRaw);
  const row = (await db.select().from(classSettings).limit(1))[0];
  return probeProvider({ provider, classApiKey: row?.aiApiKey });
}

export async function ketNoiBangKhoa(form: FormData) {
  const user = await requireRole("GV");
  const key = String(form.get("ai_api_key") || "").trim();
  const model = String(form.get("ai_model") || "").trim() || null;
  if (!key || key === "********") {
    redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent("Cần dán khóa API vừa tạo trên trang OpenAI."));
  }
  const rows = await db.select().from(classSettings);
  if (!rows[0]) redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent("Chưa có lớp."));
  const probe = await probeProvider({ provider: "cloud", classApiKey: key });
  if (!probe.ok) {
    redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent(probe.message));
  }
  await db
    .update(classSettings)
    .set({
      aiProvider: "cloud",
      aiApiKey: key,
      aiModel: model,
      aiConnectedAt: new Date(),
    })
    .where(eq(classSettings.classId, rows[0].classId));
  await audit(user.id, "KET_NOI_CHATGPT", "class_settings", rows[0].classId, "khoa_chinh_thuc");
  revalidatePath("/gv/ket-noi-ai");
  revalidatePath("/gv/cai-dat");
  revalidatePath("/gv");
  revalidatePath("/hs");
  redirect("/gv/ket-noi-ai?ok=1");
}

export async function ngatKetNoiAi() {
  const user = await requireRole("GV");
  const rows = await db.select().from(classSettings);
  if (!rows[0]) return;
  await db
    .update(classSettings)
    .set({
      aiProvider: "offline",
      aiApiKey: null,
      aiOpenaiSub: null,
      aiOpenaiEmail: null,
      aiConnectedAt: null,
    })
    .where(eq(classSettings.classId, rows[0].classId));
  await audit(user.id, "NGAT_KET_NOI_AI", "class_settings", rows[0].classId);
  revalidatePath("/gv/ket-noi-ai");
  revalidatePath("/gv/cai-dat");
  revalidatePath("/gv");
  revalidatePath("/hs");
}


