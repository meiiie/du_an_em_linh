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

/** Công thức để KaTeX — không in raw `x^{2}` ra UI. Đã có vế trái thì giữ nguyên. */
export function hamLatex(statementLatex: string | null | undefined) {
  const raw = (statementLatex || "").trim();
  if (!raw) return "";
  if (raw.startsWith("\\") || raw.includes("=")) return raw;
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

/** Tên tài liệu: phần sau dấu hai chấm / gạch. */
export function tenTaiLieuNgan(title: string) {
  const gon = (title || "").replace(/^[^:—\-]+[:—\-]\s*/, "").trim();
  const ten = gon || title;
  return ten.charAt(0).toLocaleUpperCase("vi-VN") + ten.slice(1);
}

/** Một câu trích tài liệu — bỏ câu khung «tự soạn» và ngoặc năm nghiên cứu. */
export function thanTrich(text: string) {
  const gon = (text || "").replace(/\s+/g, " ").trim();
  if (!gon) return "";
  const sach = gon.replace(/\s*\([^)]*\d{4}[^)]*\)/g, "").replace(/\s+/g, " ").trim() || gon;
  const cau =
    sach.split(/(?<=[.!?…;])\s+/).find((c) => !/tự soạn|không chép sách/i.test(c))?.trim() ||
    sach.match(/^.+?[.!?…](?:\s|$)/)?.[0]?.trim() ||
    sach;
  if (cau.length <= 120) return cau;
  return `${cau.slice(0, 117).replace(/\s+\S*$/, "")}…`;
}

/** Tên cổng duyệt — không viết «Tầng» trên mặt. */
export function tenCuaTang(tier: number) {
  if (tier === 1) return "Chấm máy";
  if (tier === 2) return "Tài liệu";
  if (tier === 3) return "Công thức";
  return String(tier);
}

/** Lý do duyệt: bỏ calque «Tầng n» — số tầng đã đứng riêng trên hàng. */
export function gonLyDoDuyet(text: string) {
  const gon = (text || "")
    .replace(/Tầng\s*\d+\s*/gi, "")
    .replace(/lời giải cấu trúc 5 bước để máy tự kiểm/gi, "lời giải đủ 5 bước để máy chấm")
    .replace(/\s+/g, " ")
    .trim();
  if (!gon) return "";
  return gon.charAt(0).toLocaleUpperCase("vi-VN") + gon.slice(1);
}

/** Tiếng Việt không để trần trong math mode — KaTeX nuốt dấu. */
export function chuanHoaLatexCongThuc(raw: string) {
  const s = (raw || "").trim();
  if (!s || /\\text\s*\{/.test(s)) return s;
  return s.replace(
    /[\p{L}']*[À-ỹĐđ][\p{L}']*(?:\s+[\p{L}']*[À-ỹĐđ][\p{L}']*)*/gu,
    (chunk) => `\\text{${chunk}}`,
  );
}

/** Lời trên phiếu — tiếng lớp 12, không mã, không ngưỡng/nấc/phát hành. */
export function loiGoiHocSinh(lyDo: string, tenKn: string) {
  const kn = tenKn.trim();
  if (lyDo.includes("kẹt")) return kn ? `Đang kẹt ở ${kn} — làm lại cùng mức.` : "Đang kẹt — làm lại cùng mức.";
  if (lyDo.includes("nâng một nấc")) return kn ? `Đã vững ${kn} — chuyển mức khó hơn.` : "Đã vững — chuyển mức khó hơn.";
  if (lyDo.includes("Cùng mức") || lyDo.includes("dạng cần ôn")) {
    return kn ? `Ôn ${kn}, cùng mức đang yếu.` : "Ôn lại, cùng mức đang yếu.";
  }
  if (lyDo.includes("Chưa có ước lượng")) return "Chưa làm bài nào — bắt đầu từ bài này.";
  if (kn) return `Ôn ${kn}.`;
  return "Làm bài này.";
}
