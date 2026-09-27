# Bản đồ mã

Harness (Anthropic 2026): tệp này là mục lục để agent biết chỗ mở, không phải tài liệu sản phẩm.

## Gốc

| Đường | Việc |
| --- | --- |
| `AGENTS.md` | Nguồn chuẩn cho mọi agent |
| `CLAUDE.md` | Nhập AGENTS + ghi chú Claude Code |
| `.claude/settings.json` | `permissions.deny` bí mật |
| `.claude/rules/` | Luật theo đường dẫn |
| `.cursor/skills/` | Skill on-demand (FE, a11y, FastAPI) |
| `docs/DESIGN.md` | Token, lưới, nút, tâm quang học (46% / 3:2) |
| `docs/doi-chieu-thiet-ke.md` | Khớp / cố ý chưa làm |
| `docs/adr/` | Quyết định đã khóa |
| `docs/AI-HARNESS.md` | Nhà AI, loopback, không fallback |
| `docs/chi-so-o-bang.md` | `k` 0-based vs YAML 1-based |
| `data/supham/` | Ngân hàng sư phạm (JSON) |
| `docker-compose.yml` | web :3000, math :8000, Postgres :5432 |
| `Dockerfile` / `render.yaml` | Deploy free một container trên Render |
| `CONTRIBUTING.md` | GitHub Flow: `main` + nhánh ngắn, không chồng PR |
| `CHANGELOG.md` / `docs/PHIEN-BAN.md` | SemVer + Keep a Changelog + release-please |
| `CODE_OF_CONDUCT.md` / `SUPPORT.md` | Community health (GitHub Insights) |
| `release-please-config.json` | Một sản phẩm, extra-files web + math + CITATION |
| `docs/TRIEN-KHAI.md` | Render, Neon, giữ thức |
| `docs/KIEM-THU.md` | Số liệu lần dựng nguyên mẫu |
| `.github/workflows/ci.yml` | phiên bản + pytest + typecheck/lint/unit + Playwright e2e |
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
| `app/hs/` | Lộ trình, ngân bài, lịch, phiếu 5 bước |
| `app/gv/` | Tổng quan (trạng thái ChatGPT + kho), duyệt, ngân hàng, sinh bài, tài liệu, công thức, kết nối ChatGPT, tiến độ, cài đặt |
| `app/hs/kho` | Kho kiến thức lớp (cùng nguồn gia sư đọc) |
| `components/app-shell.tsx` | Ray mực 220 px / ngăn kéo |
| `components/solve-client.tsx` | Phiếu 5 bước |
| `components/tutor-panel.tsx` | Composer gia sư (44 px, Enter, nhà, liên kết kho) |
| `components/kho-theo-buoc.tsx` | Bản đồ gia sư đọc theo 5 bước |
| `components/ui/` | Nút, ô, tiêu đề, hàng việc |
| `lib/actions/` | `hs`, `gv`, `auth` |
| `lib/ai-catalog.ts` / `lib/ai-harness.ts` | Chọn nhà + một lần HTTP |
| `lib/kien-thuc.ts` / `lib/kho-lop.ts` | Truy hồi kho lớp, không lời giải |
| `lib/openai-oauth.ts` | PKCE Sign in with ChatGPT (khi có client_id) |
| `lib/db/schema.ts` | Drizzle |
| `lib/tutor.ts` | Thang gợi ý; không đọc lời giải |
| `lib/learning.ts` | BKT + gợi bài |
| `scripts/migrate.ts`, `scripts/seed.ts` | Schema + dữ liệu thử |
| `tests/e2e/luong-hoc-sinh.spec.ts` | 3 ca Playwright |

## services/math

Xem `services/math/AGENTS.md`. HTTP mỏng; CAS trong sandbox.

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
