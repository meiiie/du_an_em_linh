---
name: retro
description: Hồi cứu cuối phiên — rút bài học từ phiên vừa làm và đề xuất sửa AGENTS.md, rule, skill, hook, bộ nhớ. Chỉ chạy khi chủ repo gõ /retro.
disable-model-invocation: true
---

# Hồi cứu cuối phiên

1. Liệt kê có bằng chứng: việc đã xong (commit, PR, issue), chỗ phải làm lại, lần bị hook chặn, lệnh sai, chỗ hiểu sai yêu cầu, chỗ chủ repo phải sửa lời agent.
2. Mỗi bài học đặt đúng chỗ:

   | Loại | Chỗ |
   | --- | --- |
   | Sai lặp ≥ 2 lần, đúng cho mọi phiên | `AGENTS.md` / `CLAUDE.md` — tối đa 1–2 dòng |
   | Chỉ một vùng mã | `.claude/rules/<vùng>.md` |
   | Quy trình nhiều bước | skill |
   | Phải luôn xảy ra hoặc không bao giờ được xảy ra | hook + ca kiểm thử trong `.claude/hooks/*.test.mjs` |
   | Bài học kỹ thuật cho người | `docs/` (mục bài học) |
   | Sở thích, bối cảnh của chủ repo | bộ nhớ tự động |

3. Trình bày đề xuất dạng diff ngắn. **Không tự sửa** — chờ chủ repo đồng ý.
4. Kiểm phình: `CLAUDE.md` + `AGENTS.md` gốc mỗi file dưới 200 dòng; đánh dấu chỉ dẫn đã lỗi thời (viết cho mô hình cũ, trỏ file không còn) để xóa.
