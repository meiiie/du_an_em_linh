import { LABEL4, type Muc4 } from "./levels";

const TEN_NGAN: Record<string, string> = {
  "T12.DH.01": "Đồng biến từ f′",
  "T12.DH.02": "Điểm tới hạn",
  "T12.DH.03": "Xét dấu và bảng biến thiên",
  "T12.DH.04": "Nhận biết cực trị",
  "T12.DH.05": "Tìm cực trị",
  "T12.DH.06": "Bài có tham số",
  "T12.DH.07": "Bài thực tiễn",
};

const BUOC_NGAN: Record<string, string> = {
  "B.DH.TXD": "TXĐ",
  "B.DH.DAOHAM": "y′",
  "B.DH.NGHIEM": "Nghiệm",
  "B.DH.XETDAU": "Xét dấu",
  "B.DH.KETLUAN": "Kết luận",
};

export function tenKyNangNgan(code: string, fallback?: string) {
  if (TEN_NGAN[code]) return TEN_NGAN[code];
  const gon = (fallback || "").split(/[:(]/)[0]?.trim();
  return gon || code;
}

export function tenBuocNgan(ma: string) {
  return BUOC_NGAN[ma] || ma;
}

export function nhanMuc4(code: string | null | undefined) {
  if (!code) return "—";
  return LABEL4[code as Muc4] || code;
}

/** Công thức để KaTeX — không in raw `x^{2}` ra UI. */
export function hamLatex(statementLatex: string | null | undefined) {
  const raw = (statementLatex || "").trim();
  if (!raw) return "";
  if (/^y\s*=/i.test(raw) || raw.startsWith("\\")) return raw;
  return `y = ${raw}`;
}

/** Thân đề, bỏ công thức dính cuối câu. */
export function thanDe(statementText: string) {
  const gon = statementText
    .replace(/\s*y\s*=\s*.+$/i, "")
    .replace(/\s*của hàm số\.?\s*$/i, " của hàm số")
    .replace(/\s+$/g, "")
    .replace(/[.,;:]+$/g, "");
  return gon || "Xét tính đơn điệu của hàm số";
}

export function loiGoiHocSinh(lyDo: string, tenKn: string) {
  if (lyDo.includes("kẹt")) return `Em đang kẹt ở ${tenKn} — giữ cùng mức, chưa nâng.`;
  if (lyDo.includes("nâng một nấc")) return `Đủ ngưỡng ở ${tenKn} — nâng một nấc.`;
  if (lyDo.includes("Cùng mức") || lyDo.includes("dạng cần ôn")) return `Ôn ${tenKn} cùng mức đang yếu.`;
  if (lyDo.includes("Chưa có ước lượng")) return "Chưa có ước lượng thành thạo — bắt đầu bài đã phát hành.";
  return lyDo.replace(/\s*·\s*Bloom.*$/i, "").trim();
}
