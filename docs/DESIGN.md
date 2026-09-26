# Hệ thống thiết kế — Phiếu làm bài

Không dùng kit Figma EduPlus / Eduva / EduTracker / EduFlow (Poppins, thẻ KPI, gradient). Đó chính là slop. Nghiên cứu **sản phẩm đang chạy**, lấy cấu trúc chứ không lấy thương hiệu.

## Nguồn đã xem

| Sản phẩm | Việc lấy | Việc không lấy |
| --- | --- | --- |
| [Khan Academy Wonder Blocks](https://khan.github.io/wonder-blocks/) + [learner dashboard 2025](https://qasimbrown.design/projects/ai-powered-learner-dashboard) | Việc tiếp theo đứng trước catalog; thành thạo = hàng ô; mật độ chữ cao; ít kiểu chữ | Xanh `#1865f2`, Lato, logo |
| [Brilliant solvables](https://www.paigeormiston.com/brilliant) | Một luồng bước; sai = banner ngay dưới bài; một CTA | Pear / CoFo / logo |
| [Canvas InstUI](https://instructure.design/) | Ray điều hướng đặc, việc là danh sách/bảng | Electric brand, widget KPI |
| [Google Classroom](https://support.google.com/chrome/a/answer/15210733) | Hàng việc 2 dòng (tên + meta), không lưới thẻ khóa học | Material purple |
| Linear / Stripe Dashboard | CTA mực, kẻ 1 px, không bóng mềm dưới mọi thẻ | — |

Coursera `#0056D2`, IBM `#0F62FE`, Khan blue, Canvas electric: không chép.

## Brief

Sản phẩm là **phiếu chấm đạo hàm**: học sinh đi 5 bước, giáo viên duyệt cổng. Cảm giác sổ điểm / phiếu thi, không phải landing khóa học.

## Token

| Tên | Hex | Việc |
| --- | --- | --- |
| `ink` / `board` | `#17181C` | Chữ, ray, CTA |
| `canvas` / `paper` | `#FFFFFF` | Trang — không nền xanh xám |
| `wash` | `#F6F6F7` | Hàng xen, ô nhập |
| `muted` | `#5C5F66` | Chữ phụ |
| `line` | `#E2E3E6` | Kẻ |
| `chalk` | `#F4F4F5` | Chữ trên ray |
| `mark` / `danger` | `#C81E1E` | Bút đỏ chấm — **một** màu nhớ |
| `pass` / `teal` | `#1B7A4B` | Đạt / duyệt |
| `wait` / `warn` | `#9A6700` | Chờ |
| `primary` | `#17181C` | Bí danh ink — hết xanh học thuật |

## Chữ

- **IBM Plex Sans** (`latin` + `latin-ext` + `vietnamese`, 400/500/600/700). Carbon / InstUI-adjacent, có tiếng Việt, không phải Inter / Source Sans / Poppins.
- **IBM Plex Mono** cho mã bài và số thành thạo.

## Bố cục

```
+--------+--------------------------------+
| RAY    | trang trắng                    |
| 220px  | phiếu / danh sách / bảng       |
| mực    |                                |
+--------+--------------------------------+
```

Desktop: không top bar. Điện thoại: top bar + ngăn kéo mực. Skip `#noi-dung`.

Thẻ không còn là đơn vị mặc định. Việc = hàng (Classroom). Bài đang làm = một phiếu (Brilliant). Thành thạo = hàng ô (Khan).

## Thành phần

- Nút primary = mực đặc, bán kính 6 px.
- Bước 5 bước = cột số bên trái phiếu, không chip viên thuốc.
- Sai = viền `mark` + banner dưới bước.
- Không hero navy, không số khổng lồ trên 3 thẻ giống nhau.

## A11y

Skip link, `:focus-visible` mực, `prefers-reduced-motion`, `aria-live` khi chấm. Giữ `data-testid`.
