# Kế hoạch mở rộng nội dung — từ một chủ đề ra Toán 12

| | |
| --- | --- |
| Trạng thái | đề xuất |
| Câu hỏi | Mở rộng theo thứ tự nào để học sinh lớp 12 có giá trị sớm nhất cho kỳ thi tốt nghiệp? |
| Phụ thuộc | Q1 (phạm vi), Q9 (bảng công thức chung) trong `docs/product/MUC-TIEU.md`; bộ SGK khách dùng |

## Hiện trạng

Một chủ đề: ứng dụng đạo hàm để xét tính đơn điệu và cực trị (11 kỹ năng, 31 mã lỗi, 52 thang gợi ý, 23 bài). Gói dữ liệu và quy trình bàn giao Sư phạm → Kiểm định → Build đã chạy được: đó là khuôn để nhân bản.

## Các chương Toán 12 (CT GDPT 2018, theo bộ Kết nối tri thức — cần xác nhận bộ SGK của khách)

1. Ứng dụng đạo hàm để khảo sát và vẽ đồ thị của hàm số *(đã có một phần: đơn điệu, cực trị)*
2. Vectơ và hệ trục tọa độ trong không gian
3. Các số đặc trưng đo mức độ phân tán của mẫu số liệu ghép nhóm
4. Nguyên hàm và tích phân
5. Phương pháp tọa độ trong không gian
6. Xác suất có điều kiện

## Đề xuất thứ tự

| Thứ tự | Phần | Lý do |
| --- | --- | --- |
| 1 | Hoàn tất chương 1 (giá trị lớn nhất – nhỏ nhất, tiệm cận, khảo sát đồ thị) | Dùng lại kỹ năng đạo hàm và CAS đã có; chi phí thấp nhất |
| 2 | Chương 4 (nguyên hàm, tích phân) | Kiểm được hoàn toàn bằng CAS (đạo hàm ngược, tính tích phân) |
| 3 | Chương 2 + 5 (hình học tọa độ không gian) | CAS kiểm được bằng vectơ và phương trình; cần khung bước mới |
| 4 | Chương 3 + 6 (thống kê, xác suất) | Kiểm số học được; nhiều câu đúng/sai, trả lời ngắn |

Lab xác nhận lại thứ tự bằng **ma trận đề TN THPT 2025 và 2026 chính thức** (số câu, mức theo chương) trước khi chốt.

## Việc tiếp

1. Lập ma trận đề chính thức 2025, 2026 theo chương × phần × mức → `2026-10-xx-ma-tran-de-tn.md`.
2. Chốt thứ tự, mở issue `lab/pedagogy` cho từng gói chủ đề theo «Gói nội dung» ở README.
3. Thống nhất bảng công thức chung theo SGK (Q9) để cổng tầng 3 dùng cho mọi lớp.
