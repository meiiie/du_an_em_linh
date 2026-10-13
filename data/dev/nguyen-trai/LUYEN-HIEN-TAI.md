# Bài luyện hiện tại (dev)

Mỗi học sinh tổng hợp được giao hai bài `NT-LUYEN<khối>-NB-*` trong bộ
«Luyện tập hiện tại · có gia sư», chế độ `ON_TAP`, hạn bảy ngày kể từ lúc giao.
Nội dung đề và lời giải được sao nguyên văn từ bài đã duyệt cùng khối. Các mã
mới không có fixture lịch sử và bị loại khỏi phân bố lịch sử, nên chưa nộp;
khởi động lại giữ tiến độ đã làm, không xóa bài nộp hay sửa migration.
Bộ kiểm tra giữ nguyên và không được seeder lịch sử nộp.

Toán 12 có phiếu năm bước và gợi ý định hướng cho từng bước. Toán 10–11 vẫn
làm trên giấy; adapter chỉ ở profile dev cung cấp ba bước gợi mở (phân tích đề,
lập luận, kết luận), không bật chấm CAS cho dạng câu chưa hỗ trợ. Importer dev
đọc `khung-buoc.json` và `thang_goi_y`; gợi ý không chứa đáp án.

Chính sách giao diện: chỉ mở gia sư khi `/api/hs/che-do` xác nhận `ON_TAP`
và bài chưa nộp. `DA_NOP` không có nút gia sư vì chưa có thao tác cho phép
làm lại; khi API trả lại `DANG_LAM`, gia sư mở lại. Số bước bằng không không
chặn nút. Khi chế độ chưa tải được, gia sư đóng và hiện nút thử lại.

Trang Học chọn bài ôn chưa nộp trước, ưu tiên đang làm rồi gợi bài và bài chưa
làm. Nếu chỉ còn kiểm tra, thẻ ghi rõ chế độ, thời lượng lấy từ tên bộ (nếu có),
hạn, số câu, «Bắt đầu kiểm tra» và thông báo gia sư tạm tắt. Số câu tính trên
các bài kiểm tra có cùng tên bộ và hạn trong danh sách API đã tải.
