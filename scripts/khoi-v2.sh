#!/usr/bin/env bash
# Khói cho hệ v2 đang chạy (docker compose -f compose.v2.yaml up --build --wait):
# trình duyệt → nginx của frontend → core → PostgreSQL, và math. Chỉ dùng tài khoản thử tổng hợp.
# Dùng: bash scripts/khoi-v2.sh        (FRONTEND_URL, MATH_URL đổi được qua biến môi trường)
# Mọi phản hồi gán vào biến trước khi grep: `curl | grep -q` có thể làm curl chết vì SIGPIPE dưới pipefail.
set -euo pipefail
GOC=${FRONTEND_URL:-http://127.0.0.1:4200}
MATH=${MATH_URL:-http://127.0.0.1:8000}

dat() { printf '  ✓ %s\n' "$1"; }

html=$(curl -fsS "$GOC/")
grep -q '<app-root' <<<"$html"
dat "/ trả ứng dụng Angular"
route=$(curl -fsS "$GOC/dang-nhap")
grep -q '<app-root' <<<"$route"
dat "/dang-nhap trả index.html (định tuyến phía client)"
headers=$(curl -fsSI "$GOC/")
grep -qi '^x-frame-options: DENY' <<<"$headers"
grep -qi '^x-content-type-options: nosniff' <<<"$headers"
dat "header bảo mật"
[[ $html =~ (main-[A-Za-z0-9_-]+\.js) ]]
asset=${BASH_REMATCH[1]}
asset_headers=$(curl -fsSI "$GOC/$asset")
grep -qi '^cache-control: .*immutable' <<<"$asset_headers"
dat "$asset cache dài"

body=$(curl -fsS -X POST "$GOC/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"hs.an@demo.local","password":"hocsinh123"}')
token=$(sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' <<<"$body")
test -n "$token"
me=$(curl -fsS "$GOC/api/me" -H "Authorization: Bearer $token")
grep -q '"role":"STUDENT"' <<<"$me"
dat "đăng nhập qua /api/ của frontend, /api/me trả đúng người"
test "$(curl -s -o /dev/null -w '%{http_code}' "$GOC/api/me")" = 401
dat "/api/me không token → 401"
curl -fsS -o /dev/null "$MATH/health"
dat "math /health"
echo "khói v2: đạt"
