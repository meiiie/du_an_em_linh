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

import pkg from "../package.json";

export const SITE_NAME = "Học toán với AI";
/** Một SemVer — cùng số với `apps/web/package.json` / release-please. */
export const SITE_VERSION = pkg.version;
/** 7 ký tự commit trên Render / Actions — để biết pod đang chạy bản nào. */
export function siteBan() {
  const raw = process.env.RENDER_GIT_COMMIT || process.env.GITHUB_SHA || "";
  const gon = raw.replace(/[^0-9a-f]/gi, "");
  return gon.slice(0, 7) || null;
}
export const SITE_DESC =
  "Gia sư Toán 12 — xét đơn điệu và cực trị trên phiếu 5 bước. Gia sư sửa bài, không đưa đáp án.";
export const SITE_LOCALE = "vi_VN";
