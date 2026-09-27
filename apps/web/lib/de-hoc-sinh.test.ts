import assert from "node:assert/strict";
import { test } from "node:test";
import { chuanHoaLatexCongThuc, gonLyDoDuyet, hamLatex, loiGoiHocSinh, thanDe, thanTrich, tenCuaTang, tenKyNangNgan, tenTaiLieuNgan } from "./de-hoc-sinh";

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

test("trích tài liệu một câu, bỏ ngoặc năm nghiên cứu", () => {
  assert.equal(
    thanTrich("Mỗi buổi tự viết lại quy tắc. Không mở đáp án trước."),
    "Mỗi buổi tự viết lại quy tắc.",
  );
  assert.match(
    thanTrich("Ghi chú tự soạn cho lớp 12A1 thử, không chép sách. Với hàm số xác định trên một khoảng thì xét dấu y′."),
    /xét dấu y′/,
  );
  assert.doesNotMatch(
    thanTrich("Gia sư chỉ gợi ý quy trình, không đưa kết quả (VanLehn 2006; Aleven). Khi kẹt thì gửi thầy cô."),
    /VanLehn|Aleven|2006/,
  );
});

test("rút tên tài liệu sau dấu hai chấm", () => {
  assert.equal(tenTaiLieuNgan("Ghi chú tự soạn: đơn điệu và cực trị"), "Đơn điệu và cực trị");
  assert.equal(tenTaiLieuNgan("Tham khảo phương pháp — ôn đơn điệu thế nào"), "Ôn đơn điệu thế nào");
  assert.equal(tenTaiLieuNgan("Công thức đạo hàm"), "Công thức đạo hàm");
});

test("lý do duyệt bỏ chữ tầng", () => {
  assert.equal(gonLyDoDuyet("Tầng 1 chưa kết luận được nên chưa đối chiếu quy tắc."), "Chưa kết luận được nên chưa đối chiếu quy tắc.");
  assert.equal(gonLyDoDuyet("Chưa có lời giải cấu trúc 5 bước để máy tự kiểm."), "Chưa có lời giải đủ 5 bước để máy chấm.");
  assert.equal(tenCuaTang(1), "Chấm máy");
  assert.equal(tenCuaTang(2), "Tài liệu");
  assert.equal(tenCuaTang(3), "Công thức");
  assert.doesNotMatch(tenCuaTang(1), /Tầng|cổng/i);
});

test("công thức: tiếng Việt vào \\text", () => {
  assert.equal(chuanHoaLatexCongThuc("y' \\ge 0 \\Rightarrow đồng biến"), "y' \\ge 0 \\Rightarrow \\text{đồng biến}");
  assert.equal(chuanHoaLatexCongThuc("+ \\to - : cực đại"), "+ \\to - : \\text{cực đại}");
  assert.equal(chuanHoaLatexCongThuc("(x^n)' = n x^{n-1}"), "(x^n)' = n x^{n-1}");
  assert.equal(chuanHoaLatexCongThuc("y' = 0 \\text{ hoặc } y' \\text{ không xác định}"), "y' = 0 \\text{ hoặc } y' \\text{ không xác định}");
});

test("rút tên kỹ năng", () => {
  assert.equal(tenKyNangNgan("T12.DH.03"), "Xét dấu và bảng biến thiên");
  assert.equal(tenKyNangNgan("T99.XX.01", "Một kỹ năng dài: phần chú thích"), "Một kỹ năng dài");
});
