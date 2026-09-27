import assert from "node:assert/strict";
import { test } from "node:test";
import { chamSotaGiaSu } from "./sota-gia-su";
import { chonTrichHien, soTuChuThe, tenMoKho } from "./trich-dan-ui";

test("so trích chỉ nhận 1–2 chữ số", () => {
  assert.equal(soTuChuThe("1"), 1);
  assert.equal(soTuChuThe(["3"]), 3);
  assert.equal(soTuChuThe("Đạo hàm"), null);
  assert.equal(soTuChuThe("1."), null);
});

test("một đoạn đang xem; nút mở đúng loại", () => {
  const items = [
    { loai: "cong_thuc" as const, id: "a", ten: "A", trich: "một", so: 1 },
    { loai: "tai_lieu" as const, id: "b", ten: "B", trich: "hai", so: 2, dung: true },
  ];
  assert.equal(chonTrichHien(items, 1)?.id, "a");
  assert.equal(chonTrichHien(items)?.id, "b");
  assert.equal(tenMoKho("tai_lieu"), "Mở tài liệu");
});

test("benchmark gia sư đạt đủ tiêu chí SOTA 2026-09-27", () => {
  const kq = chamSotaGiaSu();
  const thieu = kq.tieuChi.filter((t) => !t.dat).map((t) => t.id);
  assert.deepEqual(thieu, [], `thiếu: ${thieu.join(", ")}`);
  assert.equal(kq.diem, kq.toiDa);
  assert.ok(kq.toiDa >= 12);
});
