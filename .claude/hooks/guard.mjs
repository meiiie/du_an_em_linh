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

// Short-option cluster of `git commit`, e.g. -am"msg" → a, then message "msg". An option that takes
// an argument ends the scan: the rest of the cluster (or the next token) is its value, as in git.
function commitShort(token) {
  const r = { noVerify: false, all: false, values: [] };
  for (let i = 1; i < token.length; i += 1) {
    const c = token[i];
    if (c === 'n') r.noVerify = true;
    else if (c === 'a') r.all = true;
    else if ('mFCc'.includes(c)) {
      const kind = c === 'm' ? 'message' : c === 'F' ? 'file' : 'reuse';
      r.values.push({ kind, value: token.slice(i + 1) || undefined });
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

const SAFE_WITH_SECRET = /^(ls|dir|Get-ChildItem|gci|touch|test|\[|stat|Test-Path)$/i;
// Git subcommands that only report on a path, never print its contents.
const GIT_SAFE_WITH_SECRET = new Set(['status', 'check-ignore', 'ls-files']);
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

  // Reading, copying from, or writing to a secret path (cp .env.example .env would overwrite a
  // developer's real keys) all need a human.
  if (head !== 'git' && operands.some(isSecret) && !SAFE_WITH_SECRET.test(head)) deny(SECRET_READ);
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
    const pathspecs = args.filter((a) => !a.startsWith('-'));
    if (args.some((a) => ['-A', '--all', '.', ':/', '*', ':/*'].includes(a)) || (args.some((a) => a === '-u' || a === '--update') && !pathspecs.length))
      deny('Stage từng đường dẫn cụ thể thay vì git add -A / -u / . (tránh kéo theo bí mật, file rác, thay đổi của việc khác).');
    if (args.some(isSecret)) deny('Không stage file bí mật.');
  }

  // Paths named by the git command: `<rev>:<path>` (git show HEAD:.env) counts; message values
  // after -m / -F (also -am, --message) do not.
  const gitPaths = [];
  for (let k = 0; k < args.length; k += 1) {
    const a = args[k];
    if (/^(-[A-Za-z]*[mF]|--message|--file)$/.test(a)) k += 1;
    else if (!a.startsWith('-')) gitPaths.push(a.slice(a.lastIndexOf(':') + 1));
  }
  if (gitPaths.some(isSecret) && !(GIT_SAFE_WITH_SECRET.has(sub) || (sub === 'rm' && args.includes('--cached')))) deny(SECRET_READ);

  if (sub === 'push') {
    if (args.some((a) => a === '--force' || a.startsWith('+') || (/^-[A-Za-z]/.test(a) && pushForces(a))))
      deny('Không push --force. Dùng --force-with-lease trên nhánh của mình (QUY-TRINH §6).');
    if (args.some((a) => ['--all', '--branches', '--mirror', '--prune'].includes(a)))
      deny('Không push hàng loạt (--all, --branches, --mirror, --prune): có thể cập nhật hoặc xóa main. Push đúng nhánh của mình.');
    // With --repo every positional is a refspec; options that take a value consume the next token.
    const VALUE_OPTS = new Set(['-o', '--push-option', '--receive-pack', '--exec']);
    let explicitRepo = false;
    const positional = [];
    for (let k = 0; k < args.length; k += 1) {
      const a = args[k];
      if (a === '--repo') {
        explicitRepo = true;
        k += 1;
      } else if (a.startsWith('--repo=')) explicitRepo = true;
      else if (VALUE_OPTS.has(a)) k += 1;
      else if (!a.startsWith('-')) positional.push(a);
    }
    const refspecs = explicitRepo ? positional : positional.slice(1);
    const targetsMain = refspecs.some((r) => /(^|:)(refs\/heads\/)?(main|master)$/.test(r));
    const pushesCurrent = refspecs.length === 0 || refspecs.some((r) => r === 'HEAD' || r === '@');
    const branch = pushesCurrent ? git(repo, ['rev-parse', '--abbrev-ref', 'HEAD']) : '';
    if (targetsMain || branch === 'main' || branch === 'master')
      deny('Không push lên main. Tạo nhánh feat/ fix/ docs/ chore/ rồi mở PR (QUY-TRINH §4).');
  }

  if (sub === 'commit') {
    let noVerify = false;
    let bulk = false;
    const sources = []; // { kind: 'message' | 'file' | 'reuse', value }
    const LONG = { '--message': 'message', '--file': 'file', '--reuse-message': 'reuse', '--reedit-message': 'reuse' };
    for (let k = 0; k < args.length; k += 1) {
      const a = args[k];
      const eq = a.indexOf('=');
      if (LONG[a] && args[k + 1] !== undefined) sources.push({ kind: LONG[a], value: args[(k += 1)] });
      else if (eq > 0 && LONG[a.slice(0, eq)]) sources.push({ kind: LONG[a.slice(0, eq)], value: a.slice(eq + 1) });
      else if (a === '--all') bulk = true;
      else if (/^-[A-Za-z]/.test(a)) {
        const s = commitShort(a);
        noVerify ||= s.noVerify;
        bulk ||= s.all;
        for (const v of s.values) sources.push({ kind: v.kind, value: v.value ?? args[(k += 1)] });
      }
    }
    if (noVerify) deny('Không bỏ qua hook git (git commit -n = --no-verify).');
    if (bulk) deny('Không dùng git commit -a / --all: stage từng đường dẫn rồi commit, tránh kéo theo thay đổi của việc khác.');

    // Every way git takes a message is checked: -m, -F <file> / -F - (heredoc), -C / -c <commit>, --amend --no-edit.
    const messages = [];
    for (const { kind, value } of sources) {
      if (value === undefined) continue;
      if (kind === 'message') messages.push(messageOf(value));
      else if (kind === 'file') {
        if (value === '-') {
          const body = words.find((w) => /__BODY_\d+__/.test(w));
          if (!body) deny('Không kiểm được thông điệp commit đọc từ stdin. Dùng git commit -m "$(cat <<\'EOF\' … EOF)".');
          messages.push(messageOf(body));
        } else {
          if (isSecret(value)) deny(SECRET_READ);
          try {
            messages.push(readFileSync(path.resolve(repo, value), 'utf8'));
          } catch {
            deny('Không đọc được file thông điệp để kiểm (file chưa tồn tại lúc chạy hook?). Dùng git commit -m với heredoc.');
          }
        }
      } else {
        const reused = git(repo, ['log', '-1', '--format=%B', value]);
        if (!reused) deny('Không đọc được thông điệp của commit được dùng lại để kiểm.');
        messages.push(reused);
      }
    }
    if (!messages.length && args.includes('--amend') && args.includes('--no-edit')) {
      const head = git(repo, ['log', '-1', '--format=%B', 'HEAD']);
      if (head) messages.push(head);
    }
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
