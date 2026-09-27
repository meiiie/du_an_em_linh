#!/usr/bin/env node
/** Chặn file khóa / giá trị khóa bị git theo dõi. Không in khóa. */
import { execSync } from "node:child_process";
import { readFileSync } from "node:fs";

const TEN_CAM =
  /(^|\/)(\.env|\.env\.[^/]+|zaiapikey\.txt|[^/]*apikey[^/]*\.txt|[^/]+\.pem|[^/]+\.p12)$/i;
const CHO_PHEP = new Set([".env.example"]);
const GAN_KHOA =
  /(?:ZAI_API_KEY|OPENROUTER_API_KEY|LLM_API_KEY|OPENAI_API_KEY|OPENAI_OAUTH_CLIENT_SECRET)\s*[:=]\s*['"]?(?!changeme)[A-Za-z0-9_\-]{16,}/;
const HINH_KHOA = /(?:sk-|zai-|or-v1-)[A-Za-z0-9_\-]{20,}/;

const files = execSync("git ls-files -z", { encoding: "utf8" })
  .split("\0")
  .filter(Boolean);

const tenCam = files.filter((f) => TEN_CAM.test(f) && !CHO_PHEP.has(f));
if (tenCam.length) {
  console.error("Có file khóa đang được git theo dõi:", tenCam.join(", "));
  process.exit(1);
}

const lech = [];
for (const f of files) {
  if (!/\.(ts|tsx|js|mjs|md|json|yml|yaml|env|txt|sh|toml|cff|sql)$/i.test(f)) continue;
  let raw;
  try {
    raw = readFileSync(f, "utf8");
  } catch {
    continue;
  }
  if (GAN_KHOA.test(raw) || HINH_KHOA.test(raw)) lech.push(f);
}

if (lech.length) {
  console.error("Có chuỗi giống khóa API trong file đã theo dõi:", lech.join(", "));
  process.exit(1);
}

console.log("không có khóa trong git");
