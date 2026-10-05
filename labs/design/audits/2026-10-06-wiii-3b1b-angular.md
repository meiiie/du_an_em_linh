# Kiểm giao diện Angular sau khi áp token Wiii + 3b1b (A + B)

Ngày: 2026-10-06. Nhánh `feat/frontend-wiii-3b1b`. Nghiên cứu và lựa chọn: `../studies/2026-10-06-wiii-3b1b.md` (chủ repo chọn A + B).

## Cách đo

- Bản dựng production (`ng build`) phục vụ tĩnh trên `localhost`, API giả (`/api/auth/refresh` trả phiên học sinh hay giáo viên, chưa đăng nhập thì 401), Chromium của Playwright 1.63.
- 5 trạng thái × 2 khổ (1280 × 800, 390 × 844) × 2 chế độ (sáng, tối), cộng ngăn kéo mở trên điện thoại.
- Tương phản: script ẩn mọi chữ, chụp nền thật dưới từng chữ, so màu chữ với điểm ảnh phía sau (352 phần tử). Thêm một cặp đối chứng phải trượt (`--line` trên `canvas`, 1,39 : 1) để chắc script bắt được lỗi.
- Vòng focus: Tab qua mọi điểm dừng (130 lượt: đăng nhập bước 1 và 2, trang học sinh và giáo viên, ngăn kéo; 2 khổ, 2 chế độ), so ảnh lúc focus với lúc bỏ focus, lấy điểm ảnh của vòng và của nền hai bên (script của lượt kiểm độc lập, chạy lại trên bản sửa).
- Lần vẽ đầu: CSS trả chậm 2 giây, ba tổ hợp máy / lựa chọn đã lưu. Trang chưa vẽ gì cho tới khi CSS về (≈ 2,05 s); đọc điểm ảnh nền của khung đầu tiên.

## Kết quả

- Không tràn ngang, CLS = 0, chế độ đúng ở mọi trạng thái; lỗi console duy nhất là 401 cố ý của lần làm mới phiên khi chưa đăng nhập.
- Mọi chữ ≥ 4,5 : 1 (chữ lớn ≥ 3 : 1), trừ nút «Tiếp tục» khi bị vô hiệu (WCAG 1.4.3 miễn).
- Cặp khóa (sáng / tối): chữ trên nền 17,50 / 13,71; chữ phụ trên nền 6,17 / 6,99; chữ nút chính 5,03 / 5,03; chữ nút khi trỏ 6,21 / 4,56; nhãn 4,97 / 8,31; bút đỏ 5,38 / 5,63; chữ trên bảng toán 14,82 / 15,75; `focus` trên `canvas` / `wash` / `raise` 4,00 / 3,63 / 4,21 và 6,18 / 5,48 / 4,62; viền ô nhập trên `canvas` 3,41 / 4,27; kẻ bảng xét dấu `board-rule` trên bảng 4,81 / 5,11.
- Vòng focus, thấp nhất với nền hai bên: 3,14 : 1 ở chế độ sáng (mục thanh bên, cạnh nền nhạt `accent` của mục đang chọn) và 4,62 : 1 ở chế độ tối (vòng vẽ vào trong, trên `raise`). Vòng vẽ vào trong ở mọi điều khiển của thanh trên 48 px và ở nút trong ô nhập (vòng ngoài sẽ nằm trên viền ô, 1,17 : 1). Link «Bỏ qua» có viền `canvas` 6 px nên vòng không nằm trên chữ tiêu đề: cột điểm ảnh qua cạnh dưới là `canvas`, `focus`, `canvas`, 4,00 / 6,18 : 1. Bộ quét so ảnh báo 1,00 cho link này ở trang giáo viên vì viền `canvas` phủ lên chữ tiêu đề nên cả dải bị tính là «thay đổi»; số đúng là số đọc trực tiếp. Trước khi có token `focus`, vòng `accent` vẽ vào trong nút đổi giao diện tối chỉ 2,99 : 1.
- Lần vẽ đầu đúng chế độ khi CSS chậm: CSS toàn cục chặn vẽ (`inlineCritical: false`). Trước đó CSS trọng yếu nội tuyến chỉ có token sáng, chế độ tối hiện nền `#FAF9F5` suốt 2 giây.
- Ngăn kéo giữ nền `wash` như thanh bên: thử `raise` thì biểu tượng mục đang chọn (`accent`) ở chế độ tối còn 2,69 : 1.

## Ảnh

| | Sáng | Tối |
| --- | --- | --- |
| Học sinh, 1280 | [hs-1280-light.webp](2026-10-06-wiii-3b1b-angular/hs-1280-light.webp) | [hs-1280-dark.webp](2026-10-06-wiii-3b1b-angular/hs-1280-dark.webp) |
| Giáo viên, 1280 | [gv-1280-light.webp](2026-10-06-wiii-3b1b-angular/gv-1280-light.webp) | [gv-1280-dark.webp](2026-10-06-wiii-3b1b-angular/gv-1280-dark.webp) |
| Đăng nhập, 1280 | [dang-nhap-1280-light.webp](2026-10-06-wiii-3b1b-angular/dang-nhap-1280-light.webp) | [dang-nhap-1280-dark.webp](2026-10-06-wiii-3b1b-angular/dang-nhap-1280-dark.webp) |
| Đăng nhập, 390 | [dang-nhap-390-light.webp](2026-10-06-wiii-3b1b-angular/dang-nhap-390-light.webp) | [dang-nhap-390-dark.webp](2026-10-06-wiii-3b1b-angular/dang-nhap-390-dark.webp) |
| Ngăn kéo, 390 | [hs-390-light-ngan-keo.webp](2026-10-06-wiii-3b1b-angular/hs-390-light-ngan-keo.webp) | [hs-390-dark-ngan-keo.webp](2026-10-06-wiii-3b1b-angular/hs-390-dark-ngan-keo.webp) |

## Khác ảnh mô phỏng (có chủ ý)

- Trang chủ hiện trạng thái rỗng thay cho danh sách bài, thẻ «Bài tiếp theo» và hàng mức độ: core chưa có API học sinh (T021, chờ ADR 015).
- Viền ô nhập đậm hơn ảnh mô phỏng để đạt 3 : 1.
- Heading 28 px theo `docs/DESIGN.md`; ảnh mô phỏng trông gần 36 px.
- Ảnh mô phỏng vẽ đồ thị lộ cực trị lúc đang làm; giao diện thật không có đồ thị trên màn đang chấm (`docs/DESIGN.md`).
