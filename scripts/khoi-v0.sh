#!/usr/bin/env bash
# Khói cho v0 đang chạy (docker compose up --build --wait): job migrate xong, seed có dữ liệu, web và math không chạy
# bằng root, đăng nhập bằng tài khoản thử, khóa nhà AI không lộ trong `docker inspect`.
# Dùng: bash scripts/khoi-v0.sh        (WEB_URL đổi được qua biến môi trường)
# Mọi phản hồi gán vào biến trước khi grep: `curl | grep -q` có thể làm curl chết vì SIGPIPE dưới pipefail.
set -euo pipefail
GOC=${WEB_URL:-http://127.0.0.1:3000}

dat() { printf '  ✓ %s\n' "$1"; }

test "$(docker compose ps -a --format '{{.ExitCode}}' migrate)" = 0
dat "job migrate (migration + seed) thoát 0"

so_bai=$(docker compose exec -T db psql -U hoc_toan -d hoc_toan -tAc "select count(*) from problems")
test "$so_bai" -gt 0
dat "seed có $so_bai bài"

for dv in web math; do
  uid=$(docker compose exec -T "$dv" id -u)
  test "$uid" != 0
  dat "$dv chạy bằng uid $uid (không root)"
done

env_web=$(docker inspect "$(docker compose ps -q web)" --format '{{range .Config.Env}}{{println .}}{{end}}')
if grep -q '^ZAI_API_KEY=' <<<"$env_web"; then
  echo "ZAI_API_KEY lộ trong docker inspect" >&2
  exit 1
fi
grep -q '^ZAI_API_KEY_FILE=/run/secrets/zai_api_key$' <<<"$env_web"
dat "khóa Z.AI chỉ đi qua Docker secret"

suc_khoe=$(curl -fsS "$GOC/api/suc-khoe")
test -n "$suc_khoe"
dat "/api/suc-khoe"

trang=$(curl -fsS "$GOC/dang-nhap")
grep -q 'Đăng nhập' <<<"$trang"
dat "/dang-nhap"

echo "khói v0: đạt"
