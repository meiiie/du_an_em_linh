# Lab Quyết định

**Sứ mệnh:** biến một lựa chọn khó đảo ngược thành phân tích có thể kiểm lại, trước khi ai đó viết mã. Kết quả là một ADR ngắn trong `docs/adr/`; phân tích đầy đủ ở lại đây.

## Khi nào cần

Đổi stack hoặc dịch vụ; đổi mô hình dữ liệu cốt lõi; chọn nhà cung cấp (AI, OCR, hạ tầng); quyết định sư phạm có tác động rộng (thang mức, cách chấm); mọi điều chạm hiến chương.

## Khung một phân tích

1. **Câu hỏi quyết định** — một câu.
2. **Dữ kiện** — có nguồn (mã, số đo, ghi chú nghiên cứu).
3. **Tiêu chí loại** — điều phương án bắt buộc phải đạt.
4. **Tiêu chí chấm** — có trọng số, tổng 100, đặt **trước** khi chấm.
5. **Phương án** — ít nhất 3, gồm cả «giữ nguyên».
6. **Chấm điểm 1–5** + lý do cho các điểm then chốt.
7. **Độ nhạy** — đổi trọng số nào thì kết quả đổi.
8. **Khuyến nghị** + quyết định con.
9. **Rủi ro và giảm thiểu.**
10. **Điều làm quyết định này sai** — tín hiệu để mở lại.

Sau đó viết ADR (bối cảnh · quyết định · hệ quả · trạng thái) bằng skill `decision-record`. ADR ở trạng thái «Đề xuất» cho tới khi chủ repo duyệt. ADR đã chấp nhận không viết lại; muốn đổi thì viết ADR mới thay thế.

## Chỉ mục

| Ngày | Phân tích | ADR | Trạng thái |
| --- | --- | --- | --- |
| 2026-10-01 | [Kiến trúc và stack cho v2](2026-10-01-kien-truc-v2.md) | [011](../../docs/adr/011-kien-truc-v2.md) | Chấp nhận |
| 2026-10-02 | [Quyền riêng tư cho dữ liệu học sinh](2026-10-02-quyen-rieng-tu-du-lieu-hoc-sinh.md) | [012](../../docs/adr/012-quyen-rieng-tu-du-lieu-hoc-sinh.md) | Đề xuất |
| 2026-10-05 | [Mô hình trường, năm học, lớp, người dùng cho THPT](2026-10-05-mo-hinh-truong-lop-thpt.md) | [015](../../docs/adr/015-mo-hinh-truong-lop-thpt.md) | Đề xuất |
