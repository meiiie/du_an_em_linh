<!-- Tiêu đề PR: feat: / fix: / docs: / chore: / ci:  (Conventional Commits) -->

## Việc

<!-- Một ý. Nói rõ đã làm gì, không làm gì. -->

## Loại

- [ ] `feat` — tính năng trong phạm vi đã khóa
- [ ] `fix` — sửa lỗi
- [ ] `docs` — tài liệu / cấu hình GitHub
- [ ] `chore` — CI, phụ thuộc, dọn repo

## Kiểm

- [ ] `pnpm test:math` hoặc không đụng CAS / cổng / bộ lọc
- [ ] `pnpm test:web` (typecheck + lint + unit)
- [ ] E2E nếu đụng UI / đăng nhập / phiếu / gia sư
- [ ] Không đổi `data-testid` và heading e2e («Chào An», «Lớp 12A1 thử», `/3 mức/`, «Vào học»)
- [ ] Gia sư không đọc lời giải; không device-OAuth Codex
- [ ] Không commit `.env`, khóa, dữ liệu học sinh thật
- [ ] Nhánh từ `main`, PR vào `main` (không chồng PR)

## Ảnh / URL (nếu có UI)

<!-- Không bắt buộc. -->
