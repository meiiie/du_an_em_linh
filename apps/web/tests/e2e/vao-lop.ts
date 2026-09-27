import type { Page } from "@playwright/test";

/** Hai bước như cổng Neko Đoàn: email → Tiếp tục → mật khẩu → Vào học. */
export async function vaoLop(page: Page, email: string, password: string) {
  await page.getByTestId("email").fill(email);
  await page.getByRole("button", { name: "Tiếp tục" }).click();
  await page.getByTestId("password").fill(password);
  await page.getByRole("button", { name: "Vào học" }).click();
}
