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

## Lộ trình học sinh (`/hs`)

Không dashboard thẻ. Chia như phiếu + sổ — gáy 5 bước là vật đặc trưng, không phải lời chào.

```
+--------+---------------------------+------------------+
| RAY    | Chào An                   | 12A1 thử         |
| 220    | Toán 12, đơn điệu…        |                  |
|        +---------------------------+------------------+
|        | PHIẾU                     | SỔ (tab gạch)    |
|        | 1 TXĐ                     | Kỹ năng | Bài    |
|        | 2 y′     đề KaTeX         | (một panel)      |
|        | 3 …      [Làm bước tiếp]  |                  |
+--------+---------------------------+------------------+
```

Desktop (`lg`): cột phiếu `1fr` | kẻ 1 px | cột sổ `18–24rem`. Điện thoại / máy tính bảng: phiếu rồi sổ. Sổ là **tab gạch chân mực** (Carbon / InstUI) — `Kỹ năng` mặc định, `?so=giao` cho bài giao. Không viên thuốc. Tab trình duyệt ngắn: `Lộ trình` / `Ngân bài` / `Lịch` / `Kho` (template `· Học toán với AI`). Ray HS: Lộ trình, Ngân bài, Lịch, Kho.

Đề = KaTeX từ `statementLatex`. Một thang 4 mức — không Bloom trên trang này (3 mức CV 7991 chỉ ở cổng GV). Thành thạo: tên ngắn, ô sát tên, hàng yếu lên trên, hàng đang gợi tô `wash`. Bài giao ≠ ngân bài: chỉ assignment chưa đạt.

## Lưới và nhịp (SOTA 2026-09-27)

Một lưới **8 px** — cùng hệ Apple HIG, Material 3, IBM Carbon. Bậc: 4 / 8 / 12 / 16 / 24 / 32 / 48 (`--space-1` … `--space-8`). Không `20`, `10`, `14` trừ khi là cỡ chữ.

| Chỗ | Token | Lý do |
| --- | --- | --- |
| Nhãn → ô | 8 | NN/g form: đủ tách, vẫn một nhóm |
| Ô ↔ ô trong form | 16 | Một trường, không dính hàng |
| Khối trong phiếu | 16–24 | Hàng việc Classroom |
| Mục trang | 32 | Nhịp section Carbon / Stripe |
| Trang ↔ mép | 16 / 24 / 32 | `px-4` / `sm:px-6` / `lg:px-8` |

`padding` nới mục tiêu chạm. `margin` chỉ đẩy bố cục, **không** nới hit (Smashing Magazine, *Hit Areas*, 2024; Fitts 1954).

## Giải phẫu nút

Không lấy viên thuốc Material Expressive. Giữ phiếu: chữ nhật, bán kính **6 px**.

| Trục | Giá trị | Nguồn |
| --- | --- | --- |
| Cao thị giác | 40 px | Material 3 default button; Carbon productive |
| Cao chạm (thô / AAA) | 44 px | [WCAG 2.2 SC 2.5.5](https://www.w3.org/WAI/WCAG22/Understanding/target-size-enhanced.html) 44×44; [Apple HIG](https://developer.apple.com/design/human-interface-guidelines/buttons) 44×44 pt (vẫn sau Liquid Glass, 12/2025) |
| Tối thiểu AA | 24×24 | [WCAG 2.2 SC 2.5.8](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html) |
| Đệm ngang | 16 px | Material 3 Expressive (5/2025) khuyến 16 dp, không 24 |
| Chỉ biểu tượng | 44×44 | Cùng HIG / 2.5.5 — không `p-1.5` |
| Ô xét dấu trong bảng | 32×32 | Dày phiếu; trên 24 AA, dưới 44 để bảng không vỡ |

Nút `md` = `min-h-10 px-4`. `@media (pointer: coarse)` → `min-h-11`. Dùng `Button` / `buttonClasses`.

Thanh công cụ điện thoại 48 px (`h-12`) — Material touch 48 dp, cao hơn 44 một nấc lưới.

## Thành phần

- Nút primary = mực đặc, bán kính 6 px, giải phẫu trên.
- Bước 5 bước = cột số bên trái phiếu, không chip viên thuốc. Mỗi hàng bước `min-h-11`.
- Sai = viền `mark` + banner dưới bước (`px-4 py-3`).
- Composer gia sư: `textarea` tối thiểu 44, nút Gửi **luôn** 44×44 (PR #3 / WCAG 2.5.5), Enter gửi / Shift+Enter dòng, «Đang nghĩ…» thay vì stream. Lấy nhịp ChatGPT / Claude / Open WebUI — không lấy bong bóng gradient.
- Kết nối tài khoản: hai bước như Notion/Linear (mở trang chính thức → dán một lần). Không nút xanh ChatGPT, không logo.
- Không hero navy, không số khổng lồ trên 3 thẻ giống nhau.

## Tâm quang học (toán FE)

Quy tắc bố cục đứng của sản phẩm này — không căn theo cảm tính từng lần.

Mắt **không** đậu ở giữa hình học. Tâm quang học (*optical / visual center*) nằm **hơi trên** điểm giữa khung; đặt khối đúng 50% nhìn thấp (đặc biệt form nặng đáy: ô, CTA, chip). Cùng nguyên lý khung tranh / mat board: mép dưới dày hơn mép trên.

| Đại lượng | Giá trị | Việc |
| --- | --- | --- |
| Tâm hình học | 50% từ đỉnh khung | `place-items-center` / `items-center` trên cả viewport — nhìn chìm |
| Tâm quang học | **≈ 46%** từ đỉnh khung | Chỗ mắt vào trang trước |
| Tỉ lệ khoảng dư | dưới : trên = **3 : 2** | Spacer `flex-[2_1_0]` trên + `flex-[3_1_0]` dưới, cả hai `min-h-0` |
| Cấm | `translateY(-Nvh)` cứng | Lệch theo viewport; trên điện thoại dễ đè header / cắt form |

Thước: tâm hộp bao của khối chính (logo + tiêu đề + form) ≈ 45–46% `dvh` trên desktop và điện thoại đủ cao. Màn thấp (form + chân trang ≥ viewport): spacer co về 0, không overlap.

`/dang-nhap` áp đúng cái này: không header trùng logo; một mark giữa form (link `/`); chân trang nhẹ **ngoài** phép đo tâm.

Không lấy tỉ lệ vàng 1 : 1,618 cho trục đứng login — quá cao, đã loại.

Nguồn: optical center ~46% từ đỉnh (bố cục in / biển hiệu); cân quanh tâm quang học (Duke CCP *Graphic Design Principles*); mat board dày đáy; căn quang học ≠ căn số (Rails Designer). Không chép UI Google/Apple.

## A11y

Skip link, `:focus-visible` mực, `prefers-reduced-motion`, `aria-live` khi chấm. Giữ `data-testid`. `touch-action: manipulation`.

## Nguồn khoảng cách (không chép thương hiệu)

- W3C WCAG 2.2 Understanding 2.5.5 / 2.5.8 (2023, vẫn hiệu lực 2026).
- Apple Human Interface Guidelines — Buttons, *iOS/iPadOS*, cập nhật Liquid Glass 12/2025.
- Google Material 3 — Buttons; Material 3 Expressive (Google I/O / 5/2025) horizontal padding.
- IBM Carbon — 8 px spacing, productive button 40 / 48.
- Fitts, P. M. (1954). *The information capacity of the human motor system*.
- Smashing Magazine (2024). *Designing Better Target Sizes*.
