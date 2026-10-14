import { test, expect, detail, problem, noOverflow, flat } from './fixtures';

for (const family of ['B.GT', 'B.XS']) {
  test(`${family}: metadata theo đề, tập/dãy và nộp đủ năm bước`, async ({ page, api }) => {
    if (page.viewportSize()!.width > 390) await page.setViewportSize({ width: 1280, height: 1000 });
    const codes = family === 'B.GT' ? ['MIEN', 'BIENDOI', 'TRUNGGIAN', 'TINH', 'KETLUAN'] : ['DUKIEN', 'THANHPHAN1', 'THANHPHAN2', 'KETHOP', 'KETLUAN'];
    const kinds = ['DAY_GIA_TRI', 'BIEU_THUC', 'TAP_HUU_HAN', 'BIEU_THUC', 'BIEU_THUC'];
    const steps = codes.map((code, i) => ({ maBuoc: `${family}.${code}`, ten: `Bước theo đề ${i + 1}`, viec: `Nhập giá trị ở bước ${i + 1}.`, kieu: kinds[i] }));
    api.overrides.set(`GET /api/hs/bai/${problem.maBai}`, { body: { ...detail, cacBuoc: steps, khaiBaoKetLuan: [] } });
    await page.goto(`/hs/luyen/${problem.maBai}`);
    await expect(page.getByTestId('solve-screen')).toBeVisible();
    const values = ['0; 2', 'x^2', '{1; 2}', '1/2', '3/4'];
    for (let i = 0; i < steps.length; i++) {
      await expect(page.locator('.tieu-de')).toContainText(steps[i].ten);
      await page.locator(`input[data-testid="latex-${steps[i].maBuoc}"]`).fill(values[i]);
      await noOverflow(page);
      await flat(page);
      await page.getByTestId('nop-buoc').click();
      if (i < 4) await expect(page.locator('.tieu-de')).toContainText(steps[i + 1].ten);
    }
    await expect(page.getByTestId('nop-bai')).toBeVisible();
    const calls = api.calls.filter(c => c.path.endsWith('/buoc'));
    expect(calls.map(c => c.body)).toEqual(steps.map((s, i) => ({ maBuoc: s.maBuoc, dong: [{ dong: 0, latex: values[i] }] })));
    await page.getByTestId('nop-bai').click();
    await expect(page.getByTestId('da-nop')).toContainText('Đã nộp. Bài đạt.');
  });
}
