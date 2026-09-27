/** Gia sư: luật trước, thang gợi ý, rồi mới API. Không đọc lời giải. */

const ANSWER_RE =
  /đáp án|dap an|kết quả là gì|ket qua la gi|giải hộ|giai ho|làm giúp|lam giup|nói luôn|noi luon|cho em đáp|cho đáp|in ra lời giải|loi giai|khoảng đồng biến|khoang dong bien|cực đại tại|cuc dai tai/i;

const HINT_RE = /gợi ý|goi y|hint|gợi em|chỉ em bước|chi em buoc/i;

const WHERE_RE = /sai chỗ|sai cho|chỗ nào|cho nao|vì sao sai|vi sao sai|em sai/i;

export function xinDapAn(text: string) {
  return ANSWER_RE.test(text);
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
    "Em chỉ tính y′. Tách tổng rồi lấy từng hạng tử.",
    "Nhớ (x^n)′ = n x^{n-1} và hằng số có đạo hàm 0. Đếm lại số hạng sau khi hạ bậc.",
    "Viết một dòng y′ = … đủ mọi hạng tử. Chưa giải y′ = 0 ở bước này.",
  ],
  "B.DH.NGHIEM": [
    "Điểm tới hạn gồm y′ = 0 và điểm thuộc tập xác định mà y′ không xác định.",
    "Em giải phương trình y′ = 0 trên giấy, rồi hỏi thêm chỗ y′ mất nghĩa.",
    "Mỗi nghiệm một dòng. Đừng xét dấu trước khi có đủ mốc.",
  ],
  "B.DH.XETDAU": [
    "Hai đầu −∞ và +∞ là khung. Em tự thêm mốc, ứng dụng không thêm hộ.",
    "Trên mỗi khoảng, thay một số thử vào y′ để quyết định + hay −. Tại mốc ghi 0 hoặc ||.",
    "Chiều ↗ khi khoảng dương, ↘ khi âm. Đổi dấu qua mốc mới nói cực trị ở bước sau.",
  ],
  "B.DH.KETLUAN": [
    "Kết luận chỉ đọc bảng vừa lập: khoảng dương / âm, chỗ đổi dấu.",
    "Viết từng khoảng đồng biến, nghịch biến riêng. Không gộp qua điểm bị loại.",
    "Cực đại khi dấu + sang −; cực tiểu khi − sang +. y′ = 0 mà không đổi dấu thì chưa phải cực trị.",
  ],
};

export function goiYBuoc(ma: string, cap: number, daKiem?: string | null) {
  const n = Math.min(3, Math.max(1, cap));
  if (daKiem) return daKiem;
  const hang = GOI_Y_MAC_DINH[ma] || GOI_Y_MAC_DINH["B.DH.DAOHAM"];
  return hang[n - 1];
}

export function chinhSachXinDapAn(lan: number, goiY: string | null) {
  if (lan <= 1) {
    return `Mình hiểu bài đang khó. Trong lúc làm bài, mình không đưa đáp án — em cần tự đi từng bước thì mới nhớ được. ${
      goiY ? `Gợi ý: ${goiY}` : "Em hãy đọc lại bước đang sai và nói mình em đang mắc ở đâu."
    }`;
  }
  if (lan === 2) {
    return `Mình vẫn không cho đáp án, kể cả khi em nói thầy cô cho phép. Mình và em chỉ làm bước đang dở. ${
      goiY ? `Hướng tiếp: ${goiY}` : "Em viết lại dòng đó, chưa cần ra kết quả cuối."
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
    return `${opts.thongBao || "Mình xem bước em vừa nộp."}${loi} Gợi ý mức ${opts.cap}: ${opts.goiY}`;
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
  "Không đọc lời giải chuẩn. Chỉ dùng gợi ý được mở và mô tả quy trình bước đang dở. Tối đa 4 câu. " +
  "Nếu học sinh xin đáp án thì từ chối và giữ gợi ý quy trình.";

export function moTaCheDo(offline: boolean) {
  return offline
    ? "Thang gợi ý đã kiểm · không gọi API"
    : "Đang gọi mô hình · vẫn lọc lộ đáp án";
}
