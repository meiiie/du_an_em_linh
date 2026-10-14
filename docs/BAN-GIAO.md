# Bàn giao MathL+ v2

Tài liệu giúp người nhận chạy cục bộ và đi qua luồng học, dạy, quản trị bằng dữ liệu tổng hợp của **Trường THPT Nguyễn Trãi hư cấu**, năm học 2026–2027. Kiến trúc hiện hành là Angular (`apps/frontend`) → Spring Boot (`services/core`) → FastAPI/SymPy (`services/math`), dùng PostgreSQL 18. `apps/web` của v0 đã gỡ; snapshot `data/v0` chỉ giữ để đối chiếu/import.

## Chạy cục bộ

Cần PostgreSQL 18, JDK 25, Python 3.12, Node 24 và pnpm. Xem [README](../README.md#chạy-local) và [Triển khai](TRIEN-KHAI.md) cho cách chạy cả hệ bằng Docker.

1. Chạy PostgreSQL tại `localhost:5432`. Chuẩn bị CSDL `hoc_toan_core` với chủ sở hữu `hoc_toan`, theo `services/core/src/main/resources/application-dev.yaml`. Cấp mật khẩu CSDL qua biến môi trường `SPRING_DATASOURCE_PASSWORD` của tiến trình core; không ghi giá trị vào tài liệu, repo hoặc log.
2. Từ gốc repo, cài phụ thuộc:

   ```bash
   pnpm install
   python3 -m venv services/math/.venv
   services/math/.venv/bin/pip install -e "services/math[dev]"
   ```

3. Mở ba terminal. Chạy math trước core để seed có thể gọi cổng CAS:

   ```bash
   # Terminal 1, từ gốc repo:
   pnpm dev:math
   ```

   ```bash
   # Terminal 2, sau khi cấp MATHL_DEV_SEED_PASSWORD và mật khẩu CSDL:
   cd services/core
   JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   Đường dẫn JDK trên theo [SEED.md](SEED.md#bật-seed); nếu máy cài nơi khác, trỏ `JAVA_HOME` tới JDK 25 của máy. Profile `dev` chạy Flyway và tự nạp seed; không cần lệnh migrate/seed v0.

   ```bash
   # Terminal 3, từ gốc repo:
   pnpm --filter frontend dev   # ng serve
   ```

4. Mở http://localhost:4200. Frontend chuyển `/api/` tới core tại `:8080`; core gọi math tại `:8000`. Kiểm health ở http://localhost:8080/actuator/health và http://localhost:8000/health.

## Tài khoản dev

Danh sách theo vai trò và lớp nằm trong [SEED.md](SEED.md#danh-sách-lớp-và-tài-khoản). Mật khẩu do người chạy cấp qua **`MATHL_DEV_SEED_PASSWORD`**, chỉ dùng profile `dev`. Thiếu biến thì seed bỏ qua tạo tài khoản; chạy lại không đổi mật khẩu của tài khoản đã có. Khi cần demo mới, dùng CSDL/schema dev mới theo [hướng dẫn seed](SEED.md#tính-idempotent-và-dữ-liệu-đã-tồn-tại).

## Đi thử theo vai trò

Đăng xuất trước khi đổi vai trò. Chọn lớp **12A3** để thử đơn điệu và cực trị trong khung chấm đạo hàm hiện có.

- **Học sinh:** chọn tài khoản học sinh 12A3 trong SEED → **Học** → mở bài ôn hàm số để vào **Luyện**. Làm lần lượt tập xác định, đạo hàm, nghiệm, xét dấu, kết luận; chấm từng bước. Hỏi gia sư một gợi ý khi mắc lỗi: gia sư `offline` chỉ gợi ý, không đưa đáp án hay làm hộ bước. Mở bộ **“Kiểm tra hàm số — 45 phút”**: chế độ kiểm tra tắt gia sư, phía server cũng chặn gọi gia sư. Tên bộ không có nghĩa đã có đồng hồ thi; chỉ thử nộp khi còn hạn.
- **Giáo viên:** chọn Phạm Ngọc Lan hoặc Hoàng Minh Đức → **Tổng quan**, chọn 12A3 và xem cảnh báo, hạn bộ bài → **Duyệt**, xem bài chờ và căn cứ kiểm định, ghi lý do khi quyết định → **Giao bài**, chọn bài đã phát hành, học sinh, hạn và chế độ ôn/kiểm tra → **Mức**, xem bốn mức; bật **3 mức** để xem theo CV 7991. Bài chờ xem không tính là sai và không tăng số lượt BKT.
- **Quản trị:** chọn Nguyễn Thanh Tùng → **Người dùng** (`/qt`), xem/tạo người dùng tổng hợp đúng vai trò → **Lớp học** (`/qt/lop`), xem các lớp 10A1, 11A2, 12A3 và quản lý lớp, thành viên theo quyền.

## Cổng kiểm tra

Từ gốc repo, chạy các cổng tương ứng trước khi bàn giao mã v2; bộ e2e cần hệ đang chạy:

```bash
pnpm test:math
pnpm test:core                 # Maven verify + ArchUnit, cần JDK 25
pnpm test:frontend             # Angular build + Vitest
pnpm lint                     # quét khóa, FLAT, tương phản
node --test scripts/check-flat.test.mjs
pnpm --filter frontend e2e
```

Nếu chạy bằng Compose, thêm `bash scripts/khoi-v2.sh` để kiểm luồng cả hệ. Ghi lệnh, kết quả và SHA thực tế; số đo lịch sử trong [KIEM-THU.md](KIEM-THU.md) không chứng minh SHA hiện tại đã đạt. Kiểm seed và giới hạn xác minh CSDL được ghi riêng trong [SEED.md](SEED.md#kiểm-tra).

## Giới hạn đã biết

Phạm vi nghiệm thu lõi vẫn là đơn điệu và cực trị Toán 12. Seed có nội dung khối 10–12 để khảo sát; các dạng ngoài khung đạo hàm chưa được chấm tự động cho bài làm mới. Lịch sử giáo viên chấm tổng hợp không phải kết quả CAS. Gia sư mặc định `offline`, mở lời giải sau nộp mặc định tắt. Chế độ kiểm tra có hạn, chưa có engine thi, ma trận hay giám sát. Đây là demo dữ liệu tổng hợp, chưa chứng minh vận hành production.

Các mặc định và phần để sau: [ASSUMPTIONS.md](ASSUMPTIONS.md). Phạm vi dữ liệu, cổng toán và hạn seed: [SEED.md](SEED.md).

## Gia sư AI (GLM)

Gia sư v2 có adapter GLM qua Z.ai Coding Plan, giữ Spring AI autoconfig tắt. Cấp `GLM_API_KEY` ở môi trường core; mặc định `GLM_BASE_URL=https://api.z.ai/api/coding/paas/v4`, `GLM_MODEL=glm-5.3`, `GLM_TIMEOUT=45s` mỗi attempt, `GLM_FIRST_TOKEN_TIMEOUT=20s` cho token nội dung đầu tiên/idle, `GLM_MAX_TOKENS=700`. URL/model rỗng dùng mặc định; khóa rỗng thì lớp chọn GLM báo gia sư chưa sẵn sàng (không có câu soạn sẵn). Coding Plan chỉ dùng cho dev với dữ liệu tổng hợp; không ghi khóa vào mã, trình duyệt, image hay log.

Luật xin đáp án/đổi vai chạy trước nhà. Bản nháp đủ câu mới đi qua bộ lọc lộ đáp án và cổng công thức ADR 013; lỗi/chặn/hết giờ chuyển về thang cũng qua cổng, sau cùng là câu cố định không chứa toán. Retry đúng một lần cùng nhà với backoff 800 ms ±20% khi lỗi transport, HTTP 429/5xx hoặc stall trước token nội dung đầu tiên; không retry sau bất kỳ token nào, lỗi 4xx khác 429, hủy, hay phản hồi hỏng/tool-call. Timeout đóng stream của riêng attempt, không interrupt consumer hoặc hủy lượt; hủy trong backoff dừng ngay. Tổng lời gọi kể cả backoff bị chặn ở 110 giây trong deadline lượt 120 giây. SSE giữ `kho → goi → loc → xong|loi`; không có token chưa kiểm duyệt. Quyền lớp, chế độ kiểm tra và hạn được kiểm lại trước khi phát kết quả.

Giới hạn mặc định 12 lượt/học sinh/10 phút, đổi bằng `GLM_RATE_LIMIT_REQUESTS`/`GLM_RATE_LIMIT_WINDOW`; 429 trả trước SSE. Bộ đếm nằm trong tiến trình, không chia sẻ giữa replica và đặt lại khi khởi động. Log chỉ có băm SHA-256 rút gọn của id, mã bài, cấp, nhà, độ trễ và kết quả `DAT_MO_HINH`/`THANG`/`TU_CHOI`/`LOI`. Prompt che email, UUID, điện thoại và định danh có nhãn; không bảo đảm nhận ra mọi định danh viết tự do, nên vẫn giới hạn demo tổng hợp.

Chạy eval bằng Node 22 sau khi cấp `CORE_URL`, `MATHL_EVAL_EMAIL`, `MATHL_EVAL_PASSWORD`, `GLM_API_KEY` từ môi trường:

```bash
node scripts/eval-gia-su.mjs --out /tmp/mathl-gia-su-glm.json
node --test scripts/eval-gia-su.test.mjs
```

24 ca có ba mức, xin đáp án, đổi vai, chèn lệnh, lạc đề và bài kiểm tra cần 403. Nhịp mặc định 51 giây tránh chạm giới hạn, phiên đăng nhập được gia hạn; cấu hình giới hạn giống nhau ở core/script nếu muốn eval nhanh trong dev. Thiếu khóa thoát 0 với thông báo bỏ qua; thiếu bài phù hợp, SSE lỗi, lộ đáp án theo bộ dò hoặc quá ngưỡng trễ làm eval trượt. JSON không chứa lời chat hoặc thông tin đăng nhập. Chi tiết: [SEED.md](SEED.md#gia-sư-ai-glm), [bổ sung ADR 010](adr/010-sse-trang-thai-gia-su.md).

Kiểm chứng phiên hiện thực ngày 2026-10-14, SHA nền `bce4f874c9ec22940dc43954a6a6deeb1d750f63` (diff chưa commit):

```bash
# Từ services/core; JDK và Mockito agent có sẵn trong sandbox của phiên này.
JAVA_HOME=/home/box/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q -o test \
  -Dtest='*Tutor*,*GiaSu*,*Glm*,*Arch*,NullMarkedPackagesTest,WorkspaceApiTest,WorkspaceServiceTest,KienTrucRulesTuKiemTest' \
  -DargLine=-javaagent:/home/box/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar
# Từ gốc repo, dùng Node 22:
node scripts/eval-gia-su.test.mjs
node --test scripts/*.test.mjs
```

Kết quả: **112 JUnit đạt**, 0 lỗi/bỏ qua (gồm ArchUnit, tự kiểm luật và `@NullMarked`); **19 ca eval unit đạt**, toàn bộ **4 tệp test scripts đạt**. CLI không có khóa in đúng thông báo bỏ qua, exit 0; `git diff --check` đạt. Lệnh Maven không có `-javaagent` vướng Mockito tự gắn agent trong sandbox; quét khóa chuẩn vướng Node tạo `/bin/sh` với `EPERM`, nên dùng phép quét trực tiếp tương đương, thêm Java và tệp mới: **849 tệp văn bản**, không phát hiện chuỗi khóa. Chưa chạy GLM thật, CAS HTTP, PostgreSQL/Testcontainers hoặc Compose trong phiên này; không có số đo độ trễ/chi phí nhà thật. Không sửa `.github/` hay migration V1–V13, không thêm migration.

> Gia sư GLM chỉ chạy khi **lớp chọn nhà `glm`** (giáo viên: Cài đặt → Gia sư → «Gia sư AI (GLM)») **và** máy chủ có `GLM_API_KEY`. Lớp mới mặc định `offline`. Seed dev trường Nguyễn Trãi đặt sẵn `glm` cho ba lớp (dữ liệu hư cấu). Câu hỏi nghi có thông tin cá nhân (kể cả trong `$…$`/`\text{}`) không được gửi ra ngoài.


## Vận hành sau audit mục 9

Cấu hình mẫu đã bao phủ stack v2; Compose đồng bộ timeout GLM 45 giây và truyền biến gia sư, VAPID, soạn nháp. Image core không chép `data/dev`; demo dùng mount chỉ đọc. Khóa JWT hợp lệ tối thiểu 32 byte giải mã bắt buộc ngoài dev/test, thiếu hoặc sai sẽ dừng khởi động.

Script sao lưu custom PostgreSQL, kiểm thử và hướng dẫn cron, sao chép ngoài máy, phục hồi vào CSDL riêng nằm trong [TRIEN-KHAI.md](TRIEN-KHAI.md). HTTPS/reverse proxy, giám sát, đích offsite và diễn tập production vẫn chờ thông tin máy chủ; chưa cài hoặc nghiệm thu các phần đó. Chạy kiểm thử ops bằng `node --test ops/backup-postgres.test.mjs` và `./mvnw test -Dtest=JwtConfigTest` trong services/core. Profile `prod` bắt buộc `APP_IDENTITY_JWT_SECRET`; profile khác dùng khóa tạm và cảnh báo.


### Kiểm tra lane ops, 2026-10-15

SHA nền `a12b20d945e6f02815a2ae3b495267577d0df25b`; diff chưa commit.

- Từ `services/core`: `SPRING_PROFILES_ACTIVE=test JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -q -o -Dmaven.repo.local=/tmp/mathl-ops-m2 -Dtest=JwtConfigTest,AuthControllerTest,CleanArchitectureTest,DddArchitectureTest,NullMarkedPackagesTest -DargLine=-javaagent:/tmp/mathl-ops-m2/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test`: **47/47 đạt**, 0 lỗi/0 bỏ qua, exit 0. Gồm 16 JWT, 13 API xác thực và 18 kiến trúc/gói.
- Node 22.23.3: `node --test --experimental-test-isolation=none ops/backup-postgres.test.mjs`: **6/6 đạt**, exit 0. Kiểm chế độ PG/Compose, quyền tệp, giữ/xóa đúng bản, lỗi dump/mục lục, dry-run và cấu hình sai; dùng công cụ giả, chưa phục hồi archive thật.
- `shellcheck scripts/backup-postgres.sh`, `bash -n scripts/backup-postgres.sh`: exit 0.
- `docker compose --env-file /dev/null -f compose.v2.yaml config -q` và cùng lệnh với `docker-compose.yml`: exit 0, không đọc tệp bí mật. YAML Render/prod parse hợp lệ; đối chiếu 27 biến runtime tường minh trong core/math không thiếu biến ở `.env.example`. `node scripts/check-flat.mjs`: đạt; `git diff --check`: exit 0.

Lần thử toàn bộ Maven không đạt trong sandbox này: Docker socket trả `operation not permitted`, Testcontainers không khởi tạo được; các test dùng Mockito cần agent thay cho tự gắn. Bộ tập trung ở trên đã chạy với agent. Chưa build image, chạy stack khói thật, sao lưu/phục hồi PostgreSQL thật hoặc triển khai HTTPS/cron/offsite/giám sát. Không coi kiểm tra cấu hình và test dùng công cụ giả là nghiệm thu production.
