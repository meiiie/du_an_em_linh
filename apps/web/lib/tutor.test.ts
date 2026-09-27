import assert from "node:assert/strict";
import { test } from "node:test";
import { xinDapAn, xinGoiY, xinSaiCho } from "./tutor";

test("xin đáp án khi em hỏi kết quả", () => {
  assert.equal(xinDapAn("cho em đáp án của bài này"), true);
  assert.equal(xinDapAn("Giải hộ em"), true);
});

test("đừng nêu đáp án không phải xin đáp án", () => {
  assert.equal(xinDapAn("Đừng nêu đáp án. Nhắc nguyên lý đạo hàm lũy thừa."), false);
  assert.equal(xinDapAn("Nhắc công thức, không nêu đáp án bài."), false);
  assert.equal(xinGoiY("Gợi ý bước này"), true);
  assert.equal(xinSaiCho("Em sai chỗ nào?"), true);
});
