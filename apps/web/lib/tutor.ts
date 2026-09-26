const ANSWER_RE =
  /đáp án|dap an|kết quả là gì|ket qua la gi|giải hộ|giai ho|làm giúp|lam giup|nói luôn|noi luon|cho em đáp|cho đáp|in ra lời giải|loi giai/i;

export function xinDapAn(text: string) {
  return ANSWER_RE.test(text);
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
  return "Mình không đưa đáp án của bài này. Em có thể nghỉ vài phút, làm một bài dễ hơn, hoặc bấm gửi thầy cô. Đây là gia sư AI, không phải giáo viên.";
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
}) {
  if (opts.loai === "DAU_DOI_TRONG_KHOANG") {
    return "Ở bước nghiệm, có một khoảng mà y′ đổi dấu bên trong. Em tìm lại các điểm làm y′ bằng 0 hoặc không xác định. Mình không bảo em sửa dấu trước.";
  }
  const loi =
    opts.maLoi && opts.tenLoi && opts.doTinCay != null && opts.doTinCay >= opts.nguong
      ? ` Có thể em đang gặp lỗi: ${opts.tenLoi}.`
      : " Dòng hoặc ô này chưa ổn, em kiểm tra lại.";
  if (opts.cap > 0 && opts.goiY) {
    return `${opts.thongBao || "Mình xem bước em vừa nộp."}${loi} Gợi ý mức ${opts.cap}: ${opts.goiY}`;
  }
  if (opts.state === "TONG_KET") {
    return "Em đã đi hết các bước của bài này. Em thử nói lại bằng lời: em đã dùng dấu của y′ để kết luận thế nào?";
  }
  return `${opts.thongBao || "Em cứ làm tiếp bước đang dở."}${opts.thongBao ? "" : ""}${loi} Em sửa rồi nộp lại bước đó nhé.`;
}
