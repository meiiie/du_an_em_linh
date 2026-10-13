# Nguồn chuyển động và giấy phép

Ngày đọc: **2026-10-13**. Tham chiếu chỉ đọc: `/workspace/ref-transitions`, `README.md` và `LICENSE`; tác giả **Jakub Antalik / Transitions.dev**, copyright 2026. Nguồn công khai: [Transitions.dev](https://transitions.dev/), [terms](https://transitions.dev/terms.html).

MathL+ viết lại các ý tưởng chuyển trạng thái cho Angular/CSS: mở hộp thoại/tờ, đổi nội dung, hiện danh sách, báo thành công và báo lỗi. Không nhập runtime React hoặc phân phối thư viện transitions như một sản phẩm. Màu, chữ, kích thước và luật FLAT lấy từ hệ thiết kế MathL+; motion không được thêm bóng hay dải màu mép. `prefers-reduced-motion` phải cho phép dùng đầy đủ mọi chức năng khi tắt chuyển động.

Giấy phép của nguồn phân biệt hai phần: **transition/snippet/skill dùng điều khoản Transitions.dev**, cho phép dùng và sửa trong sản phẩm cá nhân/thương mại nhưng không phân phối bộ thư viện thành sản phẩm cạnh tranh; **tooling** ở `agent/`, `cli/`, `refine/`, `build/`, `scripts/` dùng MIT. Không được gọi toàn bộ nguồn là MIT. Không dùng Pro transition hoặc Agent trả phí nếu chưa có quyền tương ứng; logo và thiết kế website Transitions.dev không thuộc quyền này.

Các ảnh nguyên mẫu trong lab là tư liệu lịch sử, không phải nguồn mã motion hiện hành. Chỗ tái sử dụng hoặc viết lại từ ý tưởng cụ thể giữ nhận diện nguồn trong chú thích gần CSS. Cổng FLAT và kiểm reduced-motion bổ sung cho rà giao diện; giấy phép không là bằng chứng motion đã hoạt động trên trình duyệt.
