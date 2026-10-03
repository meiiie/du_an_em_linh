#!/bin/sh
# Bí mật đọc từ tệp theo quy ước <TÊN>_FILE (như image postgres chính thức): compose gắn Docker secret vào
# /run/secrets/…, script này nạp nội dung vào biến <TÊN> rồi chạy lệnh. Khóa vì vậy không nằm trong image, git,
# hay phần environment của `docker inspect`. Tệp trống hoặc không có thì bỏ qua: v0 chạy nhà `offline`.
set -eu

for ten in ZAI_API_KEY LLM_API_KEY OPENROUTER_API_KEY APP_ENC_KEY; do
  eval "tep=\${${ten}_FILE:-}"
  if [ -n "$tep" ] && [ -s "$tep" ]; then
    gia_tri=$(tr -d '\r\n' < "$tep")
    export "$ten=$gia_tri"
  fi
  unset "${ten}_FILE"
done

exec "$@"
