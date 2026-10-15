import { test, expect, detail, noOverflow, flat } from './fixtures';
import type { Page } from '@playwright/test';

async function usableShell(page: Page, width: number) {
  const main = page.locator('#noi-dung');
  const bounds = await main.evaluate(el => {
    const rect = el.getBoundingClientRect();
    const css = getComputedStyle(el);
    const content = el.querySelector('router-outlet + *')!.getBoundingClientRect();
    return { left: rect.left + parseFloat(css.paddingLeft), right: rect.right - parseFloat(css.paddingRight), contentLeft: content.left, contentRight: content.right };
  });
  // Desktop: ray 260 px + --space-6 (32 px), không có cột trống.
  // Dung sai một nhịp lưới 8 px vẫn bắt được cột trống khoảng 200 px.
  const padding = width >= 1024 ? 32 : 16;
  const left = (width >= 1024 ? 260 : 0) + padding;
  expect(Math.abs(bounds.left - left), 'Content starts immediately after sidebar and page padding').toBeLessThanOrEqual(8);
  expect(Math.abs(bounds.right - (width - padding))).toBeLessThanOrEqual(8);
  expect(bounds.contentLeft).toBeCloseTo(bounds.left, 0);
  expect(bounds.contentRight).toBeCloseTo(bounds.right, 0);
  await noOverflow(page);
}

for (const width of [390, 1024, 1280, 1440]) {
  test(`Phiếu luyện dùng hết vùng nội dung, gia sư không gây tràn ở ${width}`, async ({ page, api }, testInfo) => {
    test.skip(testInfo.project.name !== '1440', 'Mỗi kích thước chỉ cần chạy một lần');
    await page.setViewportSize({ width, height: 1000 });
    await page.addInitScript(() => localStorage.setItem('mathl-gia-su-rong', '480'));
    api.overrides.set('GET /api/hs/bai/E2E-01', { body: {
      ...detail, khaiBaoKetLuan: ['dong_bien', 'nghich_bien', 'cuc_dai', 'cuc_tieu'],
      baiLam: { trangThai: 'DANG_LAM', cacBuoc: detail.cacBuoc.slice(0, 4).map(s => ({
        maBuoc: s.maBuoc, dong: [], bang: [], ketQua: 'DAT', thongBao: 'Đạt', oSai: [],
      })) },
    } });
    await page.goto('/hs/luyen/E2E-01');
    const sheet = page.getByTestId('solve-screen');
    await expect(sheet).toBeVisible();
    await expect(page.locator('.tieu-de')).toContainText('Kết luận');
    await usableShell(page, width);
    const grid = page.locator('.practice-grid');
    expect((await sheet.boundingBox())!.width).toBeCloseTo((await grid.boundingBox())!.width, 0);
    await page.getByTestId('mo-gia-su').click();
    await expect(page.getByTestId('gia-su-panel')).toBeVisible();
    if (width === 1440) {
      expect((await page.getByTestId('gia-su-panel').boundingBox())!.width).toBeCloseTo(480, 0);
    }
    await noOverflow(page);
    // Khi đặt cạnh gia sư, phiếu vẫn đủ chỗ cho biểu mẫu và rail năm bước.
    expect((await sheet.boundingBox())!.width).toBeGreaterThanOrEqual(Math.min(width - 32, 480));
    await page.getByRole('button', { name: 'Đóng gia sư', exact: true }).click();
    await usableShell(page, width);
    expect((await sheet.boundingBox())!.width).toBeCloseTo((await grid.boundingBox())!.width, 0);
    await flat(page);
  });

  for (const role of ['STUDENT', 'TEACHER'] as const) {
    test(`Khung ${role} không chừa cột trống ở ${width}`, async ({ page, api }, testInfo) => {
      test.skip(testInfo.project.name !== '1440', 'Mỗi kích thước chỉ cần chạy một lần');
      api.role = role;
      await page.setViewportSize({ width, height: 1000 });
      for (const path of role === 'STUDENT' ? ['/hs', '/hs/bai', '/hs/lich', '/hs/kho'] : ['/gv', '/gv/tien-do', '/gv/ngan-hang', '/gv/giao', '/gv/duyet']) {
        await page.goto(path);
        await expect(page.locator('main h1')).toBeVisible();
        await expect(page.locator('main .khung-cho')).toHaveCount(0);
        await usableShell(page, width);
      }
    });
  }
}
