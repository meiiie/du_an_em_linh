"use server";

import { and, asc, eq } from "drizzle-orm";
import { GIOI_HAN, dungMotLuot, khoDb } from "../gioi-han";
import { caiDatLopCuaHs } from "../lop";
import { trongPhienCua } from "../rls";
import { revalidatePath } from "next/cache";
import { requireRole } from "../auth";
import { db } from "../db";
import {
  auditLogs,
  solutions,
  escalations,
  gradingResults,
  inputEvents,
  problems,
  submissionSteps,
  submissionTableCells,
  submissionTables,
  submissions,
  tutorMessages,
} from "../db/schema";
import { assertMayLearn, loadConfig, nghiDoanMo, applyMastery, recommend } from "../learning";
import { mathJob, type GradeResult } from "../math";
import type { AiPublicConfig } from "../ai-catalog";
import { caiDatGiaSuCongKhaiCho, chayHoiGiaSu, sessionFor } from "../gia-su-luot";
import { taiNguyenKhoLop } from "../kho-lop";
import { docTrichDanLuu, xemKhoTheoKhung } from "../kien-thuc";
import { loiGiaiHocSinh } from "../loi-giai";

type Line = { dong: number; latex: string; loai?: string };
type Cell = { hang: string; k: number; gia_tri: string };
type Ev = { ma_buoc: string; o: { hang: string; k: number } | null; gia_tri_cu: string | null; gia_tri_moi: string; thoi_diem: string };

export type StepPayload = {
  nop_toi: string;
  cac_buoc: {
    ma_buoc: string;
    cac_dong?: Line[];
    khai_bao?: string[];
    bang?: { loai_bang: string; cac_o: Cell[] };
  }[];
  events: Ev[];
};

export async function nopBuoc(problemId: string, body: StepPayload) {
  const user = await requireRole("HS");
  await assertMayLearn(user);
  // F-10: 40 lần nộp / phút mỗi HS (chặn spam máy chấm)
  if (!(await dungMotLuot(khoDb, `nop_buoc:${user.id}`, GIOI_HAN.nopBuoc))) {
    return { ok: false as const, thong_bao: "Em nộp nhanh quá. Đợi một phút rồi nộp lại nhé." };
  }
  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  const p = prob[0];
  if (!p || p.status !== "DA_PHAT_HANH" || !p.hamSympy) {
    return { ok: false as const, thong_bao: "Bài chưa mở hoặc không chấm được." };
  }
  const cfg = await loadConfig();
  const lyDo = nghiDoanMo(body.events || [], cfg.nguong_doan_mo_so_lan_doi_o);
  let graded: GradeResult;
  try {
    graded = await mathJob<GradeResult>("grade", {
      ham: p.hamSympy,
      nop_toi: body.nop_toi,
      cac_buoc: body.cac_buoc,
    });
  } catch (e) {
    // B-20: chi tiết lỗi dịch vụ toán chỉ vào log, học sinh nhận câu chung
    console.error("grade lỗi", e instanceof Error ? e.message : e);
    return { ok: false as const, thong_bao: "Máy chấm đang bận. Em thử lại sau ít giây nhé." };
  }
  const subId = crypto.randomUUID();
  const finished = graded.ket_qua === "DAT" && body.nop_toi === "B.DH.KETLUAN" && !graded.chua_xong;
  await db.insert(submissions).values({
    id: subId,
    studentId: user.id,
    problemId,
    status: finished ? "da_cham" : "dang_cham",
    nghiDoanMo: Boolean(lyDo),
    nghiDoanMoLyDo: lyDo,
    ketQua: graded.ket_qua,
    submittedAt: new Date(),
  });
  for (const step of body.cac_buoc) {
    for (const line of step.cac_dong || []) {
      const norm = graded.chuan_hoa?.find((c) => c.ma_buoc === step.ma_buoc && c.dong === line.dong);
      await db.insert(submissionSteps).values({
        id: crypto.randomUUID(),
        submissionId: subId,
        maBuoc: step.ma_buoc,
        dong: line.dong,
        latex: line.latex,
        rawInput: line.latex,
        normalizedInput: norm?.chuoi_chuan_hoa || null,
        normalizerVersion: graded.phien_ban_chuan_hoa || "norm-0.2",
        normalizeStatus: norm?.trang_thai_chuan_hoa || null,
      });
    }
    if (step.bang) {
      const tableId = crypto.randomUUID();
      await db.insert(submissionTables).values({
        id: tableId,
        submissionId: subId,
        maBuoc: step.ma_buoc,
        loaiBang: step.bang.loai_bang || "XET_DAU",
      });
      for (const cell of step.bang.cac_o) {
        await db.insert(submissionTableCells).values({
          id: crypto.randomUUID(),
          tableId,
          hang: cell.hang,
          k: cell.k,
          giaTri: cell.gia_tri,
        });
      }
    }
  }
  for (const ev of body.events || []) {
    await db.insert(inputEvents).values({
      id: crypto.randomUUID(),
      submissionId: subId,
      maBuoc: ev.ma_buoc,
      o: ev.o,
      giaTriCu: ev.gia_tri_cu,
      giaTriMoi: ev.gia_tri_moi,
      thoiDiem: new Date(ev.thoi_diem),
    });
  }
  await db.insert(gradingResults).values({
    id: crypto.randomUUID(),
    submissionId: subId,
    ketQua: graded.ket_qua,
    loaiKetQua: graded.loai_ket_qua,
    buocSai: graded.buoc_sai,
    maLoi: graded.ma_loi,
    doTinCay: graded.do_tin_cay,
    perBuoc: graded.per_buoc,
    thongBao: graded.thong_bao,
    cacVanDe: graded.cac_van_de ?? null,
  });
  await applyMastery({
    studentId: user.id,
    submissionId: subId,
    problemSkill: p.skillCode,
    problemMuc: p.mucDo4,
    ketQua: graded.ket_qua,
    buocSai: graded.buoc_sai,
    maLoi: graded.ma_loi,
    doTinCay: graded.do_tin_cay,
    nghi: Boolean(lyDo),
    finished,
  });
  revalidatePath("/hs");
  let tiepTheo: { id: string; code: string; lyDo: string } | null = null;
  if (finished) {
    const goi = await recommend(user.id, problemId);
    if (goi) tiepTheo = { id: goi.problem.id, code: goi.problem.code, lyDo: goi.lyDo };
  }
  // F-05: lời giải chỉ rời máy chủ SAU khi HS xong bài và lớp bật "mở lời giải sau khi nộp"
  let loiGiai: string | null = null;
  if (finished) {
    // F-08: cài đặt của lớp HS này
    const st = await caiDatLopCuaHs(user.id);
    if (st?.moLoiGiaiSauKhiNop === true) {
      const sol = (await db.select().from(solutions).where(eq(solutions.problemId, problemId)).limit(1))[0];
      loiGiai = loiGiaiHocSinh(sol?.baiLam, sol?.finalAnswer);
    }
  }
  return {
    ok: true as const,
    ket_qua: graded.ket_qua,
    loai_ket_qua: graded.loai_ket_qua,
    cac_van_de: graded.cac_van_de ?? [],
    tiep_theo: tiepTheo,
    loi_giai: loiGiai,
    buoc_sai: graded.buoc_sai,
    ma_loi: graded.ma_loi,
    thong_bao: lyDo ? `${graded.thong_bao} Bài này bị đánh dấu đoán mò nên không tính lên mức.` : graded.thong_bao,
    per_buoc: graded.per_buoc,
    finished,
    nghi_doan_mo: Boolean(lyDo),
  };
}

export async function caiDatGiaSuCongKhai(): Promise<AiPublicConfig> {
  const u = await requireRole("HS");
  return caiDatGiaSuCongKhaiCho(u.id);
}

export async function hoiGiaSu(
  problemId: string,
  text: string,
  tuyChon?: { provider?: string; model?: string; maBuoc?: string },
) {
  const user = await requireRole("HS");
  return chayHoiGiaSu({ user, problemId, text, provider: tuyChon?.provider, model: tuyChon?.model, maBuoc: tuyChon?.maBuoc });
}

export async function khoLopCongKhai() {
  await requireRole("HS");
  const nguon = await taiNguyenKhoLop();
  return {
    congThuc: nguon.congThuc,
    taiLieu: nguon.taiLieu
      .filter((d) => d.licenseStatus !== "chua_ro")
      .map((d) => ({ id: d.id, title: d.title, trich: d.text.slice(0, 280), licenseStatus: d.licenseStatus })),
    khung: xemKhoTheoKhung(nguon),
  };
}

export async function lichSuGiaSu(problemId: string) {
  const user = await requireRole("HS");
  await assertMayLearn(user);
  const sess = await sessionFor(user.id, problemId);
  // F-08 RLS: đọc tin nhắn trong phiên app.user_id = HS này (chính sách 0010 chặn tin của phiên HS khác)
  const rows = await trongPhienCua(user.id, async (tx) =>
    tx<{ role: string; content: string; citation: unknown }[]>`
      select role, content, citation from tutor_messages where session_id = ${sess.id} order by created_at asc`,
  );
  return {
    messages: rows.map((r) => ({
      role: r.role as "hs" | "gia_su",
      text: r.content,
      trichDan: r.role === "gia_su" ? docTrichDanLuu(r.citation) : undefined,
    })),
    hintCap: sess.hintCap,
  };
}

export async function guiThayCo(problemId: string) {
  const user = await requireRole("HS");
  await assertMayLearn(user);
  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  const p = prob[0];
  if (!p) return { ok: false as const, tra_loi: "Không thấy bài." };
  const skill = p.skillCode || "T12.DH.03";
  const open = await db
    .select()
    .from(escalations)
    .where(and(eq(escalations.studentId, user.id), eq(escalations.skillCode, skill)));
  if (!open.some((e) => !e.handledAt)) {
    await db.insert(escalations).values({
      id: crypto.randomUUID(),
      studentId: user.id,
      skillCode: skill,
      reason: "Em nhờ thầy cô.",
    });
  }
  await ghiNhatKy(user.id, "GUI_THAY_CO", "problem", problemId, skill);
  revalidatePath("/gv");
  return {
    ok: true as const,
    tra_loi: "Mình đã gửi lời nhờ thầy cô. Em cứ sửa bước đang dở.",
  };
}

export async function ghiNhatKy(actorId: string, action: string, entity: string, entityId: string, reason?: string) {
  await db.insert(auditLogs).values({
    id: crypto.randomUUID(),
    actorUserId: actorId,
    action,
    entity,
    entityId,
    at: new Date(),
    reason: reason || null,
  });
}
