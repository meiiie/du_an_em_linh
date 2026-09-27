# Triển khai

## Render (free, không thẻ)

Chỗ free còn nhận Docker + Postgres. Hugging Face Docker Spaces (2026) đòi PRO.

Một service web: Next.js + FastAPI/SymPy trong cùng container — tránh hai service free ăn hết 750 giờ/tháng.

1. Bấm Deploy to Render trên README (cần tài khoản GitHub, không thẻ).
2. Blueprint `render.yaml` tạo web `hoc-toan-ai` + Postgres `hoc-toan-pg`.
3. Đợi URL `*.onrender.com`. Tài khoản thử trong README.

Bản đang chạy: https://hoc-toan-ai.onrender.com

**Nhánh deploy phải là `main`.** Service cũ nếu còn trỏ nhánh `cursor/*` đã xóa sẽ build hỏng hoặc treo — Dashboard → `hoc-toan-ai` → Settings → Branch = `main` → Manual Deploy.

CD: `.github/workflows/cd.yml` chạy sau khi CI **Kiểm thử** xanh trên `main`. Tùy chọn: secret `RENDER_DEPLOY_HOOK` (Render → service → Deploy Hook) để GitHub gọi deploy; không có hook thì vẫn ping https://hoc-toan-ai.onrender.com (autoDeploy nếu đã bật).

`/` là trang chủ tĩnh (SEO), không đọc database. `/dang-nhap` mới vào lớp thử.

## Postgres 30 ngày

Postgres free của Render hết hạn 30 ngày. Trước hạn: tạo [Neon](https://neon.tech) (free lâu, không thẻ) rồi dán `DATABASE_URL` vào service.

## Giữ thức

Render free tắt web sau ~15 phút không có HTTP. Ping mỗi 5–10 phút. Thức cả tháng ≈ 720/750 giờ free. Cron job của Render là gói trả phí.

- Chắc, làm ngay: [UptimeRobot](https://uptimerobot.com) HTTP(s) 5 phút → `https://hoc-toan-ai.onrender.com/api/suc-khoe` (hoặc `/dang-nhap`)
- Cron HTTP: [cron-job.org](https://cron-job.org)
- Trong repo: `.github/workflows/giu-thuc.yml` mỗi 10 phút — **chỉ tự chạy trên `main`**. Đổi URL: biến repo `KEEP_AWAKE_URL`.

`GET /api/suc-khoe` trả JSON, không đụng database.

## File

`render.yaml`, `Dockerfile`, `scripts/start-free.sh`.
