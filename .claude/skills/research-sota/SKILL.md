---
name: research-sota
description: Nghiên cứu SOTA có nguồn và ngày — đặt câu hỏi, tìm nguồn theo bậc tin cậy, đọc nguồn gốc, ghi vào labs/research với kết luận, độ tin, hệ quả thiết kế. Dùng khi cần biết hiện nay thế giới làm thế nào trước một quyết định, hoặc khi chủ repo yêu cầu tham khảo nghiên cứu, blog kỹ thuật, sách, văn bản luật.
---

# Nghiên cứu SOTA

Hôm nay: !`date +%Y-%m-%d`. Mọi kết luận ghi «tại ngày này».

1. **Câu hỏi:** một câu; nêu nó phục vụ năng lực `C*`, ADR hay issue nào.
2. **Tìm rộng:** 2–3 cách diễn đạt, cả tiếng Việt và tiếng Anh.
3. **Đọc nguồn gốc:** chỉ trích URL đã mở và đọc. Kết quả tìm kiếm chỉ là đầu mối.
4. **Bậc nguồn:** (1) văn bản luật, tài liệu chính thức, bài báo bình duyệt; (2) preprint, báo cáo kỹ thuật của nhà cung cấp; (3) blog kỹ thuật uy tín; (4) bài tổng hợp. Kết luận quan trọng cần ≥ 2 nguồn độc lập hoặc 1 nguồn bậc 1.
5. **Ghi** `labs/research/YYYY-MM-DD-<chủ-đề>.md` theo khung trong `labs/research/README.md`; cập nhật bảng chỉ mục.
6. **Tách** dữ kiện khỏi nhận định; ghi độ tin cao / trung bình / thấp; nêu điều chưa biết.
7. **Kết thúc** bằng «Hệ quả thiết kế» cụ thể, đề xuất issue hoặc ADR nếu cần.

## Không

- Không bịa số, tác giả, năm, URL. Không chắc → ghi «chưa xác minh».
- Không dùng trí nhớ mô hình cho phiên bản phần mềm, luật, giá, mô hình AI mới nhất — luôn tra.
- Nghiên cứu cần đọc ≥ 10 nguồn → giao subagent `researcher` chạy nền, phiên chính chỉ nhận tóm tắt.
