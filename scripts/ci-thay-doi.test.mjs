import assert from 'node:assert/strict';
import { test } from 'node:test';
import { phanLoai } from './ci-thay-doi.mjs';

const chi = (...nhom) => ({ harness: false, core: false, frontend: false, math: false,
  ...Object.fromEntries(nhom.map((n) => [n, true])) });
const het = chi('harness', 'core', 'frontend', 'math');
const cases = [
  ['chỉ tài liệu', ['docs/QUY-TRINH.md', 'labs/design/README.md', 'README.md'], chi()],
  ['core', ['services/core/pom.xml'], chi('core')],
  ['frontend', ['apps/frontend/src/app/app.ts'], chi('frontend')],
  ['toán chạy cả hợp đồng core', ['services/math/app/grader.py'], chi('core', 'math')],
  ['dữ liệu sư phạm đóng gói', ['data/supham/ma-loi-DH.csv'], chi('core', 'math')],
  ['dữ liệu import lưu trữ', ['data/v0/khung-buoc.json'], chi('core', 'math')],
  ['snapshot đối chiếu', ['specs/001-lat-cat-doc/doi-chieu/bkt-v0.json'], chi('core')],
  ['hook', ['.claude/hooks/guard.mjs'], chi('harness')],
  ['lockfile', ['pnpm-lock.yaml'], chi('frontend')],
  ['cổng phẳng', ['scripts/check-flat.mjs'], chi('frontend')],
  ['core và tài liệu', ['services/core/AGENTS.md', 'docs/CODEMAP.md'], chi('core')],
  ['workflow', ['.github/workflows/ci.yml'], het],
  ['file chưa phân loại', ['tsconfig.base.json'], het],
  ['PR rỗng', [], chi()],
];
for (const [ten, files, ket] of cases) test(ten, () => assert.deepEqual(phanLoai(files), ket));
