import { expect, type Page, test } from '@playwright/test';

/** Hai bước như v0: email → Tiếp tục → mật khẩu → Vào học (chép từ dang-nhap.spec.ts). */
async function vaoLop(page: Page, email: string, matKhau: string): Promise<void> {
  await page.goto('/dang-nhap');
  await page.getByTestId('email').fill(email);
  await page.getByRole('button', { name: 'Tiếp tục' }).click();
  await page.getByTestId('password').fill(matKhau);
  await page.getByRole('button', { name: 'Vào học' }).click();
}

async function chup(page: Page, ten: string): Promise<void> {
  await page.screenshot({ path: test.info().outputPath(`${ten}-${test.info().project.name}.png`), fullPage: true });
}

/** Trang không tràn ngang ở cỡ màn đang chạy (390 / 1280 px). */
async function khongTranNgang(page: Page): Promise<void> {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
}

/** Bấm một mục của ray; dưới `lg` thì mở ngăn kéo trước, và ngăn kéo tự đóng khi đã chuyển trang. */
async function chonMuc(page: Page, testId: string): Promise<void> {
  const hep = (page.viewportSize()?.width ?? 1280) < 1024;
  if (hep) await page.getByTestId('mo-sidebar').click();
  await page.getByTestId(testId).click();
  if (hep) await expect(page.getByTestId('sidebar')).not.toHaveClass(/\bmo\b/);
}

// Bảng phụ lục của specs/001-lat-cat-doc/spec.md: mục ray → route → heading.
const HOC_SINH: readonly (readonly [string, RegExp, string])[] = [
  ['nav-hs-bai', /\/hs\/bai$/, 'Đề bài'],
  ['nav-hs-lich', /\/hs\/lich$/, 'Lịch học'],
  ['nav-hs-kho', /\/hs\/kho$/, 'Công thức và tài liệu'],
  ['nav-hs-lo-trinh', /\/hs$/, 'Chào An'],
];
const GIAO_VIEN: readonly (readonly [string, RegExp, string])[] = [
  ['nav-gv-duyet', /\/gv\/duyet$/, 'Duyệt'],
  ['nav-gv-ngan-hang', /\/gv\/ngan-hang$/, 'Đề bài'],
  ['nav-gv-tai-lieu', /\/gv\/tai-lieu$/, 'Tài liệu'],
  ['nav-gv-cong-thuc', /\/gv\/cong-thuc$/, 'Công thức'],
  ['nav-gv-tien-do', /\/gv\/tien-do$/, 'Mức lớp'],
  ['nav-gv-ket-noi-ai', /\/gv\/ket-noi-ai$/, 'Gia sư'],
  ['nav-gv-cai-dat', /\/gv\/cai-dat$/, 'Cài đặt lớp'],
  ['nav-gv-tong-quan', /\/gv$/, 'Chưa có lớp'],
];

test('học sinh: ray dẫn tới đủ bốn màn, không tràn ngang', async ({ page }) => {
  await vaoLop(page, 'hs.an@demo.local', 'hocsinh123');
  await expect(page).toHaveURL(/\/hs$/);
  await khongTranNgang(page);
  for (const [testId, url, h1] of HOC_SINH) {
    await chonMuc(page, testId);
    await expect(page).toHaveURL(url);
    await expect(page.getByRole('heading', { level: 1, name: h1 })).toBeVisible();
    await khongTranNgang(page);
  }
  await chup(page, 'khung-hs');
});

test('giáo viên: ray dẫn tới đủ tám màn, không tràn ngang', async ({ page }) => {
  await vaoLop(page, 'gv@demo.local', 'giaovien123');
  await expect(page).toHaveURL(/\/gv$/);
  for (const [testId, url, h1] of GIAO_VIEN) {
    await chonMuc(page, testId);
    await expect(page).toHaveURL(url);
    await expect(page.getByRole('heading', { level: 1, name: h1 })).toBeVisible();
    await khongTranNgang(page);
  }
  await chup(page, 'khung-gv');
});

test('điện thoại: ngăn kéo mở bằng mo-sidebar, đóng bằng dong-sidebar và Esc', async ({ page }) => {
  test.skip((page.viewportSize()?.width ?? 1280) >= 1024, 'màn rộng không có ngăn kéo');
  await vaoLop(page, 'hs.an@demo.local', 'hocsinh123');
  const ray = page.getByTestId('sidebar');
  await page.getByTestId('mo-sidebar').click();
  await expect(ray).toHaveClass(/\bmo\b/);
  await expect(page.getByTestId('dong-sidebar')).toBeFocused();
  await chup(page, 'ngan-keo');
  await page.getByTestId('dong-sidebar').click();
  await expect(ray).not.toHaveClass(/\bmo\b/);
  await expect(page.getByTestId('mo-sidebar')).toBeFocused();
  await page.getByTestId('mo-sidebar').click();
  await page.keyboard.press('Escape');
  await expect(ray).not.toHaveClass(/\bmo\b/);
});

test.describe('giảm chuyển động', () => {
  test.use({ reducedMotion: 'reduce' });

  test('điện thoại: mở ngăn kéo thì tiêu điểm vào dong-sidebar, Esc thì về mo-sidebar', async ({ page }) => {
    test.skip((page.viewportSize()?.width ?? 1280) >= 1024, 'màn rộng không có ngăn kéo');
    await vaoLop(page, 'hs.an@demo.local', 'hocsinh123');
    await page.getByTestId('mo-sidebar').click();
    await expect(page.getByTestId('dong-sidebar')).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(page.getByTestId('sidebar')).not.toHaveClass(/\bmo\b/);
    await expect(page.getByTestId('mo-sidebar')).toBeFocused();
  });
});
