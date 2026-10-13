// SPA server for production output. API traffic must be intercepted by Playwright.
import { spawnSync } from 'node:child_process';
import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { resolve, extname, sep } from 'node:path';
const root = resolve(process.env.E2E_DIST_DIR ?? 'dist/frontend/browser');
try {
  await stat(resolve(root, 'index.html'));
} catch (error) {
  if (error.code !== 'ENOENT') throw error;
  console.log('Chưa có dist frontend; đang dựng Angular cho kiểm thử.');
  const build = spawnSync(process.execPath, ['node_modules/@angular/cli/bin/ng.js', 'build'], { stdio: 'inherit' });
  if (build.error) throw build.error;
  if (build.status !== 0) process.exit(build.status ?? 1);
  await stat(resolve(root, 'index.html'));
}
const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png', '.webp': 'image/webp', '.woff2': 'font/woff2', '.woff': 'font/woff', '.json': 'application/json' };
createServer(async (req, res) => {
  const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  if (pathname.startsWith('/api/')) { res.writeHead(501); res.end('API must be mocked'); return; }
  const file = resolve(root, '.' + pathname);
  if (file !== root && !file.startsWith(root + sep)) { res.writeHead(403); res.end(); return; }
  try {
    const target = (await stat(file).catch(() => null))?.isFile() ? file : resolve(root, 'index.html');
    res.writeHead(200, { 'Content-Type': types[extname(target)] ?? 'application/octet-stream', 'Cache-Control': 'no-store' });
    res.end(await readFile(target));
  } catch { res.writeHead(500); res.end(); }
}).listen(Number(process.env.E2E_PORT ?? 4300), '127.0.0.1');
