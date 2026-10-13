# ADR 017 — Chế độ kiểm tra do máy chủ cưỡng chế

**Trạng thái:** Đề xuất (2026-10-13). Chủ repo đã yêu cầu kiểm tra không gia sư phía server; chi tiết quyền, hạn và cạnh tranh dưới đây để lead/chủ repo rà trước hiện thực. Phiên DOCS chỉ viết tài liệu.

## Bối cảnh

Ẩn cột gia sư không ngăn gọi trực tiếp API, sửa mode trong payload hoặc dùng tab cũ. Chế độ và hạn quyết định quyền làm bài nên thuộc core theo ADR 011. Luồng thi hiện là bộ bài tự kiểm có hạn, chưa là engine thi/giám sát. Phương án: [ghi chú quyết định](../../labs/decisions/2026-10-13-flat-va-che-do-kiem-tra.md).

## Quyết định đề xuất

1. Giao bài lưu `ON_TAP` hoặc `KIEM_TRA`, người nhận, bài và `dueAt` có timezone. Một giao hiệu lực mỗi bài/học sinh; giao lại cập nhật mode/hạn. Không lấy mode, người thao tác hoặc giờ từ client để cấp quyền.
2. Mọi đường tutor (SSE, REST hay adapter khác nếu có) dùng cùng policy application của core: xác thực → kiểm thành viên/đối tượng → đọc giao/mode/hạn hiện hành. Không có giao hợp lệ thì từ chối. Tự luyện nếu hỗ trợ phải có giao ôn do server tạo.
3. `KIEM_TRA` từ chối tutor bằng HTTP 403 problem+json trước khi mở SSE, truy hồi kho hoặc gọi nhà AI; không trả câu/gợi ý. Vẫn cấm khi `dueAt=null`, sau khi nộp và trong tab cũ của giao kiểm tra. Chỉ mode ôn hợp lệ cho phép tutor.
4. Server dùng Clock của mình; `now >= dueAt` là hết hạn. Hết hạn từ chối ghi bước/nộp và lượt tutor mới bằng 403 với mã lỗi phân biệt; đọc phần đã lưu theo quyền vẫn được. Không tự mở lời giải; cờ sau nộp mặc định tắt.
5. Kiểm lại mode/hạn ngay trước khi phát câu đã lọc. Đổi giao trong lúc xử lý phải hủy phát câu; tuần tự hóa ghi bước/nộp với đổi mode/hạn bằng giao dịch/khóa hoặc optimistic version phù hợp. Không giữ cache quyền cũ làm căn cứ cấp phép.
6. Policy không bỏ bộ lọc CAS hoặc cổng ADR 013 ở chế độ ôn. CAS lỗi thì fail-closed; không tự phát lại POST/tutor hay tự chuyển nhà.
7. Giao/đổi chế độ có nhật ký người/lúc/phạm vi/lý do; không log khóa hay định danh trong prompt. UI dùng `/api/hs/che-do` để ẩn gia sư và hiện hạn; UI không là ranh giới quyền.
8. Schema thiếu chỉ thêm Flyway V12+ còn trống. V1–V10 bất biến, không sửa/dùng V11 trong epic này.

## Phương án không chọn

Chỉ ẩn UI hoặc tin `mode` client dễ bị vượt. Chặn ở controller riêng lẻ dễ bỏ sót adapter. Dùng chính sách application dùng chung bảo vệ trước thao tác và trước phát kết quả, đồng thời cho integration test kiểm không gọi nhà AI.

## Nghiệm thu và hệ quả

Test core với Clock/latch cố định: ôn hợp lệ, kiểm tra, không giao, mode giả, hạn null/ranh giới/quá hạn, chéo lớp, đổi giao lúc đang chờ CAS, phiên hết hạn. Assert 403 trước SSE/provider và không ghi bài sau hạn. Playwright kiểm tab cũ, gọi API trực tiếp và đổi giờ client. Không dùng sleep để chứng minh cạnh tranh.

Chưa có auto-submit, chống gian lận, ma trận đề hay ngoại lệ gia sư trong kiểm tra. Nếu cần nhiều lượt ôn/thi đồng thời trên cùng bài, phải đổi hợp đồng sang ID lượt/giao cụ thể qua đặc tả riêng; không cấp quyền theo mã bài mơ hồ.

## Phạm vi khoá gia sư (cập nhật 2026-10-13)

Gia sư chỉ bị khoá trên chính các câu thuộc bài kiểm tra đang mở (và câu đã quá hạn). Các bài luyện khác vẫn dùng gia sư bình thường, vì bài kiểm tra có thể mở nhiều ngày và khoá toàn bộ sẽ làm học sinh mất hỗ trợ luyện tập.
