# Kiểm giao diện Angular sau khi áp token Wiii + 3b1b (A + B)

Ngày: 2026-10-06. Nhánh `feat/frontend-wiii-3b1b`. Nghiên cứu và lựa chọn: `../studies/2026-10-06-wiii-3b1b.md` (chủ repo chọn A + B).

## Cách đo

- Bản dựng production (`ng build`) phục vụ tĩnh trên `localhost`, API giả (`/api/auth/refresh` trả phiên học sinh hay giáo viên, chưa đăng nhập thì 401), Chromium của Playwright 1.63.
- 5 trạng thái × 2 khổ (1280 × 800, 390 × 844) × 2 chế độ (sáng, tối), cộng ngăn kéo mở trên điện thoại.
- Tương phản: script ẩn mọi chữ, chụp nền thật dưới từng chữ, so màu chữ với điểm ảnh phía sau (352 phần tử). Thêm một cặp đối chứng phải trượt (`--line` trên `canvas`, 1,39 : 1) để chắc script bắt được lỗi.

## Kết quả

- Không tràn ngang, CLS = 0, chế độ đúng ở mọi trạng thái; lỗi console duy nhất là 401 cố ý của lần làm mới phiên khi chưa đăng nhập.
- Mọi chữ ≥ 4,5 : 1 (chữ lớn ≥ 3 : 1), trừ nút «Tiếp tục» khi bị vô hiệu (WCAG 1.4.3 miễn).
- Cặp khóa (sáng / tối): chữ trên nền 17,50 / 13,71; chữ phụ trên nền 6,17 / 6,99; chữ nút chính 5,03 / 5,03; chữ nút khi trỏ 6,21 / 4,56; nhãn 4,97 / 8,31; bút đỏ 5,38 / 5,63; chữ trên bảng toán 14,82 / 15,75; viền focus trên `wash` 3,63 / 3,55; viền ô nhập trên `canvas` 3,41 / 4,27.

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
