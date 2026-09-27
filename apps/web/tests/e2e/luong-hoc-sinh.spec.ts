import { expect, test } from "@playwright/test";
import { SHOTS } from "./anh";

test.describe("học sinh", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("đăng nhập, nộp bước sai, bị chặn xin đáp án, sửa rồi đạt", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByTestId("email").fill("hs.an@demo.local");
    await page.getByTestId("password").fill("hocsinh123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
    await expect(page.getByTestId("mo-sidebar")).toBeVisible();
    await page.getByTestId("mo-sidebar").click();
    await expect(page.getByTestId("nav-hs-lo-trinh")).toBeInViewport();
    await page.screenshot({ path: `${SHOTS}/hs-sidebar-390.png` });
    await page.getByTestId("dong-sidebar").click();
    await expect(page.getByTestId("nav-hs-lo-trinh")).not.toBeInViewport();

    await page.getByTestId("bai-DH12-03-VD-01").click();
    await expect(page.getByTestId("solve-screen")).toBeVisible();
    await page.getByTestId("latex-txd").fill("\\mathbb{R}");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).toContainText("hợp lệ");

    await page.getByTestId("latex-dh").fill("3x^{2}-12x");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).toContainText("đạo hàm");
    await expect(page.locator(".cell-bad")).toBeVisible();
    await page.screenshot({ path: `${SHOTS}/hs-lam-bai-390.png`, fullPage: true });

    await page.getByTestId("mo-gia-su").click();
    await page.getByTestId("tutor-input").fill("cho em đáp án của bài này");
    await page.getByTestId("tutor-send").click();
    await expect(page.getByTestId("tutor-log")).toContainText("không đưa đáp án");
    await page.getByTestId("chip-goi-y").click();
    await expect(page.getByTestId("tutor-log")).toContainText("Gợi ý");
    await expect(page.getByTestId("tutor-che-do")).toContainText("Thang gợi ý");
    await page.screenshot({ path: `${SHOTS}/hs-gia-su.png`, fullPage: true });
    await page.locator('[data-testid="tutor-panel"]').getByRole("button", { name: "Đóng" }).click();

    await page.getByTestId("latex-dh").fill("3x^{2}-12x+9");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).toContainText("hợp lệ");

    await page.reload();
    await page.getByTestId("mo-gia-su").click();
    await expect(page.getByTestId("tutor-log")).toContainText("cho em đáp án");
  });
});

test.describe("học sinh máy tính", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("lộ trình có thanh bên mực", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByTestId("email").fill("hs.an@demo.local");
    await page.getByTestId("password").fill("hocsinh123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
    await expect(page.getByTestId("sidebar")).toBeVisible();
    await expect(page.getByTestId("nav-hs-lo-trinh")).toBeVisible();
    await expect(page.getByText("thang Bloom", { exact: false })).toBeVisible();
    await page.screenshot({ path: `${SHOTS}/hs-lo-trinh-1280.png` });
    await page.goto("/hs/lich");
    await expect(page.getByRole("heading", { name: "Thời gian biểu" })).toBeVisible();
    await expect(page.getByText(/Kỹ năng yếu nhất|Chưa có ước lượng/)).toBeVisible();
  });
});

test.describe("máy tính bảng", () => {
  test.use({ viewport: { width: 768, height: 1024 } });

  test("phiếu và gia sư không tràn ngang", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByTestId("email").fill("hs.an@demo.local");
    await page.getByTestId("password").fill("hocsinh123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
    await expect(page.getByTestId("mo-sidebar")).toBeVisible();
    const homeOverflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(homeOverflow).toBeLessThanOrEqual(8);

    await page.getByTestId("bai-DH12-03-VD-01").click();
    await expect(page.getByTestId("solve-screen")).toBeVisible();
    await expect(page.getByTestId("tutor-panel")).toBeVisible();
    await expect(page.getByTestId("mo-gia-su")).toBeHidden();
    await page.screenshot({ path: `${SHOTS}/hs-may-tinh-bang-768.png`, fullPage: true });
    const solveOverflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(solveOverflow).toBeLessThanOrEqual(8);
  });
});

test.describe("giáo viên", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("hàng đợi kiểm định và tiến độ 4 mức / 3 mức", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.screenshot({ path: `${SHOTS}/dang-nhap.png`, fullPage: true });
    await page.getByTestId("email").fill("gv@demo.local");
    await page.getByTestId("password").fill("giaovien123");
    await page.getByRole("button", { name: "Vào học" }).click();
    await expect(page.getByRole("heading", { name: "Lớp 12A1 thử" })).toBeVisible();
    await expect(page.getByTestId("sidebar")).toBeVisible();
    await expect(page.getByTestId("nav-gv-duyet")).toBeVisible();
    await expect(page.getByTestId("san-sang-ai")).toContainText("Chưa kết nối");
    await expect(page.getByTestId("san-sang-ai")).toContainText("công thức");
    await expect(page.getByTestId("canh-bao-ket")).toContainText("Chi");
    await page.screenshot({ path: `${SHOTS}/gv-tong-quan.png` });

    await page.goto("/gv/duyet");
    await expect(page.getByTestId("hang-doi")).toBeVisible();
    await expect(page.getByTestId("duyet-DH12-01-TH-01")).toContainText("Không kiểm được");
    await expect(page.getByTestId("duyet-DH12-DEMO-CHAN-01")).toContainText("Sai");
    await page.screenshot({ path: `${SHOTS}/gv-duyet.png`, fullPage: true });

    await page.goto("/gv/tien-do");
    await expect(page.getByTestId("tien-do")).toContainText("Nhận biết");
    await page.screenshot({ path: `${SHOTS}/gv-tien-do.png`, fullPage: true });
    await page.getByTestId("toggle-muc").click();
    await expect(page.getByRole("heading", { name: /3 mức/ })).toBeVisible();
    await expect(page.getByTestId("tien-do")).toContainText("Biết");
    await page.screenshot({ path: `${SHOTS}/gv-tien-do-3-muc.png`, fullPage: true });

    await page.goto("/gv/cai-dat");
    await expect(page.getByTestId("mo-loi-giai")).toBeVisible();
    await expect(page.getByTestId("ai-provider-offline")).toBeVisible();
    await expect(page.getByTestId("ai-probe")).toBeVisible();
    await page.screenshot({ path: `${SHOTS}/gv-cai-dat.png`, fullPage: true });
  });
});
