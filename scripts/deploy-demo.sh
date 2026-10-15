#!/usr/bin/env bash
# Triển khai/cập nhật máy chủ demo: kéo main, build, chạy, kiểm sức khỏe qua HTTPS. Không in bí mật.
# Dùng: bash scripts/deploy-demo.sh [--check]   (--check chỉ kiểm cấu hình và sức khỏe, không build)
set -euo pipefail
cd "$(dirname "$0")/.."
fail() { printf 'Lỗi: %s\n' "$1" >&2; exit 1; }
[[ -f .env ]] || fail 'Thiếu .env (chép từ .env.example rồi điền bí mật trên máy chủ).'
domain=$(grep -E '^MATHL_DOMAIN=' .env | cut -d= -f2- | tr -d '"' || true)
[[ -n $domain ]] || fail 'Thiếu MATHL_DOMAIN trong .env.'
compose=(docker compose -f compose.v2.yaml -f compose.demo.yaml)
"${compose[@]}" config -q || fail 'Cấu hình Compose không hợp lệ hoặc thiếu biến bắt buộc.'
if [[ ${1:-} != --check ]]; then
  git pull --ff-only
  BUILD_SHA=$(git rev-parse --short HEAD) "${compose[@]}" up -d --build --wait --wait-timeout 600
  docker image prune -f > /dev/null
fi
for i in $(seq 1 30); do
  if curl -fsS --max-time 10 "https://$domain/health" > /dev/null; then
    printf 'Sẵn sàng: https://%s (%s)\n' "$domain" "$(git rev-parse --short HEAD)"
    exit 0
  fi
  sleep 5
done
fail "https://$domain/health chưa trả 200 sau 150 giây; xem: ${compose[*]} logs --tail 100 core caddy"
