// Tests for the Claude Code hooks in this folder. Run: node --test .claude/hooks/*.test.mjs
import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { after, describe, test } from 'node:test';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO = path.resolve(HERE, '..', '..');
const ENV = '.e' + 'nv';
const TRAILER = 'Co-Authored-By: Claude <noreply@anthropic.com>';

function runHook(file, payload, args = []) {
  const r = spawnSync(process.execPath, [path.join(HERE, file), ...args], { input: JSON.stringify(payload), encoding: 'utf8' });
  assert.equal(r.status, 0, `hook exited ${r.status}: ${r.stderr}`);
  return r.stdout.includes('"deny"') ? 'deny' : 'allow';
}

function tempRepo(branch) {
  const dir = mkdtempSync(path.join(tmpdir(), 'hook-test-'));
  const g = (...a) => execFileSync('git', ['-C', dir, ...a], { stdio: 'ignore' });
  g('init', '-b', branch);
  g('-c', 'user.email=t@example.com', '-c', 'user.name=t', 'commit', '--allow-empty', '-m', 'init');
  return dir;
}

const onMain = tempRepo('main');
const onFeature = tempRepo('feat/x');
after(() => {
  rmSync(onMain, { recursive: true, force: true });
  rmSync(onFeature, { recursive: true, force: true });
});

const bash = (command, cwd = onFeature) => ({ tool_name: 'Bash', tool_input: { command }, cwd });
const ps = (command, cwd = onFeature) => ({ tool_name: 'PowerShell', tool_input: { command }, cwd });
const file = (tool_name, rel) => ({ tool_name, tool_input: { file_path: path.join(REPO, rel) }, cwd: REPO });
const heredocCommit = (msg) => `git add docs/a.md && git commit -m "$(cat <<'EOF'\n${msg}\nEOF\n)"`;

describe('guard.mjs', () => {
  const cases = [
    ['write .env', 'deny', file('Write', ENV)],
    ['edit nested .env.local', 'deny', file('Edit', `apps/web/${ENV}.local`)],
    ['edit .env.example', 'allow', file('Edit', `${ENV}.example`)],
    ['write private key', 'deny', file('Write', 'certs/server.pem')],
    ['edit kiemdinh result', 'deny', file('Edit', 'services/math/kiemdinh/ket-qua/ket-qua-tang1.json')],
    ['edit kiemdinh suite', 'allow', file('Edit', 'services/math/kiemdinh/bo-de-kiem-thu/cac-ca.yaml')],
    ['write doc', 'allow', file('Write', 'docs/QUY-TRINH.md')],
    ['cat .env', 'deny', bash(`cat ${ENV}`)],
    ['grep .env', 'deny', bash(`grep KEY apps/web/${ENV}`)],
    ['PowerShell Get-Content .env', 'deny', ps(`Get-Content "C:\\repo\\${ENV}"`)],
    ['cp .env.example .env', 'allow', bash(`cp ${ENV}.example ${ENV}`)],
    ['cp .env elsewhere', 'deny', bash(`cp ${ENV} /tmp/x.txt`)],
    ['cat README', 'allow', bash('cat README.md | head -20')],
    ['push origin main', 'deny', bash('git push origin main')],
    ['push HEAD:main', 'deny', bash('git push origin HEAD:main')],
    ['push feature branch', 'allow', bash('git push -u origin feat/x')],
    ['push --force', 'deny', bash('git push --force origin feat/x')],
    ['push -f', 'deny', bash('git push -f')],
    ['push +refspec', 'deny', bash('git push origin +feat/x')],
    ['push --force-with-lease', 'allow', bash('git push --force-with-lease origin feat/x')],
    ['push branch containing "main"', 'allow', bash('git push origin feat/main-fix')],
    ['bare push on feature branch', 'allow', bash('git push')],
    ['bare push on main', 'deny', bash('git push', onMain)],
    ['push origin HEAD on main', 'deny', bash('git push origin HEAD', onMain)],
    ['--no-verify', 'deny', bash(`git commit --no-verify -m "fix: x" -m "${TRAILER}"`)],
    ['commit -n', 'deny', bash(`git commit -n -m "fix: x" -m "${TRAILER}"`)],
    ['gh pr merge', 'deny', bash('gh pr merge 52 --squash')],
    ['gh pr create body mentions forbidden commands', 'allow', bash(`gh pr create --title "docs: x" --body "$(cat <<'EOF'\ngit push origin main; gh pr merge\nEOF\n)"`)],
    ['git add -A', 'deny', bash('git add -A')],
    ['git add .', 'deny', bash('git add .')],
    ['git add paths', 'allow', bash('git add docs/QUY-TRINH.md .claude/settings.json')],
    ['commit heredoc ok', 'allow', bash(heredocCommit(`docs(quy-trinh): thêm lab\n\nThân nhắc git push origin main không phải lệnh.\n\n${TRAILER}`))],
    ['commit heredoc no trailer', 'deny', bash(heredocCommit('docs: thêm lab'))],
    ['commit heredoc bad type', 'deny', bash(heredocCommit(`Cập nhật tài liệu\n\n${TRAILER}`))],
    ['commit scope with commas', 'allow', bash(heredocCommit(`fix(ux-03/04, ai-5): sửa bảng\n\n${TRAILER}`))],
    ['commit two -m', 'allow', bash(`git commit -m "feat(lab): them lab" -m "${TRAILER}"`)],
    ['commit one-line no trailer', 'deny', bash('git commit -m "fix: sua loi"')],
    ['commit -am WIP', 'deny', bash('git commit -am "WIP"')],
    ['commit -F', 'allow', bash('git commit -F .git/MSG')],
    ['commit --amend --no-edit', 'allow', bash('git commit --amend --no-edit')],
    ['commit escaped quotes', 'allow', bash(`git commit -m "fix(gv): sửa \\"Đã xử lý\\"" -m "${TRAILER}"`)],
    ['PowerShell here-string ok', 'allow', ps(`git commit -m @'\nchore(harness): thêm hook\n\n${TRAILER}\n'@`)],
    ['PowerShell here-string bad', 'deny', ps(`git commit -m @'\nthêm hook\n'@`)],
    ['rm -rf .git', 'deny', bash('rm -rf .git')],
    ['rm -rf node_modules', 'allow', bash('rm -rf apps/web/node_modules')],
    ['Read tool ignored', 'allow', { tool_name: 'Read', tool_input: { file_path: path.join(REPO, 'README.md') }, cwd: REPO }],
  ];
  for (const [name, expected, payload] of cases) {
    test(name, () => assert.equal(runHook('guard.mjs', payload), expected));
  }
});

describe('researcher-scope.mjs', () => {
  const write = (rel) => ({ tool_name: 'Write', tool_input: { file_path: path.join(REPO, rel) }, cwd: REPO });
  test('allows labs/research', () => assert.equal(runHook('researcher-scope.mjs', write('labs/research/2026-10-02-x.md'), [REPO]), 'allow'));
  test('denies docs', () => assert.equal(runHook('researcher-scope.mjs', write('docs/adr/012-x.md'), [REPO]), 'deny'));
  test('denies sibling prefix', () => assert.equal(runHook('researcher-scope.mjs', write('labs/research-old/x.md'), [REPO]), 'deny'));
});

describe('session-context.mjs', () => {
  test('prints repo state inside a git repo', () => {
    const r = spawnSync(process.execPath, [path.join(HERE, 'session-context.mjs')], { cwd: onFeature, encoding: 'utf8', timeout: 20000 });
    assert.equal(r.status, 0);
    assert.match(r.stdout, /Nhánh hiện tại: feat\/x/);
  });
});
