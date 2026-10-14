#!/usr/bin/env bash
# pg_dump custom; xem docs/TRIEN-KHAI.md để lên lịch, sao chép ngoài máy và phục hồi.
# Không đọc tệp bí mật, không đưa mật khẩu vào dòng lệnh hoặc log.
set +x
set -euo pipefail
umask 077

fail() { printf '%s\n' "$1" >&2; exit 1; }
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
backup_mode=${BACKUP_MODE:-compose}
backup_dir=${BACKUP_DIR:-/var/backups/mathl}
retention_days=${BACKUP_RETENTION_DAYS:-14}
compose_file=${BACKUP_COMPOSE_FILE:-$repo_dir/compose.v2.yaml}
compose_service=${BACKUP_COMPOSE_SERVICE:-db}
[[ $backup_mode == compose || $backup_mode == pg ]] || fail 'BACKUP_MODE phải là compose hoặc pg.'
[[ $retention_days =~ ^[1-9][0-9]{0,3}$ ]] || fail 'BACKUP_RETENTION_DAYS phải từ 1 đến 9999.'
[[ -n $backup_dir && $backup_dir != / ]] || fail 'BACKUP_DIR phải là thư mục sao lưu riêng.'
[[ $# -le 1 ]] || fail 'Dùng: backup-postgres.sh [--dry-run]'
case ${1:-} in
  --dry-run)
    printf 'Kiểm cấu hình: chế độ %s, giữ %s ngày; không tạo hoặc xóa tệp.\n' "$backup_mode" "$retention_days"
    exit 0 ;;
  '') [[ $# == 0 ]] || fail 'Dùng: backup-postgres.sh [--dry-run]' ;;
  *) fail 'Dùng: backup-postgres.sh [--dry-run]' ;;
esac

mkdir -p -- "$backup_dir"
backup_dir=$(cd -- "$backup_dir" && pwd)
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
# mktemp tránh ghi đè khi hai lượt chạy cùng giây; tệp chưa hoàn tất không mang đuôi .dump.
partial=$(mktemp "$backup_dir/mathl-$timestamp-XXXXXX.partial")
trap 'rm -f -- "$partial"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
compose=(docker compose -f "$compose_file")
if [[ $backup_mode == compose ]]; then
  # Biến POSTGRES_* được mở rộng bên trong container, không phải shell host.
  # shellcheck disable=SC2016
  "${compose[@]}" exec -T "$compose_service" sh -c '
    export PGPASSWORD="$POSTGRES_PASSWORD"
    exec pg_dump --format=custom --no-password --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"
  ' > "$partial"
  "${compose[@]}" exec -T "$compose_service" pg_restore --list < "$partial" > /dev/null
else
  # Libpq đọc PG* / PGPASSFILE; không truyền chuỗi kết nối chứa mật khẩu vào argv.
  pg_dump --format=custom --no-password > "$partial"
  pg_restore --list "$partial" > /dev/null
fi
[[ -s $partial ]] || fail 'Sao lưu rỗng; giữ nguyên các bản sao lưu cũ.'
mv -- "$partial" "${partial%.partial}.dump"
# Chỉ xóa bản của script, trong đúng thư mục này, sau khi dump và đọc mục lục thành công.
find "$backup_dir" -maxdepth 1 -type f -name 'mathl-*.dump' -mmin "+$((retention_days * 1440))" -delete
printf 'Sao lưu PostgreSQL hoàn tất; giữ %s ngày.\n' "$retention_days"
