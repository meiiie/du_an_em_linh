# Triển khai máy chủ demo cho khách

Một máy ảo Linux (khuyến nghị 2 vCPU, 4 GB RAM, 40 GB đĩa, Singapore hoặc Việt Nam), Docker Compose và Caddy tự cấp HTTPS. Dữ liệu là trường THPT Nguyễn Trãi tổng hợp (profile `dev`), chạy kèm profile `prod` để bắt buộc khóa JWT và cookie `Secure`. Không đưa mật khẩu nào vào repo.

## Bộ nhớ dự kiến

| Dịch vụ | Giới hạn | Ghi chú |
|---|---|---|
| core (Java 25) | 1 GB | `MaxRAMPercentage=75` → heap ≤ 768 MB; khởi động nhập seed và kiểm CAS vài chục giây |
| math (FastAPI + SymPy) | 1 GB | một tiến trình uvicorn; đỉnh CPU khi chấm/nhập seed |
| db (PostgreSQL 18) | 512 MB | dữ liệu demo vài chục MB |
| frontend (nginx tĩnh) | 256 MB | thực dùng < 30 MB |
| caddy | 128 MB | |
| **Tổng trần** | **≈ 2,9 GB** | 4 GB RAM để hệ điều hành và Docker còn dư; 2 GB không đủ |

GLM gọi từ core ra ngoài (HTTPS), băng thông nhỏ.

## Lần đầu

1. Tạo máy (Ubuntu 24.04), mở cổng 22, 80, 443. Cài Docker Engine và plugin Compose; tạo user không phải root, thêm vào nhóm `docker`; bật swap 2 GB.
2. DNS: bản ghi `A` của tên miền (ví dụ `mathl.<tên-miền>`) trỏ về IP máy. Nếu dùng Cloudflare, để **DNS only** (mây xám) cho lần cấp chứng chỉ đầu.
3. `git clone https://github.com/meiiie/du_an_em_linh && cd du_an_em_linh && cp .env.example .env && chmod 600 .env`, rồi điền:
   - `MATHL_DOMAIN`, `MATHL_ACME_EMAIL`
   - `POSTGRES_PASSWORD` (`openssl rand -base64 24`), `APP_IDENTITY_JWT_SECRET` (`openssl rand -base64 32`)
   - `MATHL_DEV_SEED_PASSWORD`: mật khẩu chung của tài khoản demo, chỉ gửi khách qua kênh riêng
   - `GLM_API_KEY` của khóa dành cho demo/khách (không dùng khóa Coding Plan cá nhân)
4. `bash scripts/deploy-demo.sh` → in `Sẵn sàng: https://…` khi `/health` qua HTTPS trả 200.

## Cập nhật

`bash scripts/deploy-demo.sh` (kéo `main`, build, chạy với `--wait`, kiểm HTTPS). Chỉ kiểm: `bash scripts/deploy-demo.sh --check`.

## Sao lưu

Cron hằng ngày bằng script có sẵn (chi tiết, phục hồi và diễn tập trong [TRIEN-KHAI.md](TRIEN-KHAI.md)):

```cron
15 2 * * * cd /home/deploy/du_an_em_linh && BACKUP_DIR=/var/backups/mathl BACKUP_RETENTION_DAYS=14 bash scripts/backup-postgres.sh >> /var/log/mathl-backup.log 2>&1
```

Chép thư mục sao lưu ra ngoài máy (rclone tới Google Drive/R2, hoặc snapshot của nhà cung cấp hằng tuần).

## Giám sát

- Theo dõi uptime miễn phí (UptimeRobot hoặc Better Stack) gọi `https://<tên-miền>/health` mỗi 5 phút, báo qua email.
- `docker compose -f compose.v2.yaml -f compose.demo.yaml ps` và `logs --tail 200 core` khi có cảnh báo.

## Trước buổi demo

- [ ] Hôm trước: `deploy-demo.sh --check` xanh; sao lưu thủ công một bản.
- [ ] 30 phút trước: đăng nhập lần lượt học sinh, giáo viên, quản trị để làm nóng JVM và cache; mở một bài luyện, hỏi gia sư một câu.
- [ ] Kiểm GLM: nếu gia sư lỗi hoặc chậm, giáo viên đổi nhà cung cấp lớp sang chế độ ngoại tuyến ở `/gv/cai-dat` (gợi ý cố định, không gọi AI) để buổi demo vẫn chạy.
- [ ] Tài khoản demo: `nam.nguyen.hoai@thpt-nguyentrai.edu.vn` (học sinh 12A3), `lan.pham.ngoc@…` (giáo viên), `tung.nguyen.thanh@…` (quản trị); mật khẩu từ `.env` gửi riêng.
- [ ] Mở sẵn trang theo dõi uptime; có bản sao lưu mới nhất.
