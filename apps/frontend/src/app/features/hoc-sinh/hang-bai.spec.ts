import { deBang, hamLatex, thanDe } from './hang-bai';

describe('công thức và câu đề', () => {
  it('hamLatex thêm «y =» khi đề chỉ ghi biểu thức, như v0', () => {
    expect(hamLatex('x^{3} - 6 x^{2} + 1')).toBe('y = x^{3} - 6 x^{2} + 1');
    expect(hamLatex('y = x^3 - 3x^2 + 2')).toBe('y = x^3 - 3x^2 + 2');
    expect(hamLatex('  ')).toBe('');
  });

  it('deBang xuống dòng mỗi vế ngăn bởi ,\\quad để đề dài không tràn ngang', () => {
    expect(deBang("y = x^3 - 3x^2 - 9x + 2,\\quad y' = 3(x+1)(x-3)")).toBe(
      "\\begin{gathered}y = x^3 - 3x^2 - 9x + 2 \\\\ y' = 3(x+1)(x-3)\\end{gathered}",
    );
    expect(deBang('\\frac{x}{x + 3}')).toBe('y = \\frac{x}{x + 3}');
  });

  it('thanDe bỏ công thức cuối câu (đã có trên bảng), giữ nguyên đề có công thức giữa câu', () => {
    expect(thanDe('Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số y = x^{3} - 6 x^{2} + 1.')).toBe(
      'Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số',
    );
    const giuaCau = "Cho hàm số y = x³ − 3x² − 9x + 2 có đạo hàm y' = 3(x + 1)(x − 3). Dựa vào dấu của y′, hãy tìm các khoảng đồng biến.";
    expect(thanDe(giuaCau)).toBe(giuaCau);
  });
});
