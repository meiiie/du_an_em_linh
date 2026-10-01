// SessionStart hook: prints live repo state as factual context (stdout becomes session context).
// Every probe has a short timeout and is skipped on failure so a session never waits on the network.
import { execFileSync } from 'node:child_process';

const run = (cmd, args, timeout = 4000) => {
  try {
    return execFileSync(cmd, args, { encoding: 'utf8', timeout, stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    return null;
  }
};

const branch = run('git', ['rev-parse', '--abbrev-ref', 'HEAD']);
if (branch === null) process.exit(0);

const changed = (run('git', ['status', '--porcelain']) ?? '').split('\n').filter(Boolean);
const recent = run('git', ['log', '-3', '--format=%h %s']) ?? '';
const prsRaw = run('gh', ['pr', 'list', '--state', 'open', '--limit', '15', '--json', 'number,title,headRefName,isDraft'], 6000);

const lines = ['Trạng thái repo khi mở phiên (hook SessionStart):'];
lines.push(`- Nhánh hiện tại: ${branch}${changed.length ? ` — ${changed.length} tệp chưa commit` : ' — sạch'}`);
if (branch === 'main' || branch === 'master') lines.push('- Đang ở main: quy trình yêu cầu tạo nhánh trước khi sửa (docs/QUY-TRINH.md §4).');
if (recent) lines.push(`- Commit gần nhất:\n${recent.split('\n').map((l) => `  ${l}`).join('\n')}`);

if (prsRaw === null) {
  lines.push('- Không lấy được danh sách PR (gh lỗi hoặc không có mạng). Kiểm tay bằng `gh pr list` trước khi sửa file dùng chung.');
} else {
  const prs = JSON.parse(prsRaw || '[]');
  if (prs.length === 0) lines.push('- Không có PR đang mở.');
  else {
    lines.push(`- PR đang mở (${prs.length}) — tránh sửa cùng file với PR khác (QUY-TRINH §9):`);
    for (const pr of prs) lines.push(`  #${pr.number} ${pr.headRefName}${pr.isDraft ? ' (nháp)' : ''} — ${pr.title}`);
  }
}

process.stdout.write(lines.join('\n') + '\n');
