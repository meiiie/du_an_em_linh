import { defineConfig, devices } from '@playwright/test';

// Default: built Angular SPA + page.route API mocks; no backend.
// E2E_BACKEND=1 retains the original compose integration suite.
const backend = process.env['E2E_BACKEND'] === '1';
const external = process.env['E2E_BASE_URL'];
const port = process.env['E2E_PORT'] ?? '4300';
export default defineConfig({
  testDir: './e2e',
  testMatch: backend ? ['dang-nhap.spec.ts', 'khung.spec.ts', 'luyen.spec.ts'] : 'acceptance/**/*.spec.ts',
  timeout: 30_000,
  workers: 2,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['github']] : 'list',
  outputDir: process.env['E2E_OUTPUT_DIR'] ?? 'test-results',
  webServer: backend || external ? undefined : {
    command: 'node e2e/serve-built.mjs',
    timeout: 180_000,
    url: `http://127.0.0.1:${port}/dang-nhap`,
    reuseExistingServer: false,
  },
  use: {
    baseURL: external ?? `http://127.0.0.1:${backend ? '4200' : port}`,
    serviceWorkers: 'block',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    launchOptions: process.env['E2E_CHROMIUM_EXECUTABLE'] ? { executablePath: process.env['E2E_CHROMIUM_EXECUTABLE'] } : undefined,
  },
  projects: [
    { name: '1440', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } } },
    { name: '390', use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 } } },
  ],
});
