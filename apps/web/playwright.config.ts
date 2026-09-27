import path from "path";
import { defineConfig } from "@playwright/test";

const root = path.resolve(__dirname, "../..");

export default defineConfig({
  testDir: "./tests/e2e",
  timeout: 90_000,
  expect: { timeout: 20_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  forbidOnly: Boolean(process.env.CI),
  use: {
    baseURL: "http://127.0.0.1:3000",
    locale: "vi-VN",
  },
  webServer: [
    {
      command: `"${path.join(root, "services/math/.venv/bin/uvicorn")}" app.main:app --host 127.0.0.1 --port 8000`,
      cwd: path.join(root, "services/math"),
      url: "http://127.0.0.1:8000/health",
      reuseExistingServer: !process.env.CI,
      timeout: 30_000,
    },
    {
      command: "pnpm dev",
      url: "http://127.0.0.1:3000/dang-nhap",
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
});
