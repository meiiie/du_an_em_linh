# Cấu hình GitHub (chủ repo bật một lần)

Các mục này nằm trên dashboard, không nằm trong git. Vào **Settings** của `meiiie/du_an_em_linh`.

## General

- **Description:** `Nguyên mẫu NCKH — học Toán 12 (đơn điệu và cực trị) với gia sư AI.`
- **Website:** `https://hoc-toan-ai.onrender.com`
- **Topics:** `education`, `nckh`, `nextjs`, `fastapi`, `sympy`, `vietnamese`
- Tắt **Wikis** và **Projects** (không dùng).
- Giữ **Issues**. Chỉ **Allow squash merging** + xóa nhánh nguồn sau khi gộp (lịch sử tuyến tính — release-please đọc squash title).

## Security

- **Code security** → Enable **Private vulnerability reporting**
- Dependabot đã có `.github/dependabot.yml` — bật Dependabot alerts nếu GitHub hỏi

## Branches → `main`

- Require a pull request before merging
- Require status checks: workflow **Kiểm thử**
- Do not allow bypassing (trừ chủ repo khi cứu sự cố)

## Actions

- Workflow `Kiểm thử`, `Phát hành` (release-please), `Giữ thức Render` đã trong repo.
- **Allow GitHub Actions to create and approve pull requests** (Settings → Actions → General) — để release-please mở PR cắt bản.
- Biến tùy chọn: `KEEP_AWAKE_URL` (Settings → Secrets and variables → Actions → Variables)

Sau khi gộp PR vào `main`:

1. Render: nhánh deploy = `main`.
2. Tạo GitHub Release **v0.1.0** một lần (tag từ `main`). Các bản sau: gộp PR do workflow **Phát hành** mở.
3. Cron giữ thức tự chạy trên `main`.
