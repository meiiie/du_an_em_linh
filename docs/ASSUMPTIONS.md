# MathL+ — quyết định phạm vi đã xác nhận

**Ngày xác nhận:** 2026-10-14. Các quyết định về lời giải, đồng biên soạn, tự học, nhắc học, chat và chi phí AI dưới đây đã được khách xác nhận. Chủ repo đã chốt gỡ v0, FLAT và cho Codex hiện thực trong phạm vi được giao, lead review. Phiên DOCS không commit/push/deploy. Hành vi cần kiểm theo [spec 002](../specs/002-hoan-thien-v2/spec.md), DTO theo [API_UX](API_UX.md); tài liệu không thay bằng chứng cổng.

| Mã | Nội dung | Quyết định | Giới hạn |
| --- | --- | --- | --- |
| A01 | Nghiệm thu gì? | Một vòng lõi thật cho một chủ đề Toán 12: đơn điệu và cực trị; Angular/core/math, giữ snapshot `data/v0` để import. | Khách chốt chủ đề/tính năng bổ sung; dữ liệu qua lab và bản vá nguyên văn. |
| A02 | Xem toàn bộ lời giải | Sau khi nộp đủ bài, học sinh bấm «Xem toàn bộ lời giải» và xác nhận. API nộp bài không tự trả lời giải. Kiểm tra chỉ mở sau hạn nộp; không có hạn thì tiếp tục khóa. Ghi sự kiện theo bài làm cho thống kê giáo viên và mức hiểu. | Gia sư khi đang làm vẫn chỉ gợi ý. Chỉ gọi `HintModel` sau cổng, bằng đề công khai, không gửi lời giải chuẩn; bản đủ năm bước qua bộ kiểm công thức mới được hiện. Không có mô hình hoặc không kiểm được thì dùng lời giải giáo viên đã lưu. Việc xem sau nộp không tăng lại điểm BKT. |
| A03 | Chi phí AI | Dự án trả chi phí AI giai đoạn đầu; ghi nhà cung cấp, mục đích, token đầu vào/đầu ra của lượt soạn nháp. Demo vẫn dùng gia sư `offline`. | Thanh toán, thu tiền học sinh, gói trả phí và cổng thanh toán nằm ngoài phạm vi. Chưa chốt ngân sách hay giá token; không suy diễn chi phí tiền từ số token. |
| A04 | Bloom hay 4 mức? | Lưu/hiện NB/TH/VD/VDC cho học sinh; Bloom chỉ metadata, không tạo thang sáu mức hay quy đổi một-một khi chưa có căn cứ. Ba mức CV 7991 chỉ khi giáo viên xem, không thay dữ liệu bốn mức. | Lab sư phạm xác nhận cách ánh xạ nếu khách yêu cầu. |
| A05 | Nhắc học | Web Push do học sinh chủ động bật trên từng thiết bị; khóa VAPID chỉ từ môi trường. Nhắc trong ứng dụng luôn là phương án dự phòng. Không mascot. | Thiết bị cần hỗ trợ Push API và HTTPS (localhost khi phát triển); không email, SMS hoặc Zalo. Không gửi tên học sinh hay nội dung bài ra thông báo ngoài ứng dụng. |
| A06 | SGK/sách tham khảo? | Chỉ tài liệu giáo viên tự soạn cho kho lớp; không sao chép/tải nguyên SGK hay sách có bản quyền, không kho public. `licenseStatus=tu_soan` là khai báo, không chứng nhận quyền của một sách. | Khách cung cấp quyền sử dụng và quy trình rà trước nhập nội dung khác. |
| A07 | Chat học sinh | Không xây chat học sinh hoặc mạng xã hội. | Gia sư theo bước và luồng giáo viên hiện có giữ phạm vi riêng. |
| A08 | Flashcard/Quizlet | Giữ thẻ công thức và CSV UTF-8, đặt dưới «Công cụ ôn thêm» trong kho. Không đưa lên điều hướng chính. | Nhập thủ công vào Quizlet; không đồng bộ hoặc API riêng. |
| A09 | Kiểm tra | Bài kiểm tra có hạn server chặn ghi. Lời giải chỉ mở sau hạn và sau khi đã nộp đủ bài. | Không engine thi, giám sát, auto-submit hoặc nhiều lượt thi. |
| A10 | Đồng biên soạn và tự học | «AI soạn nháp» lấy tài liệu giáo viên có quyền sử dụng trong lớp, tạo đề và lời giải; lưu vào hàng duyệt hiện có. Giáo viên xem, duyệt/chặn có lý do; bài vẫn qua bộ kiểm toán. Học sinh chọn chủ đề và bài tự học đã phát hành, nhận gợi ý theo mức hiểu. | Không tự phát hành nháp AI, không OCR, kho công khai hay tự sửa dữ liệu lab. Tự học không đổi chế độ hoặc hạn bài thầy cô đã giao. |
| A11 | Quản trị? | ADMIN tạo người dùng/lớp; SCHOOL_ADMIN chỉ phạm vi phân công; giáo viên lớp mình; ghi đè/duyệt/bulk có lý do/quyền/nhật ký. | Quyền mới cần test và hợp đồng phạm vi. |
| A12 | Dữ liệu/hạ tầng? | Dữ liệu hoàn toàn tổng hợp trong repo, prompt, log, ảnh. Không pilot thật hoặc production trong phiên này; không giả định JDK/Docker/cổng đã có. | Pilot thật theo hiến chương và ADR 012; ghi môi trường/kết quả đo thật. |
| A13 | Thiết kế và migration | FLAT, tiếng Việt đầy đủ dấu; chỉ thêm Flyway từ V16. | V14/V15 dành cho lane khác; không sửa migration cũ. |

## Nguồn và giới hạn xác minh

Quyết định không chat học sinh là lựa chọn sản phẩm đã xác nhận. Hồ sơ quyền riêng tư cho pilot thật tiếp tục theo ADR 012 và hiến chương; phiên này không xác nhận tuân thủ pháp luật hoặc cho phép dùng dữ liệu học sinh thật.

[Hướng dẫn nhập nội dung của Quizlet](https://help.quizlet.com/hc/en-us/articles/360029977151-Creating-sets-by-importing-content) mô tả nhập văn bản qua website, dùng dấu phẩy/tab giữa thuật ngữ và định nghĩa. Truy cập 2026-10-13, độ tin cao cho thao tác nhập thủ công. Nguồn này không chứng minh mọi API ghi đều không tồn tại; A08 là mặc định tích hợp không API write, không tuyên bố tuyệt đối về toàn bộ sản phẩm Quizlet. CSV có nháy/xuống dòng nên mở bằng bảng tính rồi sao chép hai cột khi nhập, không hứa Quizlet đọc trực tiếp tệp CSV tùy ý.

Mọi thay đổi quyết định phải ghi tác động vào spec/API/ADR và tiêu chí nghiệm thu, không tự thêm phạm vi theo báo cáo tham chiếu. Dự án chịu chi phí AI ban đầu; ngân sách cụ thể và mốc triển khai chưa được xác nhận.
