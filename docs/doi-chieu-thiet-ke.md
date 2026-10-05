# Đối chiếu nguyên mẫu với thiết kế giai đoạn 1

Đây không phải bản sản phẩm đầy đủ mọi khối lớp 10–12. Đây là nguyên mẫu NCKH đúng phạm vi đã khóa: một chủ đề, chạy được, demo được.

## Khớp sơ đồ gốc (khối cam / xanh)

| Khối sơ đồ | Trong nguyên mẫu |
| --- | --- |
| GV: bài NB→TH→VD→VDC | 4 mức trên bài; CV 7991 3 mức lúc xem `/gv/tien-do?muc=3` |
| GV: tài liệu / đề mẫu / công thức | Nạp tài liệu, bảng công thức khóa, kiểm 3 tầng |
| Kiểm 3 tầng | Máy SymPy · tìm trong tài liệu · đối chiếu bảng |
| Bộ bài / ôn tập cho từng HS | `assignments` + gợi bài theo kỹ năng yếu / cùng mức / chuyển mức khó hơn |
| HS: thang Bloom | Không hiện. HS chỉ thấy 4 mức trên phiếu / đề |
| Học với AI theo bước | Phiếu 5 bước + gia sư (luật → thang 3 cấp → API tùy chọn → lọc) |
| Tư vấn phương pháp + lịch + nhắc | `/hs/lich` tính theo BKT; nhắc in-app |
| Gọi API bên thứ 3 | Nhà lớp: offline / khóa chính thức / Ollama·LM Studio loopback; không fallback thầm |
| Lặp tới vận dụng cao | BKT + recommend; kẹt thì giữ mức / gửi GV |

## Khớp thiết kế

- Sơ đồ gốc: giáo viên nạp tài liệu / công thức / bài → cổng 3 tầng → học sinh làm với AI theo mức → bài cá nhân hóa → thời gian biểu → lặp đến vận dụng cao.
- Khung 5 bước `B.DH.TXD` … `B.DH.KETLUAN`. Chấm cả bước, không tô từng ô khi gõ. `k` ô bảng từ 0, thiếu điểm gán `B.DH.NGHIEM`.
- Gia sư không đọc lời giải chuẩn. Xin đáp án bị chặn. Bộ lọc SymPy trên mọi câu trả lời.
- 4 mức cho học sinh; 3 mức CV 7991 chỉ lúc xem tiến độ.
- Cổng phát hành: cả 3 tầng Đạt hoặc GV duyệt; Sai thì chặn; Không kiểm được thì chờ.
- Dữ liệu tổng hợp. Có `consent_records` và `audit_logs`. Không có cổng phụ huynh.
- Offline là nhà mặc định. Cloud cần khóa chính thức. Local chỉ loopback. Xem `docs/AI-HARNESS.md`.

## Đã bổ sung ở vòng giao diện

- Thanh bên cố định (desktop) và ngăn kéo (điện thoại), có mục đang mở. Số bài chưa làm chỉ trên tab Bài tập.
- Cài đặt lớp: bật/tắt mở lời giải sau khi nộp xong cả năm bước.
- Trích dẫn tầng 2/3 hiện đoạn tài liệu / tên công thức, không in cụm khớp hay JSON thô.
- Tạo đề báo trạng thái Đã mở / Chờ duyệt / Bị chặn, không in mã bài.
- Hệ thống thiết kế «phiếu làm bài» của v0 (IBM Plex, ray mực, danh sách thay thẻ, ô thành thạo, lưới 8 px, nút 40/44). Từ 2026-10-06, v2 giữ cấu trúc đó (danh sách thay thẻ, ô thành thạo, lưới 8 px, nút 40/44) nhưng đổi màu, chữ, bán kính theo kiểu Wiii pha bảng toán 3b1b: xem `docs/DESIGN.md`. Lấy cấu trúc Khan/Classroom/Canvas/Brilliant; không chép màu/logo.
- Harness agent: `AGENTS.md` + `CLAUDE.md` lớp theo thư mục, `.claude/settings.json`, `docs/CODEMAP.md`.
- Harness gia sư: bốn nhà tường minh, composer 44 px, khóa lớp tùy chọn, thử `GET /models`.
- Kết nối khóa chính thức cho giáo viên không chuyên: `/gv/ket-noi-ai` (hai bước ChatGPT / OpenRouter / Z.AI; OAuth định danh nếu có client_id). Trang Lớp hiện trạng thái.
- Công thức và tài liệu: `/hs/kho` tab Công thức / Tài liệu; gia sư truy hồi cùng nguồn, không đọc lời giải.

## Skill dùng khi chỉnh UI / API

- `.cursor/skills/frontend-design` — tránh cụm cream + serif + terracotta.
- `.cursor/skills/web-design-guidelines` — a11y, focus, form, motion.
- `.cursor/skills/fastapi-routers` — router / Pydantic / không import SymPy ở process API.

## Cố ý chưa làm (ngoài phạm vi nguyên mẫu)

- Mọi chủ đề Toán 10–12. Bài đúng/sai và bài tham số trong JSON sư phạm không có lời giải 5 bước nên đứng hàng chờ.
- pgvector. Tầng 2 là tìm cụm từ có trích dẫn.
- Cổng phụ huynh, email/SMS nhắc.
- Registry shadcn/ui. Dùng nguyên thủy Tailwind cùng hệ thống chữ và màu riêng.
