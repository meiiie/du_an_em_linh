import assert from "node:assert/strict";
import test from "node:test";
import { GIOI_HAN, daChamGioiHan, dungMotLuot, ipTuHeader, khoBoNho, khoaDangNhap } from "./gioi-han";

test("F-10: đăng nhập sai 5 lần / 15 phút theo email+IP thì khoá; hết cửa sổ thì mở", async () => {
  let t = 1_000_000;
  const kho = khoBoNho(() => t);
  const khoa = khoaDangNhap(" HS.An@demo.local ", "1.2.3.4");
  assert.equal(khoa, "dang_nhap:hs.an@demo.local|1.2.3.4");
  for (let i = 0; i < 4; i++) await kho.ghi(khoa);
  assert.equal(await daChamGioiHan(kho, khoa, GIOI_HAN.dangNhap), false);
  await kho.ghi(khoa);
  assert.equal(await daChamGioiHan(kho, khoa, GIOI_HAN.dangNhap), true);
  assert.equal(await daChamGioiHan(kho, khoaDangNhap("hs.an@demo.local", "5.6.7.8"), GIOI_HAN.dangNhap), false, "IP khác không bị khoá");
  t += 15 * 60 * 1000 + 1;
  assert.equal(await daChamGioiHan(kho, khoa, GIOI_HAN.dangNhap), false);
});

test("F-10: hạn mức gia sư 30 câu / 10 phút, câu thứ 31 bị chặn và không ghi", async () => {
  const kho = khoBoNho(() => 5);
  for (let i = 0; i < 30; i++) assert.equal(await dungMotLuot(kho, "gia_su:u", GIOI_HAN.giaSuNgan, GIOI_HAN.giaSuNgay), true);
  assert.equal(await dungMotLuot(kho, "gia_su:u", GIOI_HAN.giaSuNgan, GIOI_HAN.giaSuNgay), false);
  assert.equal(await kho.dem("gia_su:u", 600), 30);
  assert.equal(GIOI_HAN.doDaiCauHoi, 1000);
});

test("F-10: nộp bước 40 lần / phút, lần 41 bị chặn, phút sau nộp lại được", async () => {
  let t = 0;
  const kho = khoBoNho(() => t);
  for (let i = 0; i < 40; i++) assert.equal(await dungMotLuot(kho, "nop_buoc:u", GIOI_HAN.nopBuoc), true);
  assert.equal(await dungMotLuot(kho, "nop_buoc:u", GIOI_HAN.nopBuoc), false);
  t += 60_001;
  assert.equal(await dungMotLuot(kho, "nop_buoc:u", GIOI_HAN.nopBuoc), true);
});

test("ipTuHeader lấy IP đầu của x-forwarded-for", () => {
  const h = new Map([["x-forwarded-for", "9.9.9.9, 10.0.0.1"]]);
  assert.equal(ipTuHeader({ get: (k) => h.get(k) ?? null }), "9.9.9.9");
  assert.equal(ipTuHeader({ get: () => null }), "-");
});
