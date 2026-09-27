#!/usr/bin/env bash
# Khởi động dịch vụ toán (loopback) rồi web. Seed chỉ khi database trống.
set -euo pipefail

export NODE_ENV="${NODE_ENV:-production}"
export HOSTNAME="${HOSTNAME:-0.0.0.0}"
export PORT="${PORT:-3000}"
export MATH_SERVICE_URL="${MATH_SERVICE_URL:-http://127.0.0.1:8000}"
export APP_URL="${APP_URL:-${RENDER_EXTERNAL_URL:-http://127.0.0.1:${PORT}}}"
export SEED_IF_EMPTY="${SEED_IF_EMPTY:-1}"

cd /app/services/math
python3 -m uvicorn app.main:app --host 127.0.0.1 --port 8000 &

for _ in $(seq 1 60); do
  if curl -sf http://127.0.0.1:8000/health >/dev/null; then
    break
  fi
  sleep 1
done
curl -sf http://127.0.0.1:8000/health >/dev/null

python3 - <<'PY' &
import os
from http.server import BaseHTTPRequestHandler, HTTPServer

class H(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("content-type", "text/plain; charset=utf-8")
        self.end_headers()
        self.wfile.write("dang khoi dong".encode())

    def log_message(self, *_args):
        return

HTTPServer(("0.0.0.0", int(os.environ.get("PORT", "3000"))), H).serve_forever()
PY
BOOT_PID=$!

cd /app/apps/web
ok=0
for _ in $(seq 1 30); do
  if pnpm db:migrate; then
    ok=1
    break
  fi
  sleep 3
done
if [ "$ok" != 1 ]; then
  echo "Không kết nối được Postgres sau nhiều lần thử." >&2
  exit 1
fi

pnpm seed

kill "$BOOT_PID" 2>/dev/null || true
wait "$BOOT_PID" 2>/dev/null || true
sleep 1

exec pnpm start
