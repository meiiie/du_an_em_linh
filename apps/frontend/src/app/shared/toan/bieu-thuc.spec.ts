import { latexHienThi } from './bieu-thuc';

describe('Biểu thức hiển thị', () => {
  it.each([
    ['x**(-12)', 'x^{-12}'],
    ['(x + 1)**(2*x)', '(x + 1)^{2 x}'],
    ['sqrt(sqrt(x))', String.raw`\sqrt{\sqrt{x}}`],
    ['2*3', String.raw`2 \cdot 3`],
    ['x**2**3', 'x^{2^{3}}'],
  ])('%s → %s', (source, expected) => expect(latexHienThi(source)).toBe(expected));
  it('giữ LaTeX và biểu thức thiếu ngoặc để báo lỗi an toàn', () => {
    expect(latexHienThi(String.raw`\sin(x) + x^{12}`)).toBe(String.raw`\sin(x) + x^{12}`);
    expect(latexHienThi('sqrt(x')).toBe('sqrt(x');
    expect(latexHienThi('x**2) + 1')).toBe('x**2) + 1');
  });
});
