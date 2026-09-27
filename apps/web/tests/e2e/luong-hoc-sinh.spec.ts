import { expect, test } from "@playwright/test";
import { SHOTS } from "./anh";
import { moSoBaiGiao, vaoLop } from "./vao-lop";

test.describe("trang chủ", () => {
  test("công khai, không trắng, vào được đăng nhập", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "Học toán với AI" })).toBeVisible();
    await expect(page.getByTestId("vao-hoc")).toBeVisible();
    await page.screenshot({ path: `${SHOTS}/trang-chu.png`, fullPage: true });
    await page.getByTestId("vao-hoc").click();
    await expect(page.getByTestId("email")).toBeVisible();
    await expect(page.getByRole("button", { name: "Tiếp tục" })).toBeVisible();
  });

  test("sai mật khẩu thì hiện đúng mật khẩu thử", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.getByRole("button", { name: "Học sinh An" }).click();
    await page.getByTestId("password").fill("hocsinh1233");
    await page.getByRole("button", { name: "Vào học" }).click();
    await expect(page).toHaveURL(/loi=1/);
    await expect(page.getByTestId("loi-dang-nhap")).toContainText("hocsinh123");
    await expect(page.getByTestId("loi-dang-nhap")).toContainText("giaovien123");
    await expect(page.getByTestId("loi-dang-nhap")).not.toContainText("nằm dưới");
  });

  test("logo tab và tài sản SEO công khai", async ({ request }) => {
    const paths = [
      "/favicon.ico",
      "/icon.svg",
      "/icon-48.png",
      "/icon-96.png",
      "/icon-192.png",
      "/icon-512.png",
      "/icon-maskable.png",
      "/apple-touch-icon.png",
      "/robots.txt",
      "/sitemap.xml",
      "/manifest.webmanifest",
      "/llms.txt",
      "/.well-known/security.txt",
    ];
    for (const path of paths) {
      const res = await request.get(path);
      expect(res.status(), path).toBe(200);
    }
  });
});

test.describe("học sinh", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("đăng nhập, nộp bước sai, bị chặn xin đáp án, sửa rồi đạt", async ({ page }) => {
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.an@demo.local", "hocsinh123");
    await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
    await expect(page.getByTestId("mo-sidebar")).toBeVisible();
    await page.getByTestId("mo-sidebar").click();
    await expect(page.getByTestId("nav-hs-lo-trinh")).toBeInViewport();
    await page.screenshot({ path: `${SHOTS}/hs-sidebar-390.png` });
    await page.getByTestId("dong-sidebar").click();
    await expect(page.getByTestId("nav-hs-lo-trinh")).not.toBeInViewport();

    await page.goto("/hs/lich");
    await expect(page.getByRole("heading", { name: "Lịch học" })).toBeVisible();
    await expect(page.getByTestId("lich-tuan")).toBeVisible();
    const lichTran = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(lichTran).toBeLessThanOrEqual(8);
    await page.screenshot({ path: `${SHOTS}/hs-lich-390.png` });

    await page.goto("/hs");
    await moSoBaiGiao(page);
    await page.getByTestId("bai-DH12-03-VD-01").click();
    await expect(page.getByTestId("solve-screen")).toBeVisible();
    await expect(page.getByTestId("nop-buoc")).toBeInViewport();
    await expect(page.getByTestId("mo-gia-su")).toBeInViewport();
    const hop = await page.getByTestId("nop-buoc").boundingBox();
    const hoi = await page.getByTestId("mo-gia-su").boundingBox();
    expect(hop && hoi).toBeTruthy();
    if (hop && hoi) {
      const de =
        !(hop.x + hop.width < hoi.x || hoi.x + hoi.width < hop.x || hop.y + hop.height < hoi.y || hoi.y + hoi.height < hop.y);
      expect(de).toBe(false);
    }
    await page.getByTestId("latex-txd").fill("\\mathbb{R}");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).toContainText("hợp lệ");

    await page.getByTestId("latex-dh").fill("3x^{2}-12x");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).toContainText("đạo hàm");
    await expect(page.locator(".cell-bad")).toBeVisible();
    await page.screenshot({ path: `${SHOTS}/hs-lam-bai-390.png`, fullPage: true });

    await page.getByTestId("mo-gia-su").click();
    await expect(page.getByTestId("tutor-composer")).toBeInViewport();
    await expect(page.getByTestId("dong-gia-su")).toBeVisible();
    await expect(page.getByTestId("tutor-log")).toContainText("không đưa đáp án");
    await expect(page.getByTestId("tutor-log")).not.toContainText("Mình là gia sư AI");
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
    await vaoLop(page, "hs.an@demo.local", "hocsinh123");
    await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
    await expect(page.getByTestId("sidebar")).toBeVisible();
    await expect(page.getByTestId("nav-hs-lo-trinh")).toBeVisible();
    await expect(page.getByRole("heading", { name: "Bài tiếp theo" })).toBeVisible();
    await expect(page.getByText("12A1 thử")).toBeVisible();
    await expect(page.getByText("đơn điệu và cực trị")).toBeVisible();
    await expect(page.getByTestId("tab-ky-nang")).toBeVisible();
    await expect(page.getByTestId("tab-bai-giao")).toBeVisible();
    await expect(page.getByTestId("tab-bai-giao")).toHaveText(/Bài tập/);
    await page.screenshot({ path: `${SHOTS}/hs-lo-trinh-1280.png` });
    await page.goto("/hs/lich");
    await expect(page.getByRole("heading", { name: "Lịch học" })).toBeVisible();
    await expect(page.getByText(/Yếu nhất|Chưa làm bài/)).toBeVisible();
  });
});

test.describe("máy tính bảng", () => {
  test.use({ viewport: { width: 768, height: 1024 } });

  test("phiếu và gia sư không tràn ngang", async ({ page }) => {
    await page.goto("/dang-nhap");
    await vaoLop(page, "hs.an@demo.local", "hocsinh123");
    await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
    await expect(page.getByTestId("mo-sidebar")).toBeVisible();
    const homeOverflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(homeOverflow).toBeLessThanOrEqual(8);

    await moSoBaiGiao(page);
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

  test("duyệt bài và mức lớp 4 / 3 mức", async ({ page }) => {
    await page.goto("/dang-nhap");
    await page.screenshot({ path: `${SHOTS}/dang-nhap.png`, fullPage: true });
    await vaoLop(page, "gv@demo.local", "giaovien123");
    await expect(page.getByRole("heading", { name: "Lớp 12A1 thử" })).toBeVisible();
    await expect(page.getByTestId("sidebar")).toBeVisible();
    await expect(page.getByTestId("nav-gv-tong-quan")).toContainText("Lớp");
    await expect(page.getByTestId("nav-gv-duyet")).toContainText("Duyệt");
    await expect(page.getByTestId("nav-gv-ngan-hang")).toContainText("Đề bài");
    await expect(page.getByTestId("nav-gv-sinh-bai")).toContainText("Tạo đề");
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
    await expect(page.getByTestId("do-chinh-xac-tom-tat")).toContainText(/\d+\/\d+/);
    await expect(page.getByTestId("phien-ban")).toContainText("v0.1.0");
    await page.screenshot({ path: `${SHOTS}/gv-cai-dat.png`, fullPage: true });
  });
});
