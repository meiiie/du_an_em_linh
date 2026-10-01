# Bối cảnh Việt Nam — chương trình, đề thi, pháp lý (2026-10-01)

Câu hỏi: khung chương trình, định dạng đánh giá và luật nào ràng buộc sản phẩm? Phục vụ C3, C4, C6, bất biến dữ liệu.

> Phần pháp lý là đọc hiểu kỹ thuật, **không phải ý kiến luật sư**. Trước pilot với học sinh thật cần luật sư xác nhận.

## Chương trình và đánh giá

| Văn bản | Nội dung ràng buộc | Hệ quả |
| --- | --- | --- |
| CT GDPT môn Toán 2018 (TT 32/2018/TT-BGDĐT) | Yêu cầu cần đạt (YCCĐ) theo lớp; 5 thành phần năng lực toán: tư duy và lập luận (TDLL), mô hình hóa (MHH), giải quyết vấn đề (GQVD), giao tiếp toán học (GTTH), sử dụng công cụ (SDCC) | Mỗi bài gắn YCCĐ trích nguyên văn + năng lực (v0 đã có trong `data/supham/`) |
| CV 7991/BGDĐT-GDTrH (17/12/2024) | Đề định kỳ: 3 mức Biết / Hiểu / Vận dụng, tỉ lệ 40/30/30; trắc nghiệm 7 điểm (nhiều lựa chọn, đúng/sai, trả lời ngắn) + tự luận 3 điểm; áp dụng từ HK2 2024–2025 | Hiển thị 3 mức cho GV; bộ ôn tập theo đúng tỉ lệ và dạng câu |
| Đề TN THPT môn Toán từ 2025 | 22 câu / 90 phút: 12 câu nhiều lựa chọn, 4 câu đúng/sai × 4 ý, 6 câu trả lời ngắn (0,5 điểm/câu) | Ngân hàng phải có đủ 4 dạng câu; chấm đúng/sai theo từng ý; trả lời ngắn chuẩn hóa số |

«Vận dụng cao» không còn là mức riêng trong CV 7991 nhưng vẫn phổ biến trong ra đề và có trên sơ đồ khách. Giữ 4 mức nội bộ, quy đổi khi hiển thị (ADR 004).

## Pháp lý

| Luật | Hiệu lực | Điểm ràng buộc sản phẩm | Hành động |
| --- | --- | --- | --- |
| Luật Bảo vệ dữ liệu cá nhân 91/2025/QH15 | 01/01/2026 | Quyền được biết, đồng ý, truy cập, chỉnh sửa, xóa; dữ liệu trẻ em cần đồng ý của người đại diện theo pháp luật (trẻ từ 7 tuổi còn cần đồng ý của chính trẻ trong một số trường hợp) | Luồng đồng ý phụ huynh; tối thiểu hóa; xóa theo yêu cầu; nhật ký xử lý; đánh giá chuyển dữ liệu ra nước ngoài khi gọi API LLM |
| Luật Trí tuệ nhân tạo 134/2025/QH15 (8 chương, 35 điều) | 01/03/2026 | Lấy con người làm trung tâm; hệ thống rủi ro cao phải đánh giá sự phù hợp trước khi dùng (Điều 13–14); trong giáo dục: phù hợp lứa tuổi, phòng ngừa rủi ro khi đánh giá, phân loại người học, an toàn dữ liệu; minh bạch dữ liệu | Theo dõi danh mục hệ thống rủi ro cao Bộ KH&CN đang xây dựng; thiết kế để GV giám sát và ghi đè mọi phân loại mức; hiển thị rõ «đang học cùng AI» |

Câu hỏi cho luật sư: (1) mô hình mức hiểu và gợi ý bài có bị xếp vào «đánh giá, phân loại người học» rủi ro cao không; (2) hồ sơ đánh giá tác động chuyển dữ liệu khi gửi nội dung bài làm (đã xóa định danh) tới API nước ngoài; (3) thời hạn lưu dữ liệu học tập.

## Nguồn (truy cập 2026-10-01)

- CV 7991: https://thuvienphapluat.vn/cong-van/Giao-duc/Cong-van-7991-BGDDT-GDTrH-2024-thuc-hien-kiem-tra-danh-gia-doi-voi-cap-trung-hoc-co-so-636462.aspx · https://thuvienphapluat.vn/hoi-dap-phap-luat/83A55F0-hd-ma-tran-de-kiem-tra-theo-cong-van-7991-cap-thcs-thpt.html
- Đề TN THPT Toán: https://thuvienphapluat.vn/hoi-dap-phap-luat/cau-truc-de-thi-tot-nghiep-thpt-mon-toan-nam-2025-thay-doi-nhu-the-nao-138020660.html · https://toanmath.com/2026/06/huong-dan-giai-de-chinh-thuc-ky-thi-tot-nghiep-thpt-nam-2026-mon-toan.html
- Luật BVDLCN: https://english.luatvietnam.vn/dan-su/law-on-personal-data-protection-law-no-91-2025-qh15-405135-d1.html · https://bocongan.gov.vn/chinh-sach-phap-luat/bai-viet/luat-bao-ve-du-lieu-ca-nhan-chinh-thuc-co-hieu-luc-thi-hanh-tu-ngay-01-01-2026-1767186124
- Luật AI: https://thuvienphapluat.vn/van-ban/Cong-nghe-thong-tin/Luat-Tri-tue-nhan-tao-2025-so-134-2025-QH15-679013.aspx · https://luatvietnam.vn/tin-van-ban-moi/bo-khcn-de-nghi-de-xuat-danh-muc-he-thong-tri-tue-nhan-tao-co-rui-ro-cao-theo-luat-ai-2025-186-107414-article.html · https://giaoducthudo.giaoducthoidai.vn/luat-tri-tue-nhan-tao-co-hieu-luc-nganh-giao-duc-anh-huong-ra-sao-212418.html
