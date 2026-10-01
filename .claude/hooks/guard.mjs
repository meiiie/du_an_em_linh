// PreToolUse guard: turns the hard rules of docs/QUY-TRINH.md §4 and docs/HIEN-CHUONG.md III–IV
// into deterministic denials. Reads the hook payload on stdin; prints a deny decision or exits 0.
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import path from 'node:path';

const payload = JSON.parse(readFileSync(0, 'utf8') || '{}');
const tool = payload.tool_name;
const input = payload.tool_input ?? {};
const cwd = payload.cwd ?? process.cwd();

function deny(reason) {
  process.stdout.write(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        permissionDecision: 'deny',
        permissionDecisionReason: reason,
      },
    }),
  );
  process.exit(0);
}

const posix = (p) => String(p ?? '').replace(/\\/g, '/');
const isSecret = (p) => {
  const parts = posix(p).split('/');
  const base = parts.at(-1) ?? '';
  if (base.endsWith('.example')) return false;
  return (
    base === '.env' ||
    base.startsWith('.env.') ||
    /\.(pem|key|p12)$/.test(base) ||
    /apikey.*\.txt$/i.test(base) ||
    parts.includes('secrets')
  );
};

// ---- File tools -------------------------------------------------------------------------------
if (tool === 'Edit' || tool === 'Write' || tool === 'NotebookEdit') {
  const target = posix(path.resolve(cwd, input.file_path ?? input.notebook_path ?? ''));
  if (isSecret(target)) deny('Không ghi file bí mật (.env, khóa, *.pem). Mẫu biến nằm ở .env.example (hiến chương III).');
  if (target.includes('/services/math/kiemdinh/ket-qua/'))
    deny('Kết quả kiểm định chỉ do script chạy bộ ca ghi, không sửa tay (hiến chương IV). Chạy lại bộ ca để cập nhật.');
  process.exit(0);
}

if (tool !== 'Bash' && tool !== 'PowerShell') process.exit(0);

// ---- Shell commands ---------------------------------------------------------------------------
const CONVENTIONAL = /^(feat|fix|docs|chore|refactor|perf|test|build|ci|style|revert|hotfix)(\([^)]+\))?!?: \S/;

// Pull heredoc / here-string bodies out first so their text is never parsed as commands.
const bodies = [];
const keep = (body) => ` __BODY_${bodies.push(body) - 1}__`;
const command = String(input.command ?? '')
  .replace(/\r\n/g, '\n')
  .replace(/<<-?[ \t]*(['"]?)([A-Za-z_]\w*)\1[^\n]*\n([\s\S]*?)\n[ \t]*\2[ \t]*(?=\n|$)/g, (_, _q, _t, body) => keep(body))
  .replace(/@(['"])\n([\s\S]*?)\n\1@/g, (_, _q, body) => keep(body));

// Split into segments on unquoted && || ; | and newlines; quoted text stays inside one token.
function segments(cmd) {
  const out = [];
  let tokens = [];
  let cur = '';
  let started = false;
  let quote = null;
  const endToken = () => {
    if (started) tokens.push(cur);
    cur = '';
    started = false;
  };
  const endSegment = () => {
    endToken();
    if (tokens.length) out.push(tokens);
    tokens = [];
  };
  for (let i = 0; i < cmd.length; i += 1) {
    const c = cmd[i];
    if (quote === "'") {
      if (c === "'") quote = null;
      else cur += c;
    } else if (quote === '"') {
      if (c === '\\' && i + 1 < cmd.length && tool === 'Bash') cur += cmd[++i];
      else if (c === '"') quote = null;
      else cur += c;
    } else if (c === '\\' && i + 1 < cmd.length && tool === 'Bash') {
      cur += cmd[++i];
      started = true;
    } else if (c === '"' || c === "'") {
      quote = c;
      started = true;
    } else if (c === '&' && cmd[i + 1] === '&') {
      endSegment();
      i += 1;
    } else if (c === '|' || c === ';' || c === '\n') {
      if (c === '|' && cmd[i + 1] === '|') i += 1;
      endSegment();
    } else if (/\s/.test(c)) endToken();
    else {
      cur += c;
      started = true;
    }
  }
  endSegment();
  return out;
}

const messageOf = (token) => {
  const ref = token.match(/__BODY_(\d+)__/);
  return ref ? bodies[Number(ref[1])] : token;
};
const git = (dir, args) => {
  try {
    return execFileSync('git', ['-C', dir, ...args], { encoding: 'utf8', timeout: 3000, stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    return '';
  }
};

const READERS = /^(cat|type|more|less|head|tail|Get-Content|gc|Select-String|sls|grep|rg|findstr|source)$/i;
const COPIERS = /^(cp|copy|Copy-Item|cpi)$/i;
const DANGER_RM = ['/', '~', '.', '..', '*', '.git'];
let dir = cwd;

for (const words of segments(command)) {
  const [head, ...rest] = words;
  if (head === 'cd' || head === 'Set-Location' || head === 'pushd') {
    if (rest[0]) dir = path.resolve(dir, rest[0]);
    continue;
  }

  const operands = rest.filter((w) => !w.startsWith('-'));
  if (READERS.test(head) && operands.some(isSecret)) deny('Không đọc file bí mật qua shell (.env, khóa). Dùng .env.example để biết tên biến.');
  if (COPIERS.test(head) && operands.length && isSecret(operands[0])) deny('Không sao chép file bí mật ra chỗ khác.');
  if (head === 'rm' && rest.some((w) => /^-\w*[rR]/.test(w)) && operands.some((w) => DANGER_RM.includes(w.replace(/\/+$/, '') || '/')))
    deny('Lệnh xóa đệ quy nhắm vào thư mục gốc, repo hoặc .git bị chặn.');

  if (head === 'gh' && rest[0] === 'pr' && rest[1] === 'merge') deny('Agent không tự merge PR. Chủ repo merge sau khi duyệt (QUY-TRINH §1).');
  if (head !== 'git') continue;

  // Resolve `git -C <dir>` and other global flags before the subcommand.
  let repo = dir;
  let i = 0;
  while (i < rest.length && rest[i].startsWith('-')) {
    if (rest[i] === '-C' && rest[i + 1]) {
      repo = path.resolve(dir, rest[i + 1]);
      i += 2;
    } else if (rest[i] === '-c') i += 2;
    else i += 1;
  }
  const sub = rest[i];
  const args = rest.slice(i + 1);

  if (args.includes('--no-verify')) deny('Không bỏ qua hook git (--no-verify). Sửa nguyên nhân thay vì lách (QUY-TRINH §4).');

  if (sub === 'push') {
    if (args.some((a) => a === '--force' || a === '-f' || a.startsWith('+') || (/^-[a-z]+$/.test(a) && a.includes('f'))))
      deny('Không push --force. Dùng --force-with-lease trên nhánh của mình (QUY-TRINH §6).');
    const refspecs = args.filter((a) => !a.startsWith('-')).slice(1);
    const targetsMain = refspecs.some((r) => /(^|:)(refs\/heads\/)?(main|master)$/.test(r));
    const pushesCurrent = refspecs.length === 0 || refspecs.some((r) => r === 'HEAD' || r === '@');
    const branch = pushesCurrent ? git(repo, ['rev-parse', '--abbrev-ref', 'HEAD']) : '';
    if (targetsMain || branch === 'main' || branch === 'master')
      deny('Không push lên main. Tạo nhánh feat/ fix/ docs/ chore/ rồi mở PR (QUY-TRINH §4).');
  }

  if (sub === 'add' && args.some((a) => ['-A', '--all', '.', ':/', '*', ':/*'].includes(a)))
    deny('Stage từng đường dẫn cụ thể thay vì git add -A / git add . (tránh kéo theo bí mật và file rác).');

  if (sub === 'commit') {
    if (args.some((a) => /^-[a-zA-Z]+$/.test(a) && a.includes('n')))
      deny('Không bỏ qua hook git (git commit -n = --no-verify).');
    const usesFile = args.some(
      (a) => ['-F', '--file', '-C', '-c', '--reuse-message', '--reedit-message'].includes(a) || /^--(file|reuse-message|reedit-message)=/.test(a),
    );
    if (usesFile || (args.includes('--amend') && args.includes('--no-edit'))) continue;
    const messages = [];
    args.forEach((a, k) => {
      const next = args[k + 1];
      if ((a === '--message' || /^-[a-zA-Z]*m$/.test(a)) && next !== undefined) messages.push(messageOf(next));
      else if (a.startsWith('--message=')) messages.push(messageOf(a.slice('--message='.length)));
    });
    if (!messages.length) continue;
    const full = messages.join('\n\n');
    const subject = (full.split('\n').find((l) => l.trim()) ?? '').trim();
    if (!CONVENTIONAL.test(subject))
      deny(`Tiêu đề commit phải theo Conventional Commits: "<type>(<scope>): <tóm tắt>", type ∈ feat|fix|docs|chore|refactor|perf|test|build|ci|style|revert|hotfix. Nhận được: "${subject.slice(0, 80)}".`);
    if (!/^Co-Authored-By: .+/m.test(full))
      deny('Commit do agent viết phải có trailer "Co-Authored-By: ..." ở cuối thân (QUY-TRINH §4). Viết thân commit bằng heredoc.');
  }
}

process.exit(0);
