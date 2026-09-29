import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import test from "node:test";
import { daMaHoa, giaiMa, maHoa } from "./ma-hoa";

const env = { APP_ENC_KEY: randomBytes(32).toString("base64") };

test("F-10: khoá API lưu dạng AES-256-GCM, giải mã lại đúng, không chứa bản rõ", () => {
  const khoa = "khoa-lop-thu-0123456789abcdef";
  const luu = maHoa(khoa, env);
  assert.ok(daMaHoa(luu));
  assert.ok(!luu.includes(khoa));
  assert.notEqual(maHoa(khoa, env), luu, "IV ngẫu nhiên mỗi lần");
  assert.equal(giaiMa(luu, env), khoa);
});

test("F-10: sửa bản mã hoặc sai khoá máy chủ thì không giải mã (null)", () => {
  const luu = maHoa("sk-test", env);
  const hong = luu.slice(0, -4) + (luu.endsWith("AAAA") ? "BBBB" : "AAAA");
  assert.equal(giaiMa(hong, env), null);
  const envKhac = { APP_ENC_KEY: randomBytes(32).toString("hex") };
  assert.equal(giaiMa(luu, envKhac), null);
});

test("F-10: bản host thiếu APP_ENC_KEY thì từ chối lưu; khoá sai độ dài bị từ chối", () => {
  assert.throws(() => maHoa("sk-x", { RENDER: "true" }), /APP_ENC_KEY/);
  assert.throws(() => maHoa("sk-x", { APP_ENC_KEY: "ngan" }), /32 byte/);
});

test("F-10: giá trị cũ chưa mã hoá vẫn đọc được để mã hoá lại", () => {
  assert.equal(giaiMa("sk-cu", env), "sk-cu");
  assert.equal(giaiMa(null, env), null);
});
