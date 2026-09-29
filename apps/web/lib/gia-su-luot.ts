import { and, desc, eq } from "drizzle-orm";
import { GIOI_HAN, dungMotLuot, khoDb } from "./gioi-han";
import { caiDatLopCuaHs } from "./lop";
import type { SessionUser } from "./auth";
import { resolveProvider } from "./ai-catalog";
import { cauHinhCongKhai } from "./ai-harness";
import { db } from "./db";
import {
  errorTypes,
  escalations,
  gradingResults,
  hintLevels,
  problems,
  solutions,
  submissions,
  tutorMessages,
  tutorSessions,
} from "./db/schema";
import { goiKhoChoBuoc, taiNguyenKhoLop } from "./kho-lop";
import { docTrichDanLuu, dongKhoChoPrompt, locTrichDanTheoLoi, nhanTrichDan, type TrichDanHien } from "./kien-thuc";
import { assertMayLearn, baiDeHonMotMuc, loadConfig } from "./learning";
import { BUOC } from "./levels";
import { boDauHop, locBanGiaSu } from "./loi-gia-su";
import { callLLM } from "./llm";
import { mathJob } from "./math";
import type { GiaSuBuocSse } from "./sse";
import {
  cauHoiXocratis,
  cauNhanDauU,
  chinhSachXinDapAn,
  goiYBuoc,
  HE_THONG_GIA_SU,
  mauGiaSu,
  traLoiKhaiNiem,
  traLoiKiemKetQua,
  yDinh,
} from "./tutor";

export type HoiGiaSuKet =
  | {
      ok: true;
      tra_loi: string;
      blocked: boolean;
      cap: number;
      offline: boolean;
      provider: string;
      error: string | null;
      trich_dan: TrichDanHien[];
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

/** F-08: cài đặt AI của lớp HS này (không lấy dòng đầu của cả bảng). */
export async function caiDatAiLop(hsId: string) {
  const row = await caiDatLopCuaHs(hsId);
  return {
    classProvider: resolveProvider({ classProvider: row?.aiProvider }),
    classModel: row?.aiModel || null,
    allowLocal: row?.aiAllowLocal !== false,
    classApiKey: row?.aiApiKey || null,
  };
}

export async function caiDatGiaSuCongKhaiCho(hsId: string) {
  const lop = await caiDatAiLop(hsId);
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
  maBuoc?: string;
  onTrangThai?: (buoc: GiaSuBuocSse) => void | Promise<void>;
  signal?: AbortSignal;
}): Promise<HoiGiaSuKet> {
  const { user, problemId, text, signal } = opts;
  await assertMayLearn(user);
  // F-10: câu hỏi ≤ 1000 ký tự; hạn mức 30 câu / 10 phút và 300 câu / ngày mỗi HS (không gọi mô hình khi vượt)
  const tuChoi = (tra_loi: string, error: string): HoiGiaSuKet => ({
    ok: false,
    tra_loi,
    offline: true,
    provider: "offline",
    error,
    trich_dan: [],
  });
  if (text.length > GIOI_HAN.doDaiCauHoi) {
    return tuChoi(`Câu hỏi dài quá ${GIOI_HAN.doDaiCauHoi} ký tự. Em hỏi ngắn lại, mỗi lần một ý nhé.`, "qua_dai");
  }
  if (!(await dungMotLuot(khoDb, `gia_su:${user.id}`, GIOI_HAN.giaSuNgan, GIOI_HAN.giaSuNgay))) {
    return tuChoi("Em đã hỏi nhiều trong thời gian ngắn. Em tự làm thử bước đang tô rồi hỏi lại sau ít phút nhé.", "het_han_muc");
  }
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
  const buoc = chonBuoc({ maBuocClient: opts.maBuoc, grade, lastBuoc: sess.lastBuoc });
  const tenBuoc = BUOC.find((b) => b.ma === buoc)?.ten || "đang làm";
  const yd = yDinh(text);
  const xin = yd === "XIN_DAP_AN";
  const goi = yd === "GOI_Y";
  const hoiSai = yd === "SAI_CHO";
  // SP-09 / N5: cấp gợi ý theo (bước, loại lỗi); số lần xin đáp án đặt lại khi sang bước khác
  const loaiNay = grade?.ketQua === "SAI" && (grade.buocSai as { ma_buoc?: string } | null)?.ma_buoc === buoc ? grade.loaiKetQua || "SAI" : "CHUNG";
  const khoaCap = `${buoc}|${loaiNay}`;
  const caps: Record<string, number> = { ...((sess.hintCaps as Record<string, number> | null) || {}) };
  const capCu = caps[khoaCap] || 0;
  let capMoi = capCu;
  // Chốt 11:25 b: bộ đếm xin đáp án đặt lại khi HS nộp MỘT LẦN THỬ (bài nộp mới) hoặc sang bước mới; không theo thời gian.
  const subId = latestSub[0]?.id || "-";
  const khoaDem = `${buoc}#${subId}`;
  let answerRequests = sess.answerRequestsBuoc === khoaDem ? sess.answerRequests : 0;
  let state = sess.state;
  const hints = await db
    .select()
    .from(hintLevels)
    .where(and(eq(hintLevels.problemId, problemId), eq(hintLevels.maBuoc, buoc)));
  const soCap = hints.length ? Math.max(...hints.map((h) => h.cap)) : 3;
  let hetThang = false;
  // Chốt 11:25 a: cấp chỉ tăng khi (1) HS bấm/xin "gợi ý thêm", hoặc (2) đã thử lại ở bước này mà VẪN SAI
  // (bài nộp sai mới kể từ lần mở cấp trước). Xin đáp án không mở cấp.
  const khoaThu = `${khoaCap}#sub`;
  const thuSaiMoi =
    grade?.ketQua === "SAI" && (grade.buocSai as { ma_buoc?: string } | null)?.ma_buoc === buoc && capCu > 0 && caps[khoaThu] !== undefined && String(caps[khoaThu]) !== subId;
  // Chuyển giáo viên theo phiên (chốt 11:37): cùng bước, sau khi hết thang, thêm 2 lần xin hoặc 2 lần thử sai -> gợi ý Gửi thầy cô
  // và tạo MỘT cảnh báo kẹt cho bước đó.
  const khoaHet = `${buoc}|HET`;
  let soLanSauHet = caps[khoaHet] || 0;
  const daHetThang = capCu >= soCap;
  if (thuSaiMoi && !xin) {
    if (capMoi < soCap) capMoi += 1;
    else soLanSauHet += 1;
  }
  if (xin) {
    answerRequests += 1;
    state = "XIN_DAP_AN";
    if (daHetThang) soLanSauHet += 1;
  } else if (goi) {
    if (!thuSaiMoi) {
      if (capMoi < soCap) capMoi += 1;
      else {
        hetThang = true;
        soLanSauHet += 1;
      }
    }
    state = hetThang ? "BAI_TUONG_TU" : `GOI_Y_${capMoi}`;
  } else if (grade?.ketQua === "DAT" && (grade.perBuoc as Record<string, string> | null)?.["B.DH.KETLUAN"] === "DAT") {
    state = "TONG_KET";
  } else if (grade?.ketQua === "SAI") {
    state = capMoi > 0 ? `GOI_Y_${capMoi}` : "GIAI_THICH_LOI";
  }
  // N4: gợi ý lấy từ thang ĐÃ KIỂM ĐỊNH của chính bài; chỉ khi bài không có thang mới dùng câu mặc định
  const capHien = xin ? capCu : capMoi;
  let goiY = capHien > 0 ? (hints.length ? hints.find((h) => h.cap === capHien)?.noiDung || null : goiYBuoc(buoc, capHien)) : null;
  const facts = await db.select().from(solutions).where(eq(solutions.problemId, problemId)).limit(1);
  const suKien = (facts[0]?.protectedFacts as unknown[] | null) || [];
  // Thang mẫu Sư phạm (supham/thang-goi-y-mau, 52 thang): khi bước này vừa nộp SAI với một loại kết quả có thang RIÊNG
  // (DIEM_THIEU, SAI_DAU, SAI_KET_LUAN…), gợi ý lấy từ thang riêng đó thay cho thang chung của bài. Câu đã qua bộ lọc lộ
  // đáp án ở dịch vụ toán (bị chặn thì lùi cấp / về thang chung). Cấp để trống (null) -> không hiện câu nào, đề xuất
  // bài dễ hơn (recommend/baiDeHonMotMuc) hoặc lời + «Gửi thầy cô». Lỗi dịch vụ -> giữ thang của bài như cũ.
  let capRong = false;
  if (capHien > 0 && loaiNay !== "CHUNG" && prob[0].hamSympy) {
    try {
      const mau = await mathJob<{ rieng?: boolean; noi_dung?: string | null; hanh_dong?: string | null }>(
        "goi_y",
        {
          ham: prob[0].hamSympy,
          de_bai: prob[0].statementText,
          ma_buoc: buoc,
          loai_ket_qua: loaiNay,
          cap: capHien,
          su_kien: suKien,
        },
        8000,
      );
      if (mau?.rieng && mau.noi_dung) goiY = mau.noi_dung;
      else if (mau?.rieng && !mau.noi_dung && mau.hanh_dong === "BAI_TUONG_TU_DE_HON") {
        goiY = null;
        capRong = !xin;
      }
    } catch {
      // dịch vụ toán lỗi / quá giờ: dùng thang của bài
    }
  }
  const err = grade?.maLoi ? (await db.select().from(errorTypes).where(eq(errorTypes.code, grade.maLoi)).limit(1))[0] : null;
  const cfg = await loadConfig();
  const lop = await caiDatAiLop(user.id);
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
  const prior = await db
    .select()
    .from(tutorMessages)
    .where(eq(tutorMessages.sessionId, sess.id))
    .orderBy(desc(tutorMessages.createdAt))
    .limit(4);
  const nhoId = [
    ...new Set(prior.flatMap((m) => docTrichDanLuu(m.citation).map((x) => x.id).filter(Boolean))),
  ];
  await bao("kho");
  if (daDung(signal)) return dung();
  const kho = xin ? { taiLieu: [], congThuc: [] } : await goiKhoChoBuoc(buoc, text, nhoId);
  const mo = nhanTrichDan(kho);
  const vanDe = ((grade?.cacVanDe as { nguyen_nhan?: string }[] | null) || []).filter((v) => !v.nguyen_nhan);
  const chuyenGv = soLanSauHet >= 2;
  const baiDe = (xin && answerRequests >= 3) || hetThang || capRong ? await baiDeHon(user.id, prob[0]) : null;
  if (chuyenGv) {
    state = "CHUYEN_GIAO_VIEN";
    draft = `Em đã dùng hết gợi ý của bước ${tenBuoc} mà vẫn vướng. Em bấm «Gửi thầy cô» để thầy cô xem cùng em; mình đã báo thầy cô là em đang kẹt ở bước này.`;
    const khoaBao = `${buoc}|BAO_GV`;
    if (!caps[khoaBao]) {
      await db.insert(escalations).values({
        id: crypto.randomUUID(),
        studentId: user.id,
        skillCode: prob[0].skillCode || "T12.DH.03",
        reason: `Kẹt ở bước ${tenBuoc} bài ${prob[0].code}: đã hết thang gợi ý, xin thêm/thử lại vẫn chưa được.`,
      });
      caps[khoaBao] = 1;
    }
  } else if (xin) {
    draft = chinhSachXinDapAn(answerRequests, goiY, !grade, baiDe ? `${baiDe.code} (/hs/luyen/${baiDe.id})` : null);
  } else if (yd === "KIEM_KET_QUA") {
    draft = traLoiKiemKetQua(tenBuoc);
  } else if (yd === "KHAI_NIEM") {
    const nguon = await taiNguyenKhoLop();
    draft = traLoiKhaiNiem(
      text,
      nguon.congThuc.map((c) => ({ ten: c.title, noiDung: c.noiDung || c.latex || "" })),
    );
  } else if (
    !goi &&
    grade?.ketQua === "SAI" &&
    grade.maLoi === "ERR.DH.07" &&
    (grade.buocSai as { ma_buoc?: string } | null)?.ma_buoc === "B.DH.KETLUAN" &&
    buoc === "B.DH.KETLUAN"
  ) {
    // §(23) luật dấu U: câu cố định theo cờ toan_dung (không gọi mô hình, không bao giờ viết U)
    draft = cauNhanDauU(grade.toanDung);
  } else if (capRong) {
    // Cấp gợi ý để trống theo thang mẫu (nói thêm là lộ kết quả): không hiện câu gợi ý, không lộ lý do nội bộ.
    state = "BAI_TUONG_TU";
    draft = baiDe
      ? `Gợi ý tiếp theo của bước ${tenBuoc} sẽ nói ra kết quả, nên mình dừng ở đây. Em thử bài dễ hơn «${baiDe.code}» (/hs/luyen/${baiDe.id}) rồi quay lại bài này.`
      : `Gợi ý tiếp theo của bước ${tenBuoc} sẽ nói ra kết quả, nên mình dừng ở đây. Hiện chưa có bài dễ hơn cùng kỹ năng đã mở cho em; em thử một bài tương tự trong mục Đề bài, hoặc bấm «Gửi thầy cô» để thầy cô xem cùng.`;
  } else if (hetThang) {
    // SP-17 / AI-5.h / chốt 11:37 (1): hết thang -> đề xuất một bài CỤ THỂ dễ hơn (cùng kỹ năng, thấp hơn một mức, đã phát hành,
    // em chưa làm). Không có thì nói bằng lời và gợi ý Gửi thầy cô.
    draft = baiDe
      ? `Em đã mở hết các gợi ý của bước ${tenBuoc}. Em thử bài dễ hơn «${baiDe.code}» (/hs/luyen/${baiDe.id}) rồi quay lại bài này.`
      : `Em đã mở hết các gợi ý của bước ${tenBuoc}. Hiện chưa có bài dễ hơn cùng kỹ năng đã mở cho em; em thử một bài tương tự trong mục Đề bài, hoặc bấm «Gửi thầy cô» để thầy cô xem cùng.`;
  } else {
    const offlineText = [
      mauGiaSu({
        state,
        daNop: Boolean(grade),
        ketQua: grade?.ketQua || null,
        thongBao: grade?.thongBao || null,
        loai: grade?.loaiKetQua || null,
        maLoi: grade?.maLoi || null,
        tenLoi: err?.name || null,
        doTinCay: grade?.doTinCay ?? null,
        nguong: cfg.nguong_tin_cay_ma_loi,
        goiY,
        cap: capMoi,
        maBuoc: buoc,
        tenBuoc,
        xinSai: hoiSai,
        soVanDeKhac: Math.max(0, vanDe.length - 1),
      }),
      mo[0] ? `Em mở «${mo[0].ten}» — quy trình, không chép kết quả.` : "",
      cauHoiXocratis(buoc),
    ]
      .filter(Boolean)
      .join("\n\n");
    const history = prior
      .reverse()
      .map((m) => ({
        role: (m.role === "hs" ? "user" : "assistant") as "user" | "assistant",
        content: m.redactedContent || m.content,
      }));
    await bao("goi");
    if (daDung(signal)) return dung();
    const tinhTrang = !grade
      ? "chưa nộp bước nào"
      : grade.ketQua === "DAT"
        ? "các bước đã nộp đều đạt"
        : `bước vừa nộp có vấn đề: ${grade.loaiKetQua || grade.ketQua} (bước ${BUOC.find((b) => b.ma === (grade.buocSai as { ma_buoc?: string } | null)?.ma_buoc)?.ten || "?"})`;
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
          content: `Đề (không kèm lời giải): ${prob[0].statementText}\nBước đang làm: ${tenBuoc}\nTình trạng: ${tinhTrang}\nThông báo chấm (không có giá trị đáp án): ${grade?.thongBao || "(chưa có)"}\nGợi ý được mở: ${goiY || "(chưa)"}\nCông thức và tài liệu lớp (đã duyệt, không phải lời giải). Mỗi mục có số [n]. Nếu dùng thì viết [n] ngay sau ý đó và nhắc tên trong «»:\n${dongKhoChoPrompt(kho)}\nHọc sinh: ${text}\nTrình bày: đoạn ngắn, danh sách, $...$ / $$...$$. Không mã bước, không chữ Phiếu, không nhắc lại đề. Không chào, không tự giới thiệu — vào thẳng gợi ý hoặc một câu hỏi. Không xác nhận đúng/sai kết quả học sinh tự nêu.`,
        },
      ],
    });
    if (daDung(signal)) return dung();
    draft = locBanGiaSu(llm.text);
    offline = llm.offline;
    nha = llm.provider;
    llmError = llm.error;
    if (llm.error) persistCap = false;
  }
  // §(23): mọi câu gia sư (mô hình, mẫu, thang gợi ý) không viết dấu hợp giữa hai khoảng
  draft = boDauHop(draft);
  await bao("loc");
  if (daDung(signal)) return dung();
  let blocked = false;
  const AN_TOAN = `Mình chưa nói tiếp được chi tiết đó. Em làm lại bước ${tenBuoc} theo gợi ý đã mở rồi nộp.`;
  try {
    // F-05: bài không có sự kiện bảo vệ thì bộ lọc không có gì để so -> không gửi bản nháp của mô hình
    if (!offline && suKien.length === 0) throw new Error("thiếu protectedFacts");
    const filtered = await mathJob<{ cho_phep: boolean; loi?: boolean }>("filter", {
      ban_nhap: draft,
      su_kien: suKien,
      cau_hoc_sinh: text,
    });
    if (!filtered.cho_phep) {
      blocked = true;
      draft = filtered.loi
        ? AN_TOAN
        : goiY
          ? `Em làm theo hướng này: ${goiY}`
          : "Mình không nói tiếp chi tiết đó. Em hãy tự viết lại bước đang dở.";
      const again = await mathJob<{ cho_phep: boolean }>("filter", {
        ban_nhap: draft,
        su_kien: suKien,
        cau_hoc_sinh: text,
      });
      if (!again.cho_phep) {
        draft = "Em đọc lại đề và chỉ ra bước em đang làm. Mình không đưa kết quả.";
      }
    }
  } catch {
    // FAIL CLOSED: bộ lọc lỗi thì không gửi bản nháp, chỉ gửi câu mẫu an toàn
    blocked = true;
    draft = AN_TOAN;
  }
  draft = locBanGiaSu(draft);
  if (daDung(signal)) return dung();
  const trichDan = xin || llmError || blocked ? [] : locTrichDanTheoLoi(draft, mo);
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
      hintCap: persistCap ? capMoi : sess.hintCap,
      hintCaps: {
        ...caps,
        ...(persistCap ? { [khoaCap]: capMoi } : {}),
        // mốc bài nộp tại lúc mở cấp: bài nộp sai MỚI hơn mốc này mới được tính là "đã thử lại vẫn sai"
        ...(persistCap && capMoi > 0 ? { [khoaThu]: subId as unknown as number } : {}),
        [khoaHet]: soLanSauHet,
      },
      answerRequests,
      answerRequestsBuoc: khoaDem,
      lastBuoc: buoc,
    })
    .where(eq(tutorSessions.id, sess.id));
  return {
    ok: true,
    tra_loi: draft,
    blocked,
    cap: persistCap ? capMoi : capCu,
    offline,
    provider: nha,
    error: llmError,
    trich_dan: trichDan,
  };
}


const THU_TU = BUOC.map((b) => b.ma) as string[];

/** Bước gia sư nói tới: bước client đang mở > bước sai của lần chấm gần nhất > bước đầu tiên chưa đạt (không mặc định đạo hàm). */
export function chonBuoc(opts: {
  maBuocClient?: string | null;
  grade: { ketQua: string; buocSai: unknown; perBuoc: unknown } | null | undefined;
  lastBuoc?: string | null;
}) {
  if (opts.maBuocClient && THU_TU.includes(opts.maBuocClient)) return opts.maBuocClient;
  const g = opts.grade;
  const sai = (g?.buocSai as { ma_buoc?: string } | null)?.ma_buoc;
  if (g && g.ketQua !== "DAT" && sai && THU_TU.includes(sai)) return sai;
  if (g) {
    const per = (g.perBuoc as Record<string, string> | null) || {};
    const chua = THU_TU.find((m) => per[m] !== "DAT");
    if (chua) return chua;
    return THU_TU[THU_TU.length - 1];
  }
  return THU_TU[0];
}

async function baiDeHon(studentId: string, p: { id: string; skillCode: string | null; mucDo4: string | null }) {
  const b = await baiDeHonMotMuc(studentId, p);
  return b ? { id: b.id, code: b.code } : null;
}
