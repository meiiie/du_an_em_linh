import { mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";

function thuMucAnh() {
  if (process.env.PLAYWRIGHT_SHOTS) return process.env.PLAYWRIGHT_SHOTS;
  const uuTien = "/opt/cursor/artifacts/screenshots";
  try {
    mkdirSync(uuTien, { recursive: true });
    return uuTien;
  } catch {
    const fallback = path.join(tmpdir(), "hoc-toan-shots");
    mkdirSync(fallback, { recursive: true });
    return fallback;
  }
}

export const SHOTS = thuMucAnh();
