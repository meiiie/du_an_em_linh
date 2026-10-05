// Chạy: node --test scripts/ci-thay-doi.test.mjs
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { phanLoai } from './ci-thay-doi.mjs';

const chi = (...nhom) => ({ harness: false, core: false, frontend: false, v0: false, ...Object.fromEntries(nhom.map((n) => [n, true])) });
const het = chi('harness', 'core', 'frontend', 'v0');

const cases = [
  ['chỉ tài liệu → không job nào', ['docs/QUY-TRINH.md', 'labs/design/README.md', 'README.md'], chi()],
  ['services/core', ['services/core/pom.xml'], chi('core')],
  ['apps/frontend', ['apps/frontend/src/app/app.ts'], chi('frontend')],
  ['apps/web', ['apps/web/app/page.tsx'], chi('v0')],
  ['mã chấm từng bước của v0 → core (tệp vàng cham-v0) + v0', ['apps/web/app/hs/luyen/[id]/page.tsx'], chi('core', 'v0')],
  ['services/math → core (tệp vàng đối chiếu) + v0', ['services/math/app/grader.py'], chi('core', 'v0')],
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

// Codex #135: mọi nguồn mà tệp vàng đối chiếu v0 ghi trong «nguon» phải bật job Core, để đổi nguồn mà quên sinh lại tệp vàng
// thì TepVangDoiChieuTest được chạy và đỏ. Đường dẫn đọc từ chính tệp vàng; thư mục thì thử một tệp con.
const DOI_CHIEU = 'specs/001-lat-cat-doc/doi-chieu/';
const docNguon = (ten) => JSON.parse(readFileSync(new URL('../' + DOI_CHIEU + ten, import.meta.url), 'utf8')).nguon;
const v0 = docNguon('v0-bai.json');
const khoaBang = docNguon('khoa-bang-v0.json');
const cham = docNguon('cham-v0.json');
const loiGiai = JSON.parse(readFileSync(new URL('../services/core/src/test/resources/content/loi-giai-v0.json', import.meta.url),
  'utf8')).nguon;
const nguon = [
  v0.seed.tep,
  loiGiai.tep,
  DOI_CHIEU + 'loi-giai-v0.ts',
  ...Object.keys(v0.du_lieu),
  ...Object.keys(cham.git),
  'services/math',
  ...Object.keys(khoaBang).map((k) => ({ services_math: 'services/math', data_v0: 'data/v0', data_supham: 'data/supham',
    script: DOI_CHIEU + 'khoa-bang-v0.py' })[k] ?? assert.fail('khóa nguon chưa ánh xạ trong test: ' + k)),
];
for (const duongDan of new Set(nguon)) {
  const tep = /\.[a-z]+$/.test(duongDan) ? duongDan : duongDan + '/tep-con.txt';
  test('nguồn của tệp vàng bật core: ' + duongDan, () => assert.equal(phanLoai([tep]).core, true));
}
