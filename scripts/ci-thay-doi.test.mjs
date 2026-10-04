// Chạy: node --test scripts/ci-thay-doi.test.mjs
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { phanLoai } from './ci-thay-doi.mjs';

const chi = (...nhom) => ({ harness: false, core: false, frontend: false, v0: false, ...Object.fromEntries(nhom.map((n) => [n, true])) });
const het = chi('harness', 'core', 'frontend', 'v0');

const cases = [
  ['chỉ tài liệu → không job nào', ['docs/QUY-TRINH.md', 'labs/design/README.md', 'README.md'], chi()],
  ['services/core', ['services/core/pom.xml'], chi('core')],
  ['apps/frontend', ['apps/frontend/src/app/app.ts'], chi('frontend')],
  ['apps/web', ['apps/web/app/page.tsx'], chi('v0')],
  ['services/math', ['services/math/app/grader.py'], chi('v0')],
  ['dữ liệu đóng gói vào ảnh core → core + v0', ['data/supham/ma-loi-DH.csv'], chi('core', 'v0')],
  ['hằng nội dung v0 → core + v0', ['data/v0/khung-buoc.json'], chi('core', 'v0')],
  ['dữ liệu khác → chỉ v0', ['data/demo/lop.json'], chi('v0')],
  ['hook harness', ['.claude/hooks/guard.mjs'], chi('harness')],
  ['lockfile chung → frontend + v0', ['pnpm-lock.yaml'], chi('frontend', 'v0')],
  ['core + tài liệu', ['services/core/AGENTS.md', 'docs/CODEMAP.md'], chi('core')],
  ['workflow CI → mọi job', ['.github/workflows/ci.yml'], het],
  ['file lạ ở gốc → mọi job (an toàn mặc định)', ['tsconfig.base.json'], het],
  ['script khác → mọi job', ['scripts/kiem-phien-ban.mjs'], het],
  ['PR rỗng → không job nào', [], chi()],
];

for (const [ten, files, ket] of cases) test(ten, () => assert.deepEqual(phanLoai(files), ket));
