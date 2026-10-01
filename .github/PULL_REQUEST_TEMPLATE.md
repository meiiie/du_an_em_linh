<!-- Tiêu đề PR: Conventional Commits — feat: / fix: / docs: / chore: / ci: / refactor: / test: -->

## Việc

<!-- Một ý. Nói rõ đã làm gì, không làm gì. -->

Closes #

## Loại

- [ ] `feat` — tính năng trong phạm vi đã khóa
- [ ] `fix` — sửa lỗi
- [ ] `docs` — tài liệu / cấu hình GitHub
- [ ] `chore` — CI, phụ thuộc, harness, dọn repo

## Kiểm (số đo thật: lệnh + kết quả + SHA)

- [ ] `pnpm test:math` hoặc không đụng CAS / cổng / bộ lọc
- [ ] `pnpm test:web` (typecheck + lint + unit)
- [ ] E2E nếu đụng UI / đăng nhập / phiếu / gia sư — ảnh 390 px và 1280 px
- [ ] Bộ ca dụ đáp án (skill `tutor-safety`) nếu đụng luồng gia sư — 0 lộ
- [ ] `node --test .claude/hooks/*.test.mjs` nếu đụng `.claude/`
- [ ] `pnpm test:khoa` — không commit `.env`, khóa, dữ liệu học sinh thật
- [ ] Không đổi `data-testid` và heading e2e («Chào An», «Lớp 12A1 thử», `/3 mức/`, «Vào học»)
- [ ] Gia sư không đọc lời giải; không device-OAuth Codex
- [ ] Nhánh từ `main`, PR vào `main` (không chồng PR)

## Lab / ADR liên quan

<!-- Ghi chú lab, bản vá nguyên văn (mã), ADR. Không có thì xóa mục này. -->

## Ảnh / URL (nếu có UI)

<!-- Không bắt buộc. -->
