/** Chỉ đổi cú pháp hiển thị; không tính toán hoặc thực thi chuỗi từ máy chủ. */
export function latexHienThi(value: string): string {
  // Câu kết luận tiếng Việt cần chế độ chữ để MathLive/KaTeX giữ khoảng trắng.
  if (/[À-ỹĐđ]/.test(value) && !/[\\$^_={}]/.test(value)) {
    return `\\text{${value}}`;
  }
  // LaTeX có sẵn (kể cả lệnh có dấu *) phải giữ nguyên.
  if (value.includes('\\') || !/(\*|\b(?:sqrt|sin|cos|tan|exp|log)\s*\()/.test(value)) return value;
  if (value.length > 4000) return value;
  let i = 0;
  let depth = 0;
  const space = () => { while (/\s/.test(value[i] ?? '') && i < value.length) i++; };
  const atom = (): string => {
    space();
    let out = '';
    if (value[i] === '(') {
      i++;
      out = '(' + group() + ')';
    } else {
      const token = value.slice(i).match(/^(?:\d+(?:\.\d+)?|[a-zA-Z_]\w*)/);
      if (!token) return value[i++] ?? '';
      i += token[0].length;
      out = token[0];
      const endToken = i;
      space();
      if (/^(sqrt|sin|cos|tan|exp|log)$/.test(out) && value[i] === '(') {
        i++;
        const content = group();
        out = out === 'sqrt' ? `\\sqrt{${content}}` : `\\${out}(${content})`;
      } else i = endToken;
    }
    const endAtom = i;
    space();
    if (value.slice(i, i + 2) === '**') {
      i += 2;
      space();
      const sign = /^[+-]$/.test(value[i] ?? '') ? value[i++] : '';
      const power = atom();
      out += `^{${sign}${power.startsWith('(') && power.endsWith(')') ? power.slice(1, -1) : power}}`;
    } else i = endAtom;
    return out;
  };
  const group = (): string => {
    if (++depth > 50) throw new Error('Biểu thức quá sâu');
    let out = '';
    while (i < value.length && value[i] !== ')') {
      if (/\s/.test(value[i])) { space(); out += ' '; }
      else if (value[i] === '*') {
        i++; space();
        out += /\d\s*$/.test(out) && /\d/.test(value[i] ?? '') ? ' \\cdot ' : ' ';
      } else if (/[\w(]/.test(value[i])) out += atom();
      else out += value[i++];
    }
    if (value[i] === ')') { if (depth === 1) throw new Error('Thừa ngoặc đóng'); i++; }
    else if (depth > 1) throw new Error('Thiếu ngoặc đóng');
    depth--;
    return out.trim();
  };
  try { return group().replace(/ +/g, ' '); } catch { return value; }
}
