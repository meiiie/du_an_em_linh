import { expect, type Page, test } from '@playwright/test';

/** Hai bước như v0 (`apps/web/tests/e2e/vao-lop.ts`): email → Tiếp tục → mật khẩu → Vào học. */
async function vaoLop(page: Page, email: string, matKhau: string): Promise<void> {
  await page.getByTestId('email').fill(email);
  await page.getByRole('button', { name: 'Tiếp tục' }).click();
  await page.getByTestId('password').fill(matKhau);
  await page.getByRole('button', { name: 'Vào học' }).click();
}

/** Ảnh để rà thiết kế (390 / 1280 px), lưu cạnh kết quả của test. */
async function chup(page: Page, ten: string): Promise<void> {
  await page.screenshot({ path: test.info().outputPath(`${ten}-${test.info().project.name}.png`), fullPage: true });
}

test('học sinh: vào /hs «Chào An»; tải lại vẫn trong phiên; đăng xuất thì không vào lại được', async ({ page }) => {
  await page.goto('/dang-nhap');
  await vaoLop(page, 'hs.an@demo.local', 'hocsinh123');
  await expect(page).toHaveURL(/\/hs$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Chào An' })).toBeVisible();
  await expect(page).toHaveTitle('Học · Học toán với AI');
  await chup(page, 'hs');

  // Access token chỉ ở bộ nhớ nên mất khi tải lại; cookie HttpOnly khôi phục phiên qua /api/auth/refresh.
  await page.reload();
  await expect(page.getByRole('heading', { level: 1, name: 'Chào An' })).toBeVisible();

  await page.getByTestId('dang-xuat').click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
  await page.goto('/hs');
  await expect(page).toHaveURL(/\/dang-nhap\?returnUrl=%2Fhs$/);
});

test('giáo viên: vào /gv «Chưa có lớp»; mở /hs bị đưa về /gv', async ({ page }) => {
  await page.goto('/dang-nhap');
  await vaoLop(page, 'gv@demo.local', 'giaovien123');
  await expect(page).toHaveURL(/\/gv$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Chưa có lớp' })).toBeVisible();
  await expect(page).toHaveTitle('Lớp · Học toán với AI');
  await chup(page, 'gv');

  await page.goto('/hs');
  await expect(page).toHaveURL(/\/gv$/);
});

test('mở trang cần đăng nhập → đăng nhập xong quay về đúng trang đó', async ({ page }) => {
  await page.goto('/gv');
  await expect(page).toHaveURL(/\/dang-nhap\?returnUrl=%2Fgv$/);
  await vaoLop(page, 'gv@demo.local', 'giaovien123');
  await expect(page).toHaveURL(/\/gv$/);
});

test('sai mật khẩu → báo lỗi kèm mật khẩu thử như v0, ở lại trang', async ({ page }) => {
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Học sinh An' }).click();
  await page.getByTestId('password').fill('hocsinh1233');
  await page.getByRole('button', { name: 'Vào học' }).click();
  await expect(page.getByTestId('loi-dang-nhap')).toContainText('hocsinh123');
  await expect(page.getByTestId('loi-dang-nhap')).toContainText('giaovien123');
  await expect(page).toHaveURL(/\/dang-nhap$/);
  await chup(page, 'loi');
});

test('sai 5 lần → lần thứ 6 báo tạm khóa như v0 (F-10)', async ({ page }) => {
  // Email riêng cho mỗi lần chạy: khóa theo email + máy kéo dài 15 phút, không được khóa nhầm tài khoản thử của test khác.
  const email = `khoa-${test.info().project.name}-${Date.now()}@demo.local`;
  await page.goto('/dang-nhap');
  await page.getByTestId('email').fill(email);
  await page.getByRole('button', { name: 'Tiếp tục' }).click();
  for (let lan = 1; lan <= 5; lan++) {
    await page.getByTestId('password').fill(`sai-${lan}`);
    await page.getByRole('button', { name: 'Vào học' }).click();
    await expect(page.getByTestId('loi-dang-nhap')).toContainText('Chưa vào được');
  }
  await page.getByTestId('password').fill('sai-6');
  await page.getByRole('button', { name: 'Vào học' }).click();
  await expect(page.getByTestId('khoa-dang-nhap')).toContainText('tạm khóa 15 phút');
});

test('refresh token chỉ nằm trong cookie HttpOnly, JavaScript không đọc được', async ({ page, context }) => {
  await page.goto('/dang-nhap');
  await vaoLop(page, 'hs.an@demo.local', 'hocsinh123');
  await expect(page).toHaveURL(/\/hs$/);

  const cookie = (await context.cookies()).find((c) => c.name === 'hta_refresh');
  expect(cookie).toMatchObject({ httpOnly: true, secure: true, sameSite: 'Strict', path: '/api/auth' });
  expect(await page.evaluate(() => document.cookie)).not.toContain('hta_refresh');
  expect(await page.evaluate(() => JSON.stringify({ ...localStorage, ...sessionStorage }))).not.toMatch(/eyJ/);
});
