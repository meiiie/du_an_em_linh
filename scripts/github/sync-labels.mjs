// Đồng bộ nhãn từ .github/labels.json lên GitHub: tạo mới hoặc cập nhật, không xóa nhãn khác.
// Chạy (chủ repo): node scripts/github/sync-labels.mjs [--repo owner/name]
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';

const labels = JSON.parse(readFileSync(new URL('../../.github/labels.json', import.meta.url), 'utf8'));
const at = process.argv.indexOf('--repo');
const repo = at > -1 ? ['--repo', process.argv[at + 1]] : [];

for (const { name, color, description } of labels) {
  execFileSync('gh', ['label', 'create', name, '--color', color, '--description', description, '--force', ...repo], {
    stdio: 'inherit',
  });
}
console.log(`Đã đồng bộ ${labels.length} nhãn.`);
