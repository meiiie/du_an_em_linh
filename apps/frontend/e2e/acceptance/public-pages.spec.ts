import { test, expect } from '@playwright/test';
import { flat, noOverflow } from './fixtures';

test.beforeEach(async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'light', reducedMotion: 'reduce' });
  await page.route('**/api/auth/refresh', route => route.fulfill({ status: 401, json: {} }));
});

test('trang chủ: phạm vi THPT, giáo viên duyệt nội dung và bảng màu xanh–trắng', async ({ page }) => {
  await page.goto('/');
  await page.locator('#xem').scrollIntoViewIfNeeded();
  await expect(page.locator('app-canh-lam-viec')).toBeVisible();
  await expect(page.locator('.huy-hieu')).toContainText('Lớp 10–12');
  await expect(page.locator('.ghi-chu')).toHaveText('Bản thí điểm: Bài luyện tập theo lớp và chủ đề · Trước khi dùng');
  const roles = page.locator('app-chon-vai');
  await expect(roles).toContainText('AI soạn nháp bài tập và lời giải từ tài liệu của thầy cô');
  await expect(roles).toContainText('Thầy cô duyệt trước khi giao cho học sinh');
  await expect(roles).toContainText('Theo dõi mức hiểu của từng học sinh');
  await expect(roles).toContainText('Xem mức dùng AI của lớp');
  await expect(roles).toContainText('Xem lời giải đầy đủ sau khi nộp');
  await expect(roles).toContainText('Nhắc lịch học qua thông báo trình duyệt');
  await expect(page.locator('footer')).toContainText('© MathL+');
  await expect(page.locator('footer a[href*="github.com"]')).toHaveCount(0);
  await expect(page.locator('main')).not.toContainText('Một chủ đề');
  await expect(page.locator('app-canh-lam-viec .bang-the')).toHaveCSS('background-color', 'rgb(255, 255, 255)');
  await expect(page.locator('app-canh-lam-viec .gia-su-the')).toHaveCSS('background-color', 'rgb(239, 246, 255)');
  await expect(page.locator('app-minh-hoa-dao-ham .duong')).toHaveCSS('stroke', 'rgb(29, 78, 216)');
  await expect(page.locator('app-dai-bang')).toHaveCount(0);
  await expect(page.getByTestId('vao-hoc')).toHaveCSS('background-color', 'rgb(29, 78, 216)');
  for (const selector of ['meta[name="description"]', 'meta[property="og:description"]', 'meta[name="twitter:description"]']) {
    await expect(page.locator(selector)).toHaveAttribute('content', /Toán THPT lớp 10–12.*74 bài luyện tập.*13 chủ đề/);
  }
  const graph = JSON.parse((await page.locator('script[type="application/ld+json"]').textContent())!);
  for (const node of graph['@graph'].filter((node: { description?: string }) => node.description)) {
    expect(node.description).toContain('Toán THPT lớp 10–12');
    expect(node.description).not.toContain('Bản thử: Toán 12');
  }
  await expect(page.locator('meta[name="theme-color"][media*="light"]')).toHaveAttribute('content', '#FFFFFF');
  await noOverflow(page);
  await flat(page);
  await page.screenshot({ path: test.info().outputPath('trang-chu.png'), fullPage: true });
});

test('đăng nhập: hướng dẫn tài khoản trường và minh họa toán trên nền sáng', async ({ page }) => {
  await page.goto('/dang-nhap');
  await expect(page.getByRole('heading', { name: 'Đăng nhập', exact: true })).toBeVisible();
  await expect(page.locator('.khoi')).toContainText('Quên mật khẩu? Liên hệ giáo viên hoặc quản trị trường.');
  await expect(page.locator('.chan')).toContainText('Toán THPT · Lớp 10–12');
  await expect(page.locator('a[href*="dang-ky"], a[href*="quen-mat-khau"]')).toHaveCount(0);
  if (page.viewportSize()!.width >= 1024) {
    const panel = page.locator('app-minh-hoa-dang-nhap');
    await expect(panel).toBeVisible();
    await expect(panel).toHaveCSS('background-color', 'rgb(239, 246, 255)');
    await expect(panel).toHaveCSS('background-image', 'none');
    await expect(panel.locator('.cau')).toHaveCSS('color', 'rgb(15, 23, 42)');
  }
  await page.getByTestId('email').fill('hs.an@demo.local');
  await page.getByRole('button', { name: 'Tiếp tục', exact: true }).click();
  await expect(page.getByTestId('password')).toBeVisible();
  await expect(page.locator('.khoi')).toContainText('Quên mật khẩu? Liên hệ giáo viên hoặc quản trị trường.');
  await noOverflow(page);
  await flat(page);
  await page.screenshot({ path: test.info().outputPath('dang-nhap.png'), fullPage: true });
});
