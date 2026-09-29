"use server";

import { createHash } from "crypto";
import { caiDatLopCuaGv } from "../lop";
import { maHoa } from "../ma-hoa";
import { and, eq, inArray } from "drizzle-orm";
import { mkdir, writeFile } from "fs/promises";
import path from "path";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { requireRole } from "../auth";
import { laNhaKhoa, parseProvider } from "../ai-catalog";
import { probeProvider } from "../ai-harness";
import { db, sql } from "../db";
import {
  assignments,
  auditLogs,
  enrollments,
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
      ten: d.title,
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
  // Không có kết quả kiểm định (hoặc kết quả đã cũ vì kho đổi) = không phát hành. GV chỉ duyệt thay tầng KHÔNG KIỂM ĐƯỢC;
  // tầng SAI thì không duyệt được.
  if (!run || run.stale) {
    await audit(user.id, "DUYET_BI_CHAN", "problem", problemId, !run ? "chưa có lần kiểm định" : "kết quả kiểm định đã cũ");
    revalidatePath("/gv/duyet");
    return;
  }
  const tiers = await db.select().from(verificationTierResults).where(eq(verificationTierResults.runId, run.id));
  if (tiers.length < 3 || tiers.some((t) => t.status === "SAI")) {
    await audit(user.id, "DUYET_BI_CHAN", "problem", problemId, "có tầng SAI hoặc thiếu tầng");
    revalidatePath("/gv/duyet");
    return;
  }
  for (const t of tiers) {
    if (t.status === "KHONG_KIEM_DUOC") {
      await db
        .update(verificationTierResults)
        .set({ status: "GV_DUYET", reasonText: `GV_DUYET bởi ${user.displayName}: ${note}` })
        .where(eq(verificationTierResults.id, t.id));
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
  }>("verify", { ham: gen.ham, bai_lam: gen.bai_lam, thang_goi_y: gen.thang_goi_y || [], ...corpus });
  // Không có kết quả kiểm định đủ 3 tầng = không phát hành (chỉ tin DA_PHAT_HANH khi cả 3 tầng DAT/GV_DUYET)
  const tangOk =
    Array.isArray(verified?.tang) &&
    verified.tang.length === 3 &&
    verified.tang.every((t) => t.trang_thai === "DAT" || t.trang_thai === "GV_DUYET");
  const coSai = Array.isArray(verified?.tang) && verified.tang.some((t) => t.trang_thai === "SAI");
  const trangThai =
    verified?.trang_thai_phat_hanh === "DA_PHAT_HANH" && tangOk ? "DA_PHAT_HANH" : coSai ? "BI_CHAN" : "CHO_GIAO_VIEN_DUYET";
  verified.trang_thai_phat_hanh = trangThai;
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
      // SP-08: cấp gợi ý rỗng (có ly_do_trong) không tạo dòng gợi ý
      if (!cap.noi_dung || !String(cap.noi_dung).trim()) continue;
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

async function caiDatCuaGv(user: Awaited<ReturnType<typeof requireRole>>) {
  const { setting } = await caiDatLopCuaGv(user);
  return setting ? [setting] : [];
}

export async function luuCaiDatLop(form: FormData) {
  const user = await requireRole("GV");
  const mo = form.get("mo_loi_giai") === "on";
  const provider = parseProvider(form.get("ai_provider"));
  const model = String(form.get("ai_model") || "").trim() || null;
  const allowLocal = form.get("ai_allow_local") === "on";
  const keyRaw = String(form.get("ai_api_key") || "").trim();
  const xoaKey = form.get("xoa_ai_api_key") === "on";
  // F-08: chỉ lớp GV này dạy
  const rows = await caiDatCuaGv(user);
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
  else if (keyRaw && keyRaw !== "********") {
    // F-10: không lưu rõ; thiếu APP_ENC_KEY trên host thì bỏ qua khoá (không lưu), các cài đặt khác vẫn lưu
    try {
      patch.aiApiKey = maHoa(keyRaw);
    } catch (e) {
      console.error("luuCaiDatLop:", e instanceof Error ? e.message : "không mã hoá được khoá");
    }
  }
  await db.update(classSettings).set(patch).where(eq(classSettings.classId, rows[0].classId));
  await audit(user.id, "LUU_CAI_DAT_AI", "class_settings", rows[0].classId, provider);
  revalidatePath("/gv/cai-dat");
  revalidatePath("/hs");
}

export async function kiemTraNhaCungCap(providerRaw: string) {
  const user = await requireRole("GV");
  const provider = parseProvider(providerRaw);
  const row = (await caiDatCuaGv(user))[0];
  const r = await probeProvider({ provider, classApiKey: row?.aiApiKey, classProvider: row?.aiProvider });
  return { ok: r.ok, message: r.message, models: r.models };
}

function loiDanKhoa(provider: ReturnType<typeof parseProvider>): string {
  if (provider === "openrouter") return "Cần dán khóa vừa tạo trên OpenRouter.";
  if (provider === "zai") return "Cần dán khóa vừa tạo trên Z.AI.";
  return "Cần dán khóa API vừa tạo trên trang OpenAI.";
}

export async function ketNoiBangKhoa(form: FormData) {
  const user = await requireRole("GV");
  const key = String(form.get("ai_api_key") || "").trim();
  const model = String(form.get("ai_model") || "").trim() || null;
  const provider = parseProvider(form.get("ai_provider") || "cloud");
  if (!laNhaKhoa(provider)) {
    redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent("Nhà này không dùng khóa API."));
  }
  if (!key || key === "********") {
    redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent(loiDanKhoa(provider)));
  }
  // F-08: chỉ lớp GV này dạy
  const rows = await caiDatCuaGv(user);
  if (!rows[0]) redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent("Chưa có lớp."));
  const probe = await probeProvider({ provider, classApiKey: key });
  if (!probe.ok) {
    redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent(probe.message));
  }
  try {
    await db
      .update(classSettings)
      .set({
        aiProvider: provider,
        aiApiKey: maHoa(key), // F-10: AES-256-GCM, không lưu rõ
        aiModel: model,
        aiConnectedAt: new Date(),
      })
      .where(eq(classSettings.classId, rows[0].classId));
  } catch {
    redirect("/gv/ket-noi-ai?loi=" + encodeURIComponent("Không lưu được khóa. Không giữ khóa trên form."));
  }
  const viec = provider === "openrouter" ? "KET_NOI_OPENROUTER" : provider === "zai" ? "KET_NOI_ZAI" : "KET_NOI_CHATGPT";
  await audit(user.id, viec, "class_settings", rows[0].classId, "khoa_chinh_thuc");
  revalidatePath("/gv/ket-noi-ai");
  revalidatePath("/gv/cai-dat");
  revalidatePath("/gv");
  revalidatePath("/hs");
  redirect("/gv/ket-noi-ai?ok=1");
}

export async function ngatKetNoiAi() {
  const user = await requireRole("GV");
  // F-08: chỉ lớp GV này dạy
  const rows = await caiDatCuaGv(user);
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



/**
 * Giao bộ bài (brief: giáo viên duyệt và giao bộ bài theo mức). Chỉ giao bài ĐÃ PHÁT HÀNH.
 * Đích: cả lớp (mọi HS trong các lớp của giáo viên) hoặc một học sinh; có tên bộ và hạn nộp.
 * Bài đã giao cho học sinh đó thì cập nhật tên bộ/hạn, không nhân đôi.
 */
export async function giaoBoBai(form: FormData) {
  const user = await requireRole("GV");
  const ids = form.getAll("problemId").map(String).filter(Boolean);
  const dich = String(form.get("dich") || "lop");
  const ten = String(form.get("tenBo") || "").trim().slice(0, 120) || "Bộ bài";
  const hanRaw = String(form.get("han") || "").trim();
  const han = hanRaw ? new Date(`${hanRaw}T23:59:00+07:00`) : null;
  if (!ids.length) redirect("/gv/ngan-hang?loi=" + encodeURIComponent("Chọn ít nhất một bài"));
  if (han && Number.isNaN(han.getTime())) redirect("/gv/ngan-hang?loi=" + encodeURIComponent("Hạn nộp không hợp lệ"));

  const pubs = await db
    .select({ id: problems.id })
    .from(problems)
    .where(and(inArray(problems.id, ids), eq(problems.status, "DA_PHAT_HANH")));
  const hopLe = pubs.map((p) => p.id);
  if (!hopLe.length) redirect("/gv/ngan-hang?loi=" + encodeURIComponent("Chỉ giao được bài đã phát hành"));

  // Chỉ giao cho học sinh thuộc lớp mà giáo viên này dạy (cách ly dữ liệu theo lớp)
  const lopGv = await db
    .select({ classId: enrollments.classId })
    .from(enrollments)
    .where(and(eq(enrollments.userId, user.id), eq(enrollments.roleInClass, "GV")));
  const lopIds = lopGv.map((l) => l.classId);
  const hsTrongLop = lopIds.length
    ? await db
        .select({ userId: enrollments.userId })
        .from(enrollments)
        .where(and(inArray(enrollments.classId, lopIds), eq(enrollments.roleInClass, "HS")))
    : [];
  const tapHs = new Set(hsTrongLop.map((h) => h.userId));
  const hsIds = dich === "lop" ? [...tapHs] : tapHs.has(dich) ? [dich] : [];
  if (!hsIds.length) redirect("/gv/ngan-hang?loi=" + encodeURIComponent("Không có học sinh hợp lệ để giao"));

  const daCo = await db
    .select()
    .from(assignments)
    .where(and(inArray(assignments.studentId, hsIds), inArray(assignments.problemId, hopLe)));
  const khoa = new Map(daCo.map((a) => [`${a.studentId}|${a.problemId}`, a.id]));
  const now = new Date();
  let moi = 0;
  for (const hs of hsIds) {
    for (const pid of hopLe) {
      const id = khoa.get(`${hs}|${pid}`);
      if (id) {
        await db
          .update(assignments)
          .set({ setName: ten, dueAt: han, assignedBy: user.id, assignedAt: now, status: "assigned" })
          .where(eq(assignments.id, id));
      } else {
        moi++;
        await db.insert(assignments).values({
          id: crypto.randomUUID(),
          problemId: pid,
          studentId: hs,
          status: "assigned",
          setName: ten,
          dueAt: han,
          assignedBy: user.id,
          assignedAt: now,
        });
      }
    }
  }
  await audit(user.id, "GIAO_BO_BAI", "assignment_set", ten, `${hopLe.length} bài × ${hsIds.length} HS (mới ${moi})`);
  revalidatePath("/gv/ngan-hang");
  revalidatePath("/hs/bai");
  revalidatePath("/hs");
  redirect("/gv/ngan-hang?da_giao=" + encodeURIComponent(ten));
}
