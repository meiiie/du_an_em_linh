import assert from "node:assert/strict";
import { test } from "node:test";
import { hamLatex, loiGoiHocSinh, thanDe, tenKyNangNgan } from "./de-hoc-sinh";

test("bọc latex hàm số", () => {
  assert.equal(hamLatex("x^{2}"), "y = x^{2}");
  assert.equal(hamLatex("y = x^3 - 6x^2"), "y = x^3 - 6x^2");
  assert.equal(hamLatex("f'(x)=x^{2}(x-2)"), "f'(x)=x^{2}(x-2)");
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
  assert.equal(
    loiGoiHocSinh("Em đang kẹt — giữ cùng mức và cùng kỹ năng vừa yếu, chưa nâng nấc.", "xét dấu"),
    "Đang kẹt ở xét dấu — làm lại cùng mức.",
  );
  assert.equal(
    loiGoiHocSinh("Đủ ngưỡng thành thạo nên nâng một nấc (sơ đồ: bài khó hơn một mức).", "điểm tới hạn"),
    "Đã vững điểm tới hạn — chuyển mức khó hơn.",
  );
  assert.equal(
    loiGoiHocSinh("Cùng mức hiện tại, cùng kỹ năng đang yếu — dạng cần ôn.", "cực trị"),
    "Ôn cực trị, cùng mức đang yếu.",
  );
  assert.equal(loiGoiHocSinh("Chưa có ước lượng thành thạo, bắt đầu bài đã phát hành.", ""), "Chưa làm bài nào — bắt đầu từ bài này.");
  assert.doesNotMatch(loiGoiHocSinh("Đủ ngưỡng thành thạo nên nâng một nấc.", "xét dấu"), /ngưỡng|nấc|phát hành|Em /);
});

test("rút tên kỹ năng", () => {
  assert.equal(tenKyNangNgan("T12.DH.03"), "Xét dấu và bảng biến thiên");
  assert.equal(tenKyNangNgan("T99.XX.01", "Một kỹ năng dài: phần chú thích"), "Một kỹ năng dài");
});
