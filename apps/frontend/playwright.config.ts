import { defineConfig, devices } from '@playwright/test';

/**
 * e2e v2 chạy trên cả hệ đã dựng: `docker compose -f compose.v2.yaml up --build --wait` ở gốc repo, rồi
 * `pnpm --filter frontend e2e`. Trình duyệt → nginx (:4200) → services/core → PostgreSQL, tài khoản thử của profile dev.
 * Mỗi test chạy ở hai cỡ màn: 390 px (điện thoại) và 1280 px.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['github']] : 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://127.0.0.1:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: '390', use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 } } },
    { name: '1280', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } },
  ],
});
