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

Chủ repo cần kiểm tra cấu hình Blueprint, domain/`SITE_URL`, hạn mức và chi phí của tài khoản Render trước khi triển khai. Tài khoản demo/profile `dev` chỉ phục vụ nghiệm thu dữ liệu tổng hợp. Đưa hệ thống cho người dùng thật cần cấu hình profile và xác thực phù hợp, lưu trữ bền vững, backup và giám sát. Phiên này chỉ chuẩn bị cấu hình, không chứng minh production đã chạy.

Health của core là `/actuator/health`; math là `/health`; frontend phục vụ trang `/`. Không dùng endpoint v0 `/api/suc-khoe`. CD cần hook từng dịch vụ do chủ repo cấu hình; health HTTP 200 không thay thế kiểm luồng đăng nhập, giao bài và chấm bước. Xem [Render Blueprint specification](https://render.com/docs/blueprint-spec) để đối chiếu trường cấu hình hiện hành.

Không dán khóa AI vào Blueprint, git, log hoặc artifact. Gia sư mặc định `offline`; triển khai nhà chính thức chỉ sau khi có adapter, bảo quản khóa và cổng lọc được kiểm chứng. Các giới hạn phạm vi: [`ASSUMPTIONS.md`](ASSUMPTIONS.md).


## Nhắc học và đồng biên soạn (quyết định 2026-10-14)

Core đọc `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT` từ môi trường. Khóa là cặp P-256 dạng base64url (public 65 byte dạng uncompressed, private 32 byte), subject là địa chỉ `mailto:` hoặc HTTPS của bên vận hành. Khóa thiếu, sai định dạng hoặc không khớp thì UI dùng nhắc trong ứng dụng. Không lưu khóa vào repo. Service worker `/nhac-hoc-sw.js` chỉ hiện lời nhắc chung và mở `/hs/lich`; không cache API hoặc phiên đăng nhập. Máy chủ gửi Web Push không payload với VAPID ES256 theo [RFC 8030](https://www.rfc-editor.org/rfc/rfc8030.html) và [RFC 8292](https://www.rfc-editor.org/rfc/rfc8292.html). Chu kỳ mặc định 60 giây, timeout gửi 10 giây, bỏ subscription hết hiệu lực 404/410. Lịch sử gửi chống gửi lại cùng nhắc cho cùng subscription, lưu trong PostgreSQL. Endpoint chỉ nhận các host push trình duyệt được hỗ trợ; không cho URL tùy ý hoặc chuyển hướng.

Soạn nháp dùng `CONTENT_AI_API_KEY` và `CONTENT_AI_MODEL` (mặc định `glm-5.3`), gọi API chính thức Z.AI qua URL cố định. Khóa trống thì trả thông báo chưa sẵn sàng, vẫn nhập bài thủ công được. Chỉ tài liệu giáo viên có quyền dùng, tối đa 20.000 ký tự, được gửi; không gửi tên/tài khoản/bài làm học sinh. Dự án trả phí ban đầu; `ai_usage` ghi token nhà cung cấp trả về cho lượt nháp, kể cả nháp bị từ chối sau đó. Nhánh giải thích sau nộp tái dùng `HintModel` nếu có; cổng không trả số token nên lưu NULL và đếm lượt gọi, không bịa số đo. Chức năng thanh toán ngoài phạm vi. Không bật hoặc sửa cấu hình gia sư trong thay đổi này.

Flyway mới: `V16__confirmed_client_decisions.sql`; V14/V15 để lane khác sử dụng. Cần chạy verify trên máy có Docker để thực thi các test Testcontainers; sandbox không được Docker socket không chứng minh migration đã chạy trên PostgreSQL.
