# Đóng góp

Repo dùng **GitHub Flow**. `main` luôn là bản có thể demo / deploy. Một việc → một nhánh → một pull request vào `main`.

Quy trình đầy đủ cho người, agent và lab: [`docs/QUY-TRINH.md`](docs/QUY-TRINH.md). Nguyên tắc không thương lượng: [`docs/HIEN-CHUONG.md`](docs/HIEN-CHUONG.md).

## Nhánh

| Nhánh | Việc |
| --- | --- |
| `main` | Phát hành. CI phải xanh. Render deploy từ đây sau khi gộp. |
| `feat/<ten-ngan>` | Tính năng |
| `fix/<ten-ngan>` | Sửa lỗi |
| `docs/<ten-ngan>` | Chỉ tài liệu / cấu hình GitHub |

Tên nhánh: chữ thường, gạch nối, một ý. Ví dụ: `feat/lich-hoc`, `fix/cookie-secure`.

**Không** mở PR chồng (nhánh A vào B vào C). **Không** đẩy thẳng lên `main`.

Các nhánh `cursor/*-91a8` cũ là chồng PR lúc dựng nguyên mẫu — đã gom vào `main` qua một PR. Đừng tạo thêm chồng đó.

## Phạm vi đã khóa

Đọc `AGENTS.md` trước khi mở rộng. Không làm:

- Thêm chủ đề lớp 10–12 ngoài đơn điệu / cực trị
- Device-OAuth ChatGPT / Codex, client_id nội bộ
- Gia sư đọc lời giải chuẩn
- Đổi `data-testid` và heading e2e («Chào An», «Lớp 12A1 thử», `/3 mức/`, «Vào học»)
- POST skill ra ngoài repo

## Lệnh

```bash
pnpm dev:math
pnpm dev:web
pnpm db:migrate && pnpm seed
pnpm test:math
pnpm test:web
pnpm --filter web test:e2e
```

Sửa UI → `apps/web`. Sửa CAS / cổng / bộ lọc → `services/math` (sandbox). Bản đồ: `docs/CODEMAP.md`.

## Commit / tiêu đề PR

[Conventional Commits](https://www.conventionalcommits.org/) (như Angular / repo Google). Squash merge lấy **một** tiêu đề:

- `feat:` tính năng → SemVer MINOR
- `fix:` sửa lỗi → PATCH
- `docs:` / `chore:` / `ci:` / `test:` không tăng số
- `feat!:` hoặc footer `BREAKING CHANGE:` → breaking

Chi tiết và release-please: [`docs/PHIEN-BAN.md`](docs/PHIEN-BAN.md).

## Pull request

1. Nhánh mới từ `main` đã kéo mới nhất.
2. Một ý, tiêu đề Conventional Commits, điền mẫu `.github/PULL_REQUEST_TEMPLATE.md`.
3. CI `.github/workflows/ci.yml` phải xanh (`pnpm test:version` cùng số SemVer).
4. Không dán khóa, `.env`, `zaiapikey.txt`, hay dữ liệu học sinh thật. CI `scripts/kiem-khoa.mjs` từ chối nếu git theo dõi file khóa.

## Báo lỗi / đề xuất

Dùng mẫu issue. Tìm issue trùng trước khi mở mới.
