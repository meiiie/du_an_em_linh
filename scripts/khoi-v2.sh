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

# Runner seed chạy sau readiness; chờ lịch sử hoàn tất trước khi kiểm dữ liệu.
COMPOSE=(docker compose -f "$(dirname "$0")/../compose.v2.yaml")
psql_v2() { "${COMPOSE[@]}" exec -T db psql -U hoc_toan -d hoc_toan_core -tAc "$1"; }
for _ in $(seq 1 90); do
  log=$("${COMPOSE[@]}" logs --no-color core 2>&1 || true)
  grep -q 'Lịch sử trường hư cấu: ' <<<"$log" && break
  sleep 2
done
grep -q 'Lịch sử trường hư cấu: ' <<<"$log" || { echo "Seed Nguyễn Trãi chưa hoàn tất sau 180 s"; exit 1; }
expected=$(python3 -c 'import json,sys; print(len(json.load(open(sys.argv[1]))))' "$(dirname "$0")/../data/dev/nguyen-trai/bai-tap.json")
test "$(psql_v2 'select count(*) from problems')" = "$expected"
test "$(psql_v2 "select string_agg(name, ',' order by name) from classes where name in ('10A1','11A2','12A3')")" = '10A1,11A2,12A3'
for lop in 10A1 11A2 12A3; do
  test "$(psql_v2 "select count(*) from assignments a join classes c on c.id=a.class_id where c.name='$lop' and a.status='DA_GIAO'")" -gt 0
  test "$(psql_v2 "select count(*) from assignments a join classes c on c.id=a.class_id where c.name='$lop' and a.mode='KIEM_TRA'")" = 80
done
dat "Nguyễn Trãi: $expected bài, ba lớp 10A1/11A2/12A3 có giao bài và 8 bài kiểm tra mỗi học sinh"

# Không in mật khẩu/token, kể cả khi gọi bash -x. Core ghi tệp 0600 bằng UID 1001.
set +x
password=$("${COMPOSE[@]}" exec -T core cat "${MATHL_DEV_SEED_PASSWORD_FILE:-/tmp/mathl-dev-seed-password}")
test -n "$password"
dang_nhap() {
  local payload body
  payload=$(printf '%s' "$password" | python3 -c 'import json,sys; print(json.dumps({"email":sys.argv[1],"password":sys.stdin.read()}))' "$1")
  body=$(curl -fsS -X POST "$GOC/api/auth/login" -H 'Content-Type: application/json' --data-binary @- <<<"$payload")
  python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])' <<<"$body"
}
token=$(dang_nhap nam.nguyen.hoai@thpt-nguyentrai.edu.vn)
unset password
me=$(curl -fsS "$GOC/api/me" -H "Authorization: Bearer $token")
grep -q '"role":"STUDENT"' <<<"$me"
dat "đăng nhập học sinh Nguyễn Hoài Nam qua frontend"
test "$(curl -s -o /dev/null -w '%{http_code}' "$GOC/api/me")" = 401
curl -fsS -o /dev/null "$MATH/health"
dat "/api/me không token → 401; math /health đạt"

ds=$(curl -fsS "$GOC/api/hs/bai" -H "Authorization: Bearer $token")
python3 -c 'import json,sys; rows=json.load(sys.stdin); assert len(rows)>=10, len(rows); assert all(r["tenKyNang"] for r in rows)' <<<"$ds"
modes=$(curl -fsS "$GOC/api/hs/che-do" -H "Authorization: Bearer $token")
test_code=$(python3 -c 'import json,sys; rows=json.load(sys.stdin); tests=[r for r in rows if r["status"]=="KIEM_TRA"]; assert len(tests)==8, len(tests); print(tests[0]["id"])' <<<"$modes")
dat "Nam thấy bài được giao; endpoint chế độ trả 8 bài kiểm tra"

# Gia sư phải chặn bài kiểm tra; bài ôn đáp lại bằng luồng SSE offline.
request() { printf '{"maBai":"%s","maBuoc":"B.DH.TXD","cap":1,"cauHoi":"Em cần gợi ý bước đầu"}' "$1"; }
status=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$GOC/api/hs/gia-su" \
  -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$(request "$test_code")")
test "$status" = 403
on_tap=$(python3 -c 'import json,sys; print(next(r["maBai"] for r in json.load(sys.stdin) if r["maBai"].startswith("NT-LUYEN12-") and r["trangThai"]!="DA_NOP"))' <<<"$ds")
tutor=$(curl -fsS --max-time 30 -X POST "$GOC/api/hs/gia-su" \
  -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$(request "$on_tap")")
grep -Eq '^event:(xong|loi)' <<<"$tutor"
dat "gia sư ôn tập trả SSE; gia sư kiểm tra bị chặn 403"

csv=$(curl -fsS "$GOC/api/hs/flashcards/export.csv" -H "Authorization: Bearer $token")
grep -q 'thuật ngữ,định nghĩa' <<<"$csv"
test "$(wc -l <<<"$csv")" -gt 1
dat "flashcard CSV có tiêu đề và công thức"
echo "khói v2: đạt"
