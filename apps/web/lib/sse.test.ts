import assert from "node:assert/strict";
import { test } from "node:test";
import { chuTrangThaiGiaSu, docJsonSse, gomSse, vietSse } from "./sse";

test("vietSse / gomSse một event một việc", () => {
  const raw = vietSse("trang_thai", { buoc: "kho" }) + vietSse("xong", { ok: true, tra_loi: "Em xét dấu." });
  const { events, leftover } = gomSse(raw);
  assert.equal(leftover, "");
  assert.equal(events.length, 2);
  assert.equal(events[0].event, "trang_thai");
  assert.deepEqual(JSON.parse(events[0].data), { buoc: "kho" });
  assert.equal(events[1].event, "xong");
  assert.equal(JSON.parse(events[1].data).tra_loi.includes("đáp án"), false);
});

test("gomSse giữ mảnh dở — không bịa câu", () => {
  const { events, leftover } = gomSse("event: trang_thai\ndata: {\"buoc\":\"goi\"}\n\nevent: xong\ndata: {\"ok\"");
  assert.equal(events.length, 1);
  assert.equal(events[0].event, "trang_thai");
  assert.match(leftover, /xong/);
});

test("docJsonSse bỏ event hỏng, không ném", () => {
  assert.equal(docJsonSse("không phải json"), null);
  assert.deepEqual(docJsonSse<{ buoc: string }>('{"buoc":"loc"}'), { buoc: "loc" });
});

test("chữ trạng thái không nói đang stream token", () => {
  assert.equal(chuTrangThaiGiaSu(null), "Đang nghĩ…");
  assert.equal(chuTrangThaiGiaSu("kho"), "Đang mở công thức lớp…");
  assert.equal(chuTrangThaiGiaSu("goi"), "Đang hỏi gia sư…");
  assert.equal(chuTrangThaiGiaSu("loc"), "Đang lọc khỏi đáp án…");
  for (const b of ["kho", "goi", "loc"] as const) {
    assert.equal(chuTrangThaiGiaSu(b).includes("token"), false);
  }
});
