import assert from "node:assert/strict";
import { test } from "node:test";
import { locTinNhan, redact } from "./llm";
import { HE_THONG_GIA_SU } from "./tutor";

test("redact tên demo và email, giữ chữ giáo viên", () => {
  assert.equal(redact("An hỏi cô"), "[ten] hỏi cô");
  assert.equal(redact("không phải giáo viên"), "không phải giáo viên");
  assert.equal(redact("giao vien"), "giao vien");
  assert.equal(redact("hs.an@demo.local gọi 0912345678"), "[email] gọi [sdt]");
  assert.equal(redact("dán sk-abcdefghijklmnop vào đây"), "dán [khoa] vào đây");
  assert.doesNotMatch(redact("dán zai-abcdefghijklmnop vào đây"), /zai-/);
});

test("locTinNhan không sửa prompt hệ thống", () => {
  assert.match(HE_THONG_GIA_SU, /không phải giáo viên/);
  const cleaned = locTinNhan([
    { role: "system", content: HE_THONG_GIA_SU },
    { role: "user", content: "An hỏi giáo viên: em@demo.local" },
  ]);
  assert.equal(cleaned[0].content, HE_THONG_GIA_SU);
  assert.match(cleaned[0].content, /không phải giáo viên/);
  assert.equal(cleaned[0].content.includes("[ten]"), false);
  assert.equal(cleaned[1].content, "[ten] hỏi giáo viên: [email]");
});
