# Bản đồ mã

Harness (Anthropic 2026): tệp này là mục lục để agent biết chỗ mở, không phải tài liệu sản phẩm.

## Gốc

| Đường | Việc |
| --- | --- |
| `AGENTS.md` | Nguồn chuẩn cho mọi agent |
| `CLAUDE.md` | Nhập AGENTS + ghi chú Claude Code |
| `docs/HIEN-CHUONG.md` | Hiến chương kỹ thuật: 8 nguyên tắc, cổng chất lượng — đứng trên mọi quy ước |
| `docs/QUY-TRINH.md` | Quy trình người + agent + lab: lệnh kích hoạt, chuẩn GitHub, kịch bản xử lý |
| `docs/product/MUC-TIEU.md` | Mục tiêu sản phẩm đọc từ sơ đồ khách: năng lực C1–C10, đối chiếu v0, câu hỏi mở |
| `docs/product/LO-TRINH.md` | Lộ trình v2 theo pha P0–P5, issue P1 |
| `services/core/` | v2 (ADR 011): Spring Boot 4.1, Java 25 — xem mục `services/core` |
| `apps/frontend/` | v2 (ADR 011): Angular 22 — xem mục `apps/frontend` |
| `labs/` | 5 lab (design, pedagogy, evals, research, decisions) — ghi chú có ngày, nâng lên `docs/` khi chốt |
| `.claude/settings.json` | `permissions.deny` (bí mật, lockfile) + hook `SessionStart`, `PreToolUse` |
| `.claude/hooks/` | `guard.mjs` (chặn thao tác cấm), `session-context.mjs` (trạng thái repo), `researcher-scope.mjs`; test `hooks.test.mjs` |
| `.claude/rules/` | Luật theo đường dẫn (toán, gia sư, migration, tài liệu, dữ liệu sư phạm, UI) |
| `.claude/skills/` | Skill dự án: `implement-issue`, `ship-check`, `lab`, `retro`, `tutor-safety`, `math-engine`, `math-pedagogy`, `research-sota`, `decision-record`, `design-study` |
| `.claude/agents/` | Subagent: `pedagogy-reviewer`, `math-verifier`, `privacy-reviewer`, `design-critic`, `researcher` |
| `.cursor/skills/` | Skill on-demand cho Cursor (FE, a11y, FastAPI) |
| `.coderabbit.yaml` | Review tự động theo vùng, bám hiến chương |
| `.github/labels.json` + `scripts/github/sync-labels.mjs` | Bộ nhãn ưu tiên / trạng thái / vùng / lab |
| `.github/rulesets/main-protection.json` | Ruleset bảo vệ `main` (ranh giới phía máy chủ) — áp dụng theo `docs/GITHUB.md` |
| `specs/002-hoan-thien-v2/` | Epic tiếp nối: U0–U15, API và nghiệm thu, task T100+; không là báo cáo đã hoàn thành |
| `docs/ASSUMPTIONS.md` | Mặc định phạm vi, giới hạn và việc chưa được yêu cầu |
| `scripts/check-flat.mjs` + `.test.mjs` | Cổng FLAT, quét source frontend và báo tệp/dòng |
| `docs/DESIGN.md` | Token, lưới, nút, tâm quang học (46% / 3:2) |
| `docs/doi-chieu-thiet-ke.md` | Khớp / cố ý chưa làm |
| `docs/adr/` | Quyết định đã khóa |
| `docs/AI-HARNESS.md` | Nhà AI, loopback, không fallback; khóa OpenRouter / Z.AI |
| `docs/chi-so-o-bang.md` | `k` 0-based vs YAML 1-based |
| `data/supham/` | Ngân hàng sư phạm (JSON); `tai-lieu/`: tài liệu tự soạn của lab (bản vá `sp-tai-lieu-*`) |
| `data/v0/` | Hằng nội dung của v0 (khung 5 bước, BKT, 3 tài liệu, bảng công thức) snapshot nguyên văn từ seed v0 đã gỡ, giữ cho importer core; nguồn và cách sinh: `data/v0/NGUON.md` |
| `docker-compose.yml` | Điểm vào mặc định, include `compose.v2.yaml`: frontend / core / math / PostgreSQL |
| `compose.v2.yaml` | v2: PostgreSQL 18 (chỉ trong mạng compose), core :8080 (profile `dev`: tài khoản thử, lớp «12A1 thử», nội dung, giao bài thử bằng SQL), math :8000, frontend :4200 (nginx, `/api/` → core); chờ health từng dịch vụ |
| `apps/frontend/Dockerfile` + `nginx/default.conf.template` | Build Angular trên Node 24 → nginx không root; header bảo mật, cache dài cho tệp có hash, định tuyến phía client |
| `render.yaml` | Blueprint frontend / core / math riêng biệt và PostgreSQL; chưa triển khai production trong phiên này |
| `CONTRIBUTING.md` | GitHub Flow: `main` + nhánh ngắn, không chồng PR |
| `CHANGELOG.md` / `docs/PHIEN-BAN.md` | SemVer + Keep a Changelog + release-please |
| `CODE_OF_CONDUCT.md` / `SUPPORT.md` | Community health (GitHub Insights) |
| `release-please-config.json` | Một sản phẩm, extra-files frontend + math + CITATION |
| `docs/TRIEN-KHAI.md` | Compose demo, Blueprint v2, giới hạn triển khai |
| `docs/KIEM-THU.md` | Số liệu lần dựng nguyên mẫu |
| `.github/workflows/ci.yml` | job «Phân loại thay đổi» (`scripts/ci-thay-doi.mjs`, quét khóa, số phiên bản) chọn job theo đường dẫn: harness, core (Maven + image), frontend (build + Vitest), compose v2 (dựng cả hệ + `scripts/khoi-v2.sh` khi đổi core / frontend / dữ liệu snapshot), math (pytest) |
| `.github/workflows/phat-hanh.yml` | release-please trên `main` |
| `.github/workflows/cd.yml` | CD khi chủ repo cấu hình hook frontend/core/math; xem giới hạn trong `docs/TRIEN-KHAI.md` |
| `.github/workflows/giu-thuc.yml` | Cron ping URL health được cấu hình (chỉ chạy trên `main`) |
| `.github/ISSUE_TEMPLATE/` / `PULL_REQUEST_TEMPLATE.md` | Mẫu issue / PR |
| `.github/dependabot.yml` | npm, pip, GitHub Actions |
| `.github/release.yml` | Nhóm ghi chú Release trên GitHub UI |
| `scripts/kiem-phien-ban.mjs` | Một SemVer trên root / frontend / math / core / CITATION |
| `docs/GITHUB.md` | Checklist Settings (mô tả, topic, bảo vệ `main`) |

## services/math

Xem `services/math/AGENTS.md`. HTTP mỏng; CAS trong sandbox.

## apps/frontend

Xem `apps/frontend/AGENTS.md`. Angular 22 zoneless; tính năng ở `src/app/features/`, nguyên thủy UI ở `src/app/shared/ui/`. Có `/dang-nhap` gọi `services/core`, khung theo vai trò với guard; hợp đồng UX mới ở `docs/API_UX.md`; phiên ở `src/app/core/auth/` (#57).

## services/core

Xem `services/core/AGENTS.md`. Gói gốc `vn.hoctapcanman.core`; module nghiệp vụ là gói con ba tầng `domain` → `application` → `infrastructure`, ArchUnit kiểm. Module đầu tiên: `identity` (đăng nhập, làm mới, đăng xuất; JWT HS256 + refresh token lưu băm, gửi trình duyệt qua cookie HttpOnly; giới hạn đăng nhập sai F-10), Flyway `V1__identity.sql`; `classroom` (lớp, ghi danh, cài lớp, cảnh báo; cổng `ClassMembership` và `CanhBaoGiaoVien` cho module khác), Flyway `V3__classroom.sql`.

## Luồng chính

```
GV nạp tài liệu / công thức / bài
        → cổng 3 tầng (verify.py)
        → DA_PHAT_HANH | CHO_GIAO_VIEN_DUYET | BI_CHAN
HS làm 5 bước (Angular → core practice → CAS)
        → BKT (core mastery)
        → gia sư (core tutor → lọc CAS)
```

## Cổng hợp đồng

`POST` dịch vụ toán (xem `app/routers.py`): chấm bước, kiểm định, lọc, sinh biến thể, máy giải. Frontend không gọi SymPy trực tiếp; core dùng client dịch vụ toán.
