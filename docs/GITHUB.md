# Cấu hình GitHub (chủ repo bật một lần)

Các mục này nằm trên dashboard, không nằm trong git. Vào **Settings** của `meiiie/du_an_em_linh`.

## General

- **Description:** `Nguyên mẫu NCKH — học Toán 12 (đơn điệu và cực trị) với gia sư AI.`
- **Website:** `https://hoc-toan-ai.onrender.com`
- **Topics:** `education`, `nckh`, `nextjs`, `fastapi`, `sympy`, `vietnamese`
- Tắt **Wikis** và **Projects** (không dùng).
- Giữ **Issues**. Tắt **Allow merge commits** nếu muốn chỉ squash — khuyến nghị **Allow squash merging** + xóa nhánh nguồn sau khi gộp.

## Security

- **Code security** → Enable **Private vulnerability reporting**
- Dependabot đã có `.github/dependabot.yml` — bật Dependabot alerts nếu GitHub hỏi

## Branches → `main`

- Require a pull request before merging
- Require status checks: workflow **Kiểm thử**
- Do not allow bypassing (trừ chủ repo khi cứu sự cố)

## Actions

- Workflow `Kiểm thử` và `Giữ thức Render` đã trong repo.
- Biến tùy chọn: `KEEP_AWAKE_URL` (Settings → Secrets and variables → Actions → Variables)

Sau khi gộp PR vào `main`: trên Render đặt nhánh deploy = `main`; cron giữ thức sẽ tự chạy.
