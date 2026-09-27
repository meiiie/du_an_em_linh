import assert from "node:assert/strict";
import { test } from "node:test";
import { cacGioSlot, ghepNhacVaoSlot, ngayTrongTuanHienTai, thuBuoiTiep, thuHomNay } from "./lich";

const slots = [
  { thu: "Thứ Hai", gio: "19:00", viec: "Ôn" },
  { thu: "Thứ Tư", gio: "19:00", viec: "Làm bài" },
  { thu: "Thứ Sáu", gio: "19:30", viec: "Sửa" },
  { thu: "Chủ Nhật", gio: "09:00", viec: "Ôn lại" },
];

test("thứ hôm nay theo giờ Việt Nam", () => {
  assert.equal(thuHomNay(new Date("2026-09-27T12:00:00Z")), "Chủ Nhật");
  assert.equal(thuHomNay(new Date("2026-09-27T17:00:00Z")), "Thứ Hai");
});

test("tuần 27/9/2026 là 21–27", () => {
  const tuan = ngayTrongTuanHienTai(new Date("2026-09-27T12:00:00Z"));
  assert.deepEqual(
    tuan.map((d) => `${d.ma} ${d.ngay}`),
    ["T2 21", "T3 22", "T4 23", "T5 24", "T6 25", "T7 26", "CN 27"],
  );
});

test("giờ trên bảng tăng dần", () => {
  assert.deepEqual(cacGioSlot(slots), ["09:00", "19:00", "19:30"]);
});

test("buổi tiếp là buổi hôm nay hoặc buổi kế", () => {
  assert.equal(thuBuoiTiep(slots, "Chủ Nhật"), "Chủ Nhật");
  assert.equal(thuBuoiTiep(slots, "Thứ Ba"), "Thứ Tư");
  assert.equal(thuBuoiTiep(slots, "Thứ Bảy"), "Chủ Nhật");
});

test("nhắc Chủ Nhật dính buổi Chủ Nhật; tối nay chủ nhật thì để riêng", () => {
  const { slots: ghep, roi } = ghepNhacVaoSlot(
    slots,
    [
      { id: "a", title: "Buổi tối nay", body: "Mở một bài.", sendAt: "19:00" },
      { id: "b", title: "Nhắc cuối tuần", body: "Xem lại bước bị tô.", sendAt: "Chủ Nhật 09:00" },
    ],
    "Chủ Nhật",
  );
  assert.deepEqual(ghep.find((s) => s.thu === "Chủ Nhật")?.nhac, ["Xem lại bước bị tô."]);
  assert.equal(roi.length, 1);
  assert.equal(roi[0].id, "a");
});
