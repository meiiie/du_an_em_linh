import AxeBuilder from '@axe-core/playwright';
import { test, expect, flat, noOverflow } from './fixtures';

test('Gia sư: toggle, shortcut, persistence, resize hoặc sheet và focus', async ({ page }) => {
  await page.goto('/hs/luyen/E2E-01');
  await expect(page).toHaveURL('/hs/luyen/E2E-01');
  const mathInput = page.locator('math-field').first();
  await expect(mathInput).toBeVisible();
  await expect(mathInput.getByRole('textbox')).toHaveAccessibleName(await mathInput.getAttribute('aria-label') ?? '');
  const toggle = page.getByTestId('mo-gia-su');
  const panel = page.getByTestId('gia-su-panel');
  await expect(toggle).toBeVisible();
  const before = await page.getByTestId('solve-screen').boundingBox();
  await toggle.click();
  await expect(panel).toBeVisible();
  await expect(panel.getByRole('log')).toContainText('Gia sư chỉ gợi ý cách làm, không đưa đáp án');
  if (page.viewportSize()!.width > 1024) {
    const solve = await page.getByTestId('solve-screen').boundingBox();
    const box = await panel.boundingBox();
    expect(box!.width).toBeCloseTo(400, 0);
    expect(solve!.width).toBeLessThan(before!.width);
    expect(box!.x).toBeGreaterThanOrEqual(solve!.x + solve!.width);
    const handle = panel.getByRole('separator');
    const h = await handle.boundingBox();
    await page.mouse.move(h!.x + h!.width / 2, h!.y + 100);
    await page.mouse.down();
    await page.mouse.move(h!.x + h!.width / 2 - 60, h!.y + 100);
    await page.mouse.up();
    await expect(handle).toHaveAttribute('aria-valuenow', '460');
    await toggle.click();
    await expect(panel).toHaveCount(0);
    await page.keyboard.press('Control+/');
    await expect(panel).toBeVisible();
  } else {
    await expect(panel).toHaveJSProperty('open', true);
    const box = await panel.boundingBox();
    expect(box!.height).toBeCloseTo(page.viewportSize()!.height, 0);
    const fab = await toggle.boundingBox();
    expect(fab!.width).toBeCloseTo(48, 0);
    for (let i = 0; i < 12; i++) {
      await page.keyboard.press('Tab');
      expect(await panel.evaluate(el => el.contains(document.activeElement))).toBe(true);
    }
  }
  await flat(page);
  await noOverflow(page);
  expect((await new AxeBuilder({ page }).exclude('math-field').analyze()).violations.filter(v => v.impact === 'serious' || v.impact === 'critical')).toEqual([]); // math-field: MathLive bên thứ ba tự lồng textbox
  await page.reload();
  await expect(panel).toBeVisible();
  if (page.viewportSize()!.width > 1024) expect((await panel.boundingBox())!.width).toBeCloseTo(460, 0);
  await page.keyboard.press('Escape');
  await expect(panel).toHaveCount(0);
  await page.reload();
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
});

test('Gia sư: KaTeX, Enter và Shift+Enter, chips ánh xạ ba cấp', async ({ page, api }) => {
  await page.route('**/api/hs/gia-su', async route => {
    api.calls.push({method:'POST',path:'/api/hs/gia-su',body:route.request().postDataJSON()});
    await route.fulfill({contentType:'text/event-stream', body:'event: xong\ndata: {"message":"Xét dấu của $f\u0027(x)$ trước."}\n\n'});
  });
  await page.goto('/hs/luyen/E2E-01');
  await page.getByTestId('mo-gia-su').click();
  const panel = page.getByTestId('gia-su-panel');
  for (const cap of [1, 2, 3]) {
    await panel.getByRole('button', {name:'Gợi ý bước tiếp'}).click();
    await expect(panel.locator('article')).toHaveCount(cap * 2);
  }
  const composer = page.getByTestId('gia-su-composer');
  await composer.fill('Em xét $x^2$');
  await composer.press('Shift+Enter');
  await expect(composer).toHaveValue('Em xét $x^2$\n');
  await composer.press('Enter');
  await expect(panel.locator('article')).toHaveCount(8);
  await expect(panel.locator('.katex').first()).toBeVisible();
  expect(api.calls.filter(c=>c.path==='/api/hs/gia-su').map(c=>c.body.cap)).toEqual([1,2,3,3]);
});

test('Kiểm tra: toggle/FAB tạm tắt kể cả khi đã lưu trạng thái mở', async ({ page, api }) => {
  api.mode='KIEM_TRA';
  await page.addInitScript(()=>localStorage.setItem('mathl-gia-su-mo','true'));
  await page.goto('/hs/luyen/E2E-01');
  await expect(page.getByTestId('bang-kiem-tra')).toContainText('Đang kiểm tra — gia sư tạm tắt');
  await expect(page.getByTestId('mo-gia-su')).toBeDisabled();
  await expect(page.getByTestId('mo-gia-su')).toHaveAttribute('title','Đang kiểm tra — gia sư tạm tắt');
  await page.keyboard.press('Control+/');
  await expect(page.getByTestId('gia-su-panel')).toHaveCount(0);
  expect(api.calls.filter(c=>c.path==='/api/hs/gia-su')).toEqual([]);
  expect((await new AxeBuilder({page}).exclude('math-field').analyze()).violations.filter(v=>v.impact==='serious'||v.impact==='critical')).toEqual([]);
});

test('Gia sư: đang chờ, reduced motion và sửa câu hỏi sau lỗi', async ({ page, api }) => {
  await page.emulateMedia({ reducedMotion:'reduce' });
  let release!: () => void;
  const pending = new Promise<void>(resolve => release = resolve);
  const fail = async (route: import('@playwright/test').Route) => {
    api.calls.push({method:'POST',path:'/api/hs/gia-su',body:route.request().postDataJSON()});
    await pending;
    await route.fulfill({status:503,body:''});
  };
  await page.route('**/api/hs/gia-su', fail);
  await page.goto('/hs/luyen/E2E-01');
  await page.getByTestId('mo-gia-su').click();
  const panel = page.getByTestId('gia-su-panel');
  await panel.getByRole('button', {name:'Giải thích công thức'}).click();
  await expect(panel.locator('.typing')).toBeVisible();
  expect(await panel.locator('.typing span').evaluate(el=>getComputedStyle(el).animationName)).toBe('none');
  await expect(panel.getByTestId('gia-su-chip').first()).toBeDisabled();
  release();
  await expect(panel.getByRole('alert')).toContainText('Kiểm tra kết nối');
  await panel.getByRole('button', {name:'Hỏi lại'}).click();
  await expect(page.getByTestId('gia-su-composer')).toHaveValue('Giải thích công thức');
  await expect(page.getByTestId('gia-su-composer')).toBeFocused();
  expect(api.calls.filter(c=>c.path==='/api/hs/gia-su')).toHaveLength(1);
  await page.unroute('**/api/hs/gia-su', fail);
  await panel.getByRole('button', {name:'Gửi câu hỏi'}).click();
  await expect(panel.getByRole('log')).toContainText('Hãy xem điều kiện xác định');
});
