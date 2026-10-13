# ADR 014 — FLAT thay chỉ dẫn bóng và viền nhấn trong DESIGN

**Trạng thái:** Chấp nhận theo chỉ đạo trực tiếp của chủ repo ngày 2026-10-13; ghi lại quyết định đã có, không yêu cầu duyệt lại.

## Bối cảnh

Hướng Wiii + bảng toán 3b1b ngày 2026-10-06 có ghi chú/nguyên mẫu dùng bóng hoặc vạch trái. Chủ repo đã thay các chỉ dẫn đó bằng FLAT tuyệt đối. Phân tích phương án: [ghi chú quyết định](../../labs/decisions/2026-10-13-flat-va-che-do-kiem-tra.md).

## Quyết định

- Không `box-shadow`, kể cả inset; không token/utility bóng, viền nhấn trái/inline-start, dải màu ở mép hoặc khối 3D. Không mô phỏng bóng bằng filter/drop-shadow/pseudo-element hay hover nâng thẻ.
- Phân cấp bằng nền `canvas`/`wash`/`raise`, chữ, khoảng trắng và viền 1 px đều bốn cạnh. Mục đang chọn dùng nền/chữ và `aria-current`. Focus dùng `outline` 2 px.
- Luật này ưu tiên hơn mọi chỉ dẫn trái luật trong [DESIGN](../DESIGN.md), nguyên mẫu, rule và skill; áp dụng sáng/tối, mobile/desktop, toast/dialog/nav/flashcard.
- Giữ token MathL+, cấu trúc Wiii và bảng toán Manim. Flashcard đổi mặt bằng nội dung hoặc fade 2D, không xoay 3D. Motion theo DESIGN và `prefers-reduced-motion`.
- Cổng có tên chuẩn `node scripts/check-flat.mjs`; không tạo script thứ hai theo tên cũ `kiem-phang.mjs` trong kế hoạch tham chiếu. Test `node --test scripts/check-flat.test.mjs`, đo `node scripts/tuong-phan-token.mjs`, rà ảnh để bắt màu mép/hình/CSS động ngoài scanner.

## Hệ quả

Nguyên mẫu lịch sử có thể giữ để đối chiếu nhưng không làm chuẩn hiện thực khi vi phạm luật này. Scanner không chứng minh toàn bộ hình ảnh phẳng: U15 vẫn kiểm trực quan 390/1280/1440 px hai theme. ADR không tự xác nhận cổng hay giao diện đã đạt.
