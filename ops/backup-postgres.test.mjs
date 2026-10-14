import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, readdirSync, statSync, utimesSync, existsSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const script = resolve('scripts/backup-postgres.sh');
function fixture(t) {
  const root = mkdtempSync(join(tmpdir(), 'mathl-backup-test-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const bin = join(root, 'bin');
  const backups = join(root, 'backups');
  mkdirSync(bin);
  mkdirSync(backups);
  const trace = join(root, 'trace');
  const fake = `#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$0" "$@" >> "$TEST_TRACE"
case "$0" in
  */pg_dump)
    printf 'PGDMP-test\\n'
    exit "\${TEST_DUMP_EXIT:-0}" ;;
  */pg_restore)
    exit "\${TEST_RESTORE_EXIT:-0}" ;;
  */docker)
    if [[ "$*" == *pg_dump* ]]; then
      printf 'PGDMP-test\\n'
      exit "\${TEST_DUMP_EXIT:-0}"
    fi
    exit "\${TEST_RESTORE_EXIT:-0}" ;;
esac
`;
  for (const name of ['pg_dump', 'pg_restore', 'docker']) writeFileSync(join(bin, name), fake, { mode: 0o700 });
  const env = { PATH: `${bin}:${process.env.PATH}`, BACKUP_DIR: backups, BACKUP_MODE: 'pg',
    BACKUP_RETENTION_DAYS: '2', TEST_TRACE: trace };
  return { root, backups, trace, run: (extra = {}, args = []) => spawnSync('bash', [script, ...args], {
    env: { ...env, ...extra }, encoding: 'utf8',
  }) };
}

test('PG mode publishes private custom dump, removes only expired script archives', t => {
  const f = fixture(t);
  for (const name of ['mathl-old.dump', 'other.dump', 'mathl-incomplete.partial', 'mathl-recent.dump']) {
    writeFileSync(join(f.backups, name), 'old');
    if (name !== 'mathl-recent.dump') utimesSync(join(f.backups, name), new Date(0), new Date(0));
  }
  const result = f.run();
  assert.equal(result.status, 0, result.stderr);
  const files = readdirSync(f.backups);
  assert.ok(!files.includes('mathl-old.dump'));
  for (const name of ['other.dump', 'mathl-incomplete.partial', 'mathl-recent.dump']) assert.ok(files.includes(name));
  const archive = files.find(n => /^mathl-\d{8}T\d{6}Z-.*\.dump$/.test(n));
  assert.ok(archive);
  assert.equal(statSync(join(f.backups, archive)).mode & 0o777, 0o600);
  assert.match(readFileSync(join(f.backups, archive), 'utf8'), /^PGDMP/);
  assert.match(readFileSync(f.trace, 'utf8'), /--format=custom\n--no-password/);
  assert.match(readFileSync(f.trace, 'utf8'), /pg_restore\n--list/);
});

for (const failure of ['TEST_DUMP_EXIT', 'TEST_RESTORE_EXIT']) {
  test(`${failure}: no published partial archive or deletion of old backup`, t => {
    const f = fixture(t);
    writeFileSync(join(f.backups, 'mathl-old.dump'), 'old');
    utimesSync(join(f.backups, 'mathl-old.dump'), new Date(0), new Date(0));
    assert.notEqual(f.run({ [failure]: '1' }).status, 0);
    assert.deepEqual(readdirSync(f.backups), ['mathl-old.dump']);
  });
}

test('Compose mode supports chosen file/service and leaves credentials in container', t => {
  const f = fixture(t);
  const result = f.run({ BACKUP_MODE: 'compose', BACKUP_COMPOSE_FILE: '/tmp/test-compose.yaml',
    BACKUP_COMPOSE_SERVICE: 'postgres-test', PGPASSWORD: 'synthetic-password-for-test' });
  assert.equal(result.status, 0, result.stderr);
  const trace = readFileSync(f.trace, 'utf8');
  assert.match(trace, /compose\n-f\n\/tmp\/test-compose.yaml\nexec\n-T\npostgres-test/);
  assert.match(trace, /\$POSTGRES_PASSWORD/);
  assert.ok(!`${trace}${result.stdout}${result.stderr}`.includes('synthetic-password-for-test'));
});

test('dry run validates without creating directory or invoking tools', t => {
  const f = fixture(t);
  const target = join(f.root, 'absent');
  assert.equal(f.run({ BACKUP_DIR: target }, ['--dry-run']).status, 0);
  assert.ok(!existsSync(target));
  assert.ok(!existsSync(f.trace));
});

test('invalid retention/mode and unexpected arguments fail before side effects', t => {
  const f = fixture(t);
  for (const value of ['0', '-1', 'abc', '10000']) assert.notEqual(f.run({ BACKUP_RETENTION_DAYS: value }).status, 0);
  assert.notEqual(f.run({ BACKUP_MODE: 'unknown' }).status, 0);
  assert.notEqual(f.run({}, ['--dry-run', 'extra']).status, 0);
  assert.ok(!existsSync(f.trace));
  assert.deepEqual(readdirSync(f.backups), []);
});
