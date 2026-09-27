import assert from "node:assert/strict";
import { test } from "node:test";
import { chamDoChinhXac, chamLoiGiaSu, loRoDapAn } from "./do-chinh-xac";

test("lộ rõ bắt khoảng và cực trị, không bắt gợi ý", () => {
  assert.equal(loRoDapAn("Đồng biến trên (1;3)."), true);
  assert.equal(loRoDapAn("Cực đại tại x = -1"), true);
  assert.equal(loRoDapAn("Em tính y' từng hạng tử."), false);
  assert.equal(loRoDapAn("Đồng biến khi đạo hàm dương."), false);
});

test("chamLoiGiaSu: rỗng, mã bước, tiếng Việt, [n]", () => {
  assert.equal(chamLoiGiaSu("").rong, true);
  assert.equal(chamLoiGiaSu("B.DH.TXD rồi").maBuoc, true);
  assert.equal(chamLoiGiaSu("Em nhớ [1] đạo hàm lũy thừa.").coTrich, true);
  assert.equal(chamLoiGiaSu("Em nhớ đạo hàm.").tiengViet, true);
});

test("thẻ đo độ chính xác đủ điểm", () => {
  const kq = chamDoChinhXac();
  const thieu = kq.tieuChi.filter((t) => !t.dat).map((t) => t.id);
  assert.deepEqual(thieu, [], `thiếu: ${thieu.join(", ")}`);
  assert.equal(kq.diem, kq.toiDa);
  assert.equal(kq.sota.diem, kq.sota.toiDa);
});
