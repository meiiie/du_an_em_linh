import { anKhoa } from "./an-khoa";

/** Chào / «Mình là AI…» đầu lượt — giữ phần việc phía sau. */
const MO_DAU_CHAO = /^(?:chào\s+em|xin\s+chào(?:\s+em)?)\s*[,.!?:…]\s*/i;
const CAU_GIOI_THIEU =
  /^(?:mình|tôi)\s+là\s+(?:một\s+)?(?:ai\b|gia\s+sư|trợ\s+lý|mô\s+hình)[^.!?\n]*[.!?…]?\s*/i;

export function boLoiTuGioiThieu(raw: string): string {
  let s = (raw || "").replace(/^\uFEFF/, "").trim();
  if (!s) return "";
  for (let i = 0; i < 4; i++) {
    const truoc = s;
    s = s.replace(MO_DAU_CHAO, "").trim();
    s = s.replace(CAU_GIOI_THIEU, "").trim();
    if (s === truoc) break;
  }
  return s || "Em nói mình đang mắc chỗ nào ở bước này.";
}

/** Lưu và hiện: gỡ chào, che khóa, rồi mới chuẩn hóa công thức. */
export function locBanGiaSu(raw: string): string {
  return anKhoa(boLoiTuGioiThieu(raw));
}

/** Bỏ mã bước khỏi mặt phiếu — An không cần B.DH.DAOHAM. */
export function locMatHienThi(raw: string): string {
  return (raw || "")
    .replace(/\bB\.DH\.[A-Z]+\b/g, "")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/[ \t]{2,}/g, " ");
}

/** Chuẩn hóa công thức để remark-math + KaTeX đọc — không đụng chữ thường. */

const HANG_RAO =
  /```(?:latex|tex|math|katex)[ \t]*\n([\s\S]*?)```/gi;

const MOI_TRUONG =
  /\\begin\{(align\*?|aligned|alignedat|equation\*?|gather\*?|gathered|multline\*?|split|cases|pmatrix|bmatrix|vmatrix|Vmatrix|matrix|smallmatrix|array)\}[\s\S]*?\\end\{\1\}/g;

function namTrongTien(s: string, offset: number) {
  return (s.slice(0, offset).split("$$").length - 1) % 2 === 1;
}

export function chuanHoaLatexGiaSu(raw: string): string {
  let s = locMatHienThi(locBanGiaSu(raw)).replace(/\r\n/g, "\n").trim();

  s = s.replace(/\\\[\s*([\s\S]+?)\s*\\\]/g, (_m, t: string) => `\n\n$$\n${t.trim()}\n$$\n\n`);
  s = s.replace(/\\\(\s*([\s\S]+?)\s*\\\)/g, (_m, t: string) => `$${t.trim()}$`);

  s = s.replace(HANG_RAO, (_m, t: string) => {
    const inner = String(t).trim();
    if (!inner) return "";
    if (inner.includes("$$")) return `\n\n${inner}\n\n`;
    return `\n\n$$\n${inner}\n$$\n\n`;
  });

  s = s.replace(MOI_TRUONG, (m, _env: string, offset: number) => {
    if (namTrongTien(s, offset)) return m;
    return `\n\n$$\n${m}\n$$\n\n`;
  });

  s = s.replace(/\$\$([\s\S]+?)\$\$/g, (_m, t: string) => `\n\n$$\n${t.trim()}\n$$\n\n`);
  s = s.replace(/(?<!\$)\$[ \t]+([^$\n]+?)[ \t]+\$(?!\$)/g, (_m, t: string) => `$${t.trim()}$`);
  return s.replace(/\n{3,}/g, "\n\n").trim();
}
