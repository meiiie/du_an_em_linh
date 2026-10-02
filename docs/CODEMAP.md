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
| `docs/DESIGN.md` | Token, lưới, nút, tâm quang học (46% / 3:2) |
| `docs/doi-chieu-thiet-ke.md` | Khớp / cố ý chưa làm |
| `docs/adr/` | Quyết định đã khóa |
| `docs/AI-HARNESS.md` | Nhà AI, loopback, không fallback; khóa OpenRouter / Z.AI |
| `docs/chi-so-o-bang.md` | `k` 0-based vs YAML 1-based |
| `data/supham/` | Ngân hàng sư phạm (JSON); `tai-lieu/`: tài liệu tự soạn của lab (bản vá `sp-tai-lieu-*`) |
| `data/v0/` | Hằng nội dung của v0 (khung 5 bước, BKT, 3 tài liệu, bảng công thức) chép nguyên văn từ `apps/web/scripts/seed.ts` cho importer v2; nguồn và cách sinh: `data/v0/NGUON.md` |
| `docker-compose.yml` | web :3000, math :8000, Postgres :5432 |
| `compose.v2.yaml` | v2: PostgreSQL 18 (chỉ trong mạng compose), core :8080 (profile `dev`), math :8000, frontend :4200 (nginx, `/api/` → core); chờ health từng dịch vụ |
| `apps/frontend/Dockerfile` + `nginx/default.conf.template` | Build Angular trên Node 24 → nginx không root; header bảo mật, cache dài cho tệp có hash, định tuyến phía client |
| `Dockerfile` / `render.yaml` | Deploy free một container trên Render |
| `CONTRIBUTING.md` | GitHub Flow: `main` + nhánh ngắn, không chồng PR |
| `CHANGELOG.md` / `docs/PHIEN-BAN.md` | SemVer + Keep a Changelog + release-please |
| `CODE_OF_CONDUCT.md` / `SUPPORT.md` | Community health (GitHub Insights) |
| `release-please-config.json` | Một sản phẩm, extra-files web + math + CITATION |
| `docs/TRIEN-KHAI.md` | Render, Neon, giữ thức |
| `docs/KIEM-THU.md` | Số liệu lần dựng nguyên mẫu |
| `.github/workflows/ci.yml` | job «Phân loại thay đổi» (`scripts/ci-thay-doi.mjs`, quét khóa, số phiên bản) chọn job theo đường dẫn: harness, core (Maven + image), frontend (build + Vitest), compose v2 (dựng cả hệ + `scripts/khoi-v2.sh` khi đổi core / frontend / v0), v0 (pytest, typecheck/lint/unit, Playwright e2e) |
| `.github/workflows/phat-hanh.yml` | release-please trên `main` |
| `.github/workflows/cd.yml` | Sau CI xanh → hook Render + ping trang chủ |
| `.github/workflows/giu-thuc.yml` | Cron 10 phút ping `/api/suc-khoe` (chỉ chạy trên `main`) |
| `.github/ISSUE_TEMPLATE/` / `PULL_REQUEST_TEMPLATE.md` | Mẫu issue / PR |
| `.github/dependabot.yml` | npm, pip, GitHub Actions |
| `.github/release.yml` | Nhóm ghi chú Release trên GitHub UI |
| `scripts/kiem-phien-ban.mjs` | Một SemVer trên root / web / math / CITATION |
| `docs/GITHUB.md` | Checklist Settings (mô tả, topic, bảo vệ `main`) |

## apps/web

Next.js 15. Server action nói chuyện với Postgres và `MATH_SERVICE_URL`.

| Đường | Việc |
| --- | --- |
| `app/page.tsx` | Trang chủ công khai (SEO, JSON-LD, không DB) |
| `app/not-found.tsx` | 404 noindex |
| `public/favicon.ico` + `icon.svg` + PNG | Logo tab ổn định (không đặt ICO trong `app/` — xung đột Next) |
| `public/llms.txt` / `public/.well-known/security.txt` | Máy AI / security.txt |
| `lib/site.ts` | URL / tên / mô tả chuẩn hóa |
| `app/sitemap.ts` / `robots.ts` / `manifest.ts` | SEO máy tìm kiếm |
| `app/dang-nhap/page.tsx` + `components/login-form.tsx` | Đăng nhập 2 bước (noindex), tham chiếu Neko Đoàn |
| `app/api/suc-khoe/route.ts` | GET JSON giữ thức Render — không đụng DB |
| `app/hs/` | Học (phiếu + sổ), đề bài, lịch, phiếu 5 bước |
| `app/gv/` | Lớp, duyệt, đề bài, tạo đề, tài liệu, công thức, gia sư, mức, cài lớp |
| `app/hs/kho` | Kho kiến thức lớp (cùng nguồn gia sư đọc) |
| `components/app-shell.tsx` | Ray mực 220 px / ngăn kéo |
| `components/phieu-viec-tiep.tsx` / `so-nav.tsx` | Bài tiếp theo + tab sổ `/hs` |
| `lib/de-hoc-sinh.ts` | KaTeX đề, tên kỹ năng ngắn, lời gợi |
| `components/solve-client.tsx` | Phiếu 5 bước |
| `components/tutor-panel.tsx` | Composer gia sư (44 px, Enter, nhà, liên kết kho) |
| `components/kho-theo-buoc.tsx` | Bản đồ gia sư đọc theo 5 bước |
| `components/ui/` | Nút, ô, tiêu đề, hàng việc |
| `lib/actions/` | `hs`, `gv`, `auth` |
| `lib/ai-catalog.ts` / `lib/ai-harness.ts` | Chọn nhà + một lần HTTP |
| `lib/kien-thuc.ts` / `lib/kho-lop.ts` | Truy hồi kho lớp, số [n], nhớ id lượt trước, không lời giải |
| `lib/openai-oauth.ts` | PKCE Sign in with ChatGPT (khi có client_id) |
| `lib/db/schema.ts` | Drizzle |
| `lib/tutor.ts` | Thang gợi ý; không đọc lời giải |
| `lib/learning.ts` | BKT + gợi bài |
| `scripts/migrate.ts`, `scripts/seed.ts` | Schema + dữ liệu thử |
| `tests/e2e/luong-hoc-sinh.spec.ts` | 3 ca Playwright |

## services/math

Xem `services/math/AGENTS.md`. HTTP mỏng; CAS trong sandbox.

## apps/frontend

Xem `apps/frontend/AGENTS.md`. Angular 22 zoneless; tính năng ở `src/app/features/`, nguyên thủy UI ở `src/app/shared/ui/`. Có `/dang-nhap` gọi `services/core`, khung `/hs`, `/gv` có guard vai trò; phiên ở `src/app/core/auth/` (#57).

## services/core

Xem `services/core/AGENTS.md`. Gói gốc `vn.hoctapcanman.core`; module nghiệp vụ là gói con ba tầng `domain` → `application` → `infrastructure`, ArchUnit kiểm. Module đầu tiên: `identity` (đăng nhập, làm mới, đăng xuất; JWT HS256 + refresh token lưu băm, gửi trình duyệt qua cookie HttpOnly; giới hạn đăng nhập sai F-10), Flyway `V1__identity.sql`; `classroom` (lớp, ghi danh, cài lớp, cảnh báo; cổng `ClassMembership` và `CanhBaoGiaoVien` cho module khác), Flyway `V3__classroom.sql`.

## Luồng chính

```
GV nạp tài liệu / công thức / bài
        → cổng 3 tầng (verify.py)
        → DA_PHAT_HANH | CHO_GIAO_VIEN_DUYET | BI_CHAN
HS làm 5 bước (solve-client → grader)
        → BKT (learning.ts)
        → gia sư (tutor + harness + leakfilter)
```

## Cổng hợp đồng

`POST` dịch vụ toán (xem `app/routers.py`): chấm bước, kiểm định, lọc, sinh biến thể, máy giải. Web không gọi SymPy trực tiếp.
