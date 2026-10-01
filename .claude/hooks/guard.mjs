// PreToolUse guard: turns the hard rules of docs/QUY-TRINH.md §4 and docs/HIEN-CHUONG.md III–IV
// into deterministic denials. Hooks run in every permission mode, so this is the layer that holds
// even when permission rules are bypassed or anchored at a different working directory.
// Reads the hook payload on stdin; prints a deny decision or exits 0. Internal errors fail open.
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
// A `.env…` reference inside inline code, e.g. python -c "open('.env')"; ignores process.env, import.meta.env.
const mentionsSecret = (text) => [...String(text).matchAll(/(?<![\w$])\.env(?:\.[\w-]+)*/g)].some((m) => isSecret(m[0]));

const SECRET_READ = 'Không đọc / ghi file bí mật (.env, khóa, *.pem). Tên biến xem ở .env.example (hiến chương III).';

// ---- File tools -------------------------------------------------------------------------------
if (['Read', 'Grep', 'Edit', 'Write', 'NotebookEdit'].includes(tool)) {
  const raw = input.file_path ?? input.notebook_path ?? input.path;
  if (raw) {
    const target = posix(path.resolve(cwd, raw));
    if (isSecret(target)) deny(SECRET_READ);
    if (tool !== 'Read' && tool !== 'Grep' && target.includes('/services/math/kiemdinh/ket-qua/'))
      deny('Kết quả kiểm định chỉ do script chạy bộ ca ghi, không sửa tay (hiến chương IV). Chạy lại bộ ca để cập nhật.');
  }
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

// Short-option cluster of `git commit`, e.g. -am"msg" → a, then message "msg". Options after which
// the rest of the cluster is an argument end the scan (git's own parsing rule).
function commitShort(token) {
  const r = { noVerify: false, usesFile: false, message: undefined, needsNext: false };
  for (let i = 1; i < token.length; i += 1) {
    const c = token[i];
    if (c === 'n') r.noVerify = true;
    else if (c === 'm') {
      const rest = token.slice(i + 1);
      if (rest) r.message = rest;
      else r.needsNext = true;
      return r;
    } else if ('FCc'.includes(c)) {
      r.usesFile = true;
      return r;
    } else if ('Stu'.includes(c) || !/[A-Za-z]/.test(c)) return r;
  }
  return r;
}
// Short-option cluster of `git push`: -f forces; -o takes the rest as its argument.
function pushForces(token) {
  for (let i = 1; i < token.length; i += 1) {
    if (token[i] === 'f') return true;
    if (token[i] === 'o' || !/[A-Za-z]/.test(token[i])) return false;
  }
  return false;
}

const SAFE_WITH_SECRET = /^(ls|dir|Get-ChildItem|gci|touch|test|\[|stat|Test-Path|git)$/i;
const COPIERS = /^(cp|copy|Copy-Item|cpi)$/i;
const INTERPRETERS = /^(python3?|py|node|deno|bun|perl|ruby|php|bash|sh|zsh|pwsh|powershell)(\.exe)?$/i;
const DANGER_RM = ['/', '~', '.', '..', '*', '.git'];
let dir = cwd;

for (const words of segments(command)) {
  const [head, ...rest] = words;
  if (head === 'cd' || head === 'Set-Location' || head === 'pushd') {
    if (rest[0]) dir = path.resolve(dir, rest[0]);
    continue;
  }

  // Operands with redirection marks stripped: `< .env`, `<.env`, `2>> out.txt`.
  const operands = rest
    .filter((w) => !/^-/.test(w))
    .map((w) => w.replace(/^\d*[<>]+&?/, ''))
    .filter(Boolean);

  if (operands.some(isSecret) && !SAFE_WITH_SECRET.test(head)) {
    if (!COPIERS.test(head)) deny(SECRET_READ);
    if (isSecret(operands[0])) deny('Không sao chép file bí mật ra chỗ khác.');
  }
  if (INTERPRETERS.test(head) && rest.some(mentionsSecret)) deny(SECRET_READ);
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

  if (sub === 'add') {
    if (args.some((a) => ['-A', '--all', '.', ':/', '*', ':/*'].includes(a)))
      deny('Stage từng đường dẫn cụ thể thay vì git add -A / git add . (tránh kéo theo bí mật và file rác).');
    if (args.some(isSecret)) deny('Không stage file bí mật.');
  }

  if (sub === 'push') {
    if (args.some((a) => a === '--force' || a.startsWith('+') || (/^-[A-Za-z]/.test(a) && pushForces(a))))
      deny('Không push --force. Dùng --force-with-lease trên nhánh của mình (QUY-TRINH §6).');
    const refspecs = args.filter((a) => !a.startsWith('-')).slice(1);
    const targetsMain = refspecs.some((r) => /(^|:)(refs\/heads\/)?(main|master)$/.test(r));
    const pushesCurrent = refspecs.length === 0 || refspecs.some((r) => r === 'HEAD' || r === '@');
    const branch = pushesCurrent ? git(repo, ['rev-parse', '--abbrev-ref', 'HEAD']) : '';
    if (targetsMain || branch === 'main' || branch === 'master')
      deny('Không push lên main. Tạo nhánh feat/ fix/ docs/ chore/ rồi mở PR (QUY-TRINH §4).');
  }

  if (sub === 'commit') {
    let noVerify = false;
    let usesFile = false;
    const messages = [];
    for (let k = 0; k < args.length; k += 1) {
      const a = args[k];
      if (a === '--message' && args[k + 1] !== undefined) messages.push(messageOf(args[(k += 1)]));
      else if (a.startsWith('--message=')) messages.push(messageOf(a.slice('--message='.length)));
      else if (['--file', '--reuse-message', '--reedit-message'].includes(a) || /^--(file|reuse-message|reedit-message)=/.test(a)) usesFile = true;
      else if (/^-[A-Za-z]/.test(a)) {
        const s = commitShort(a);
        noVerify ||= s.noVerify;
        usesFile ||= s.usesFile;
        if (s.message !== undefined) messages.push(messageOf(s.message));
        else if (s.needsNext && args[k + 1] !== undefined) messages.push(messageOf(args[(k += 1)]));
      }
    }
    if (noVerify) deny('Không bỏ qua hook git (git commit -n = --no-verify).');
    if (usesFile || (args.includes('--amend') && args.includes('--no-edit')) || !messages.length) continue;
    const full = messages.join('\n\n');
    const subject = (full.split('\n').find((l) => l.trim()) ?? '').trim();
    if (!CONVENTIONAL.test(subject))
      deny(`Tiêu đề commit phải theo Conventional Commits: "<type>(<scope>): <tóm tắt>", type ∈ feat|fix|docs|chore|refactor|perf|test|build|ci|style|revert|hotfix. Nhận được: "${subject.slice(0, 80)}".`);
    if (!/^Co-Authored-By: .+/m.test(full))
      deny('Commit do agent viết phải có trailer "Co-Authored-By: ..." ở cuối thân (QUY-TRINH §4). Viết thân commit bằng heredoc.');
  }
}

process.exit(0);
