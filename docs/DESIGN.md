# Hệ thống thiết kế — Học toán với AI

Ngôn ngữ hình ảnh của một **lớp học số THPT**, không phải landing SaaS và không phải trang “AI slop”. Tham khảo cấu trúc chrome của các LMS hàng đầu (Coursera, edX, Khan Academy): canvas trắng, một sans humanist, CTA xanh học thuật, panel bài giảng navy. **Không** dùng logo, chữ, hay mã màu thương hiệu của họ.

Coursera voltage blue `#0056D2` và dấu C bị khóa thương hiệu — không chép.

## Brief

| Trục | Quyết định |
| --- | --- |
| Chủ đề | Toán 12, đạo hàm: đơn điệu và cực trị |
| Người dùng | Học sinh và giáo viên Việt Nam |
| Việc chính | Làm 5 bước / duyệt cổng 3 tầng / xem tiến độ |
| Cảm giác | Lớp học nghiêm túc, sáng, dễ đọc công thức |

## Token

| Tên | Hex | Việc |
| --- | --- | --- |
| `canvas` | `#FFFFFF` | Thẻ, top bar, sidebar |
| `paper` | `#F3F6FB` | Nền trang |
| `ink` | `#1A1D26` | Chữ chính |
| `muted` | `#5C6578` | Chữ phụ |
| `line` | `#D5DCE8` | Đường kẻ 1 px |
| `primary` | `#0B5CAB` | CTA, liên kết, mục đang mở |
| `primary-hover` | `#094A8C` | Hover CTA |
| `navy` / `board` | `#0A2540` | Panel bài giảng, cột đăng nhập |
| `chalk` | `#F7FAFC` | Chữ trên navy |
| `teal` | `#0F766E` | Thành công, gia sư (màu phụ) |
| `warn` | `#B45309` | Chờ duyệt |
| `danger` | `#B42318` | Sai / bác / kẹt |

`clay` trong Tailwind là bí danh của `primary` — terracotta cũ đã bỏ vì trùng cụm cream + serif + đất nung.

## Chữ

Một họ: **Source Sans 3** (`next/font/google`, `latin` + `latin-ext` + `vietnamese`, `display: swap`).

Coursera dùng Source Sans Pro; Source Sans 3 là thế hệ sau và có subset tiếng Việt. Không thêm Literata / Be Vietnam Pro.

| Bậc | Cỡ / nặng | Dùng |
| --- | --- | --- |
| Display | 32 / 600 | `h1` trang |
| Title | 24 / 600 | `h2` khối |
| Body | 16 / 400, leading 1.5 | Đoạn |
| Small | 14 / 400 | Phụ, form |
| Micro | 12 / 600 | Huy hiệu |

Cột số dùng `tabular-nums`. Tiêu đề `text-pretty`. Không viết HOA giãn chữ cho kicker.

## Bố cục

```
+--------------------------------------------------+
| TOP BAR trắng, cao 56px, kẻ dưới                 |
| [menu] [mark] Học toán với AI     [tên] [Thoát]  |
+--------+-----------------------------------------+
| SIDE   | #noi-dung  max 72rem                    |
| 256px  | nền paper, thẻ canvas                   |
| trắng  |                                         |
+--------+-----------------------------------------+
```

- Desktop: top bar xuyên ngang, sidebar dưới top bar, cố định trái.
- Điện thoại: top bar + ngăn kéo; overlay `overscroll-contain`.
- Căn trái. Lưới 8 px. Bán kính thẻ 16 px, nút 8 px.
- Một chỗ đậm: panel navy (lời chào lộ trình, cột trái đăng nhập). Phần còn lại im.

## Thành phần

- **Nút:** hình chữ nhật 8 px. Primary = `primary`. Secondary = canvas + kẻ. Destructive = `danger`.
- **Chip bước 5 bước:** được đánh số vì đúng là chuỗi. Đang mở = primary; sai = danger.
- **Huy hiệu trạng thái:** hình chữ nhật 6 px, không viên thuốc, không uppercase.
- **Thẻ:** canvas, kẻ 1 px, không bóng xám đồng loạt.
- **Ô form:** nhãn bọc control, `focus-visible` vòng primary 2 px.

## A11y

- Skip link → `#noi-dung`.
- `:focus-visible` bắt buộc; không `outline: none` trần.
- `prefers-reduced-motion`: tắt transition.
- Nút chỉ icon có `aria-label`. Icon trang trí `aria-hidden`.
- Thông báo chấm có `aria-live="polite"`.

## Việc cố ý không làm

- Không dark mode ở nguyên mẫu.
- Không registry shadcn.
- Không đổ gradient trang trí, không accent một từ trong tiêu đề.
- Không đổi `data-testid` khi restyle.
