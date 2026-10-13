# ADR 018 — Bảng màu xanh–trắng

**Trạng thái:** Chấp nhận theo chỉ đạo trực tiếp của chủ repo ngày 2026-10-13 (lane THEME).

## Bối cảnh

Chủ repo yêu cầu thay điểm nhấn đất nung và nền nâu/kem của khung Wiii bằng bảng màu giáo dục xanh–trắng. Cấu trúc giao diện, tiếng Việt, bảng toán tối và quy tắc FLAT giữ theo hợp đồng hiện hành. Các ghi chú lab ngày 2026-10-06 lưu tham chiếu và số đo lịch sử, không làm nguồn màu cho lần đổi này.

## Quyết định

- `apps/frontend/src/styles.css`, hai khối `:root` / `:root[data-theme='dark']`, là nơi duy nhất định nghĩa màu thực thi. Component dùng biến CSS. Bảng trong `docs/DESIGN.md` là bản đối chiếu được cổng kiểm tra xác nhận, không phải nơi tính màu.
- Sáng: canvas slate-50 `#F8FAFC`, wash blue-50 `#EFF6FF`, raise trắng; mực slate-900, chữ phụ slate-600. Hành động/nhãn xanh `#1D4ED8`, hover `#1E40AF`, chữ trắng. Chọn / điều hướng dùng tint xanh `#DBEAFE`.
- Tối: canvas navy `#0B1220`, wash `#111A2E`, raise `#17233A`; chữ slate-200. Hành động xanh sáng `#60A5FA`, hover đậm hơn `#3B82F6`, chữ navy; không dùng chữ trắng trên nút xanh sáng. Nền chọn navy xanh `#1E3656` giữ AA cho cả chữ phụ.
- Viền trang trí slate-300 (sáng) / slate-700 (tối); viền điều khiển slate-500 để đạt ≥ 3 : 1. Focus outline 2 px dùng màu xanh của theme. Không bóng, không dải màu mép.
- Trạng thái đạt xanh lá, chờ amber, lỗi đỏ. Nền trạng thái có thể pha tint với bề mặt; chữ trạng thái dùng token đạt AA. Đỏ chỉ cho lỗi / sai và dấu âm trong vùng toán.
- Bốn mức có token riêng: Nhận biết xanh, Thông hiểu teal, Vận dụng amber, Vận dụng cao indigo. Chúng không mượn token trạng thái; luôn kèm tên/chữ, không truyền nghĩa chỉ qua màu.
- Bảng toán luôn navy, chữ slate sáng, màu biểu thức hài hòa với khung: xanh / teal / vàng / đỏ / amber / xanh lá. `board-line` chỉ trang trí; `board-rule` dùng cho đường kẻ mang nghĩa.

## Kiểm chứng và hệ quả

`node scripts/tuong-phan-token.mjs` đo CSS trực tiếp ở cả hai theme: chữ và màu mức ≥ 4,5 : 1; viền điều khiển / focus / kẻ bảng mang nghĩa ≥ 3 : 1. Đo riêng chữ nút thường / hover và chữ trên nền chọn. Cổng kiểm bản đối chiếu DESIGN, token chưa được đo và các viền trang trí được ghi dưới ngưỡng. Test cổng dùng palette lỗi để xác nhận cổng thực sự từ chối.

Cổng bàn giao: `node scripts/tuong-phan-token.mjs && node scripts/check-flat.mjs && node --test scripts/*.test.mjs`, `ng build`, `ng test --watch=false` trong `apps/frontend`. Kết quả đo và chạy thật được báo khi bàn giao, không gán số đo của ảnh cũ cho bảng màu mới.

Lane này không sửa thư mục `apps/frontend/src/app/features/hoc-sinh/luyen/**`, template, TypeScript hay ảnh bitmap. CSS trong vùng được phép nhận màu mới qua token; ảnh mô phỏng cũ cần được thay trong công việc nội dung ảnh riêng nếu còn màu ấm. Việc tách token không đổi nội dung sư phạm hoặc logic chấm bài.
