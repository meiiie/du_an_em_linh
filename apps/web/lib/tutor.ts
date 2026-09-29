/** Gia sư: luật trước, thang gợi ý, rồi mới API. Không đọc lời giải. */

/**
 * Nhận diện ý định (thử gia sư giai đoạn 2, N3/N8): chỉ bắt MẪU XIN, không bắt cụm chủ đề
 * ("khoảng đồng biến nghĩa là gì" là câu khái niệm, không phải xin đáp án).
 */
const ANSWER_RE = new RegExp(
  [
    "đáp án", "dap an", "cho em (xin )?(kết quả|lời giải|bảng|các khoảng|khoảng|nghiệm|giá trị)", "giải (hộ|giúp|dùm)",
    "làm (hộ|giúp|dùm)", "nói luôn", "in (ra )?lời giải", "lời giải đầy đủ", "loi giai", "bảng biến thiên mẫu", "bảng mẫu",
    "kết quả (là gì|bằng bao nhiêu)", "bằng bao nhiêu", "là bao nhiêu", "từ (số )?mấy", "số mấy", "mấy điểm cực trị",
    "có cực trị không", "âm hay dương\\?", "tăng hay giảm", "chọn một thôi", "trả lời có hoặc không", "nói .?đúng.? hoặc .?sai",
    "bài tương tự với số y hệt", "giải thích ngược từ kết quả", "liệt kê các khoảng",
    "\\banswer\\b", "\\bsolution\\b", "what are the", "just the numbers", "solve (it|this)", "tell me the",
  ].join("|"),
  "i",
);
const JAILBREAK_RE = /bỏ qua (mọi |tất cả )?(hướng dẫn|chỉ dẫn|luật)|ignore (the |all |previous |your )|developer mode|\[system\]|protected_facts|máy giải toán không giới hạn|thầy (giáo|cô)? ?cho phép|em là giáo viên/i;
const HINT_RE = /gợi ý|goi y|\bhint\b|gợi em|chỉ em bước|chi em buoc|không biết bắt đầu|bắt đầu từ đâu/i;
const WHERE_RE = /sai chỗ|sai cho|chỗ nào|cho nao|vì sao sai|vi sao sai|em sai/i;
const KHAI_NIEM_RE = /nghĩa là (gì|sao)|là gì|là sao|định nghĩa|khái niệm|hiểu thế nào|tại sao phải|vì sao phải|what does .* mean/i;
const CUA_BAI_NAY = /(hàm|bài) (số )?này|của nó|\bthis function\b/i;
const KIEM_RE = /(đúng (không|chưa|chứ)|phải không|có phải|đúng hông|is (it|this) (right|correct))/i;

/** «Đừng nêu đáp án» là ràng buộc, không phải xin đáp án. */
const TU_CHOI_DAP_AN = /(đừng|chớ|không|khong)\s+.{0,28}(đáp án|dap an|lời giải|loi giai)/i;

export type YDinh = "XIN_DAP_AN" | "KIEM_KET_QUA" | "KHAI_NIEM" | "GOI_Y" | "SAI_CHO" | "KHAC";

export function yDinh(text: string): YDinh {
  const t = (text || "").trim();
  if (!t) return "KHAC";
  const coSo = /\d|\(|;|âm |dương |một|hai|ba\b/i.test(t);
  if (JAILBREAK_RE.test(t)) return "XIN_DAP_AN";
  // Câu hỏi khái niệm CHUNG ("... nghĩa là gì", "định nghĩa", "khái niệm") không nhắc tới bài này -> trả lời khái niệm (A14, A15).
  // Câu hỏi mẩu kết quả CỦA BÀI NÀY (A11, C06, D02, B04) vẫn là xin đáp án.
  if (/nghĩa là (gì|sao)|định nghĩa|khái niệm/i.test(t) && !CUA_BAI_NAY.test(t) && !/đáp án|lời giải|kết quả/i.test(t)) return "KHAI_NIEM";
  if (!TU_CHOI_DAP_AN.test(t) && ANSWER_RE.test(t)) return "XIN_DAP_AN";
  if (KIEM_RE.test(t) && coSo) return "KIEM_KET_QUA";
  if (HINT_RE.test(t)) return "GOI_Y";
  if (WHERE_RE.test(t)) return "SAI_CHO";
  if (KHAI_NIEM_RE.test(t)) return CUA_BAI_NAY.test(t) ? "XIN_DAP_AN" : "KHAI_NIEM";
  return "KHAC";
}

export function xinDapAn(text: string) {
  return yDinh(text) === "XIN_DAP_AN";
}

export function xinGoiY(text: string) {
  return yDinh(text) === "GOI_Y";
}

export function xinSaiCho(text: string) {
  return yDinh(text) === "SAI_CHO";
}

/** VanLehn / Andes: gợi ý khi em hỏi. Aleven: không bottom-out đáp án. */
export const GOI_Y_MAC_DINH: Record<string, [string, string, string]> = {
  "B.DH.TXD": [
    "Bước này chỉ hỏi hàm còn nghĩa ở đâu. Em nhìn từng thành phần: chia, căn, log.",
    "Em viết điều kiện tồn tại rồi lấy phần giao. Đa thức thường không bị loại điểm.",
    "",
  ],
  "B.DH.DAOHAM": [
    "Em chỉ tính $y'$. Hàm có dạng tổng, tích hay thương? Chọn quy tắc hợp với dạng đó.",
    "Tổng: đạo hàm từng hạng tử, $(x^n)' = n x^{n-1}$. Thương: $(u/v)' = (u'v - uv')/v^2$.",
    "Viết một dòng $y' = \\ldots$ đủ mọi hạng tử. Chưa giải $y' = 0$ ở bước này.",
  ],
  "B.DH.NGHIEM": [
    "Điểm tới hạn gồm $y' = 0$ và điểm thuộc tập xác định mà $y'$ không xác định.",
    "Em giải phương trình $y' = 0$ trên giấy, rồi hỏi thêm chỗ $y'$ mất nghĩa.",
    "Mỗi nghiệm một dòng. Đừng xét dấu trước khi có đủ mốc.",
  ],
  "B.DH.XETDAU": [
    "Hai đầu $-\\infty$ và $+\\infty$ là khung. Em tự thêm mốc, ứng dụng không thêm hộ.",
    "Trên mỗi khoảng, thay một số thử vào $y'$ để quyết định $+$ hay $-$. Tại mốc ghi $0$ hoặc $\\|$.",
    "Chiều ↗ khi khoảng dương, ↘ khi âm. Đổi dấu qua mốc mới nói cực trị ở bước sau.",
  ],
  "B.DH.KETLUAN": [
    "Kết luận chỉ đọc bảng vừa lập: khoảng dương / âm, chỗ đổi dấu.",
    "Viết từng khoảng đồng biến, nghịch biến riêng. Không gộp qua điểm bị loại.",
    "Cực đại khi dấu $+$ sang $-$; cực tiểu khi $-$ sang $+$. $y' = 0$ mà không đổi dấu thì chưa phải cực trị.",
  ],
};

/** Chỉ dùng khi bài KHÔNG có thang gợi ý đã kiểm định trong hint_levels. Cấp trống trả null. */
export function goiYBuoc(ma: string, cap: number, daKiem?: string | null) {
  const n = Math.min(3, Math.max(1, cap));
  if (daKiem) return daKiem;
  const hang = GOI_Y_MAC_DINH[ma] || GOI_Y_MAC_DINH["B.DH.TXD"];
  return hang[n - 1] || null;
}

/** Aleven / Help Tutor: gợi ý nguyên lý, không operative bottom-out. */
export function cauHoiXocratis(ma: string) {
  const hang: Record<string, string> = {
    "B.DH.TXD": "Em tự hỏi: chỗ nào của hàm có thể làm mất nghĩa?",
    "B.DH.DAOHAM": "Em tự hỏi: hàm có dạng tổng, tích hay thương, và quy tắc nào dùng cho dạng đó?",
    "B.DH.NGHIEM": "Em tự hỏi: ngoài y′ = 0, còn điểm nào y′ mất nghĩa trên tập xác định?",
    "B.DH.XETDAU": "Em tự hỏi: trên mỗi khoảng, một số thử cho dấu gì?",
    "B.DH.KETLUAN": "Em tự hỏi: dấu đổi ở mốc nào, và có bị loại điểm không?",
  };
  return hang[ma] || hang["B.DH.TXD"];
}

export function chinhSachXinDapAn(lan: number, goiY: string | null, chuaNop = false, baiDe?: string | null) {
  const nop = chuaNop ? " Em nộp bước đang làm trước, mình mới tô được chỗ sai." : "";
  const goi = goiY ? `\n\n**Gợi ý.** ${goiY}` : "";
  if (lan <= 1) {
    return `Mình hiểu bài đang khó. Trong lúc làm bài, mình không đưa đáp án — em cần tự đi từng bước thì mới nhớ được.${nop}${
      goi || "\n\nEm hãy đọc lại bước đang sai và nói mình em đang mắc ở đâu."
    }`;
  }
  if (lan === 2) {
    return `Mình vẫn không cho đáp án, kể cả khi em nói thầy cô cho phép. Mình và em chỉ làm bước đang dở.${
      goi || "\n\nEm viết lại dòng đó, chưa cần ra kết quả cuối."
    }`;
  }
  return `Mình không đưa đáp án của bài này. Em có thể nghỉ vài phút, ${baiDe ? `làm bài dễ hơn «${baiDe}»` : "làm một bài dễ hơn"}, hoặc bấm Gửi thầy cô.`;
}

export function mauGiaSu(opts: {
  state: string;
  daNop: boolean;
  ketQua: string | null;
  thongBao: string | null;
  loai: string | null;
  maLoi: string | null;
  tenLoi: string | null;
  doTinCay: number | null;
  nguong: number;
  goiY: string | null;
  cap: number;
  maBuoc: string;
  tenBuoc: string;
  xinSai?: boolean;
  soVanDeKhac?: number;
}) {
  const goi = opts.goiY ? `\n\n**Gợi ý.** ${opts.goiY}` : "";
  // N1: chưa nộp thì không nói "vừa nộp" hay "chưa ổn"
  if (!opts.daNop) {
    return `Em chưa nộp bước nào của bài này. Em đang ở bước ${opts.tenBuoc}: viết bước đó rồi bấm Nộp, mình mới chấm và tô được chỗ cần sửa.${goi}`;
  }
  if (opts.ketQua === "DAT") {
    if (opts.state === "TONG_KET") {
      return "Em đã đi hết các bước của bài này. Em thử nói lại bằng lời: em đã dùng dấu của y′ để kết luận thế nào?";
    }
    return `Các bước em đã nộp đều đạt. Giờ em làm bước ${opts.tenBuoc} rồi nộp.${goi}`;
  }
  if (opts.ketQua === "KHONG_KIEM_DUOC") {
    return `${opts.thongBao || "Máy chưa đọc được dòng vừa nộp."} Em viết lại theo mẫu gợi ý trong ô rồi nộp lại.${goi}`;
  }
  const con = opts.soVanDeKhac ? ` Sửa xong chỗ này, còn ${opts.soVanDeKhac} chỗ cần xem lại.` : "";
  if (opts.loai === "SAI_THU_TU_MOC") {
    return `Các mốc trên hàng x chưa theo thứ tự tăng dần. Em sắp lại các mốc từ trái sang phải trước, rồi mới xét dấu từng khoảng.${con}${goi}`;
  }
  if (opts.loai === "DAU_DOI_TRONG_KHOANG") {
    return `Có một khoảng em dựng mà y′ đổi dấu bên trong. Em tìm lại các điểm làm y′ bằng 0 hoặc không xác định. Mình không bảo em sửa dấu trước.${con}${goi}`;
  }
  const loi =
    opts.maLoi && opts.tenLoi && opts.doTinCay != null && opts.doTinCay >= opts.nguong ? ` Có thể em đang gặp lỗi: ${opts.tenLoi}.` : "";
  const tb = opts.thongBao || `Bước ${opts.tenBuoc} chưa ổn, em kiểm tra lại.`;
  if (opts.xinSai) return `${tb}${loi} Mình chỉ tô chỗ đang sai, không sửa hộ từng số.${con}`;
  return `${tb}${loi}${con}${goi || " Em sửa rồi nộp lại bước đó nhé."}`;
}

/** Câu khái niệm (N3): trả lời bằng thẻ công thức/tài liệu lớp, không đụng số của bài. */
export function traLoiKhaiNiem(text: string, the: { ten: string; noiDung: string }[]) {
  const t = text.toLowerCase();
  const chon =
    the.find((c) => t.includes("đồng biến") && /đơn điệu|đồng biến/i.test(c.ten + c.noiDung)) ||
    the.find((c) => /cực (đại|tiểu|trị)/.test(t) && /cực trị/i.test(c.ten)) ||
    the.find((c) => /tới hạn/.test(t) && /tới hạn/i.test(c.ten)) ||
    the.find((c) => /đạo hàm|y'|y′/.test(t) && /đạo hàm/i.test(c.ten)) ||
    the[0];
  if (!chon) return "Đây là câu hỏi khái niệm. Em mở Kho công thức của lớp, mục liên quan, đọc định nghĩa rồi hỏi lại mình chỗ chưa rõ.";
  return `Theo «${chon.ten}» trong kho công thức của lớp: ${chon.noiDung}\n\nEm thử diễn đạt lại bằng lời của mình, rồi áp vào bước đang làm.`;
}

export function traLoiKiemKetQua(tenBuoc: string) {
  return `Mình không xác nhận đúng hay sai từng kết quả qua tin nhắn. Em ghi kết quả vào bước ${tenBuoc} rồi bấm Nộp: bộ chấm sẽ tô đúng dòng hoặc ô cần sửa.`;
}

export const HE_THONG_GIA_SU =
  "Bạn là gia sư toán THPT tiếng Việt, gọi học sinh là em. Xưng mình, không xưng cô hay thầy, không phải giáo viên. " +
  "Không tự giới thiệu lại mỗi lượt — không mở «Chào em», không «Mình là AI/gia sư/trợ lý». Bắt đầu thẳng vào việc. " +
  "Không nêu đáp án, khoảng đơn điệu cuối, điểm cực trị, hay giá trị cực trị. " +
  "Không đọc lời giải chuẩn. Chỉ dùng công thức và tài liệu lớp đã duyệt, cùng gợi ý đã mở. " +
  "Không bịa công thức ngoài danh sách đã cho. Nếu dùng mục [n] đã mở, viết [n] sát cuối ý (không thay số đầu dòng danh sách) và nhắc đúng tên trong «». " +
  "Hỏi Socratic đúng một câu về quy trình, không hỏi đáp án (KITE 2026: gợi ý / chỗ sai / quy trình). " +
  "Xin chỗ sai: chỉ tô bước đang sai, không sửa hộ số. Xin gợi ý: nguyên lý, không bottom-out (Aleven). " +
  "Tối đa 4 câu. Nếu học sinh xin đáp án thì từ chối và giữ gợi ý quy trình. " +
  "Trình bày như phiếu: mỗi ý một đoạn hoặc một dòng danh sách. Công thức $...$ cùng dòng, $$...$$ một mình một dòng. Không # tiêu đề, không hàng rào mã, không mã bước, không chữ «Phiếu». Câu hỏi để đoạn cuối. " +
  "Ví dụ dạng: đoạn ngắn; $$(x^n)' = n x^{n-1}$$; một danh sách; câu hỏi ở cuối.";

export function moTaCheDo(arg: boolean | { provider?: string; offline: boolean; error?: string | null }) {
  const offline = typeof arg === "boolean" ? arg : arg.offline;
  const provider = typeof arg === "boolean" ? (arg ? "offline" : "cloud") : arg.provider || (arg.offline ? "offline" : "cloud");
  const error = typeof arg === "boolean" ? null : arg.error;
  if (error) {
    if (provider === "ollama") return "Ollama · lỗi · không chuyển nhà khác";
    if (provider === "lmstudio") return "LM Studio · lỗi · không chuyển nhà khác";
    if (provider === "cloud") return "ChatGPT của lớp · lỗi · không chuyển nhà khác";
    if (provider === "openrouter") return "OpenRouter của lớp · lỗi · không chuyển nhà khác";
    if (provider === "zai") return "Z.AI của lớp · lỗi · không chuyển nhà khác";
    return "Thang gợi ý";
  }
  if (provider === "offline" || offline) return "Thang gợi ý";
  if (provider === "ollama") return "Ollama trên máy này";
  if (provider === "lmstudio") return "LM Studio trên máy này";
  if (provider === "openrouter") return "OpenRouter của lớp";
  if (provider === "zai") return "Z.AI của lớp";
  return "ChatGPT của lớp";
}
