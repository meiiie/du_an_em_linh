// PreToolUse hook registered by the `researcher` subagent: file writes only under labs/research/.
// argv[2] = project root (passed by the hook config); falls back to the payload cwd.
import { readFileSync } from 'node:fs';
import path from 'node:path';

const payload = JSON.parse(readFileSync(0, 'utf8') || '{}');
const projectRoot = process.argv[2] || payload.cwd || process.cwd();
const norm = (p) => {
  const s = path.resolve(p).replace(/\\/g, '/');
  return process.platform === 'win32' ? s.toLowerCase() : s;
};

const target = norm(path.resolve(payload.cwd ?? projectRoot, payload.tool_input?.file_path ?? ''));
const allowed = norm(path.join(projectRoot, 'labs', 'research'));

if (!target.startsWith(`${allowed}/`)) {
  process.stdout.write(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        permissionDecision: 'deny',
        permissionDecisionReason: 'Subagent researcher chỉ được ghi trong labs/research/. Trả kết quả về agent chính để ghi chỗ khác.',
      },
    }),
  );
}
process.exit(0);
