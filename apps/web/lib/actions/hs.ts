"use server";

import { and, desc, eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";
import { requireRole } from "../auth";
import { db } from "../db";
import {
  auditLogs,
  errorTypes,
  gradingResults,
  hintLevels,
  inputEvents,
  problems,
  solutions,
  submissionSteps,
  submissionTableCells,
  submissionTables,
  submissions,
  tutorMessages,
  tutorSessions,
} from "../db/schema";
import { assertMayLearn, loadConfig, nghiDoanMo, applyMastery } from "../learning";
import { mathJob, type GradeResult } from "../math";
import { callLLM } from "../llm";
import { chinhSachXinDapAn, mauGiaSu, xinDapAn } from "../tutor";

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
  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  const p = prob[0];
  if (!p || p.status !== "DA_PHAT_HANH" || !p.hamSympy) {
    return { ok: false as const, thong_bao: "Bài chưa phát hành hoặc không chấm được bằng khung 5 bước." };
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
    return { ok: false as const, thong_bao: e instanceof Error ? e.message : "Không gọi được dịch vụ toán." };
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
        normalizerVersion: "norm-0.1",
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
  });
  await applyMastery({
    studentId: user.id,
    submissionId: subId,
    problemSkill: p.skillCode,
    ketQua: graded.ket_qua,
    buocSai: graded.buoc_sai,
    maLoi: graded.ma_loi,
    doTinCay: graded.do_tin_cay,
    nghi: Boolean(lyDo),
    finished,
  });
  revalidatePath("/hs");
  return {
    ok: true as const,
    ket_qua: graded.ket_qua,
    loai_ket_qua: graded.loai_ket_qua,
    buoc_sai: graded.buoc_sai,
    ma_loi: graded.ma_loi,
    thong_bao: lyDo ? `${graded.thong_bao} Bài này bị đánh dấu đoán mò nên không tính lên mức.` : graded.thong_bao,
    per_buoc: graded.per_buoc,
    finished,
    nghi_doan_mo: Boolean(lyDo),
  };
}

async function sessionFor(studentId: string, problemId: string) {
  const rows = await db
    .select()
    .from(tutorSessions)
    .where(and(eq(tutorSessions.studentId, studentId), eq(tutorSessions.problemId, problemId)))
    .orderBy(desc(tutorSessions.startedAt));
  if (rows[0]) return rows[0];
  const id = crypto.randomUUID();
  await db.insert(tutorSessions).values({
    id,
    studentId,
    problemId,
    state: "HS_LAM_BAI",
    hintCap: 0,
    answerRequests: 0,
    sameErrorRepeats: 0,
    lastBuoc: null,
  });
  return (await db.select().from(tutorSessions).where(eq(tutorSessions.id, id)))[0];
}

export async function hoiGiaSu(problemId: string, text: string) {
  const user = await requireRole("HS");
  await assertMayLearn(user);
  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  if (!prob[0] || prob[0].status !== "DA_PHAT_HANH") {
    return { ok: false as const, tra_loi: "Bài chưa phát hành." };
  }
  const sess = await sessionFor(user.id, problemId);
  const latestSub = await db
    .select()
    .from(submissions)
    .where(and(eq(submissions.studentId, user.id), eq(submissions.problemId, problemId)))
    .orderBy(desc(submissions.submittedAt))
    .limit(1);
  const grade = latestSub[0]
    ? (await db.select().from(gradingResults).where(eq(gradingResults.submissionId, latestSub[0].id)).limit(1))[0]
    : null;
  const buoc = (grade?.buocSai as { ma_buoc?: string } | null)?.ma_buoc || sess.lastBuoc || "B.DH.DAOHAM";
  let hintCap = sess.hintCap;
  let answerRequests = sess.answerRequests;
  let state = sess.state;
  const xin = xinDapAn(text) || /gợi ý|goi y/i.test(text);
  if (xinDapAn(text)) {
    answerRequests += 1;
    state = "GOI_Y";
    if (hintCap < 3) hintCap += 1;
  } else if (/gợi ý|goi y/i.test(text)) {
    if (hintCap < 3) hintCap += 1;
    state = `GOI_Y_${hintCap}`;
  } else if (grade?.ketQua === "DAT") {
    state = "TONG_KET";
  } else if (grade?.ketQua === "SAI") {
    state = hintCap > 0 ? `GOI_Y_${hintCap}` : "GIAI_THICH_LOI";
    if (sess.lastBuoc === buoc) {
      /* giữ cấp */
    }
  }
  const hints = await db
    .select()
    .from(hintLevels)
    .where(and(eq(hintLevels.problemId, problemId), eq(hintLevels.maBuoc, buoc)));
  const capRow = hints.find((h) => h.cap === Math.max(hintCap, xinDapAn(text) ? hintCap : 0));
  const goiY = hintCap > 0 ? capRow?.noiDung || hints.find((h) => h.cap === hintCap)?.noiDung || null : null;
  const err = grade?.maLoi ? (await db.select().from(errorTypes).where(eq(errorTypes.code, grade.maLoi)).limit(1))[0] : null;
  const cfg = await loadConfig();
  let draft: string;
  if (xinDapAn(text)) {
    draft = chinhSachXinDapAn(answerRequests, goiY);
  } else {
    const offline = mauGiaSu({
      state,
      thongBao: grade?.thongBao || null,
      loai: grade?.loaiKetQua || null,
      maLoi: grade?.maLoi || null,
      tenLoi: err?.name || null,
      doTinCay: grade?.doTinCay ?? null,
      nguong: cfg.nguong_tin_cay_ma_loi,
      goiY,
      cap: hintCap,
    });
    const llm = await callLLM({
      purpose: "tutor_turn",
      pseudonymId: user.pseudonymId,
      offlineText: offline,
      messages: [
        {
          role: "system",
          content:
            "Bạn là gia sư toán THPT, nói tiếng Việt, gọi học sinh là em. Không nêu đáp án, khoảng đơn điệu cuối, hay giá trị cực trị. Chỉ dùng gợi ý được đưa. Tối đa 4 câu. Nói rõ đây là AI.",
        },
        {
          role: "user",
          content: `Đề: ${prob[0].statementText}\nBước: ${buoc}\nLoại: ${grade?.loaiKetQua || "chua_nop"}\nGợi ý được mở: ${goiY || "(chưa)"}\nHọc sinh: ${text}`,
        },
      ],
    });
    draft = llm.text;
  }
  const facts = await db.select().from(solutions).where(eq(solutions.problemId, problemId)).limit(1);
  let blocked = false;
  try {
    const filtered = await mathJob<{ cho_phep: boolean }>("filter", {
      ban_nhap: draft,
      su_kien: facts[0]?.protectedFacts || [],
    });
    if (!filtered.cho_phep) {
      blocked = true;
      draft = goiY
        ? `Mình giữ lại câu vừa rồi vì có thể lộ kết quả. Em làm theo hướng này: ${goiY}`
        : "Mình không nói tiếp chi tiết đó. Em hãy tự viết lại bước đang dở.";
      const again = await mathJob<{ cho_phep: boolean }>("filter", {
        ban_nhap: draft,
        su_kien: facts[0]?.protectedFacts || [],
      });
      if (!again.cho_phep) {
        draft = "Em đọc lại đề và chỉ ra bước em đang làm. Mình không đưa kết quả.";
      }
    }
  } catch {
    draft = "Gia sư đang bận. Em cứ sửa bước được tô và nộp lại.";
  }
  await db.insert(tutorMessages).values({
    id: crypto.randomUUID(),
    sessionId: sess.id,
    role: "hs",
    content: text,
    redactedContent: text,
    blockedByFilter: false,
  });
  await db.insert(tutorMessages).values({
    id: crypto.randomUUID(),
    sessionId: sess.id,
    role: "gia_su",
    content: draft,
    redactedContent: draft,
    blockedByFilter: blocked,
  });
  await db
    .update(tutorSessions)
    .set({ state, hintCap, answerRequests, lastBuoc: buoc })
    .where(eq(tutorSessions.id, sess.id));
  return { ok: true as const, tra_loi: draft, blocked, cap: hintCap };
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
