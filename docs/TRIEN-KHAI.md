# Triển khai

**Hiện hành từ 2026-10-13:** frontend Angular, core Spring Boot, dịch vụ toán và PostgreSQL. `apps/web`, Dockerfile một-container v0 và `scripts/start-free.sh` đã gỡ. Các ghi chú Render v0 trong lịch sử Git không là hướng dẫn cho kiến trúc này. Phiên hoàn thiện không triển khai production.

## Demo bằng Docker

```bash
docker compose up --build --wait
bash scripts/khoi-v2.sh
pnpm --filter frontend e2e
```

Compose mặc định include `compose.v2.yaml`: frontend :4200, core :8080, math :8000. PostgreSQL 18 chỉ trong mạng nội bộ. Nginx chuyển `/api/` tới core; trình duyệt không gọi CAS trực tiếp. Profile `dev` nạp tài khoản và nội dung tổng hợp. Dừng bằng `docker compose down`; `down -v` xóa dữ liệu.

## Blueprint Render

`render.yaml` định nghĩa ba dịch vụ Docker riêng và CSDL `hoc_toan_core`. Các image build từ `apps/frontend/Dockerfile`, `services/core/Dockerfile`, `services/math/Dockerfile`; build context là gốc repo. Core gọi math bằng địa chỉ nội bộ; nginx gọi core bằng địa chỉ nội bộ.

Chủ repo cần kiểm tra cấu hình Blueprint, domain/`SITE_URL`, hạn mức và chi phí của tài khoản Render trước khi triển khai. Tài khoản demo/profile `dev` chỉ phục vụ nghiệm thu dữ liệu tổng hợp. Đưa hệ thống cho người dùng thật cần cấu hình profile và xác thực phù hợp, lưu trữ bền vững, sao lưu và giám sát theo các mục dưới đây. Phiên này chỉ chuẩn bị cấu hình, không chứng minh production đã chạy.

Health của core là `/actuator/health`; math là `/health`; frontend phục vụ trang `/`. Không dùng endpoint v0 `/api/suc-khoe`. CD cần hook từng dịch vụ do chủ repo cấu hình; health HTTP 200 không thay thế kiểm luồng đăng nhập, giao bài và chấm bước. Xem [Render Blueprint specification](https://render.com/docs/blueprint-spec) để đối chiếu trường cấu hình hiện hành.

Không dán khóa AI vào Blueprint, git, log hoặc artifact. Khóa gia sư (`GLM_API_KEY`) và soạn nháp (`CONTENT_AI_API_KEY`) độc lập, chỉ cấp cho core; kiểm nghiệm nhà và mô hình trước khi bật cho lớp. Các giới hạn phạm vi: [`ASSUMPTIONS.md`](ASSUMPTIONS.md).


## Nhắc học và đồng biên soạn (quyết định 2026-10-14)

Core đọc `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT` từ môi trường. Khóa là cặp P-256 dạng base64url (public 65 byte dạng uncompressed, private 32 byte), subject là địa chỉ `mailto:` hoặc HTTPS của bên vận hành. Khóa thiếu, sai định dạng hoặc không khớp thì UI dùng nhắc trong ứng dụng. Không lưu khóa vào repo. Service worker `/nhac-hoc-sw.js` chỉ hiện lời nhắc chung và mở `/hs/lich`; không cache API hoặc phiên đăng nhập. Máy chủ gửi Web Push không payload với VAPID ES256 theo [RFC 8030](https://www.rfc-editor.org/rfc/rfc8030.html) và [RFC 8292](https://www.rfc-editor.org/rfc/rfc8292.html). Chu kỳ mặc định 60 giây, timeout gửi 10 giây, bỏ subscription hết hiệu lực 404/410. Lịch sử gửi chống gửi lại cùng nhắc cho cùng subscription, lưu trong PostgreSQL. Endpoint chỉ nhận các host push trình duyệt được hỗ trợ; không cho URL tùy ý hoặc chuyển hướng.

Soạn nháp dùng `CONTENT_AI_API_KEY` và `CONTENT_AI_MODEL` (mặc định `glm-5.3`), gọi API chính thức Z.AI qua URL cố định. Khóa trống thì trả thông báo chưa sẵn sàng, vẫn nhập bài thủ công được. Chỉ tài liệu giáo viên có quyền dùng, tối đa 20.000 ký tự, được gửi; không gửi tên/tài khoản/bài làm học sinh. Dự án trả phí ban đầu; `ai_usage` ghi token nhà cung cấp trả về cho lượt nháp, kể cả nháp bị từ chối sau đó. Nhánh giải thích sau nộp tái dùng `HintModel` nếu có; cổng không trả số token nên lưu NULL và đếm lượt gọi, không bịa số đo. Chức năng thanh toán ngoài phạm vi. Không bật hoặc sửa cấu hình gia sư trong thay đổi này.

Flyway mới: `V16__confirmed_client_decisions.sql`; V14/V15 để lane khác sử dụng. Cần chạy verify trên máy có Docker để thực thi các test Testcontainers; sandbox không được Docker socket không chứng minh migration đã chạy trên PostgreSQL.


## Cấu hình dev và production

[`.env.example`](../.env.example) liệt kê biến runtime core, CAS, nginx/build và công cụ sao lưu; mọi khóa/mật khẩu đều trống. Maven không tự đọc tệp môi trường. Compose truyền biến host cho container; không đưa tệp cấu hình bí mật vào image. `LLM_*` của runtime v0 đã bỏ vì core v2 không đọc; `ZAI_*` chỉ còn cho công cụ thử riêng `ops/thu-zai.mjs`.

`compose.v2.yaml` là **demo**, cố định profile `dev`, mật khẩu CSDL cục bộ và mount `./data/dev:/app/noi-dung/dev:ro`; không dùng nguyên cấu hình đó cho người dùng thật. Image core chỉ chép `data/supham` và `data/v0`, không chứa `data/dev`. Script khói vẫn đọc tệp mật khẩu trong container core như [SEED.md](SEED.md).

Production đặt `SPRING_PROFILES_ACTIVE=prod`, cấp CSDL và mật khẩu riêng, không bật `dev` hay gắn `data/dev`. `application-prod.yaml` tắt nhập nội dung lúc khởi động và giữ cookie refresh `Secure`. Khi profile `prod` bật (kể cả cùng dev/test), core dừng khởi động với thông báo tiếng Việt nếu thiếu `APP_IDENTITY_JWT_SECRET`; khóa sai base64 hoặc dưới 32 byte bị từ chối ở mọi profile. Profile khác (mặc định, dev, test, staging) thiếu khóa thì dùng khóa tạm, phiên mất khi khởi động lại, nên mọi môi trường có người dùng thật, kể cả staging, phải bật `prod`. Tạo khóa bằng `openssl rand -base64 32`, lưu trong kho bí mật của bên vận hành; thay khóa sẽ làm access token cũ hết hiệu lực. Render Blueprint yêu cầu cấp khóa này, không tự sinh chuỗi chưa được kiểm định dạng. Profile `test` chỉ dùng khi chạy kiểm thử.

Các mặc định Compose khớp core: `GLM_TIMEOUT=45s`, `GLM_FIRST_TOKEN_TIMEOUT=20s`, `GLM_MAX_TOKENS=700`, `GIA_SU_TEMPERATURE=0.8`, `GIA_SU_TOP_P=0.9`, `GIA_SU_TOKEN_BUDGET=6000`; truyền cả giới hạn lượt GLM, khóa VAPID và cấu hình soạn nháp. Khóa API trống không cản khởi động. Khi thay biến môi trường, tạo lại container core. Cặp VAPID và subject chỉ cấp khi bật Web Push.

## Sao lưu PostgreSQL

[`scripts/backup-postgres.sh`](../scripts/backup-postgres.sh) dùng `pg_dump --format=custom`, tên UTC `mathl-YYYYMMDDTHHMMSSZ-XXXXXX.dump`, quyền tệp `0600`. Script kiểm mục lục với `pg_restore --list`, đổi tên tệp tạm sau thành công, rồi xóa các bản `mathl-*.dump` quá `BACKUP_RETENTION_DAYS` ngày (mặc định 14) ngay trong thư mục sao lưu. Dump lỗi hoặc mục lục lỗi thì không công bố tệp dở và không xóa bản cũ. `--dry-run` chỉ kiểm cấu hình, không gọi Docker/PostgreSQL, không tạo hoặc xóa tệp.

```bash
# Demo Compose: dùng pg_dump/pg_restore trong service db, không cần client trên host.
BACKUP_DIR=/tmp/mathl-backups bash scripts/backup-postgres.sh
# Máy chủ: cài client PostgreSQL 18, cấp PG* và xác thực qua môi trường hoặc PGPASSFILE.
BACKUP_MODE=pg BACKUP_DIR=/var/backups/mathl bash scripts/backup-postgres.sh
# Kiểm cấu hình trước khi lên lịch.
BACKUP_MODE=pg bash scripts/backup-postgres.sh --dry-run
```

Chế độ `compose` đọc `POSTGRES_USER`, `POSTGRES_DB`, `POSTGRES_PASSWORD` **bên trong** service; đổi tệp/service bằng `BACKUP_COMPOSE_FILE` và `BACKUP_COMPOSE_SERVICE`. Chế độ `pg` dùng libpq `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGSSLMODE` và xác thực `PGPASSWORD` hoặc `PGPASSFILE`; không truyền mật khẩu qua argv. Không bật `bash -x`. Cấp quyền đọc mọi bảng cần sao lưu; thư mục riêng phải ghi được bởi tài khoản chạy lịch. Dùng đường dẫn tuyệt đối cho cron và tệp Compose tùy chỉnh.

Ví dụ cron mỗi ngày lúc 02:15 theo múi giờ máy chủ (chọn lại giờ với bên vận hành):

```cron
# Cấp PG* / PGPASSFILE trong môi trường lịch; không ghi mật khẩu vào crontab.
15 2 * * * BACKUP_MODE=pg BACKUP_DIR=/var/backups/mathl BACKUP_RETENTION_DAYS=14 /opt/mathl/scripts/backup-postgres.sh >> /var/log/mathl-backup.log 2>&1
```

Sau mỗi lần thành công, tác vụ vận hành riêng sao chép dump qua SSH hoặc API kho đối tượng tới nơi **ngoài máy chủ** (kho mã hóa, tài khoản riêng, chính sách lưu giữ riêng). Kiểm kích thước/checksum sau sao chép trước khi ghi nhận thành công. Lịch sao chép, đích nhận, chủ thể nhận cảnh báo và ngân sách đang chờ thông tin máy chủ. Theo dõi exit code, dung lượng còn lại, tuổi bản dump gần nhất và việc sao chép; cảnh báo nếu quá 26 giờ chưa có bản mới hoặc sao chép thất bại. Bản trên cùng đĩa không bảo vệ được khi máy chủ mất. Dump một CSDL không gồm role toàn cụm, cấu hình máy chủ, bí mật hoặc tệp tải lên bên ngoài CSDL; bên vận hành phải bảo quản các phần đó riêng. Xem [pg_dump PostgreSQL 18](https://www.postgresql.org/docs/18/app-pgdump.html).

### Diễn tập phục hồi

Mỗi tháng và trước thay đổi schema lớn, lấy một bản từ kho ngoài máy, kiểm mục lục rồi phục hồi vào **CSDL mới, riêng biệt**. Không phục hồi đè hệ đang chạy. Ví dụ demo trên service db:

```bash
docker compose -f compose.v2.yaml exec -T db pg_restore --list < /var/backups/mathl/BAN_DA_CHON.dump > /dev/null
docker compose -f compose.v2.yaml exec -T db createdb -U hoc_toan mathl_restore_drill
docker compose -f compose.v2.yaml exec -T db pg_restore \
  --username=hoc_toan --dbname=mathl_restore_drill --no-owner --no-privileges \
  --single-transaction --exit-on-error < /var/backups/mathl/BAN_DA_CHON.dump
# Chế độ host: cấp PG* trỏ tới CSDL phục hồi mới, rồi:
pg_restore --no-owner --no-privileges --single-transaction --exit-on-error \
  --dbname=mathl_restore_drill /var/backups/mathl/BAN_DA_CHON.dump
```

Ghi thời gian phục hồi, phiên bản PostgreSQL, số bảng/bản ghi và `flyway_schema_history`; chạy core riêng với CSDL phục hồi, kiểm đăng nhập, giao bài, bài làm và chấm bước. Lưu kết quả diễn tập cùng thời điểm dump, không ghi dữ liệu/khóa vào báo cáo. `pg_restore --list` chỉ kiểm đọc archive, không chứng minh phục hồi thành công. Xem [pg_restore PostgreSQL 18](https://www.postgresql.org/docs/18/app-pgrestore.html). Chưa có số đo phục hồi production, RPO/RTO phải được bên vận hành chốt; lịch hằng ngày chỉ là cấu hình mẫu.

## HTTPS và giám sát — chờ thông tin máy chủ

Chưa cài reverse proxy hoặc chứng chỉ trong phiên này. Bên vận hành cần cung cấp tên miền, DNS trỏ về máy chủ, quyền truy cập, cổng 80/443 và chọn Caddy hoặc nginx để kết thúc TLS trước frontend. Cần chứng chỉ tin cậy, gia hạn tự động, chuyển HTTP sang HTTPS, `SITE_URL` đúng URL HTTPS, forwarding header và dải proxy mà core tin cậy; giữ core, math và PostgreSQL trong mạng riêng. Với Caddy, tên miền và khả năng truy cập cổng xác minh là điều kiện cho HTTPS tự động: [tài liệu Caddy](https://caddyserver.com/docs/automatic-https). Web Push và cookie refresh cần ngữ cảnh HTTPS khi ra Internet.

Giám sát chưa được triển khai: cần đầu mối nhận cảnh báo, kiểm uptime HTTPS, core readiness/liveness, math `/health`, lỗi 5xx, timeout GLM và tình trạng sao lưu/đĩa. Không công khai actuator ngoài health, không ghi khóa/token hoặc nội dung học sinh vào log. Việc cài proxy, chứng chỉ, lịch cron/offsite và hệ cảnh báo chờ thông tin máy chủ; chưa tuyên bố production đã vận hành.
