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

# Nội dung theo lớp (T012b): lớp «12A1 thử» của profile dev có 5 tài liệu, bảng 6 dòng đã khóa qua kiem-dong-cong-thuc,
# và trạng thái phát hành đúng tệp vàng của v0 (specs/001-lat-cat-doc/doi-chieu/v0-bai.json: 14 / 2 / 1).
for _ in $(seq 1 90); do
  log=$("${COMPOSE[@]}" logs --no-color core 2>&1 || true)
  grep -q 'Nhập nội dung cho lớp' <<<"$log" && break
  sleep 2
done
if ! grep -q 'Nhập nội dung cho lớp .*phát hành' <<<"$log"; then
  grep 'Nhập nội dung cho lớp' <<<"$log" || echo "core chưa ghi dòng kết quả nhập theo lớp sau 180 s"
  exit 1
fi
psql_v2() { "${COMPOSE[@]}" exec -T db psql -U hoc_toan -d hoc_toan_core -tAc "$1"; }
LOP="(select id from classes where name = '12A1 thử')"
test "$(psql_v2 "select count(*) from documents where class_id = $LOP")" = 5
test "$(psql_v2 "select count(*) from formula_sheets where class_id = $LOP and status = 'KHOA'")" = 1
phat_hanh=$(psql_v2 "select string_agg(status || '=' || n, ',' order by status) from (select status, count(*) n from problem_releases where class_id = $LOP group by status) s")
test "$phat_hanh" = "BI_CHAN=1,CHO_GIAO_VIEN_DUYET=2,DA_PHAT_HANH=14" || { echo "phát hành của lớp: $phat_hanh"; exit 1; }
dat "lớp «12A1 thử»: 5 tài liệu, bảng đã khóa, phát hành 14 / 2 / 1 như v0"

# Giao bài thử (T021): du-lieu-thu/giao-bai.sql chạy sau nhập theo lớp, giao 14 bài đang phát hành cho An, Bình, Chi.
for _ in $(seq 1 30); do
  log=$("${COMPOSE[@]}" logs --no-color core 2>&1 || true)
  grep -q 'Giao bài thử: ' <<<"$log" && break
  sleep 2
done
grep -q 'Giao bài thử: ' <<<"$log" || { echo "core chưa ghi dòng giao bài thử sau 60 s"; exit 1; }
giao=$(psql_v2 "select count(*) from assignments where class_id = $LOP and status = 'DA_GIAO'")
test "$giao" = 42 || { echo "lớp có $giao lượt giao, cần 42 (14 bài x 3 học sinh)"; exit 1; }
dat "giao bài thử bằng SQL: 42 lượt giao"

dang_nhap() {
  local body
  body=$(curl -fsS -X POST "$GOC/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"hocsinh123\"}")
  sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' <<<"$body"
}
an=$(dang_nhap hs.an@demo.local)
ds=$(curl -fsS "$GOC/api/hs/bai" -H "Authorization: Bearer $an")
so=$(grep -o '"maBai"' <<<"$ds" | wc -l)
test "$so" -eq 14 || { echo "An thấy $so bài, cần 14"; exit 1; }
grep -q '"tenKyNang":"' <<<"$ds"
dat "An thấy 14 bài được giao qua /api/hs/bai"

# Đường ghi qua dịch vụ toán thật, bằng Chi để danh sách của An giữ nguyên cho người thử giao diện. Gửi lại thì nhận lại
# đúng lần chấm đã ghi, nên chạy khói lần hai vẫn đạt.
chi=$(dang_nhap hs.chi@demo.local)
de=$(curl -fsS "$GOC/api/hs/bai/DH12-NB-01" -H "Authorization: Bearer $chi")
grep -q '"khaiBaoKetLuan":\["dong_bien","nghich_bien","cuc_dai","cuc_tieu"\]' <<<"$de"
grep -q '"ten":"Tập xác định"' <<<"$de"
buoc=$(curl -fsS -X POST "$GOC/api/hs/bai/DH12-NB-01/buoc" -H "Authorization: Bearer $chi" -H 'Content-Type: application/json' \
  -d '{"maBuoc":"B.DH.TXD","dong":[{"dong":0,"latex":"\\mathbb{R}"}]}')
grep -q '"ketQua":"DAT"' <<<"$buoc" || { echo "nộp bước TXD: $buoc"; exit 1; }
grep -q '"buocKe":"B.DH.DAOHAM"' <<<"$buoc"
ds_chi=$(curl -fsS "$GOC/api/hs/bai" -H "Authorization: Bearer $chi")
test "$(grep -o '"trangThai":"DANG_LAM","soBuocDat":1,"soBuoc":5' <<<"$ds_chi" | wc -l)" -eq 1 || { echo "danh sách của Chi: $ds_chi"; exit 1; }
test "$(curl -s -o /dev/null -w '%{http_code}' "$GOC/api/hs/bai/DH12-01-TH-01" -H "Authorization: Bearer $chi")" = 404
dat "Chi xem đề, nộp bước TXD qua dịch vụ toán thật (DAT), danh sách đổi sang đang làm; bài chờ duyệt 404"
echo "khói v2: đạt"
