import { and, desc, eq } from "drizzle-orm";
import type { SessionUser } from "./auth";
import { resolveProvider } from "./ai-catalog";
import { cauHinhCongKhai } from "./ai-harness";
import { db } from "./db";
import {
  classSettings,
  errorTypes,
  gradingResults,
  hintLevels,
  problems,
  solutions,
  submissions,
  tutorMessages,
  tutorSessions,
} from "./db/schema";
import { goiKhoChoBuoc } from "./kho-lop";
import { dongKhoChoPrompt, nhanTrichDan } from "./kien-thuc";
import { assertMayLearn, loadConfig } from "./learning";
import { BUOC } from "./levels";
import { callLLM } from "./llm";
import { mathJob } from "./math";
import type { GiaSuBuocSse } from "./sse";
import { cauHoiXocratis, chinhSachXinDapAn, goiYBuoc, HE_THONG_GIA_SU, mauGiaSu, xinDapAn, xinGoiY, xinSaiCho } from "./tutor";

export type HoiGiaSuKet =
  | {
      ok: true;
      tra_loi: string;
      blocked: boolean;
      cap: number;
      offline: boolean;
      provider: string;
      error: string | null;
      trich_dan: { loai: string; id: string; ten: string; trich: string }[];
    }
  | {
      ok: false;
      tra_loi: string;
      offline: true;
      provider: "offline";
      error: string | null;
      trich_dan: [];
    };

export async function sessionFor(studentId: string, problemId: string) {
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

export async function caiDatAiLop() {
  const row = (await db.select().from(classSettings).limit(1))[0];
  return {
    classProvider: resolveProvider({ classProvider: row?.aiProvider }),
    classModel: row?.aiModel || null,
    allowLocal: row?.aiAllowLocal !== false,
    classApiKey: row?.aiApiKey || null,
  };
}

export async function caiDatGiaSuCongKhaiCho() {
  const lop = await caiDatAiLop();
  return cauHinhCongKhai(lop);
}

function daDung(signal?: AbortSignal) {
  return Boolean(signal?.aborted);
}

export async function chayHoiGiaSu(opts: {
  user: SessionUser;
  problemId: string;
  text: string;
  provider?: string;
  model?: string;
  onTrangThai?: (buoc: GiaSuBuocSse) => void | Promise<void>;
  signal?: AbortSignal;
}): Promise<HoiGiaSuKet> {
  const { user, problemId, text, signal } = opts;
  await assertMayLearn(user);
  const bao = async (buoc: GiaSuBuocSse) => {
    if (daDung(signal)) return;
    await opts.onTrangThai?.(buoc);
  };
  const dung = (): HoiGiaSuKet => ({
    ok: false,
    tra_loi: "Đã dừng. Không gửi lại câu hỏi.",
    offline: true,
    provider: "offline",
    error: "aborted",
    trich_dan: [],
  });

  const prob = await db.select().from(problems).where(eq(problems.id, problemId)).limit(1);
  if (!prob[0] || prob[0].status !== "DA_PHAT_HANH") {
    return {
      ok: false,
      tra_loi: "Bài chưa mở.",
      offline: true,
      provider: "offline",
      error: null,
      trich_dan: [],
    };
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
    sessionProvider: opts.provider,
    allowLocal: lop.allowLocal,
  });
  let draft: string;
  let offline = true;
  let nha: string = "offline";
  let llmError: string | null = null;
  let persistCap = true;
  await bao("kho");
  if (daDung(signal)) return dung();
  const kho = await goiKhoChoBuoc(buoc, text);
  const trichDan = nhanTrichDan(kho);
  if (xin) {
    draft = chinhSachXinDapAn(answerRequests, goiY, !grade);
  } else {
    const offlineText = [
      mauGiaSu({
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
      }),
      kho.congThuc[0] ? `Em mở «${kho.congThuc[0].ten}» — quy trình, không chép kết quả.` : "",
      cauHoiXocratis(buoc),
    ]
      .filter(Boolean)
      .join("\n\n");
    const prior = await db
      .select()
      .from(tutorMessages)
      .where(eq(tutorMessages.sessionId, sess.id))
      .orderBy(desc(tutorMessages.createdAt))
      .limit(4);
    const history = prior
      .reverse()
      .map((m) => ({
        role: (m.role === "hs" ? "user" : "assistant") as "user" | "assistant",
        content: m.redactedContent || m.content,
      }));
    await bao("goi");
    if (daDung(signal)) return dung();
    const llm = await callLLM({
      purpose: "tutor_turn",
      pseudonymId: user.pseudonymId,
      provider,
      model: opts.model || lop.classModel,
      classApiKey: lop.classApiKey,
      classProvider: lop.classProvider,
      offlineText,
      signal,
      messages: [
        { role: "system", content: HE_THONG_GIA_SU },
        ...history,
        {
          role: "user",
          content: `Đề (không kèm lời giải): ${prob[0].statementText}\nBước đang làm: ${BUOC.find((b) => b.ma === buoc)?.ten || "bước này"}\nTình trạng: ${grade ? "đã nộp" : "chưa nộp"}\nGợi ý được mở: ${goiY || "(chưa)"}\nCông thức và tài liệu lớp (đã duyệt, không phải lời giải — nếu dùng thì nhắc đúng tên):\n${dongKhoChoPrompt(kho)}\nHọc sinh: ${text}\nTrình bày: đoạn ngắn, danh sách, $...$ / $$...$$. Không mã bước, không chữ Phiếu, không nhắc lại đề.`,
        },
      ],
    });
    if (daDung(signal)) return dung();
    draft = llm.text;
    offline = llm.offline;
    nha = llm.provider;
    llmError = llm.error;
    if (llm.error) persistCap = false;
  }
  await bao("loc");
  if (daDung(signal)) return dung();
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
  if (daDung(signal)) return dung();
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
    citation: trichDan.length ? trichDan : null,
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
    ok: true,
    tra_loi: draft,
    blocked,
    cap: persistCap ? nextHintCap : sess.hintCap,
    offline,
    provider: nha,
    error: llmError,
    trich_dan: trichDan,
  };
}
