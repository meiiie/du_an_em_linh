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

# Nội dung chủ đề: core (profile dev) nhập từ /app/noi-dung của ảnh khi khởi động (T012a, T003b). Importer chạy sau khi
# liveness đã UP, nên chờ dòng log kết quả; nhập lỗi thì core vẫn chạy với CSDL rỗng, nên phải đỏ ở đây, không im lặng.
COMPOSE=(docker compose -f "$(dirname "$0")/../compose.v2.yaml")
for _ in $(seq 1 90); do
  log=$("${COMPOSE[@]}" logs --no-color core 2>&1 || true)
  grep -q 'Nhập nội dung chung' <<<"$log" && break
  sleep 2
done
if grep -q 'Nhập nội dung chung không xong' <<<"$log" || ! grep -q 'Nhập nội dung chung: ' <<<"$log"; then
  grep 'Nhập nội dung chung' <<<"$log" || echo "core chưa ghi dòng kết quả nhập nội dung sau 180 s"
  exit 1
fi
bai=$("${COMPOSE[@]}" exec -T db psql -U hoc_toan -d hoc_toan_core -tAc 'select count(*) from problems')
test "$bai" = 17 || { echo "core nhập $bai bài, cần 17 như v0"; exit 1; }
dat "core nhập nội dung chung từ ảnh: 17 bài như v0"
echo "khói v2: đạt"
