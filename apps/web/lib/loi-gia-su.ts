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
  let s = locMatHienThi(raw).replace(/\r\n/g, "\n").trim();

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
