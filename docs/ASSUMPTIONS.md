# MathL+ — mặc định khi hoàn thiện ngày 2026-10-13

Chủ repo yêu cầu thực hiện liên tục, không hỏi lại, không commit/push. Các lựa chọn dưới đây giúp phạm vi có thể kiểm chứng; đây là quyết định của phiên, không phải xác nhận khách đã trả lời các câu hỏi mở. API hiện thực và giới hạn kỹ thuật ghi ở [`API_UX.md`](API_UX.md).

| Câu hỏi | Mặc định |
| --- | --- |
| Kiến trúc | Chỉ Angular + Spring Boot + dịch vụ toán; gỡ `apps/web`, giữ snapshot `data/v0` cho importer |
| Nội dung | Một chủ đề Toán 12: đơn điệu và cực trị; chỉ dùng dữ liệu sư phạm hiện có, không tự thêm lớp/chuyên đề hay sửa `data/supham`/kiểm định |
| Gia sư | Không đưa đáp án cuối, không làm hộ bước, không đọc lời giải chuẩn; chế độ kiểm tra khóa chat phía server |
| Nhà AI và chi phí | Mặc định offline. Không coi giao diện chọn nhà là tích hợp thật. ChatGPT/OpenRouter/Z.AI cần adapter API chính thức, bảo quản khóa và kiểm lọc đầy đủ trước khi bật; ngân sách token chưa được xác định |
| Mức hiểu | Bốn mức Nhận biết/Thông hiểu/Vận dụng/Vận dụng cao; ba mức CV 7991 chỉ khi giáo viên xem; không thêm thang Bloom sáu mức trên màn HS |
| Nhắc học | Trong ứng dụng; không tự gửi email, web push, Zalo hoặc thông báo bên ngoài |
| Flashcard/Quizlet | Lấy công thức hiện có, CSV để người dùng nhập thủ công; không quảng cáo đồng bộ API Quizlet hay xuất Word/PDF nếu chưa hiện thực |
| Kho nội dung | Theo lớp/quyền đang có; không tạo kho public hay đăng sách có bản quyền |
| Tính năng mở rộng | Không chat xã hội/bạn bè, OCR, ma trận đề hoặc khóa học online trong phiên này; cần yêu cầu và thiết kế riêng |
| Quản trị | Tài khoản/lớp thuộc phạm vi quyền của người thao tác; thao tác nhạy cảm phải kiểm quyền và lưu nhật ký |
| Dữ liệu nghiệm thu | Hoàn toàn tổng hợp; không dùng dữ liệu học sinh thật trong repo, issue, prompt hoặc ảnh nghiệm thu |
| Deploy | Chạy/kiểm demo nếu môi trường cho phép, chuẩn bị cấu hình; không triển khai production |
| Thiết kế | FLAT ưu tiên hơn hướng dẫn cũ: không bóng/inset/token shadow/viền nhấn trái/dải màu mép; focus bằng outline |

Nguồn đầu vào là phân tích yêu cầu và kế hoạch UX chỉ đọc ở `/workspace/du_an_em_linh-notes`. Không nhập lại báo cáo lịch sử thành tuyên bố tính năng đã hoạt động. Chỉ số kiểm thử phải gắn với lệnh và kết quả thực tế của phiên; mọi phần chưa chạy hoặc chưa tích hợp phải được báo rõ.

Thiết kế backend của phiên dùng thang gợi ý offline có sẵn, rồi gọi CAS lọc lộ đáp án và kiểm lời giảng trước khi trả; lỗi CAS phải đóng. Chế độ kiểm tra từ chối tutor kể cả gọi thẳng API. Kho công thức lấy phiên bản đã khóa; CSV thoát dữ liệu có thể thành công thức bảng tính. Đây là hành vi mã/hợp đồng cần được kiểm thử, không thay thế bằng chứng Maven/compose thực sự đã chạy. Import tài liệu hỗ trợ văn bản; không suy diễn hỗ trợ PDF scan/OCR từ nút tải lên.
