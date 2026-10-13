import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { scanSource, runCheck } from './check-flat.mjs';

test('rejects all visible box shadows, including inset and vendor spelling', () => {
  const issues = scanSource('.a {\n box-shadow: inset 0 0 0 1px red;\n -webkit-box-shadow: 0 2px 8px #000; }', 'a.css');
  assert.deepEqual(issues.map(i => i.line), [2, 3]);
  assert.ok(issues.every(i => i.rule === 'shadow'));
});

test('allows flat surfaces, outline focus and shadow reset', () => {
  assert.deepEqual(scanSource('.a { box-shadow: none !important; border: 1px solid var(--line); outline: 2px solid var(--focus); border-left: 0; border-inline-start: none; }', 'a.css'), []);
});

test('rejects left accent declarations and shadow variables', () => {
  const issues = scanSource(':root { --shadow-sm: none; } .a { border-left: 3px solid var(--accent); border-inline-start-color: red; color: var(--shadow-md); }', 'a.css');
  assert.deepEqual(issues.map(i => i.rule), ['shadow-token', 'edge-stripe', 'edge-stripe', 'shadow-token']);
});

test('extracts inline Angular styles and template utility classes', () => {
  const source = '// box-shadow: red\n@Component({ styles: [`\n .a { box-shadow: 0 0 3px red; }\n`], template: `<div class="hover:shadow-lg" style="border-inline-start: 4px solid red"></div>` })';
  const issues = scanSource(source, 'a.ts');
  assert.equal(issues.length, 3);
  assert.ok(issues.some(i => i.rule === 'shadow' && i.line === 3));
  assert.ok(issues.some(i => i.rule === 'shadow-utility'));
});

test('ignores comments, CSS content and unrelated TypeScript strings', () => {
  const css = '/* box-shadow: red; --shadow-sm: x */ .a { content: "box-shadow: red; shadow-lg"; background: url("shadow-icon.svg"); }';
  assert.deepEqual(scanSource(css, 'a.css'), []);
  assert.deepEqual(scanSource('const label = "box-shadow: red; shadow-lg"; const url = "https://host/box-shadow";', 'a.ts'), []);
  assert.deepEqual(scanSource('<!-- <div class="shadow-lg"> --> <p>shadow-lg</p>', 'a.html'), []);
});

test('checks Angular bound classes, ngClass and bound box-shadow', () => {
  const html = '<div [class.shadow-md]="open" [ngClass]="{\'shadow-lg\': open}" [style.box-shadow]="open ? \'0 2px 4px red\' : \'none\'"></div>';
  assert.equal(scanSource(html, 'a.html').length, 3);
  assert.deepEqual(scanSource('<div class="shadow-none" [style.box-shadow]="\'none\'"></div>', 'a.html'), []);
});

test('detects shadow utility in CSS apply and thin colored pseudo edge', () => {
  assert.equal(scanSource('.a { @apply shadow-sm; } .a::before { position:absolute; inset-inline-start:0; width:3px; height:100%; background:var(--accent); }', 'a.css').length, 2);
});

test('CLI recursively checks frontend source and fails with path and line', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'mathl-flat-'));
  try {
    await writeFile(join(dir, 'bad.css'), '.a {\n box-shadow: 1px 1px red;\n}');
    const output = [];
    assert.equal(await runCheck(dir, message => output.push(message)), 1);
    assert.match(output.join('\n'), /bad\.css:2/);
    await writeFile(join(dir, 'bad.css'), '.a { box-shadow: none; }');
    assert.equal(await runCheck(dir, () => {}), 0);
  } finally { await rm(dir, { recursive: true, force: true }); }
});
