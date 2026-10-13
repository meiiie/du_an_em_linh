import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runCheck } from './tuong-phan-token.mjs';
import test from 'node:test';

const css = readFileSync('apps/frontend/src/styles.css', 'utf8');
const design = readFileSync('docs/DESIGN.md', 'utf8');

function check(editCss = x => x, editDoc = x => x) {
  const output = [];
  const status = runCheck(editCss(css), editDoc(design), message => output.push(message));
  return { status, output: output.join('\n') };
}

test('the shipped palette meets AA in both themes and matches its documented mirror', () => {
  const result = check();
  assert.equal(result.status, 0, result.output);
  assert.match(result.output, /token đều được đo và khớp styles.css/);
});

test('measures actual CSS even if the documentation still lists a passing colour', () => {
  const result = check(x => x.replace('--muted: #475569;', '--muted: #cbd5e1;'));
  assert.equal(result.status, 1);
  assert.match(result.output, /TRƯỢT muted \/ canvas/);
  assert.match(result.output, /LỆCH CSS --muted/);
});

test('rejects white button text against the dark theme blue action', () => {
  const result = check(x => x.replace('--action-ink: #0b1220;', '--action-ink: #ffffff;'));
  assert.equal(result.status, 1);
  assert.match(result.output, /TRƯỢT action-ink \/ action/);
});

test('rejects a level colour with inadequate contrast', () => {
  const result = check(x => x.replace('--level-1: #0f766e;', '--level-1: #5eead4;'));
  assert.equal(result.status, 1);
  assert.match(result.output, /TRƯỢT level-1 \/ raise/);
});

test('rejects undocumented CSS colour tokens', () => {
  const result = check(x => x.replace(':root {', ':root {\n  --unmeasured: #123456;'));
  assert.equal(result.status, 1);
  assert.match(result.output, /THIẾU TÀI LIỆU token --unmeasured/);
});

test('rejects handwritten contrast results that can become stale', () => {
  const result = check(undefined, x => x.replace('Nền trang |', 'Nền trang 6,41 : 1 |'));
  assert.equal(result.status, 1);
  assert.match(result.output, /TỈ LỆ CHÉP TAY/);
});
