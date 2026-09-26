# Đối chiếu nguyên mẫu với thiết kế giai đoạn 1

Đây không phải bản sản phẩm đầy đủ mọi khối lớp 10–12. Đây là nguyên mẫu NCKH đúng phạm vi đã khóa: một chủ đề, chạy được, demo được.

## Khớp thiết kế

- Sơ đồ gốc: giáo viên nạp tài liệu / công thức / bài → cổng 3 tầng → học sinh làm với AI theo mức → bài cá nhân hóa → thời gian biểu → lặp đến vận dụng cao.
- Khung 5 bước `B.DH.TXD` … `B.DH.KETLUAN`. Chấm cả bước, không tô từng ô khi gõ. `k` ô bảng từ 0, thiếu điểm gán `B.DH.NGHIEM`.
- Gia sư không đọc lời giải chuẩn. Xin đáp án bị chặn. Bộ lọc SymPy trên mọi câu trả lời.
- 4 mức cho học sinh; 3 mức CV 7991 chỉ lúc xem tiến độ.
- Cổng phát hành: cả 3 tầng Đạt hoặc GV duyệt; Sai thì chặn; Không kiểm được thì chờ.
- Dữ liệu tổng hợp. Có `consent_records` và `audit_logs`. Không có cổng phụ huynh.
- Offline khi không có `LLM_API_KEY`.

## Đã bổ sung ở vòng giao diện

- Thanh bên cố định (desktop) và ngăn kéo (điện thoại), có mục đang mở và số bài chờ.
- Cài đặt lớp: bật/tắt mở lời giải sau khi nộp xong cả năm bước.
- Trích dẫn tầng 2/3 hiện bằng lời, không in JSON thô.
- Sinh biến thể báo mã bài và trạng thái cổng.
- Hệ thống thiết kế LMS (token, Source Sans 3, top bar + sidebar sáng) ghi ở `docs/DESIGN.md`. Không chép màu/logo Coursera.

## Skill dùng khi chỉnh UI / API

- `.cursor/skills/frontend-design` — tránh cụm cream + serif + terracotta.
- `.cursor/skills/web-design-guidelines` — a11y, focus, form, motion.
- `.cursor/skills/fastapi-routers` — router / Pydantic / không import SymPy ở process API.

## Cố ý chưa làm (ngoài phạm vi nguyên mẫu)

- Mọi chủ đề Toán 10–12. Bài đúng/sai và bài tham số trong JSON sư phạm không có lời giải 5 bước nên đứng hàng chờ.
- pgvector. Tầng 2 là tìm cụm từ có trích dẫn.
- Cổng phụ huynh, email/SMS nhắc.
- Registry shadcn/ui. Dùng nguyên thủy Tailwind cùng hệ thống chữ và màu riêng.
