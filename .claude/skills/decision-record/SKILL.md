---
name: decision-record
description: Lập phân tích quyết định có chấm điểm trong labs/decisions rồi viết ADR ngắn trong docs/adr. Dùng trước mọi lựa chọn khó đảo ngược — stack, dịch vụ, mô hình dữ liệu, nhà cung cấp AI hoặc OCR, thay đổi sư phạm diện rộng, hoặc điều gì chạm hiến chương.
---

# Quyết định → ADR

ADR hiện có: !`ls docs/adr`

1. **Phân tích** ở `labs/decisions/YYYY-MM-DD-<chủ-đề>.md` theo khung 10 mục của `labs/decisions/README.md`:
   - đặt tiêu chí loại và tiêu chí chấm có trọng số **trước** khi chấm;
   - ít nhất 3 phương án, gồm «giữ nguyên»;
   - kiểm độ nhạy; nêu điều làm quyết định này sai.
2. **Dữ kiện có nguồn:** đường dẫn mã, số đo (lệnh + SHA), ghi chú `labs/research/`.
3. **ADR** ở `docs/adr/<số kế tiếp, 3 chữ số>-<chủ-đề>.md`:

   ```markdown
   # ADR NNN — <Tiêu đề>

   **Trạng thái:** Đề xuất (YYYY-MM-DD) — chờ chủ repo duyệt.

   ## Bối cảnh
   <vì sao phải quyết; link phân tích>

   ## Quyết định
   - <gạch đầu dòng kiểm được>

   ## Hệ quả
   - <ADR bị thay thế, việc phải làm, rủi ro>
   ```

4. Cập nhật chỉ mục `labs/decisions/README.md`.
5. Không viết mã phụ thuộc quyết định trước khi chủ repo đổi trạng thái thành «Chấp nhận (ngày)».
6. ADR đã chấp nhận không sửa nội dung; muốn đổi → ADR mới ghi «Thay thế ADR NNN».
