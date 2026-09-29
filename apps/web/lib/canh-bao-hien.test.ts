import { test } from "node:test";
import assert from "node:assert/strict";
import { dungCanhBao, gioCanhBao } from "./canh-bao-hien";
import { laNhacLaiGoiY, tenBuoc } from "./ket-buoc";

test("gioCanhBao: giờ theo múi Việt Nam (UTC+7), dạng HH:MM · dd/MM", () => {
  assert.equal(gioCanhBao(new Date("2026-09-29T08:42:00Z")), "15:42 · 29/09");
  assert.equal(gioCanhBao(new Date("2026-09-29T17:05:00Z")), "00:05 · 30/09");
});

test("dungCanhBao: đủ ai, kỹ năng, bước, bài, lý do; href theo HS + bài", () => {
  const c = dungCanhBao(
    { id: "e1", studentId: "s1", skillCode: "K", maBuoc: "B.DH.DAOHAM", problemId: "p1", reason: "Em nhờ thầy cô ở bước Đạo hàm.", loai: "NHO_GV", createdAt: new Date("2026-09-29T08:00:00Z") },
    new Map([["s1", "Chi"]]),
    new Map([["K", "Điểm tới hạn"]]),
    new Map([["p1", { id: "p1", code: "DH12-TH-02" }]]),
  );
  assert.equal(c.ten, "Chi");
  assert.equal(c.kyNang, "Điểm tới hạn");
  assert.equal(c.buoc, "Đạo hàm");
  assert.equal(c.bai?.code, "DH12-TH-02");
  assert.equal(c.nho, true);
  assert.equal(c.href, "/gv/hoc-sinh/s1?bai=p1");
  const k = dungCanhBao(
    { id: "e2", studentId: "s2", skillCode: "K", maBuoc: null, problemId: null, reason: "Kẹt 3 lượt", loai: "KET", createdAt: new Date() },
    new Map(),
    new Map([["K", "Điểm tới hạn"]]),
    new Map(),
  );
  assert.equal(k.bai, null);
  assert.equal(k.buoc, null);
  assert.equal(k.href, "/gv/hoc-sinh/s2");
});

test("laNhacLaiGoiY chỉ nhận đúng chip «Gợi ý bước này»; tenBuoc đọc mã bước", () => {
  assert.equal(laNhacLaiGoiY("Gợi ý bước này"), true);
  assert.equal(laNhacLaiGoiY("Gợi ý thêm"), false);
  assert.equal(laNhacLaiGoiY("gợi ý cho em"), false);
  assert.equal(tenBuoc("B.DH.XETDAU"), "Bảng xét dấu");
});
