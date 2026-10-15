import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import test from 'node:test';

test('API qua nginx giữ SSE không đệm, không nén và chờ đủ lượt gia sư', () => {
  const conf = readFileSync(new URL('../apps/frontend/nginx/default.conf.template', import.meta.url), 'utf8');
  const api = conf.match(/location \^~ \/api\/ \{([^}]+)\}/)?.[1];
  assert.ok(api);
  for (const directive of ['proxy_buffering off;', 'proxy_cache off;', 'gzip off;', 'add_header X-Accel-Buffering no always;']) {
    assert.ok(api.includes(directive), directive);
  }
  assert.ok(Number(api.match(/proxy_read_timeout (\d+)s;/)?.[1]) >= 135);
  assert.ok(api.includes('proxy_set_header Accept-Encoding "";'));
});
