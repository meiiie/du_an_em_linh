export interface PhanVanBanToan {
  text: string;
  math: boolean;
  block?: boolean;
}

/** Bỏ phần chữ trong lệnh LaTeX khi nhận diện ngôn ngữ ngoài công thức. */
function boChu(text: string): string {
  let out = '';
  for (let i = 0; i < text.length; i++) {
    if (text.startsWith('\\text{', i)) {
      i += 6;
      let depth = 1;
      for (; i < text.length && depth; i++) {
        if (text[i] === '{') depth++;
        if (text[i] === '}') depth--;
      }
      i--;
      out += ' ';
    } else out += text[i];
  }
  return out;
}

/** Nhận cả công thức từ trường latex của seed và toán xen trong văn xuôi. */
export function tachVanBanToan(text: string): PhanVanBanToan[] {
  const parts: PhanVanBanToan[] = [];
  const coToan = (s: string) => /\\[a-zA-Z]+|[_^=]/.test(s);
  const coChu = (s: string) => /[À-ỹĐđ]/.test(s) || /\b(?:[A-Z][a-z]{2,}|[a-z]{3,})\b/.test(s.replace(/\\(?:begin|end)\{[^{}]*\}/g, '').replace(/\\[a-zA-Z]+/g, ''));
  const coHtml = (s: string) => /<\/?[a-z][^>]*>/i.test(s);
  const themChu = (chu: string) => {
    if (!chu) return;
    if (!coChu(boChu(chu)) && !coHtml(chu) && coToan(chu)) {
      parts.push({ text: chu, math: true });
      return;
    }
    let start = 0;
    // Khoảng trắng trong ngoặc thuộc lệnh (\text, \frac...), không kết thúc công thức.
    for (let i = 0; i < chu.length;) {
      if (/\s/.test(chu[i])) { i++; continue; }
      const from = i;
      let depth = 0;
      do {
        if (chu[i] === '{') depth++;
        if (chu[i] === '}') depth--;
        i++;
      } while (i < chu.length && (depth > 0 || !/\s/.test(chu[i])));
      const token = chu.slice(from, i);
      if (!coToan(token) || coChu(boChu(token)) || coHtml(token)) continue;
      const formula = token.replace(/[.,:;!?]+$/, '');
      parts.push({ text: chu.slice(start, from), math: false }, { text: formula, math: true });
      start = from + formula.length;
    }
    parts.push({ text: chu.slice(start), math: false });
  };
  const re = /(?<!\\)(?:\$\$([\s\S]+?)\$\$|\$([^$\n]+)(?<!\\)\$|\\\(([\s\S]+?)\\\)|\\\[([\s\S]+?)\\\])/g;
  let start = 0;
  for (const m of text.matchAll(re)) {
    themChu(text.slice(start, m.index));
    parts.push({ text: m[1] ?? m[2] ?? m[3] ?? m[4], math: true, block: !!(m[1] ?? m[4]) });
    start = m.index + m[0].length;
  }
  themChu(text.slice(start));
  return parts;
}
