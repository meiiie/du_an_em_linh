---
name: ship-check
description: Cổng kiểm trước khi push hoặc mở PR — xác định vùng đã đụng, chạy đúng bộ kiểm của vùng, quét khóa, rà phạm vi diff, gom bằng chứng cho thân PR. Dùng trước mọi `git push` / `gh pr create`, hoặc khi được hỏi «sẵn sàng PR chưa».
allowed-tools: Bash(git diff *) Bash(git status *) Bash(git log *) Bash(git rev-parse *) Bash(pnpm test:*) Bash(node --test *)
---

# Cổng trước PR

Thay đổi so với `origin/main`:
!`git diff --stat origin/main...HEAD 2>/dev/null | tail -25 || true`

Chưa commit:
!`git status --short || true`

## 1. Vùng → cổng

| Đã đụng | Chạy | Ghi vào PR |
| --- | --- | --- |
| `services/math/**` | `pnpm test:math`; đụng chấm / cổng / bộ lọc thì thêm bộ `kiemdinh/` liên quan (skill `math-engine`) | Số đạt, SHA |
| `apps/web/**` | `pnpm test:web`; e2e `pnpm --filter web test:e2e` nếu đụng UI, đăng nhập, phiếu, gia sư | Số đạt, ảnh 390 + 1280 |
| Vùng gia sư (`.claude/rules/tutor-safety.md`) | Thêm các bước của skill `tutor-safety` | Bảng ca dụ đáp án |
| `.claude/**` (hook, skill, rule, agent) | `node --test .claude/hooks/*.test.mjs`; skill / rule mới: kiểm frontmatter YAML hợp lệ | Số đạt |
| Migration SQL | Migrate trên CSDL trống và đã seed | Lệnh + kết quả |
| `docs/**`, `labs/**` | Link tương đối mở được; số liệu có nguồn | — |
| `services/core/**`, `apps/frontend/**` (v2) | Theo `AGENTS.md` của thư mục | Số đạt |

Luôn chạy: `pnpm test:khoa` (quét khóa trong git).

## 2. Rà diff

- Mỗi dòng đổi truy được về issue; không sửa lân cận.
- Không xóa `data-testid` hay heading e2e đã khóa: `git diff origin/main...HEAD | grep -E '^-.*data-testid'` phải rỗng hoặc có lý do trong issue.
- Chữ UI mới: tiếng Việt có dấu, đúng giọng người dùng (hiến chương V).
- Không `console.log` tạm, không `TODO` thiếu issue, không marker xung đột.
- Đổi hành vi, hợp đồng, quy ước → tài liệu liên quan đã sửa trong cùng PR.

## 3. Bằng chứng

Mỗi cổng ghi: lệnh, kết quả bằng số, `git rev-parse --short HEAD`. Không viết «đã test». Cổng nào không chạy được → ghi lý do và rủi ro.

## 4. Kết luận

Báo **SẴN SÀNG** hoặc **CHƯA** kèm danh sách cổng đỏ. Không push khi còn cổng đỏ chưa giải thích.
