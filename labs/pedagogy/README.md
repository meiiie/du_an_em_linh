# Lab Sư phạm

**Sứ mệnh:** quyết định dạy cái gì, theo thứ tự nào, ở mức nào, và khi học sinh sai thì hỏi gợi mở ra sao. Lab là chủ sở hữu nội dung sư phạm. Mã chỉ hiện thực những gì lab đã chốt.

## Lab đang sở hữu

| Tài sản | Chỗ | Hiện có (v0, 29/09/2026) |
| --- | --- | --- |
| Danh mục kỹ năng (mã `T<lớp>.<mạch>.<số>`, YCCĐ trích nguyên văn, tiên quyết, mức áp dụng) | `data/supham/danh-muc-ky-nang-DH.json` | 11 kỹ năng, chủ đề đạo hàm (đơn điệu, cực trị) |
| Mã lỗi (`ERR.<mạch>.<số>`: bước, loại kết quả, dấu hiệu nhận biết bằng CAS, gợi ý sửa, nhóm lỗi) | `data/supham/ma-loi-DH.csv` | 31 mã |
| Lược đồ bài (4 mức, 3 mức, Bloom, năng lực, dạng câu, khung bước, đáp án nội bộ ẩn) | `services/math/kiemdinh/03-schema-bai-tap.json`, ví dụ `data/supham/03-vi-du-bai-tap.json` | 23 bài trong seed |
| Thang gợi ý 3 cấp theo (bước, loại kết quả) | `services/math/app/data/thang-goi-y-mau/` | 52 thang, 3 họ hàm |
| Khung ngắn NB/TH | `data/supham/bai-khung-ngan.seed-v01.json` | 8 bài |

## Nguyên tắc nghề

- **Mức:** lưu 4 mức + Bloom trên mỗi bài; quy đổi 3 mức CV 7991 chỉ khi hiển thị (ADR 004; bảng ở `docs/product/MUC-TIEU.md` §4).
- **Gợi ý:** thang 3 cấp, cấp sau cụ thể hơn cấp trước, **không bao giờ** tới mức nêu đáp án (bottom-out). Cấp cuối hết gợi ý → bài dễ hơn hoặc «gửi thầy cô».
- **Lỗi:** mỗi mã lỗi phải có dấu hiệu nhận biết **máy kiểm được** (CAS), không chỉ mô tả chữ.
- **Nâng 1 nấc:** chỉ nâng khi đạt ngưỡng thành thạo ở mức hiện tại; tiên quyết chưa đạt thì lùi về kỹ năng tiên quyết.
- **Nguồn:** YCCĐ trích nguyên văn CT GDPT 2018 kèm trang; thuật ngữ theo bộ SGK khách dùng (hỏi khách: Kết nối tri thức / Chân trời sáng tạo / Cánh diều).

## Gói nội dung cho một chủ đề mới

Một chủ đề chỉ được giao cho Build khi đủ gói sau, và lab Kiểm định đã chạy bộ ca:

1. Kỹ năng + tiên quyết (đồ thị), YCCĐ nguyên văn.
2. Mã lỗi thường gặp, mỗi mã có dấu hiệu CAS.
3. Khung bước cho từng dạng bài tự luận; định dạng chấm cho nhiều lựa chọn, đúng/sai nhiều ý, trả lời ngắn.
4. Bài mẫu đủ 4 mức × các dạng câu, có lời giải ẩn, đáp án nội bộ dạng SymPy.
5. Thang gợi ý 3 cấp cho từng (bước, loại kết quả).
6. Mục bảng công thức của chủ đề (để cổng tầng 3 dùng).

## Đang mở

- [`2026-10-01-ke-hoach-mo-rong-noi-dung.md`](2026-10-01-ke-hoach-mo-rong-noi-dung.md) — kế hoạch mở rộng từ một chủ đề ra chương trình Toán 12.
- [`2026-10-02-tai-lieu-quy-tac-dao-ham.md`](2026-10-02-tai-lieu-quy-tac-dao-ham.md) — bản vá `sp-tai-lieu-0001`: tài liệu tự soạn làm căn cứ tầng 2 cho quy tắc lũy thừa, tổng, thương (#84).
- [`2026-10-02-tai-lieu-diem-toi-han-cuc-tri.md`](2026-10-02-tai-lieu-diem-toi-han-cuc-tri.md) — bản vá `sp-tai-lieu-0002`: căn cứ tầng 2 cho định nghĩa điểm tới hạn và «không đổi dấu thì chưa phải cực trị» (#103).
