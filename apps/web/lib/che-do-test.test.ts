import assert from "node:assert/strict";
import { test } from "node:test";
import { cheDoTest } from "./che-do-test";

test("chế độ test chỉ bật khi APP_ENV/NODE_ENV=test và không chạy trên Render", () => {
  assert.equal(cheDoTest({ APP_ENV: "test" }), true);
  assert.equal(cheDoTest({ NODE_ENV: "test" }), true);
  assert.equal(cheDoTest({}), false);
  assert.equal(cheDoTest({ APP_ENV: "production" }), false);
  assert.equal(cheDoTest({ APP_ENV: "test", RENDER: "true" }), false);
  assert.equal(cheDoTest({ APP_ENV: "test", RENDER_SERVICE_ID: "srv-x" }), false);
  assert.equal(cheDoTest({ APP_ENV: "test", RENDER_EXTERNAL_URL: "https://x.onrender.com" }), false);
});
