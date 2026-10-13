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
