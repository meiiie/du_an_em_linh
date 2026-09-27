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
| `docs/DESIGN.md` | Token, lưới, giải phẫu nút |
| `docs/doi-chieu-thiet-ke.md` | Khớp / cố ý chưa làm |
| `docs/adr/` | Quyết định đã khóa |
| `docs/AI-HARNESS.md` | Nhà AI, loopback, không fallback |
| `docs/chi-so-o-bang.md` | `k` 0-based vs YAML 1-based |
| `data/supham/` | Ngân hàng sư phạm (JSON) |
| `docker-compose.yml` | web :3000, math :8000, Postgres :5432 |

## apps/web

Next.js 15. Server action nói chuyện với Postgres và `MATH_SERVICE_URL`.

| Đường | Việc |
| --- | --- |
| `app/dang-nhap/page.tsx` | Vào lớp thử |
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
