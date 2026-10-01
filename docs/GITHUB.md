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

## Branches → `main` (ruleset)

Kiểm ngày 2026-10-01: `main` **chưa có** branch protection hay ruleset; secret scanning và push protection đã bật. Hook trong `.claude/` chỉ là hàng rào chống nhầm phía máy dev — ranh giới thật phải nằm trên GitHub.

Ruleset sẵn trong repo: [`.github/rulesets/main-protection.json`](../.github/rulesets/main-protection.json) — chặn xóa nhánh, chặn force-push, lịch sử tuyến tính, bắt buộc PR, bắt buộc mọi luồng review đã resolve, bắt buộc 4 job CI xanh (Harness, Dịch vụ toán, Web, Playwright e2e) và nhánh cập nhật với `main`.

- **0 approval bắt buộc:** PR của agent mở dưới tài khoản chủ repo; GitHub không cho tự approve PR của mình.
- **Không bypass, kể cả admin:** agent dùng token của chủ repo, nên bypass cho admin cũng là bypass cho agent. Cứu sự cố: tạm tắt ruleset rồi bật lại.

Áp dụng (chủ repo, **sau khi** PR có job Harness đã merge, nếu không PR cũ sẽ chờ một check không bao giờ chạy):

```bash
gh api -X POST repos/meiiie/du_an_em_linh/rulesets --input .github/rulesets/main-protection.json
```

## Actions

- Workflow `Kiểm thử`, `Triển khai` (CD sau CI xanh), `Phát hành` (release-please), `Giữ thức Render` đã trong repo.
- **Allow GitHub Actions to create and approve pull requests** (Settings → Actions → General) — để release-please mở PR cắt bản.
- Biến tùy chọn: `KEEP_AWAKE_URL`. Secret bắt buộc để CD deploy: `RENDER_DEPLOY_HOOK` (Deploy Hook của service `hoc-toan-ai`).
- Render: Branch = **`main`**, autoDeploy bật.

Sau khi gộp PR vào `main`:

1. Render: nhánh deploy = `main` (bắt buộc — nhánh cũ đã xóa).
2. Tag **v0.1.0** đã có. Các bản sau: gộp PR do workflow **Phát hành** mở.
3. Cron giữ thức tự chạy trên `main`.
