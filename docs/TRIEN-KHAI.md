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
