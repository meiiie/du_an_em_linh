---
name: angular-frontend
description: Chuẩn frontend v2 trong apps/frontend — Angular 22 signal-first (zoneless, OnPush mặc định, Signal Forms, resource / httpResource, @angular/aria, Vitest), KaTeX hiển thị và MathLive nhập công thức, gia sư qua SSE trạng thái, tiếng Việt và truy cập. Dùng khi tạo hoặc sửa mã trong apps/frontend, hoặc port component từ LMS.
paths:
  - "apps/frontend/**"
---

# apps/frontend — Angular 22

Quyết định: ADR 011. Ràng buộc ngắn: `.claude/rules/angular-frontend.md`. Thiết kế: `docs/DESIGN.md` + lab Thiết kế. Lệnh: `pnpm --filter frontend dev | build | test` (Node `^22.22.3 || ^24.15.0 || >=26`); bản đồ và gotcha: `apps/frontend/AGENTS.md`.

## Mặc định của Angular 22 — đừng viết lại

- Component standalone mặc định: không ghi `standalone: true`.
- App mới chạy zoneless (từ v21): không thêm `zone.js`; giao diện cập nhật qua signal.
- Component mới OnPush mặc định (v22): không giữ state có thể đổi ngoài signal.
- Control flow `@if` / `@for` (luôn có `track`) / `@switch` / `@defer`.
- DI bằng `inject()`; `input()`, `output()`, `model()`; `viewChild()`, `contentChild()`.
- State: `signal`, `computed`, `linkedSignal`. `effect` chỉ để đồng bộ ra ngoài (thư viện DOM, storage), không để đồng bộ state với state.
- Đọc dữ liệu: `httpResource()` / `resource()`. Ghi dữ liệu: `HttpClient` + cập nhật signal.
- Form mới: Signal Forms (`@angular/forms/signals`): `form()`, chỉ thị `[formField]`, validator `required`, `minLength`, `maxLength`, `pattern`, `email`, `min`, `max`. Không trộn Reactive Forms trong cùng một form.
- Widget truy cập (tab, menu, listbox, tree, accordion): `@angular/aria`, không tự viết ARIA.
- Test: Vitest.

## Cấu trúc (theo LMS, chia theo tính năng)

```text
src/app/
  core/      auth, guard, interceptor, xử lý lỗi (port từ LMS fe/src/app/core)
  api/       client có kiểu, hằng endpoint, type khớp DTO của services/core
  features/  hoc-sinh/…, giao-vien/…, dang-nhap
  shared/    ui (nút, ô, tab…), toan (KaTeX, nhập công thức), layout
```

Giữ route `/hs`, `/gv`, `/dang-nhap` và `data-testid` của v0 cho màn tương đương, để e2e kiểm tương đương được (pha P2).

## Toán trên giao diện

- Hiển thị bằng KaTeX, `throwOnError: false`: công thức lỗi thì chữ mờ, không hộp đỏ (như v0).
- Nhập bằng MathLive `<math-field>`, bọc thành control dùng được với Signal Forms theo tài liệu custom control của Angular; luôn kèm ô gõ LaTeX thường dự phòng (bàn phím ảo không mở được vẫn làm bài được).
- Port từ LMS: `shared/blocks/formula-block`, `shared/components/math-quick-toolbar`, `shared/components/enriched-input` — nâng lên v22 khi chép.
- Client **không** tính đúng / sai và không giữ đáp án. Mọi kết quả chấm đến từ `services/core`.

## Gia sư trên giao diện

- Nhận SSE trạng thái (`kho` → `goi` → `loc` → `xong`); hiện «Đang nghĩ…» theo trạng thái; câu chỉ hiện sau khi qua bộ lọc (ADR 010).
- Dừng = `AbortController`; lỗi → «Hỏi lại» đổ câu vào ô, không tự gửi lại.
- Gia sư chỉ mở khi học sinh hỏi; dưới `lg` là tờ toàn màn, nút Đóng 44 px.

## Chữ, thiết kế, truy cập

- Mỗi chữ một việc; tiếng Việt có dấu; giọng lớp 12 / phòng giáo viên (hiến chương V).
- Kiểm 390 px và 1280 px; mục tiêu chạm 44 px với `pointer: coarse`; `prefers-reduced-motion`; `aria-live` khi chấm.

## Bẫy đã gặp ở LMS (`LMS_hohulili/docs/reference/FRONTEND_GOTCHAS.md`, `docs/LESSONS_LEARNED_2026-04-27.md`)

- Nếu dùng SSR: `outputMode` chỉ đặt trong cấu hình production, không ở `options` hay `development` (dev server chậm ~7 s mỗi request).
- Rich text: đọc nội dung trực tiếp từ instance editor lúc lưu, không tin binding (mất nội dung khi chèn khối).
- Mọi nhánh render có trạng thái rỗng; không để màn trắng.
- Dịch vụ ngoài (avatar, font CDN) phải qua CSP; ưu tiên tạo tại chỗ.
