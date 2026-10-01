---
name: math-pedagogy
description: Kiến thức sư phạm của dự án — thang 4 mức ↔ Bloom ↔ 3 mức CV 7991, thang gợi ý không bottom-out, mã lỗi có dấu hiệu CAS, quy tắc «nâng 1 nấc», câu hỏi gợi mở, phương pháp học có bằng chứng. Dùng khi soạn hoặc review bài tập, gợi ý, prompt gia sư, logic chọn bài tiếp theo, lời tư vấn học, hoặc dữ liệu trong data/supham.
---

# Sư phạm toán của dự án

Nguồn chuẩn phạm vi: `docs/product/MUC-TIEU.md`. Lab chủ quản: `labs/pedagogy/`.

## Thang mức

| 4 mức | Bloom sửa đổi | 3 mức CV 7991 |
| --- | --- | --- |
| Nhận biết | Nhớ | Biết |
| Thông hiểu | Hiểu | Hiểu |
| Vận dụng | Vận dụng | Vận dụng |
| Vận dụng cao | Phân tích · Đánh giá · Sáng tạo | Vận dụng (gộp) |

Lưu 4 mức và Bloom trên mỗi bài; quy đổi 3 mức chỉ khi hiển thị cho giáo viên (ADR 004). Đề định kỳ theo CV 7991: 40 % Biết, 30 % Hiểu, 30 % Vận dụng.

## Thang gợi ý 3 cấp

- Cấp 1 định hướng: nhắc khái niệm hoặc công thức cần dùng, hỏi học sinh nên nhìn vào đâu.
- Cấp 2 khoanh vùng: chỉ ra bộ phận sai, đặt câu hỏi để học sinh tự kiểm.
- Cấp 3 cụ thể nhất có thể **mà không nêu kết quả**, không nêu điểm thử dẫn thẳng tới kết luận (bản vá `sp-sua-thang-0001`).
- Hết cấp 3 → bài dễ hơn cùng kỹ năng hoặc «gửi thầy cô». Không bao giờ bottom-out (VanLehn 2011; Aleven et al. 2016).

## Câu hỏi gợi mở

- Tốt: «Hàm số xác định ở đâu? Điểm này có thuộc tập xác định không?» (ERR.DH.02)
- Tốt: «x² có đổi dấu khi x đi qua 0 không?» (ERR.DH.05)
- Xấu: «Khoảng nghịch biến là (1; 3).» — nêu kết quả.
- Xấu: «Em thay x = 2 vào y′ sẽ thấy âm.» — điểm thử dẫn thẳng tới kết luận.

## Mã lỗi

Định dạng `ERR.<mạch>.<số>`, cột theo `data/supham/ma-loi-DH.csv`: `ma_loi, ky_nang_chinh, ma_buoc, ma_buoc_phu, loai_ket_qua_lien_quan, mo_ta, dau_hieu_nhan_biet, goi_y_sua, ky_nang_phu, nhom_loi_chung`. `dau_hieu_nhan_biet` phải kiểm được bằng CAS; `goi_y_sua` là câu hỏi gợi mở, không phải lời giải.

## Chọn bài tiếp theo (C8)

1. Lỗi lặp ở một dạng → bài cùng dạng, cùng mức (chữa lỗi).
2. Tiên quyết chưa đạt `muc_toi_thieu` → lùi về kỹ năng tiên quyết.
3. Đạt ngưỡng thành thạo ở mức hiện tại → nâng **đúng một** mức hoặc một bậc độ khó. Không nhảy mức.
4. Mỗi gợi ý bài kèm lý do hiển thị được («chữa lỗi», «củng cố», «nâng 1 nấc»); giáo viên ghi đè được.

## Tư vấn phương pháp học (C9)

Có bằng chứng mạnh (Dunlosky et al. 2013): luyện truy hồi, học cách quãng. Trung bình: luyện xen kẽ các dạng bài (hiệu quả với toán, Rohrer & Taylor 2007), tự giải thích. Yếu: đọc lại, tô đậm. Liều lượng đều đặn quan trọng (≥ 12 giờ / 8 tuần trong báo cáo LearnLM 2026): kế hoạch chia nhỏ, không dồn trước thi.

## Ngôn ngữ

Thuật ngữ SGK CT 2018: tập xác định, đạo hàm, điểm cực đại / cực tiểu, đồng biến / nghịch biến, bảng xét dấu, bảng biến thiên. Giọng lớp 12, câu ngắn. Minh bạch «đang học cùng AI» bằng nhãn giao diện, không bằng lời chào ở mỗi lượt (`docs/DESIGN.md`).
