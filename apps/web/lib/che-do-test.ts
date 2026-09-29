/**
 * Chế độ test (chỉ để chạy Playwright / demo cục bộ): APP_ENV=test (hoặc NODE_ENV=test) VÀ không chạy trên Render.
 * Bản host không bao giờ bật, kể cả khi ai đó lỡ đặt APP_ENV=test trên Render.
 */
export function cheDoTest(env: Record<string, string | undefined> = process.env): boolean {
  const laTest = env.APP_ENV === "test" || env.NODE_ENV === "test";
  const laHost = Boolean(env.RENDER || env.RENDER_SERVICE_ID || env.RENDER_EXTERNAL_URL);
  return laTest && !laHost;
}
