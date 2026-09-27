import { expect, test } from "@playwright/test";
import { mkdir } from "fs/promises";

const SHOTS = "/opt/cursor/artifacts/screenshots";

test.beforeAll(async () => {
  await mkdir(SHOTS, { recursive: true });
});

test.describe("harness gia sư", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("composer Enter, nhà local lỗi không chuyển đám mây", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByTestId("email").fill("hs.an@demo.local");
    await page.getByTestId("password").fill("hocsinh123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await page.getByTestId("bai-DH12-03-VD-01").click();
    await expect(page.getByTestId("tutor-panel")).toBeVisible();
    await expect(page.getByTestId("tutor-composer")).toBeVisible();
    await expect(page.getByTestId("tutor-provider")).toHaveValue("offline");

    await page.getByTestId("tutor-input").fill("Gợi ý bước này");
    await page.getByTestId("tutor-input").press("Enter");
    await expect(page.getByTestId("tutor-log")).toContainText("Gợi ý");
    await expect(page.getByTestId("tutor-che-do")).toContainText("Thang gợi ý");

    await page.getByTestId("tutor-provider").selectOption("ollama");
    await page.getByTestId("chip-goi-y").click();
    await expect(page.getByTestId("tutor-log")).toContainText(/không chuyển|Không nối|Không gửi lại/);
    await expect(page.getByTestId("tutor-che-do")).toContainText("không chuyển");
    await expect(page.getByTestId("tutor-log")).not.toContainText("BÍ MẬT");
    await page.screenshot({ path: `${SHOTS}/hs-gia-su-harness.png`, fullPage: true });
  });
});

test.describe("cài đặt nhà AI", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("giáo viên thử offline và thấy Ollama từ chối không fallback", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByTestId("email").fill("gv@demo.local");
    await page.getByTestId("password").fill("giaovien123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await page.goto("/gv/cai-dat");
    await page.getByTestId("ai-probe-provider").selectOption("offline");
    await page.getByTestId("ai-probe-chay").click();
    await expect(page.getByTestId("ai-probe-ket")).toContainText("không gọi mạng");
    await page.getByTestId("ai-probe-provider").selectOption("ollama");
    await page.getByTestId("ai-probe-chay").click();
    await expect(page.getByTestId("ai-probe-ket")).toContainText(/Không nối|không chuyển|loopback|hết giờ|trả/);
    await page.screenshot({ path: `${SHOTS}/gv-cai-dat-ai.png`, fullPage: true });
  });
});
