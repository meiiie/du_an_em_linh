/** URL công khai — metadata, canonical, sitemap. */
export function siteUrl(): URL {
  const raw = (process.env.APP_URL || process.env.RENDER_EXTERNAL_URL || "https://hoc-toan-ai.onrender.com").replace(
    /\/$/,
    "",
  );
  try {
    return new URL(raw);
  } catch {
    return new URL("https://hoc-toan-ai.onrender.com");
  }
}

export const SITE_NAME = "Học toán với AI";
export const SITE_DESC =
  "Nguyên mẫu NCKH: gia sư Toán 12 — xét đơn điệu và cực trị theo phiếu 5 bước. Gia sư sửa bài, không đưa đáp án. Demo chạy khi không có khóa API.";
export const SITE_LOCALE = "vi_VN";
