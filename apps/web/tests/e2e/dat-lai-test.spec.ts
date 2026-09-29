import { expect, test } from "@playwright/test";
import { vaoLop } from "./vao-lop";

/** Nút "Đặt lại dữ liệu" chỉ có ở chế độ test (APP_ENV=test, không Render); ngoài chế độ test không render và route 404. */
test.describe("nút đặt lại dữ liệu thử", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("chỉ hiện khi /api/test/reset bật; HS đặt lại chính mình", async ({ page }) => {
    const r = await page.request.post("/api/test/reset", { data: { email: "khong-ton-tai@demo.local" } });
    const bat = r.status() !== 404;
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.an@demo.local", "hocsinh123");
    await page.waitForURL(/\/hs/);
    if (!bat) {
      await expect(page.getByTestId("dat-lai-test")).toHaveCount(0);
      return;
    }
    await expect(page.getByTestId("khoi-dat-lai-test")).toContainText("Chỉ môi trường test");
    page.once("dialog", (d) => d.accept());
    const [res] = await Promise.all([
      page.waitForResponse((x) => x.url().endsWith("/api/test/reset") && x.request().method() === "POST"),
      page.getByTestId("dat-lai-test").click(),
    ]);
    expect(res.status()).toBe(200);
    expect(JSON.parse(res.request().postData() || "{}")).toEqual({ email: "hs.an@demo.local" });
  });
});
