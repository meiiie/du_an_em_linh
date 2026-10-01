// Tests for the Claude Code hooks in this folder. Run: node --test .claude/hooks/*.test.mjs
import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
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
  g('-c', 'user.email=t@example.com', '-c', 'user.name=t', 'commit', '--allow-empty', '-m', 'chore: init', '-m', TRAILER);
  return dir;
}

const onMain = tempRepo('main');
const onFeature = tempRepo('feat/x');
writeFileSync(path.join(onFeature, 'msg-ok.txt'), `docs: thông điệp từ file\n\n${TRAILER}\n`);
writeFileSync(path.join(onFeature, 'msg-bad.txt'), 'WIP\n');
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
    // Review #52 (Codex P1, 2nd pass): copying onto .env would overwrite a developer's real keys.
    ['cp .env.example .env', 'deny', bash(`cp ${ENV}.example ${ENV}`)],
    ['printf > .env', 'deny', bash(`printf 'TOKEN=x' > ${ENV}`)],
    ['tee .env', 'deny', bash(`echo x | tee ${ENV}`)],
    ['mv onto .env', 'deny', bash(`mv tmp.txt ${ENV}`)],
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
    // Review #52 (Codex P2, 2nd pass): every message source is validated.
    ['commit -F valid file', 'allow', bash('git commit -F msg-ok.txt')],
    ['commit --file= invalid file', 'deny', bash('git commit --file=msg-bad.txt')],
    ['commit -F missing file', 'deny', bash('git commit -F khong-co.txt')],
    ['commit -F secret file', 'deny', bash(`git commit -F ${ENV}`)],
    ['commit -F - heredoc ok', 'allow', bash(`git commit -F - <<'EOF'\nchore(x): qua stdin\n\n${TRAILER}\nEOF`)],
    ['commit -F - heredoc bad', 'deny', bash(`git commit -F - <<'EOF'\nWIP\nEOF`)],
    ['commit --amend --no-edit (HEAD conforms)', 'allow', bash('git commit --amend --no-edit')],
    ['commit -C HEAD (reused message conforms)', 'allow', bash('git commit -C HEAD')],
    // Review #52 (Codex P2, 2nd pass): no bulk staging.
    ['git add -u (pathless)', 'deny', bash('git add -u')],
    ['git add -u with path', 'allow', bash('git add -u apps/web')],
    ['commit -a', 'deny', bash(`git commit -a -m "fix: x" -m "${TRAILER}"`)],
    ['commit --all', 'deny', bash(`git commit --all -m "fix: x" -m "${TRAILER}"`)],
    ['commit escaped quotes', 'allow', bash(`git commit -m "fix(gv): sửa \\"Đã xử lý\\"" -m "${TRAILER}"`)],
    ['PowerShell here-string ok', 'allow', ps(`git commit -m @'\nchore(harness): thêm hook\n\n${TRAILER}\n'@`)],
    ['PowerShell here-string bad', 'deny', ps(`git commit -m @'\nthêm hook\n'@`)],
    ['rm -rf .git', 'deny', bash('rm -rf .git')],
    ['rm -rf node_modules', 'allow', bash('rm -rf apps/web/node_modules')],
    ['Read normal file', 'allow', { tool_name: 'Read', tool_input: { file_path: path.join(REPO, 'README.md') }, cwd: REPO }],
    // Review #52 (Codex P1): Read / Grep tools are guarded too — hooks run in every permission mode.
    ['Read nested .env.local', 'deny', file('Read', `apps/web/${ENV}.local`)],
    ['Read .env.example', 'allow', file('Read', `${ENV}.example`)],
    ['Read .env above cwd', 'deny', { tool_name: 'Read', tool_input: { file_path: `../${ENV}` }, cwd: path.join(REPO, 'apps') }],
    ['Grep a secret file', 'deny', { tool_name: 'Grep', tool_input: { pattern: 'KEY', path: path.join(REPO, ENV) }, cwd: REPO }],
    ['Grep a directory', 'allow', { tool_name: 'Grep', tool_input: { pattern: 'KEY', path: path.join(REPO, 'apps/web') }, cwd: REPO }],
    // Review #52 (Codex P1): any command touching a secret path, not only a reader allowlist.
    ['sed reads .env', 'deny', bash(`sed -n '1p' ${ENV}`)],
    ['awk reads nested .env.local', 'deny', bash(`awk '{print}' apps/web/${ENV}.local`)],
    ['input redirect < .env', 'deny', bash(`cat < ${ENV}`)],
    ['attached redirect <.env', 'deny', bash(`wc -l <${ENV}`)],
    ['loop reading .env', 'deny', bash(`while read l; do echo "$l"; done < ${ENV}`)],
    ['python inline opens .env', 'deny', bash(`python -c "print(open('${ENV}').read())"`)],
    ['node inline process.env', 'allow', bash('node -e "console.log(process.env.HOME)"')],
    ['write to .env via echo', 'deny', bash(`echo X > ${ENV}`)],
    ['ls .env', 'allow', bash(`ls -la ${ENV}`)],
    ['test -f .env', 'allow', bash(`test -f ${ENV} && echo ok`)],
    ['cat .env.example', 'allow', bash(`cat ${ENV}.example`)],
    ['git add -f .env', 'deny', bash(`git add -f ${ENV}`)],
    ['git rm --cached .env', 'allow', bash(`git rm --cached ${ENV}`)],
    // Review #52 (Codex P2): attached short options.
    ['commit -m"WIP" attached', 'deny', bash('git commit -m"WIP"')],
    ['commit -m"..." attached + trailer', 'allow', bash(`git commit -m"docs: sửa lỗi chính tả" -m "${TRAILER}"`)],
    ['commit -am"..." is bulk staging', 'deny', bash(`git commit -am"docs: sửa lỗi chính tả" -m "${TRAILER}"`)],
    ['attached message with letter n is not -n', 'allow', bash(`git commit -m"fix: thêm nút" -m "${TRAILER}"`)],
    ['message token starting with -n', 'allow', bash(`git commit -m "-n không còn bị hiểu nhầm" -m "${TRAILER}"`.replace('"-n', '"fix: -n'))],
    ['commit -nm cluster', 'deny', bash(`git commit -nm "fix: x" -m "${TRAILER}"`)],
    ['push -uf cluster', 'deny', bash('git push -uf origin feat/x')],
    ['push -o option is not force', 'allow', bash('git push -ofoo origin feat/x')],
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
