/** Gia sư: luật trước, thang gợi ý, rồi mới API. Không đọc lời giải. */

const ANSWER_RE =
  /đáp án|dap an|kết quả là gì|ket qua la gi|giải hộ|giai ho|làm giúp|lam giup|nói luôn|noi luon|cho em đáp|cho đáp|in ra lời giải|loi giai|khoảng đồng biến|khoang dong bien|cực đại tại|cuc dai tai/i;

const HINT_RE = /gợi ý|goi y|hint|gợi em|chỉ em bước|chi em buoc/i;

const WHERE_RE = /sai chỗ|sai cho|chỗ nào|cho nao|vì sao sai|vi sao sai|em sai/i;

/** «Đừng nêu đáp án» là ràng buộc, không phải xin đáp án. */
const TU_CHOI_DAP_AN =
  /(đừng|chớ|chơ|không|khong)\s+.{0,28}(đáp án|dap an|lời giải|loi giai|khoảng đồng biến|cực đại tại)/i;

export function xinDapAn(text: string) {
  const t = (text || "").trim();
  if (TU_CHOI_DAP_AN.test(t)) return false;
  return ANSWER_RE.test(t);
}

export function xinGoiY(text: string) {
  return HINT_RE.test(text) && !xinDapAn(text);
}

export function xinSaiCho(text: string) {
  return WHERE_RE.test(text) && !xinDapAn(text);
}

/** VanLehn / Andes: gợi ý khi em hỏi. Aleven: không bottom-out đáp án. */
export const GOI_Y_MAC_DINH: Record<string, [string, string, string]> = {
  "B.DH.TXD": [
    "Bước này chỉ hỏi hàm còn nghĩa ở đâu. Em nhìn từng thành phần: chia, căn, log.",
    "Em viết điều kiện tồn tại rồi lấy phần giao. Đa thức thường không bị loại điểm.",
    "Nếu không có mẫu hay căn chẵn, tập xác định là cả đường thẳng thực. Em viết ký hiệu đó, đừng nhảy sang đạo hàm.",
  ],
  "B.DH.DAOHAM": [
    "Em chỉ tính $y'$. Tách tổng rồi lấy từng hạng tử.",
    "Nhớ $(x^n)' = n x^{n-1}$ và hằng số có đạo hàm $0$. Đếm lại số hạng sau khi hạ bậc.",
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

export function goiYBuoc(ma: string, cap: number, daKiem?: string | null) {
  const n = Math.min(3, Math.max(1, cap));
  if (daKiem) return daKiem;
  const hang = GOI_Y_MAC_DINH[ma] || GOI_Y_MAC_DINH["B.DH.DAOHAM"];
  return hang[n - 1];
}

/** Aleven / Help Tutor: gợi ý nguyên lý, không operative bottom-out. */
export function cauHoiXocratis(ma: string) {
  const hang: Record<string, string> = {
    "B.DH.TXD": "Em tự hỏi: chỗ nào của hàm có thể làm mất nghĩa?",
    "B.DH.DAOHAM": "Em tự hỏi: mỗi hạng tử hạ bậc thế nào, hằng số đi đâu?",
    "B.DH.NGHIEM": "Em tự hỏi: ngoài y′ = 0, còn điểm nào y′ mất nghĩa trên tập xác định?",
    "B.DH.XETDAU": "Em tự hỏi: trên mỗi khoảng, một số thử cho dấu gì?",
    "B.DH.KETLUAN": "Em tự hỏi: dấu đổi ở mốc nào, và có bị loại điểm không?",
  };
  return hang[ma] || hang["B.DH.DAOHAM"];
}

export function chinhSachXinDapAn(lan: number, goiY: string | null, chuaNop = false) {
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
  return "Mình không đưa đáp án của bài này. Em có thể nghỉ vài phút, làm một bài dễ hơn, hoặc bấm Gửi thầy cô. Đây là gia sư AI, không phải giáo viên.";
}

export function mauGiaSu(opts: {
  state: string;
  thongBao: string | null;
  loai: string | null;
  maLoi: string | null;
  tenLoi: string | null;
  doTinCay: number | null;
  nguong: number;
  goiY: string | null;
  cap: number;
  maBuoc: string;
  xinSai?: boolean;
}) {
  if (opts.loai === "DAU_DOI_TRONG_KHOANG") {
    return "Ở bước nghiệm, có một khoảng mà y′ đổi dấu bên trong. Em tìm lại các điểm làm y′ bằng 0 hoặc không xác định. Mình không bảo em sửa dấu trước.";
  }
  const loi =
    opts.maLoi && opts.tenLoi && opts.doTinCay != null && opts.doTinCay >= opts.nguong
      ? ` Có thể em đang gặp lỗi: ${opts.tenLoi}.`
      : opts.thongBao
        ? ""
        : " Dòng hoặc ô này chưa ổn, em kiểm tra lại.";
  if (opts.xinSai && opts.thongBao) {
    return `${opts.thongBao}${loi} Mình chỉ tô bước đang sai, không sửa hộ từng số.`;
  }
  if (opts.cap > 0 && opts.goiY) {
    return `${opts.thongBao || "Mình xem bước em vừa nộp."}${loi}\n\n**Gợi ý.** ${opts.goiY}`;
  }
  if (opts.state === "TONG_KET") {
    return "Em đã đi hết các bước của bài này. Em thử nói lại bằng lời: em đã dùng dấu của y′ để kết luận thế nào?";
  }
  if (!opts.thongBao) {
    return `${goiYBuoc(opts.maBuoc, 1)} Em làm rồi nộp bước đó, mình mới chấm được.`;
  }
  return `${opts.thongBao}${loi} Em sửa rồi nộp lại bước đó nhé.`;
}

export const HE_THONG_GIA_SU =
  "Bạn là gia sư toán THPT tiếng Việt, gọi học sinh là em. Nói rõ đây là AI, không phải giáo viên. " +
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
    return "Thang gợi ý đã kiểm · không gọi API";
  }
  if (provider === "offline" || offline) return "Thang gợi ý đã kiểm · không gọi API";
  if (provider === "ollama") return "Ollama trên máy này · vẫn lọc lộ đáp án";
  if (provider === "lmstudio") return "LM Studio trên máy này · vẫn lọc lộ đáp án";
  if (provider === "openrouter") return "OpenRouter của lớp · vẫn lọc lộ đáp án";
  if (provider === "zai") return "Z.AI của lớp · vẫn lọc lộ đáp án";
  return "ChatGPT của lớp · vẫn lọc lộ đáp án";
}
