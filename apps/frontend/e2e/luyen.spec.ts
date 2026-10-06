import { expect, type Page, test } from '@playwright/test';

/**
 * Màn Luyện bài trên cả hệ thật (nginx → core → dịch vụ toán), dữ liệu giao bằng SQL của profile dev. Chỉ nộp bài làm
 * sai, nên chạy lại bao nhiêu lần cũng ra cùng kết quả: bài vẫn «Đang làm», không nộp bài.
 */
async function vaoLop(page: Page): Promise<void> {
  await page.goto('/dang-nhap');
  await page.getByTestId('email').fill('hs.an@demo.local');
  await page.getByRole('button', { name: 'Tiếp tục' }).click();
  await page.getByTestId('password').fill('hocsinh123');
  await page.getByRole('button', { name: 'Vào học' }).click();
  await expect(page).toHaveURL(/\/hs$/);
}

const tranNgang = (page: Page) => page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);

test('bước sai: máy chấm thật trả câu, câu cuộn vào màn hình, bước được đánh dấu sai', async ({ page }) => {
  await vaoLop(page);
  await page.goto('/hs/luyen/GEN-huu_ti-5');
  await expect(page.getByRole('heading', { level: 1, name: 'Luyện bài' })).toBeAttached();
  await expect(page).toHaveTitle('Luyện · MathL+');
  await page.getByTestId('step-B.DH.TXD').click();

  await page.getByTestId('latex-txd').fill(String.raw`\mathbb{R}`);
  const [res] = await Promise.all([
    page.waitForResponse((r) => r.url().endsWith('/api/hs/bai/GEN-huu_ti-5/buoc')),
    page.getByTestId('nop-buoc').click(),
  ]);
  expect(res.status()).toBe(200);
  expect((await res.json()).ketQua).toBe('SAI');

  const cau = page.getByTestId('cham-thong-bao');
  await expect(cau).toHaveText('Bước tập xác định, dòng 1 cần xem lại.');
  await expect(cau).toBeInViewport();
  await expect(page.getByTestId('step-B.DH.TXD')).toHaveAttribute('data-tt', 'sai');
  await expect(page.getByTestId('nop-buoc')).toHaveText('Kiểm tra lại');
  expect(await tranNgang(page)).toBe(0);
});

test('bài khung ngắn: bước đề cho sẵn bị khóa, bảng xét dấu cuộn trong khung, trang không tràn ngang', async ({ page }) => {
  await vaoLop(page);
  await page.goto('/hs/luyen/DH12-03-NB-01');
  await expect(page.getByTestId('de-cho-san')).toContainText('Em bắt đầu từ bước xét dấu.');
  await expect(page.getByTestId('step-B.DH.TXD')).toBeDisabled();

  for (const moc of ['-1', '3']) {
    await page.getByTestId('moc-nhap').fill(moc);
    await page.getByTestId('moc-nhap').press('Enter');
  }
  await expect(page.getByTestId('x-1')).toContainText('3');
  await page.getByTestId('dau-4-+').click();
  await expect(page.getByTestId('dau-4-+')).toHaveAttribute('aria-pressed', 'true');
  expect(await tranNgang(page)).toBe(0);
});
