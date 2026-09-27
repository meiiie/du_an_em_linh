import assert from "node:assert/strict";
import { test } from "node:test";
import { chuanHoaLatexGiaSu } from "./loi-gia-su";

test("đổi \\[ \\] và \\( \\) sang $ cho KaTeX", () => {
  const vao = "Nguyên lý: \\((x^n)' = n x^{n-1}\\).\n\\[y = x^n\\]";
  const ra = chuanHoaLatexGiaSu(vao);
  assert.match(ra, /\$\(x\^n\)' = n x\^\{n-1\}\$/);
  assert.match(ra, /\$\$\ny = x\^n\n\$\$/);
  assert.equal(ra.includes("\\["), false);
});

test("bỏ mã bước trên mặt phiếu", () => {
  const s = chuanHoaLatexGiaSu("Bước B.DH.DAOHAM: nhớ $(x^n)' = n x^{n-1}$.");
  assert.equal(s.includes("B.DH.DAOHAM"), false);
  assert.match(s, /\$\(x\^n\)'/);
});

test("chữ thường và «tên công thức» giữ nguyên", () => {
  const s = chuanHoaLatexGiaSu("Em mở «Đạo hàm lũy thừa». Không nêu đáp án.");
  assert.match(s, /«Đạo hàm lũy thừa»/);
  assert.match(s, /Không nêu đáp án/);
  assert.equal(s.includes("$$"), false);
});

test("hàng rào latex thành $$", () => {
  const ra = chuanHoaLatexGiaSu("Nhớ:\n```latex\n(x^n)' = n x^{n-1}\n```");
  assert.match(ra, /\$\$\n\(x\^n\)' = n x\^\{n-1\}\n\$\$/);
  assert.equal(ra.includes("```"), false);
});

test("hàng rào đã có \\[ không bọc $$ lần hai", () => {
  const ra = chuanHoaLatexGiaSu("```tex\n\\[y' = 3x^2\\]\n```");
  assert.match(ra, /\$\$\ny' = 3x\^2\n\$\$/);
  assert.equal((ra.match(/\$\$/g) || []).length, 2);
});

test("bọc align trần, không bọc align đã nằm trong $$", () => {
  const tho = "\\begin{align} y' &= 3x^2 \\end{align}";
  const raTran = chuanHoaLatexGiaSu(tho);
  assert.match(raTran, /\$\$\n\\begin\{align\} y' &= 3x\^2 \\end\{align\}\n\$\$/);

  const raRoi = chuanHoaLatexGiaSu(`$$\n${tho}\n$$`);
  assert.equal((raRoi.match(/\\begin\{align\}/g) || []).length, 1);
  assert.equal((raRoi.match(/\$\$/g) || []).length, 2);
});

test("siết $ có khoảng trắng, không đụng $$", () => {
  const ra = chuanHoaLatexGiaSu("Nhớ $ (x^n)' = n x^{n-1} $ rồi dừng.");
  assert.match(ra, /\$\(x\^n\)' = n x\^\{n-1\}\$/);
  assert.equal(ra.includes("$ ("), false);
});
