/**
 * F-10: mã hoá khoá API của lớp (class_settings.ai_api_key) bằng AES-256-GCM.
 * Khoá máy chủ: APP_ENC_KEY = 32 byte, dạng base64 (Render `generateValue`) hoặc 64 ký tự hex.
 * Định dạng lưu: "enc:v1:<iv b64>:<tag b64>:<bản mã b64>".
 * - Trên Render (bản host) thiếu APP_ENC_KEY thì KHÔNG lưu khoá (ném lỗi), không bao giờ lưu rõ.
 * - Máy dev/test không có Render: dùng khoá dev cố định (chỉ để chạy thử), có cảnh báo.
 * - Giá trị cũ chưa mã hoá (không có tiền tố) vẫn đọc được; caiDatLop mã hoá lại khi đọc.
 */
import { createCipheriv, createDecipheriv, createHash, randomBytes } from "crypto";

const TIEN_TO = "enc:v1:";
type Env = Record<string, string | undefined>;

function laHost(env: Env) {
  return Boolean(env.RENDER || env.RENDER_SERVICE_ID || env.RENDER_EXTERNAL_URL);
}

export function docKhoaMay(env: Env = process.env): Buffer {
  const raw = (env.APP_ENC_KEY || "").trim();
  if (raw) {
    const buf = /^[0-9a-fA-F]{64}$/.test(raw) ? Buffer.from(raw, "hex") : Buffer.from(raw, "base64");
    if (buf.length !== 32) throw new Error("APP_ENC_KEY phải là 32 byte (base64 hoặc 64 hex).");
    return buf;
  }
  if (laHost(env)) throw new Error("Thiếu APP_ENC_KEY trên máy chủ: không lưu khoá API.");
  return createHash("sha256").update("hoc-toan-ai:chi-dung-cho-may-dev").digest();
}

export function daMaHoa(v: string | null | undefined) {
  return typeof v === "string" && v.startsWith(TIEN_TO);
}

export function maHoa(plain: string, env: Env = process.env): string {
  const key = docKhoaMay(env);
  const iv = randomBytes(12);
  const c = createCipheriv("aes-256-gcm", key, iv);
  const ct = Buffer.concat([c.update(plain, "utf8"), c.final()]);
  const tag = c.getAuthTag();
  return `${TIEN_TO}${iv.toString("base64")}:${tag.toString("base64")}:${ct.toString("base64")}`;
}

/** Giải mã; giá trị cũ không tiền tố trả nguyên; hỏng/sai khoá trả null (coi như chưa có khoá). */
export function giaiMa(v: string | null | undefined, env: Env = process.env): string | null {
  if (v == null || v === "") return null;
  if (!daMaHoa(v)) return v;
  try {
    const [ivB, tagB, ctB] = v.slice(TIEN_TO.length).split(":");
    const d = createDecipheriv("aes-256-gcm", docKhoaMay(env), Buffer.from(ivB, "base64"));
    d.setAuthTag(Buffer.from(tagB, "base64"));
    return Buffer.concat([d.update(Buffer.from(ctB, "base64")), d.final()]).toString("utf8");
  } catch {
    console.error("giaiMa: không giải mã được khoá lớp (sai APP_ENC_KEY hoặc dữ liệu hỏng)");
    return null;
  }
}
