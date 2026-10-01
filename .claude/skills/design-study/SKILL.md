---
name: design-study
description: Chạy một nghiên cứu thiết kế UI/UX trong labs/design — brief, tham chiếu sản phẩm thật, 2–3 phương án, nguyên mẫu HTML ở 390 và 1280 px, phê bình a11y / chữ Việt / công thức, đề xuất cập nhật docs/DESIGN.md. Dùng khi thiết kế hoặc thiết kế lại một màn, luồng, component, hoặc khi chủ repo gõ «lab design …».
---

# Nghiên cứu thiết kế

Quy trình và thước chất lượng: `labs/design/README.md`. Nguồn chuẩn hiện hành: `docs/DESIGN.md`.

1. **Brief** trong `labs/design/studies/YYYY-MM-DD-<chủ-đề>.md`: người dùng, việc cần làm, năng lực `C*`, ràng buộc, thước đo xong.
2. **Tham chiếu:** ≥ 3 sản phẩm đang chạy cho đúng bài toán → `labs/design/references/<sản-phẩm>.md` (lấy gì, không lấy gì). Không chép màu, logo, font độc quyền.
3. **Phương án:** 2–3 hướng khác nhau thật sự, mỗi hướng một câu nêu đánh đổi.
4. **Nguyên mẫu** `labs/design/prototypes/<chủ-đề>-<phương-án>.html`: một file, CSS nội tuyến, chữ Việt thật (không lorem), đủ trạng thái rỗng / đang chờ / lỗi / thành công, KaTeX nếu có công thức. Xem ở 390 px và 1280 px; chụp ảnh nếu có công cụ trình duyệt.
5. **Phê bình:** subagent `design-critic`; skill cấp người dùng nếu có (`better-interface`, `better-accessibility`, `better-typography`, `better-layout`, `better-writing`, `apple-design`). Kiểm WCAG 2.2 AA (tương phản, focus, mục tiêu chạm 44 px), dấu tiếng Việt không bị cắt, công thức dài không tràn.
6. **Quyết định:** ghi hướng chọn và lý do trong file nghiên cứu; đề xuất thay đổi `docs/DESIGN.md` bằng PR riêng, chủ repo duyệt.
