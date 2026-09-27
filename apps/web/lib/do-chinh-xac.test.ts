import assert from "node:assert/strict";
import { test } from "node:test";
import { chamDoChinhXac, chamLoiGiaSu, CONG_BO, khopCongBo, loRoDapAn } from "./do-chinh-xac";

test("lộ rõ bắt khoảng và cực trị, không bắt gợi ý", () => {
  assert.equal(loRoDapAn("Đồng biến trên (1;3)."), true);
  assert.equal(loRoDapAn("Cực đại tại x = -1"), true);
  assert.equal(loRoDapAn("Em tính y' từng hạng tử."), false);
  assert.equal(loRoDapAn("Đồng biến khi đạo hàm dương."), false);
  assert.equal(loRoDapAn("Cực tiểu tại x = 3"), true);
});

test("chamLoiGiaSu: rỗng, mã bước, tiếng Việt, [n]", () => {
  assert.equal(chamLoiGiaSu("").rong, true);
  assert.equal(chamLoiGiaSu("B.DH.TXD rồi").maBuoc, true);
  assert.equal(chamLoiGiaSu("Em nhớ [1] đạo hàm lũy thừa.").coTrich, true);
  assert.equal(chamLoiGiaSu("Em nhớ đạo hàm.").tiengViet, true);
  assert.equal(chamLoiGiaSu("Cô là AI gia sư.").xungCo, true);
  assert.equal(chamLoiGiaSu("Em nhớ đạo hàm lũy thừa.").xungCo, false);
});

test("thẻ đo độ chính xác đủ điểm", () => {
  const kq = chamDoChinhXac();
  const thieu = kq.tieuChi.filter((t) => !t.dat).map((t) => t.id);
  assert.deepEqual(thieu, [], `thiếu: ${thieu.join(", ")}`);
  assert.equal(kq.diem, kq.toiDa);
  assert.equal(kq.sota.diem, kq.sota.toiDa);
});

test("số công bố khớp file kiemdinh mẫu", () => {
  assert.equal(
    khopCongBo({
      tang1: {
        so_ca: CONG_BO.tang1.soCa,
        so_ca_loi: CONG_BO.tang1.loi,
        loi_bat_duoc: CONG_BO.tang1.bat,
        loi_bo_lot_DAT: 0,
        dung_bao_nham: 0,
      },
      buoc5: { so_ca: 16, so_ca_loi: 12, loi_bat_duoc: 12 },
      locM3: { recall_lo_ro: ["38/38", 1], chan_nham: ["0/28", 0] },
    }),
    true,
  );
  assert.equal(khopCongBo({ tang1: { so_ca: 99 } }), false);
});
