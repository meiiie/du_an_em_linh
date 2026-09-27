import assert from "node:assert/strict";
import { test } from "node:test";
import { hamLatex, loiGoiHocSinh, thanDe, tenKyNangNgan } from "./de-hoc-sinh";

test("bọc latex hàm số", () => {
  assert.equal(hamLatex("x^{2}"), "y = x^{2}");
  assert.equal(hamLatex("y = x^3 - 6x^2"), "y = x^3 - 6x^2");
  assert.equal(hamLatex(""), "");
});

test("tách thân đề khỏi công thức", () => {
  assert.equal(
    thanDe("Tìm các khoảng đồng biến, nghịch biến của hàm số y = x³ − 6x² + 9x + 2."),
    "Tìm các khoảng đồng biến, nghịch biến của hàm số",
  );
  assert.equal(
    thanDe("Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số y = x^{2}."),
    "Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số",
  );
});

test("đổi lời gợi sang tiếng học sinh", () => {
  assert.match(loiGoiHocSinh("Em đang kẹt — giữ cùng mức và cùng kỹ năng vừa yếu, chưa nâng nấc.", "xét dấu"), /xét dấu/);
  assert.equal(
    loiGoiHocSinh("Đủ ngưỡng thành thạo nên nâng một nấc (sơ đồ: bài khó hơn một mức).", "điểm tới hạn"),
    "Đủ ngưỡng ở điểm tới hạn — nâng một nấc.",
  );
});

test("rút tên kỹ năng", () => {
  assert.equal(tenKyNangNgan("T12.DH.03"), "Xét dấu và bảng biến thiên");
  assert.equal(tenKyNangNgan("T99.XX.01", "Một kỹ năng dài: phần chú thích"), "Một kỹ năng dài");
});
