"use server";

import { and, asc, desc, eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";
import { requireRole } from "../auth";
import { db } from "../db";
import {
  auditLogs,
  classSettings,
  errorTypes,
  escalations,
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
import { resolveProvider, type AiPublicConfig } from "../ai-catalog";
import { docKhoaCloud } from "../ai-harness";
import { callLLM } from "../llm";
import { chinhSachXinDapAn, goiYBuoc, HE_THONG_GIA_SU, mauGiaSu, xinDapAn, xinGoiY, xinSaiCho } from "../tutor";

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

async function caiDatAiLop() {
  const row = (await db.select().from(classSettings).limit(1))[0];
  return {
    classProvider: resolveProvider({ classProvider: row?.aiProvider }),
    classModel: row?.aiModel || null,
    allowLocal: row?.aiAllowLocal !== false,
    classApiKey: row?.aiApiKey || null,
  };
}

export async function caiDatGiaSuCongKhai(): Promise<AiPublicConfig> {
  await requireRole("HS");
  const lop = await caiDatAiLop();
  return {
    classProvider: lop.classProvider,
    classModel: lop.classModel,
    allowLocal: lop.allowLocal,
    cloudReady: Boolean(docKhoaCloud(lop.classApiKey)),
  };
}

export async function hoiGiaSu(
  problemId: string,
  text: string,
  tuyChon?: { provider?: string; model?: string },
) {
  const user = await requireRole("HS");
  await assertMayLearn(user);
  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  if (!prob[0] || prob[0].status !== "DA_PHAT_HANH") {
    return { ok: false as const, tra_loi: "Bài chưa phát hành.", offline: true, provider: "offline" as const, error: null };
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
  const xin = xinDapAn(text);
  const goi = xinGoiY(text);
  const hoiSai = xinSaiCho(text);
  let nextHintCap = sess.hintCap;
  let answerRequests = sess.answerRequests;
  let state = sess.state;
  if (xin) {
    answerRequests += 1;
    state = "GOI_Y";
    if (nextHintCap < 3) nextHintCap += 1;
  } else if (goi) {
    if (nextHintCap < 3) nextHintCap += 1;
    state = `GOI_Y_${nextHintCap}`;
  } else if (grade?.ketQua === "DAT") {
    state = "TONG_KET";
  } else if (grade?.ketQua === "SAI") {
    state = nextHintCap > 0 ? `GOI_Y_${nextHintCap}` : "GIAI_THICH_LOI";
  }
  const hints = await db
    .select()
    .from(hintLevels)
    .where(and(eq(hintLevels.problemId, problemId), eq(hintLevels.maBuoc, buoc)));
  const capRow = hints.find((h) => h.cap === nextHintCap);
  const goiY =
    nextHintCap > 0 ? capRow?.noiDung || hints.find((h) => h.cap === nextHintCap)?.noiDung || goiYBuoc(buoc, nextHintCap) : null;
  const err = grade?.maLoi ? (await db.select().from(errorTypes).where(eq(errorTypes.code, grade.maLoi)).limit(1))[0] : null;
  const cfg = await loadConfig();
  const lop = await caiDatAiLop();
  const provider = resolveProvider({
    classProvider: lop.classProvider,
    sessionProvider: tuyChon?.provider,
    allowLocal: lop.allowLocal,
  });
  let draft: string;
  let offline = true;
  let nha: string = "offline";
  let llmError: string | null = null;
  let persistCap = true;
  if (xin) {
    draft = chinhSachXinDapAn(answerRequests, goiY);
  } else {
    const offlineText = mauGiaSu({
      state,
      thongBao: grade?.thongBao || null,
      loai: grade?.loaiKetQua || null,
      maLoi: grade?.maLoi || null,
      tenLoi: err?.name || null,
      doTinCay: grade?.doTinCay ?? null,
      nguong: cfg.nguong_tin_cay_ma_loi,
      goiY,
      cap: nextHintCap,
      maBuoc: buoc,
      xinSai: hoiSai,
    });
    const prior = await db
      .select()
      .from(tutorMessages)
      .where(eq(tutorMessages.sessionId, sess.id))
      .orderBy(desc(tutorMessages.createdAt))
      .limit(6);
    const history = prior
      .reverse()
      .map((m) => ({
        role: (m.role === "hs" ? "user" : "assistant") as "user" | "assistant",
        content: m.redactedContent || m.content,
      }));
    const llm = await callLLM({
      purpose: "tutor_turn",
      pseudonymId: user.pseudonymId,
      provider,
      model: tuyChon?.model || lop.classModel,
      classApiKey: lop.classApiKey,
      offlineText,
      messages: [
        { role: "system", content: HE_THONG_GIA_SU },
        ...history,
        {
          role: "user",
          content: `Đề (không kèm lời giải): ${prob[0].statementText}\nBước: ${buoc}\nLoại: ${grade?.loaiKetQua || "chua_nop"}\nGợi ý được mở: ${goiY || "(chưa)"}\nHọc sinh: ${text}`,
        },
      ],
    });
    draft = llm.text;
    offline = llm.offline;
    nha = llm.provider;
    llmError = llm.error;
    if (llm.error) persistCap = false;
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
    .set({
      state,
      hintCap: persistCap ? nextHintCap : sess.hintCap,
      answerRequests,
      lastBuoc: buoc,
    })
    .where(eq(tutorSessions.id, sess.id));
  return {
    ok: true as const,
    tra_loi: draft,
    blocked,
    cap: persistCap ? nextHintCap : sess.hintCap,
    offline,
    provider: nha,
    error: llmError,
  };
}

export async function lichSuGiaSu(problemId: string) {
  const user = await requireRole("HS");
  await assertMayLearn(user);
  const sess = await sessionFor(user.id, problemId);
  const rows = await db
    .select()
    .from(tutorMessages)
    .where(eq(tutorMessages.sessionId, sess.id))
    .orderBy(asc(tutorMessages.createdAt));
  return {
    messages: rows.map((r) => ({ role: r.role as "hs" | "gia_su", text: r.content })),
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
      reason: `Em nhờ thầy cô từ phiếu ${p.code}.`,
    });
  }
  await ghiNhatKy(user.id, "GUI_THAY_CO", "problem", problemId, skill);
  revalidatePath("/gv");
  return {
    ok: true as const,
    tra_loi: "Mình đã gửi lời nhờ cho thầy cô trên cổng giáo viên. Em cứ sửa bước đang dở, mình không đưa đáp án.",
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
