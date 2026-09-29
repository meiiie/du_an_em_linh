import { expect, test } from "@playwright/test";
import { SHOTS } from "./anh";
import { moSoBaiGiao, vaoLop } from "./vao-lop";

/**
 * Thang gợi ý mẫu Sư phạm (supham/thang-goi-y-mau): nộp SAI ở một bước có thang RIÊNG theo loại kết quả thì gợi ý lấy từ
 * thang đó; cấp để trống (null) không hiện câu rỗng / "cấp 3/3" mà đề xuất bài dễ hơn hoặc «Gửi thầy cô».
 */
test.describe("thang gợi ý mẫu", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  // Đặt lại bài làm của HS khi có /api/test/reset (APP_ENV=test). CI không bật APP_ENV=test (route trả 404) nên mỗi
  // ca dùng một HS riêng, trên DB vừa seed, chưa nộp sai TXĐ/đạo hàm ở bài này.
  async function datLai(request: import("@playwright/test").APIRequestContext, email: string) {
    const r = await request.post("/api/test/reset", { data: { email } });
    if (r.status() !== 404) expect(r.ok()).toBe(true);
  }

  async function moBai(page: import("@playwright/test").Page, email: string) {
    await datLai(page.request, email);
    await page.goto("/dang-nhap");
    await vaoLop(page, email, "hocsinh123");
    await page.waitForURL(/\/hs/);
    await moSoBaiGiao(page);
    await page.getByTestId("bai-DH12-03-VD-01").click();
    await expect(page.getByTestId("solve-screen")).toBeVisible();
  }

  test("đạo hàm sai → gợi ý cấp 1 của thang riêng DAOHAM (SAI_GIA_TRI hoặc SAI_BIEN_DOI theo bộ chấm), không phải thang chung", async ({ page }) => {
    await moBai(page, "hs.binh@demo.local");
    await page.getByTestId("latex-txd").fill("\\mathbb{R}");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).toContainText("hợp lệ");
    await page.getByTestId("latex-dh").fill("3x^{2}-12x");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).toContainText("đạo hàm");
    await page.getByTestId("mo-gia-su").click();
    await page.getByTestId("chip-goi-y").click();
    // bậc ba: DAOHAM/SAI_GIA_TRI/1 hoặc DAOHAM/SAI_BIEN_DOI/1 (bộ chấm hiện xếp "3x^2-12x" là SAI_BIEN_DOI)
    await expect(page.getByTestId("tutor-log")).toContainText(
      /mỗi hạng tử đã được lấy đạo hàm đúng quy tắc chưa|Dòng biến đổi sau có còn bằng dòng y' ngay trước nó với mọi x không/,
    );
    await page.screenshot({ path: `${SHOTS}/hs-thang-mau-dao-ham.png`, fullPage: true });
  });

  test("TXĐ sai của đa thức → cấp 1, 2 của thang SAI_TXD; cấp 3 để trống → bài dễ hơn hoặc Gửi thầy cô, không câu rỗng", async ({ page }) => {
    await moBai(page, "hs.chi@demo.local");
    await page.getByTestId("latex-txd").fill("(0;+\\infty)");
    await page.getByTestId("nop-buoc").click();
    await expect(page.getByTestId("cham-thong-bao")).not.toContainText("hợp lệ");
    await page.getByTestId("mo-gia-su").click();
    const log = page.getByTestId("tutor-log");
    await page.getByTestId("chip-goi-y").click();
    await expect(log).toContainText("Tập xác định là tập các giá trị x làm cho biểu thức của hàm số có nghĩa");
    await page.getByTestId("chip-goi-y").click();
    await expect(log).toContainText("em đã loại bỏ hoặc giới hạn x vì lý do gì");
    await page.getByTestId("chip-goi-y").click();
    await expect(log).toContainText(/bài dễ hơn «|Gửi thầy cô/);
    await expect(log).not.toContainText(/cấp 3\/3|ly_do_trong|siêu dữ liệu/);
    await page.screenshot({ path: `${SHOTS}/hs-thang-mau-cap-rong.png`, fullPage: true });
  });
});
