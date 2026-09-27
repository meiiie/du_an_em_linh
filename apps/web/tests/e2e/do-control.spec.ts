import { expect, test } from "@playwright/test";
import { vaoLop } from "./vao-lop";

async function box(page: import("@playwright/test").Page, sel: string) {
  const loc = page.locator(sel).first();
  await expect(loc).toBeVisible();
  return loc.evaluate((el) => {
    const s = getComputedStyle(el);
    const r = el.getBoundingClientRect();
    return {
      h: Math.round(r.height),
      w: Math.round(r.width),
      px: parseFloat(s.paddingLeft),
      radius: parseFloat(s.borderRadius),
    };
  });
}

test("giải phẫu nút 40/44 và ô 40", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.goto("/dang-nhap");
  const email = await box(page, '[data-testid="email"]');
  const tiep = await box(page, 'button:has-text("Tiếp tục")');
  expect(email.h).toBeGreaterThanOrEqual(40);
  expect(tiep.h).toBeGreaterThanOrEqual(40);
  expect(tiep.px).toBeGreaterThanOrEqual(16);
  expect(tiep.radius).toBe(6);

  await page.getByTestId("email").fill("hs.an@demo.local");
  await page.getByRole("button", { name: "Tiếp tục" }).click();
  const submit = await box(page, 'button:has-text("Vào học")');
  expect(submit.h).toBeGreaterThanOrEqual(40);
  expect(submit.px).toBeGreaterThanOrEqual(16);
  expect(submit.radius).toBe(6);
  await page.getByTestId("password").fill("hocsinh123");
  await page.getByRole("button", { name: "Vào học" }).click();
  await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();

  const nav = await box(page, '[data-testid="nav-hs-lo-trinh"]');
  const cta = await box(page, 'a:has-text("Làm bước tiếp")');
  expect(nav.h).toBeGreaterThanOrEqual(44);
  expect(cta.h).toBeGreaterThanOrEqual(40);
  expect(cta.px).toBeGreaterThanOrEqual(16);

  await page.getByTestId("bai-DH12-03-VD-01").click();
  const step = await box(page, '[data-testid="step-B.DH.TXD"]');
  const nop = await box(page, '[data-testid="nop-buoc"]');
  const send = await box(page, '[data-testid="tutor-send"]');
  expect(step.h).toBeGreaterThanOrEqual(44);
  expect(nop.h).toBeGreaterThanOrEqual(40);
  expect(send.h).toBeGreaterThanOrEqual(44);
  expect(send.w).toBeGreaterThanOrEqual(44);
});

test("nút icon điện thoại 44", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/dang-nhap");
  await vaoLop(page, "hs.an@demo.local", "hocsinh123");
  await expect(page.getByRole("heading", { name: "Chào An" })).toBeVisible();
  const burger = await box(page, '[data-testid="mo-sidebar"]');
  expect(burger.h).toBe(44);
  expect(burger.w).toBe(44);
  await page.getByTestId("mo-sidebar").click();
  const close = await box(page, '[data-testid="dong-sidebar"]');
  const nav = await box(page, '[data-testid="nav-hs-lo-trinh"]');
  expect(close.h).toBe(44);
  expect(close.w).toBe(44);
  expect(nav.h).toBeGreaterThanOrEqual(44);
});
