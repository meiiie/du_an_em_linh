import assert from "node:assert/strict";
import { test } from "node:test";
import { boLoiTuGioiThieu, chuanHoaLatexGiaSu, locBanGiaSu } from "./loi-gia-su";

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

test("gỡ chào và tự giới thiệu, giữ việc", () => {
  assert.equal(boLoiTuGioiThieu("Chào em, mình là AI gợi ý. Em tính $y'$."), "Em tính $y'$.");
  assert.match(boLoiTuGioiThieu("Chào em, mình nhắc nguyên lý đạo hàm."), /nhắc nguyên lý/);
  assert.equal(boLoiTuGioiThieu(""), "");
  assert.match(boLoiTuGioiThieu("Mình là gia sư AI."), /mắc chỗ nào/);
  assert.equal(locBanGiaSu("Em nhớ sk-abcdefghijklmnop rồi.").includes("sk-"), false);
});

test("§(23) gia sư không viết dấu hợp giữa hai khoảng", async () => {
  const { boDauHop, locBanGiaSu } = await import("./loi-gia-su");
  assert.equal(boDauHop("đồng biến trên (-∞; -1) ∪ (3; +∞)."), "đồng biến trên (-∞; -1) và (3; +∞).");
  assert.equal(boDauHop("$(-\\infty;1) \\cup (1;+\\infty)$"), "$(-\\infty;1) và (1;+\\infty)$");
  assert.equal(boDauHop("(-oo; 0) U [2; 5]"), "(-oo; 0) và [2; 5]");
  assert.equal(boDauHop("Um, em thử lại"), "Um, em thử lại");
  assert.ok(!/∪/.test(locBanGiaSu("Nghịch biến trên (-1; 1) ∪ (1; 3)")));
});
