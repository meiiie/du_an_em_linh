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
    await expect(page.getByTestId("tutor-trich-dan")).toContainText(/Đạo hàm|Cực trị|Đơn điệu|Ghi chú/);

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
    await expect(page.getByRole("heading", { name: "Lớp 12A1 thử" })).toBeVisible();
    await page.goto("/gv/cai-dat");
    await expect(page.getByRole("heading", { name: "Cài đặt lớp" })).toBeVisible();
    await expect(page.getByTestId("ai-probe")).toBeVisible();
    await page.getByTestId("ai-probe-provider").selectOption("offline");
    await page.getByTestId("ai-probe-chay").click();
    await expect(page.getByTestId("ai-probe-ket")).toContainText("không gọi mạng");
    await page.getByTestId("ai-probe-provider").selectOption("ollama");
    await page.getByTestId("ai-probe-chay").click();
    await expect(page.getByTestId("ai-probe-ket")).toContainText(/Không nối|không chuyển|loopback|hết giờ|trả/);
    await page.screenshot({ path: `${SHOTS}/gv-cai-dat-ai.png`, fullPage: true });
  });

  test("kết nối ChatGPT: hai bước khóa chính thức, không OAuth lậu", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByTestId("email").fill("gv@demo.local");
    await page.getByTestId("password").fill("giaovien123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await expect(page.getByRole("heading", { name: "Lớp 12A1 thử" })).toBeVisible();
    await page.goto("/gv/ket-noi-ai");
    await expect(page.getByRole("heading", { name: "Kết nối ChatGPT" })).toBeVisible();
    await expect(page.getByTestId("ket-noi-chatgpt")).toBeVisible();
    await expect(page.getByTestId("mo-trang-khoa-openai")).toHaveAttribute("href", /platform\.openai\.com\/api-keys/);
    await expect(page.getByTestId("mo-trang-khoa-openai")).toContainText("Mở ChatGPT");
    await expect(page.getByTestId("oauth-chua-dk")).toContainText("OPENAI_OAUTH_CLIENT_ID");
    await expect(page.getByTestId("ket-noi-trang-thai")).toContainText("Chưa kết nối");
    await expect(page.getByTestId("kho-theo-buoc")).toContainText("Đạo hàm");
    await page.screenshot({ path: `${SHOTS}/gv-ket-noi-chatgpt.png`, fullPage: true });
    await page.goto("/gv/tai-lieu");
    await expect(page.getByTestId("gia-su-doc-kho")).toContainText("Đạo hàm");
    await page.screenshot({ path: `${SHOTS}/gv-tai-lieu-kho.png`, fullPage: true });
  });
});

test.describe("kho kiến thức", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("học sinh xem cùng kho gia sư đọc", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByTestId("email").fill("hs.an@demo.local");
    await page.getByTestId("password").fill("hocsinh123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
    await page.getByTestId("nav-hs-kho").click();
    await expect(page.getByRole("heading", { name: "Kiến thức gia sư được đọc" })).toBeVisible();
    await expect(page.getByTestId("kho-theo-buoc")).toContainText("Kết luận");
    await expect(page.getByTestId("kho-cong-thuc")).toContainText("Đạo hàm");
    await expect(page.getByTestId("kho-tai-lieu")).toContainText("đơn điệu");
    await page.screenshot({ path: `${SHOTS}/hs-kho-kien-thuc.png`, fullPage: true });
  });
});
