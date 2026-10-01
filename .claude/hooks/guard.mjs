// PreToolUse guard: turns the hard rules of docs/QUY-TRINH.md §4 and docs/HIEN-CHUONG.md III–IV
// into deterministic denials. Hooks run in every permission mode, so this is the layer that holds
// even when permission rules are bypassed or anchored at a different working directory.
// It is an accident guard, not a security boundary: a shell command has unbounded spellings. The
// boundary is server-side (branch ruleset on main, secret-scanning push protection).
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

// gitignore-style glob → RegExp (*, **, ?, {a,b}, [..]); enough to test whether a glob can select a secret.
function globRegex(glob) {
  const body = (g) => {
    let re = '';
    for (let i = 0; i < g.length; i += 1) {
      const c = g[i];
      if (c === '*') {
        if (g[i + 1] === '*') {
          re += '.*';
          i += 1;
          if (g[i + 1] === '/') i += 1;
        } else re += '[^/]*';
      } else if (c === '?') re += '[^/]';
      else if (c === '{' && g.indexOf('}', i) > i) {
        const end = g.indexOf('}', i);
        re += `(?:${g.slice(i + 1, end).split(',').map(body).join('|')})`;
        i = end;
      } else if (c === '[' && g.indexOf(']', i) > i) {
        const end = g.indexOf(']', i);
        re += g.slice(i, end + 1).replace(/^\[!/, '[^');
        i = end;
      } else re += c.replace(/[.+^$()|\\{}[\]]/g, '\\$&');
    }
    return re;
  };
  return new RegExp(`^(?:${body(glob)})$`);
}
const SECRET_SAMPLES = ['.env', '.env.local', '.env.production', 'server.pem', 'id.key', 'cert.p12', 'zaiapikey.txt', 'secrets/token.txt'];
const HAS_GLOB = /[*?[{]/;
// Can this glob select a secret? Tested under any directory prefix: the glob's own literal prefix
// and a few sample depths, so `apps/**/.env*` is caught as well as `.env*`.
function globSelectsSecret(glob, { dotfilesNeedDot = false } = {}) {
  let re;
  try {
    re = globRegex(glob);
  } catch {
    return true;
  }
  const literal = glob.slice(0, glob.search(HAS_GLOB) + 1).replace(/[^/]*$/, '');
  const prefixes = ['', 'a/', 'a/b/', literal, `${literal}x/`, `${literal}x/y/`];
  // Shell globs (bash, no dotglob) never match a leading dot unless the pattern starts with one.
  const base = glob.split('/').pop() ?? '';
  const samples = dotfilesNeedDot && !base.startsWith('.') ? SECRET_SAMPLES.filter((s) => !s.split('/').pop().startsWith('.')) : SECRET_SAMPLES;
  return prefixes.some((p) => samples.some((s) => re.test(p + s)));
}

// ---- File tools -------------------------------------------------------------------------------
if (['Read', 'Grep', 'Edit', 'Write', 'NotebookEdit'].includes(tool)) {
  // A ripgrep glob overrides .gitignore, so `glob: ".env*"` over a directory would read secrets.
  if (tool === 'Grep' && input.glob && globSelectsSecret(String(input.glob)))
    deny('Glob của Grep khớp được file bí mật (glob của ripgrep ghi đè .gitignore). Dùng glob có đuôi file cụ thể, ví dụ "src/**/*.ts".');
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

// Pull heredoc / here-string bodies out first so their text is never parsed as commands. The rest
// of the heredoc's first line stays (cat <<'EOF' > .env).
const bodies = [];
const keep = (body) => ` __BODY_${bodies.push(body) - 1}__`;
const command = String(input.command ?? '')
  .replace(/\r\n/g, '\n')
  .replace(/<<-?[ \t]*(['"]?)([A-Za-z_]\w*)\1([^\n]*)\n([\s\S]*?)\n[ \t]*\2[ \t]*(?=\n|$)/g, (_, _q, _t, rest, body) => keep(body) + rest)
  .replace(/@(['"])\n([\s\S]*?)\n\1@/g, (_, _q, body) => keep(body));

// Command substitutions found while tokenizing — $(…) outside single quotes, and `…` in Bash —
// are queued so their inner commands get checked like top-level ones.
const SUBS = [];
function substitutionEnd(cmd, i) {
  if (cmd[i] === '`') return tool === 'Bash' ? cmd.indexOf('`', i + 1) : -1;
  if (cmd[i] !== '$' || cmd[i + 1] !== '(') return -1;
  let depth = 0;
  for (let j = i + 1; j < cmd.length; j += 1) {
    if (cmd[j] === '(') depth += 1;
    else if (cmd[j] === ')' && --depth === 0) return j;
  }
  return -1;
}

// Split into segments on unquoted && || ; | & (Bash) and newlines; quoted text stays inside one
// token. Each segment also lists the files its unquoted redirections open (> f, 2>>f, &>f, >|f, < f).
function segments(cmd) {
  const out = [];
  let tokens = [];
  let redirects = [];
  let cur = '';
  let started = false;
  let quote = null;
  let redirectAt = -1; // index in `cur` of its first unquoted < or >
  let targetNext = false; // the previous token ended with a redirection operator: this token is its file
  const endToken = () => {
    if (started) {
      if (targetNext) redirects.push(cur);
      targetNext = false;
      if (redirectAt >= 0) {
        for (const [, op, amp, target] of cur.slice(redirectAt).matchAll(/([<>]+)([|&]?)([^<>]*)/g)) {
          if (op.startsWith('<<')) continue; // heredoc / here-string: text, not a file
          if (amp === '&' && /^(\d+-?|-)$/.test(target)) continue; // 2>&1, >&-: descriptors, not files
          if (target) redirects.push(target);
          else targetNext = true;
        }
      }
      tokens.push(cur);
    }
    cur = '';
    started = false;
    redirectAt = -1;
  };
  const endSegment = () => {
    endToken();
    if (tokens.length) out.push(Object.assign(tokens, { redirects }));
    tokens = [];
    redirects = [];
    targetNext = false;
  };
  for (let i = 0; i < cmd.length; i += 1) {
    const c = cmd[i];
    const subEnd = quote === "'" ? -1 : substitutionEnd(cmd, i);
    if (subEnd > i) {
      SUBS.push(c === '`' ? cmd.slice(i + 1, subEnd) : cmd.slice(i + 2, subEnd));
      cur += cmd.slice(i, subEnd + 1);
      started = true;
      i = subEnd;
    } else if (quote === "'") {
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
    } else if (c === '&' && tool === 'Bash' && cmd[i + 1] !== '>' && !/[<>]$/.test(cur)) {
      endSegment(); // `a & b` runs b too; 2>&1, &>f and >&2 are redirections
    } else if (c === '|' && redirectAt >= 0 && cur.endsWith('>')) {
      cur += c; // >| writes even under noclobber; not a pipe
    } else if (c === '|' || c === ';' || c === '\n') {
      if (c === '|' && cmd[i + 1] === '|') i += 1;
      endSegment();
    } else if (/\s/.test(c)) endToken();
    else {
      if (redirectAt < 0 && (c === '<' || c === '>')) redirectAt = cur.length;
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
  const r = { noVerify: false, all: false, edit: false, values: [] };
  for (let i = 1; i < token.length; i += 1) {
    const c = token[i];
    if (c === 'n') r.noVerify = true;
    else if (c === 'a') r.all = true;
    else if (c === 'e') r.edit = true;
    else if ('mFCc'.includes(c)) {
      if (c === 'c') r.edit = true; // -c = --reedit-message: reuse, then open the editor
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

// Commands that name a path without reading its contents (redirections are checked separately).
const SAFE_WITH_SECRET = /^(ls|dir|Get-ChildItem|gci|touch|test|\[|stat|Test-Path|echo|printf|Write-Output|Write-Host)$/i;
// A glob operand the shell (or the command) can expand to a secret: cat .env*, head apps/*/.env.*
// Tokens with whitespace were quoted text (messages, inline code), not globs.
const secretGlob = (op) => HAS_GLOB.test(op) && !/\s/.test(op) && globSelectsSecret(posix(op), { dotfilesNeedDot: tool === 'Bash' });
const secretGlobReason = (g) =>
  `"${g}" có thể khớp file bí mật (.env*, *.pem, *.key, *apikey*.txt). Dùng đường dẫn cụ thể hoặc glob có đuôi file, ví dụ "src/*.ts".`;
// Git subcommands that only report on a path, never print its contents.
const GIT_SAFE_WITH_SECRET = new Set(['status', 'check-ignore', 'ls-files']);
const INTERPRETERS = /^(python3?|py|node|deno|bun|perl|ruby|php|bash|sh|zsh|pwsh|powershell)(\.exe)?$/i;
const DANGER_RM = ['/', '~', '.', '..', '*', '.git'];

// Launchers run the command that follows them; they and VAR=value prefixes are peeled off so
// `env GIT_TRACE=1 git push origin main` is judged as `git push origin main`.
const LAUNCHERS = new Set(['env', 'command', 'builtin', 'exec', 'nohup', 'time', 'nice', 'timeout', 'sudo', 'doas', 'stdbuf', 'xargs', '&']);
const LAUNCHER_VALUE_OPTS = {
  env: ['-u', '-C', '-S'],
  nice: ['-n'],
  timeout: ['-s', '-k'],
  sudo: ['-u', '-g', '-C', '-h', '-p', '-r', '-t', '-U'],
  xargs: ['-I', '-n', '-L', '-P', '-d', '-E', '-s', '-a'],
};
function unwrap(tokens) {
  const w = [...tokens];
  const nested = [];
  let viaXargs = false;
  for (;;) {
    while (w.length && /^[A-Za-z_][A-Za-z0-9_]*=/.test(w[0])) w.shift();
    if (!w.length || !LAUNCHERS.has(w[0])) return { words: w, nested, viaXargs };
    const name = w.shift();
    if (name === 'xargs') viaXargs = true;
    while (w.length && w[0].startsWith('-')) {
      const opt = w.shift();
      if ((LAUNCHER_VALUE_OPTS[name] ?? []).includes(opt)) {
        const value = w.shift();
        if (name === 'env' && opt === '-S' && value) nested.push(value); // env -S "cmd args"
      }
    }
    if (name === 'timeout' && w.length) w.shift(); // DURATION
  }
}

// Nested command strings — substitutions (SUBS), bash -c, pwsh -Command, cmd /c, eval — join the queue.
const queue = segments(command);
let budget = 200;
let dir = cwd;

while ((queue.length || SUBS.length) && budget-- > 0) {
  while (SUBS.length) queue.push(...segments(SUBS.shift()));
  if (!queue.length) continue;
  const seg = queue.shift();
  // A redirection opens its file before the command runs (> truncates it, < reads it), so the
  // target is checked whatever the command: git status > .env, ls > .env, : > .env.
  if (seg.redirects.some((t) => isSecret(t) || secretGlob(t))) deny(SECRET_READ);
  const { words, nested, viaXargs } = unwrap(seg);
  for (const n of nested) queue.push(...segments(n));
  if (!words.length) continue;
  const [head, ...rest] = words;
  if (/^(bash|sh|zsh|dash|ksh)(\.exe)?$/i.test(head)) {
    const c = rest.findIndex((a) => /^-[A-Za-z]*c[A-Za-z]*$/.test(a));
    if (c >= 0 && rest[c + 1] !== undefined) queue.push(...segments(rest[c + 1]));
  }
  if (/^(pwsh|powershell)(\.exe)?$/i.test(head)) {
    const c = rest.findIndex((a) => /^-(c|command|ec|encodedcommand)$/i.test(a));
    if (c >= 0 && /^-(ec|encodedcommand)$/i.test(rest[c])) deny('Không chạy PowerShell -EncodedCommand: hook không đọc được nội dung lệnh.');
    if (c >= 0) queue.push(...segments(rest.slice(c + 1).join(' ')));
  }
  if (/^cmd(\.exe)?$/i.test(head)) {
    const c = rest.findIndex((a) => /^\/[ck]$/i.test(a));
    if (c >= 0) queue.push(...segments(rest.slice(c + 1).join(' ')));
  }
  if (/^(eval|Invoke-Expression|iex)$/i.test(head)) queue.push(...segments(rest.join(' ')));
  if (viaXargs && (head === 'git' || head === 'gh')) deny('Không chạy git / gh qua xargs: hook không kiểm được đối số đến từ stdin.');
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
  // developer's real keys) all need a human. So does a glob that can expand to one.
  if (head !== 'git' && !SAFE_WITH_SECRET.test(head)) {
    if (operands.some(isSecret)) deny(SECRET_READ);
    const g = operands.find(secretGlob);
    if (g) deny(secretGlobReason(g));
  }
  if (INTERPRETERS.test(head) && rest.some(mentionsSecret)) deny(SECRET_READ);
  if (head === 'rm' && rest.some((w) => /^-\w*[rR]/.test(w)) && operands.some((w) => DANGER_RM.includes(w.replace(/\/+$/, '') || '/')))
    deny('Lệnh xóa đệ quy nhắm vào thư mục gốc, repo hoặc .git bị chặn.');

  if (head === 'gh') {
    // Inherited flags (-R / --repo, --hostname) may come before the subcommand.
    const g = [];
    for (let k = 0; k < rest.length; k += 1) {
      if (['-R', '--repo', '--hostname'].includes(rest[k])) k += 1;
      else if (!rest[k].startsWith('-')) g.push(rest[k]);
    }
    const apiMerge = g[0] === 'api' && rest.some((a) => /^\/?repos\/[^\s]+\/pulls\/\d+\/merge$/.test(a) || /^query=[\s\S]*\b(mergePullRequest|enablePullRequestAutoMerge)\b/.test(a));
    if ((g[0] === 'pr' && g[1] === 'merge') || apiMerge) deny('Agent không tự merge PR. Chủ repo merge sau khi duyệt (QUY-TRINH §1).');
  }
  if (head !== 'git') continue;

  // Resolve global options before the subcommand; several take a separate value (git -h).
  const GIT_GLOBAL_VALUE = new Set(['-C', '-c', '--git-dir', '--work-tree', '--namespace', '--super-prefix', '--config-env', '--attr-source']);
  let repo = dir;
  const repoOpts = []; // --git-dir / --work-tree, reused when asking git for the current branch
  let i = 0;
  while (i < rest.length && rest[i].startsWith('-')) {
    const opt = rest[i];
    if (opt === '-C' && rest[i + 1]) repo = path.resolve(dir, rest[i + 1]);
    if ((opt === '--git-dir' || opt === '--work-tree') && rest[i + 1]) repoOpts.push(`${opt}=${rest[i + 1]}`);
    if (/^--(git-dir|work-tree)=/.test(opt)) repoOpts.push(opt);
    i += GIT_GLOBAL_VALUE.has(opt) ? 2 : 1;
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

  // Paths named by the git command: operands, `<rev>:<path>` (git show HEAD:.env), and values of
  // path-bearing options (--output=.env would truncate the file). Message values after -m / -F
  // (also -am, --message=) are text, not paths.
  const PATH_VALUE_OPTS = new Set(['--output', '--output-directory', '-o']);
  const gitPaths = [];
  for (let k = 0; k < args.length; k += 1) {
    const a = args[k];
    if (/^(-[A-Za-z]*[mF]|--message|--file)$/.test(a)) k += 1;
    else if (/^--(message|file)=/.test(a)) continue;
    else if (PATH_VALUE_OPTS.has(a)) gitPaths.push(args[(k += 1)] ?? '');
    else if (/^--[^=]+=/.test(a)) gitPaths.push(a.slice(a.indexOf('=') + 1));
    else if (!a.startsWith('-')) gitPaths.push(a.slice(a.lastIndexOf(':') + 1));
  }
  if (!(GIT_SAFE_WITH_SECRET.has(sub) || (sub === 'rm' && args.includes('--cached')))) {
    if (gitPaths.some(isSecret)) deny(SECRET_READ);
    const g = gitPaths.find(secretGlob);
    if (g) deny(secretGlobReason(g));
  }

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
    const branch = pushesCurrent ? git(repo, [...repoOpts, 'rev-parse', '--abbrev-ref', 'HEAD']) : '';
    if (targetsMain || branch === 'main' || branch === 'master')
      deny('Không push lên main. Tạo nhánh feat/ fix/ docs/ chore/ rồi mở PR (QUY-TRINH §4).');
  }

  if (sub === 'commit') {
    let noVerify = false;
    let bulk = false;
    let edit = false;
    const sources = []; // { kind: 'message' | 'file' | 'reuse', value }
    const LONG = { '--message': 'message', '--file': 'file', '--reuse-message': 'reuse', '--reedit-message': 'reuse' };
    for (let k = 0; k < args.length; k += 1) {
      const a = args[k];
      const eq = a.indexOf('=');
      if (/^--reedit-message(=|$)/.test(a)) edit = true; // = -c: reuse a message, then open the editor
      if (LONG[a] && args[k + 1] !== undefined) sources.push({ kind: LONG[a], value: args[(k += 1)] });
      else if (eq > 0 && LONG[a.slice(0, eq)]) sources.push({ kind: LONG[a.slice(0, eq)], value: a.slice(eq + 1) });
      else if (a === '--all') bulk = true;
      else if (a === '--edit') edit = true;
      else if (/^--(fixup|squash)(=|$)/.test(a))
        deny('Không dùng git commit --fixup / --squash: PR được squash khi merge; viết commit mới theo Conventional Commits.');
      else if (/^-[A-Za-z]/.test(a)) {
        const s = commitShort(a);
        noVerify ||= s.noVerify;
        bulk ||= s.all;
        edit ||= s.edit;
        for (const v of s.values) sources.push({ kind: v.kind, value: v.value ?? args[(k += 1)] });
      }
    }
    if (noVerify) deny('Không bỏ qua hook git (git commit -n = --no-verify).');
    if (bulk) deny('Không dùng git commit -a / --all: stage từng đường dẫn rồi commit, tránh kéo theo thay đổi của việc khác.');
    if (edit) deny('Không dùng git commit -e / --edit: thông điệp phải cố định (-m với heredoc) để hook kiểm được.');

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
      const headMessage = git(repo, ['log', '-1', '--format=%B', 'HEAD']);
      if (headMessage) messages.push(headMessage);
    }
    if (!messages.length) deny('git commit cần thông điệp cố định (-m với heredoc) để hook kiểm được; không mở trình soạn thảo.');
    const full = messages.join('\n\n');
    const subject = (full.split('\n').find((l) => l.trim()) ?? '').trim();
    if (!CONVENTIONAL.test(subject))
      deny(`Tiêu đề commit phải theo Conventional Commits: "<type>(<scope>): <tóm tắt>", type ∈ feat|fix|docs|chore|refactor|perf|test|build|ci|style|revert|hotfix. Nhận được: "${subject.slice(0, 80)}".`);
    if (!/^Co-Authored-By: .+/m.test(full))
      deny('Commit do agent viết phải có trailer "Co-Authored-By: ..." ở cuối thân (QUY-TRINH §4). Viết thân commit bằng heredoc.');
  }
}

process.exit(0);
