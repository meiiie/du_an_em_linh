#!/usr/bin/env node
// Phân loại file đổi của một PR → job CI nào cần chạy. In `nhom=true|false` (dạng $GITHUB_OUTPUT).
// An toàn mặc định: file không thuộc nhóm nào và không chỉ là tài liệu → chạy mọi job.
// Dùng: node scripts/ci-thay-doi.mjs <base-sha> <head-sha>   (thiếu base → chạy mọi job)
import { execFileSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const GOI_GOC = [/^package\.json$/, /^pnpm-lock\.yaml$/, /^pnpm-workspace\.yaml$/];

export const NHOM = {
  harness: [/^\.claude\//],
  // data/supham, data/v0 được đóng gói vào ảnh core (T003b): job Core kiểm chúng có trong ảnh.
  core: [/^services\/core\//, /^data\/(supham|v0)\//],
  frontend: [/^apps\/frontend\//, ...GOI_GOC],
  v0: [/^apps\/web\//, /^services\/math\//, /^data\//, /^scripts\/(migrate|seed)/, ...GOI_GOC],
};

// Chỉ đọc, không vào bản dựng nào: đổi riêng các file này thì không cần job nào.
const CHI_TAI_LIEU = [/^docs\//, /^labs\//, /^[^/]+\.md$/, /^\.github\/(ISSUE_TEMPLATE|PULL_REQUEST_TEMPLATE)/, /^LICENSE$/];

const tatCa = (giaTri) => Object.fromEntries(Object.keys(NHOM).map((k) => [k, giaTri]));

export function phanLoai(files) {
  const ket = tatCa(false);
  for (const f of files) {
    const nhom = Object.entries(NHOM).filter(([, mau]) => mau.some((r) => r.test(f)));
    if (nhom.length) for (const [k] of nhom) ket[k] = true;
    else if (!CHI_TAI_LIEU.some((r) => r.test(f))) return tatCa(true);
  }
  return ket;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const [base, head] = process.argv.slice(2);
  const files =
    base && !/^0+$/.test(base)
      ? execFileSync('git', ['diff', '--name-only', '--no-renames', `${base}...${head}`], { encoding: 'utf8' }).split('\n').filter(Boolean)
      : null;
  const ket = files ? phanLoai(files) : tatCa(true);
  for (const [k, v] of Object.entries(ket)) console.log(`${k}=${v}`);
}
