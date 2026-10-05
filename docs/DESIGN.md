# Hệ thống thiết kế — Phòng Wiii, bảng phấn 3b1b

**Hướng từ 2026-10-06 (chủ repo):** giao diện v2 (`apps/frontend`, Angular) theo kiểu ứng dụng Wiii của chủ repo, pha toán học kiểu 3Blue1Brown. Khung ứng dụng ấm như Wiii (sáng mặc định, tối theo máy hoặc nút đổi); mọi vùng toán (công thức lớn, bảng xét dấu, đồ thị) là tấm bảng tối kiểu 3b1b ở cả hai chế độ. Nghiên cứu, ảnh mô phỏng và lý do: `labs/design/studies/2026-10-06-wiii-3b1b.md`. Cấu trúc trang (phiếu, hàng việc, mục lục năm bước, quy tắc chữ) giữ như dưới; màu, chữ, bán kính, bóng đổi theo hướng mới. v0 (`apps/web`) đóng băng, giữ giao diện cũ.

Không dùng kit Figma EduPlus / Eduva / EduTracker / EduFlow (Poppins, thẻ KPI, gradient). Đó chính là slop. Nghiên cứu **sản phẩm đang chạy**, lấy cấu trúc chứ không lấy thương hiệu.

## Nguồn đã xem

| Sản phẩm | Việc lấy | Việc không lấy |
| --- | --- | --- |
| [Khan Academy Wonder Blocks](https://khan.github.io/wonder-blocks/) + [learner dashboard 2025](https://qasimbrown.design/projects/ai-powered-learner-dashboard) | Bài tiếp theo đứng trước catalog; mức vững = hàng ô; mật độ chữ cao; ít kiểu chữ | Xanh `#1865f2`, Lato, logo |
| [Brilliant solvables](https://www.paigeormiston.com/brilliant) | Một luồng bước; sai = banner ngay dưới bài; một CTA | Pear / CoFo / logo |
| [Canvas InstUI](https://instructure.design/) | Ray điều hướng đặc, việc là danh sách/bảng | Electric brand, widget KPI |
| [Google Classroom](https://support.google.com/chrome/a/answer/15210733) | Hàng việc 2 dòng (tên + meta), không lưới thẻ khóa học | Material purple |
| Linear / Stripe Dashboard | Kẻ 1 px, không bóng mềm dưới mọi thẻ | — |
| Wiii desktop (`github.com/meiiie/wiii`, `wiii-desktop/src/styles/globals.css`, commit `6b1c30b`; đọc mã 2026-10-06, độ tin cao cho giá trị CSS) | Bảng màu ấm sáng / tối theo biến CSS, đất nung cho hành động, bóng ba mức rất nhẹ, easing `cubic-bezier(0.165, 0.85, 0.45, 1)`, thanh bên 260 px, vệt màu mờ ở góc nền; thẻ thương hiệu: lưới mờ trên nền than, nhãn chữ hoa giãn chữ màu xanh nhạt | Mascot Neko, logo, chữ «Wiii» |
| 3Blue1Brown / Manim (stencil `github.com/ghanemja/stencil`, `pptx-math-3b1b/scripts/style.py`, commit `8f5c5a2`; mã màu đối chiếu 3b1b/manim `fafa083` và ManimCE `8dbfd3b` ngày 2026-10-06, độ tin cao; nền `#1C1C1C` là lựa chọn của stencil, Manim mặc định `#333333`) | Nền bảng `#1C1C1C`, màu nhấn BLUE_C `#58C4DD`, TEAL_C `#5CD0B3`, YELLOW_D `#F4D345`, RED_C `#FC6255`, GOLD_C `#F0AC5F`, GREEN_C `#83C167`, GREY_C `#888888` cho kẻ mang nghĩa; nét cong dày bo tròn; công thức serif Computer Modern (KaTeX) | Video, nhân vật Pi |
| Desmos (`www.desmos.com/calculator`; xem bằng Chromium 2026-10-06, một hàm, độ tin trung bình) | Đồ thị là đối tượng chính, lưới mờ, điểm đặc biệt có chấm, bấm thì hiện tọa độ | Màu, logo |

Coursera `#0056D2`, IBM `#0F62FE`, Khan blue, Canvas electric: không chép. Từ 2026-10-06, nền kem và đất nung của Wiii được dùng theo chỉ đạo của chủ repo (thay dòng cấm «cream + Literata + terracotta» cũ); Literata vẫn không dùng.

## Brief

Sản phẩm là **một trang toán**: công thức là điểm nhìn, năm bước là mục lục, gia sư chỉ mở khi được hỏi. Công thức, bảng xét dấu và đồ thị nằm trên tấm bảng tối kiểu 3b1b, nên toán nổi lên khỏi khung ấm. Chữ giao diện tiết chế; biểu thức mang sự biểu đạt. Không landing phần mềm (khẩu hiệu lớn, khung trình duyệt, ba thẻ tính năng).

## Token

Chữ đạt ≥ 4,5 : 1, thành phần giao diện ≥ 3 : 1. Số trong bảng tính theo WCAG 2.x (độ chói sRGB) bằng `node scripts/tuong-phan-token.mjs`: script đọc thẳng bảng này, CI chạy ở mọi PR, đỏ khi một cặp dưới ngưỡng hay khi một chỗ ghi «dưới ngưỡng» lại đạt. Số đo trên giao diện đã vẽ (điểm ảnh thật, vòng focus, lần vẽ đầu) thuộc PR áp token vào Angular (#146), chưa có trong cây này. Chế độ tối: `prefers-color-scheme: dark`, hay `data-theme` trên `<html>` khi người dùng bấm nút đổi (nhớ trong `localStorage`).

| Tên | Sáng | Tối | Việc |
| --- | --- | --- | --- |
| `canvas` | `#FAF9F5` | `#1E1D1B` | Nền trang (Wiii `--surface`) |
| `wash` | `#F0EEE6` | `#282724` | Thanh bên (cả khi là ngăn kéo trên điện thoại), khối phụ |
| `raise` | `#FFFFFF` | `#353330` | Thẻ nổi, ô nhập. Không đặt `accent` hay `mark` làm chữ / biểu tượng trên `raise` tối (`accent` 2,99 : 1, `mark` 4,21 : 1) |
| `line` | `#D8D5CD` | `#3A3935` | Kẻ chia 1 px (trang trí, không phải ranh giới duy nhất của một điều khiển) |
| `line-strong` | `#8A877F` | `#82807A` | Viền ô nhập, nút phụ, ranh giới điều khiển: ≥ 3 : 1 trên `canvas`, `wash`, `raise` (WCAG 1.4.11) |
| `ink` | `#141413` | `#E8E8E4` | Chữ chính |
| `ink-2` | `#3D3D3A` | `#C9C8C2` | Chữ phụ đậm |
| `muted` | `#5F5E58` | `#A8A7A2` | Chữ phụ (Wiii `--text-tertiary` chỉnh đậm cho AA) |
| `accent` | `#C75B39` | `#C75B39` | Vạch mục đang chọn, dấu «+» của chữ hiệu (sáng) |
| `focus` | `#C75B39` (3,63–4,21 : 1 trên `wash`, `canvas`, `raise`) | `#D78970` = `accent` 72 % pha trắng như Wiii (4,62–6,18 : 1; `#C75B39` chỉ 2,99 : 1 trên `raise` tối) | Viền focus 2 px, `outline-offset: 2px` để viền không chạm nền nút. Mọi điều khiển trong thanh trên 48 px vẽ viền vào trong (`-2px`): điều khiển cao 44 px, viền ngoài bị cắt ở mép trên màn; `focus` vẫn ≥ 3 : 1 với nền nút |
| `action` | `#AE5630` | `#AE5630` | Nền nút chính, chữ trắng 5,03 : 1 (`#C75B39` với chữ trắng chỉ 4,21 : 1) |
| `action-hover` | `#9A4A28`, chữ trắng 6,21 : 1 | `#C4633A`, chữ `#141413` 4,56 : 1 | Nút chính khi trỏ; trên nền tối chữ trắng chỉ 4,04 : 1 nên đổi sang chữ mực |
| `label` | `#2C6FB0` | `#58C4DD` | Nhãn khu vực chữ hoa nhỏ giãn chữ; dấu «+» của chữ hiệu (tối) |
| `pass` | `#1A7A45` | `#5CD0B3` | Đạt |
| `mark` | `#B83B2E` | `#FC6255` | Bút đỏ: sai — vẫn **một** màu nhớ. Chữ `mark` chỉ trên `canvas`, `wash` (trên `raise` tối chỉ 4,21 : 1) |
| `wait` | `#8A5A00` | `#F0AC5F` | Chờ |
| `board` | `#1C1C1C` | `#161615` | Tấm bảng toán (luôn tối) |
| `board-line` | `#3A3A3A` | `#34332F` | Kẻ trang trí trên bảng (lưới đồ thị, viền tấm bảng): 1,50 / 1,43 : 1, không mang nghĩa |
| `board-rule` | `#888888` | `#888888` | Kẻ mang nghĩa trên bảng: đường kẻ bảng xét dấu (hàng, cột điểm tới hạn), trục đồ thị. Manim `GREY_C`, 4,81 / 5,11 : 1 trên `board` |
| `board-ink` | `#F2EFE6` | `#F2EFE6` | Chữ và công thức trên bảng |
| `m-blue` / `m-teal` / `m-yellow` / `m-red` / `m-gold` / `m-green` | `#58C4DD` / `#5CD0B3` / `#F4D345` / `#FC6255` / `#F0AC5F` / `#83C167` | như sáng | Màu Manim, **chỉ** trên bảng: đường cong xanh, điểm đặc biệt vàng, dấu dương ngọc, dấu âm đỏ |

Bảng này là nguồn chuẩn của giá trị và việc; `apps/frontend/src/styles.css` hiện thực theo bảng (từ PR #146; trước đó CSS còn token cũ). Đổi màu thì sửa bảng trước, chạy `node scripts/tuong-phan-token.mjs`, rồi sửa CSS theo.

## Chữ

- Giao diện: chồng phông hệ thống như Wiii — `system-ui, -apple-system, "Segoe UI", "Noto Sans", sans-serif`. Chạy offline, không tải phông, tiếng Việt đủ dấu trên Windows, macOS, Android, iOS. Bỏ IBM Plex.
- Công thức: KaTeX (họ Computer Modern, đúng chất 3b1b). Mã bài và số: `ui-monospace, "Cascadia Mono", Consolas, monospace`.
- Chữ hiệu «MathL+»: đậm 800, giãn −0,02 em; dấu «+» màu `accent` (sáng) hay `label` (tối; cùng giá trị BLUE_C nhưng là token của khung, vì màu Manim chỉ trên bảng).
- Heading trang 28/36, đậm 700. Nhãn khu vực: 12 px, chữ hoa, giãn 0,14 em, màu `label`.

## Bố cục

```
+--------+--------------------------------+
| THANH  | trang `canvas`                 |
| BÊN    | phiếu / danh sách / bảng       |
| 260px  | toán trên tấm `board`          |
| `wash` |                                |
+--------+--------------------------------+
```

Desktop: không top bar. Điện thoại: top bar + ngăn kéo (cùng khối thanh bên, nền `wash`, bóng `lg`). Mục đang chọn: nền nhạt theo `accent` và vạch trái 3 px `accent`. Nền trang có vệt màu mờ ở góc như Wiii; chế độ tối thêm lưới mờ 32 px như thẻ thương hiệu Wiii. Bóng ba mức của Wiii chỉ cho thẻ nổi và ngăn kéo, không đặt dưới mọi thẻ. Skip `#noi-dung`.

Thẻ không còn là đơn vị mặc định. Việc = hàng (Classroom). Bài đang làm = một phiếu (Brilliant). Thành thạo = hàng ô (Khan).

## Lộ trình học sinh (`/hs`)

Không dashboard thẻ. Phiếu là bài kế: thân đề, công thức lớn, một dòng `02 / Đạo hàm` của bước đang làm, rồi `Làm bước tiếp`. Không năm ô tròn bằng nhau.

```
+--------+---------------------------+------------------+
| THANH  | Chào An                   | 12A1 thử         |
| BÊN 260| Đơn điệu và cực trị       |                  |
|        +---------------------------+------------------+
|        | PHIẾU                     | SỔ (tab gạch)    |
|        | đề                        | Kỹ năng | Bài tập |
|        | công thức lớn             | (một panel)      |
|        | 02 / Đạo hàm              |                  |
|        | [Làm bước tiếp]           |                  |
+--------+---------------------------+------------------+
```

Desktop (`lg`): cột phiếu `1fr` | kẻ 1 px | cột sổ `18–24rem`. Điện thoại / máy tính bảng: phiếu rồi sổ. Sổ là **tab gạch chân `accent`** (Carbon / InstUI) — `Kỹ năng` mặc định, `?so=giao` cho bài tập. Không viên thuốc. Tab trình duyệt ngắn: `Học` / `Đề bài` / `Lịch` / `Công thức` (template `· MathL+` ở v2, #124; v0 giữ `· Học toán với AI`). Ray HS cùng bốn chữ đó (testid `nav-hs-*` giữ nguyên).

Đề = KaTeX từ `statementLatex`. Một thang 4 mức — không Bloom trên mặt học sinh (3 mức CV 7991 chỉ ở cổng GV). Sổ kỹ năng: tên + mức 4 + ô vững; không câu giải thích UI. Hàng yếu lên trên, hàng đang gợi tô `wash`. Tab Bài tập ≠ Đề bài: chỉ bài thầy cô giao chưa đạt. Số chưa làm chỉ trên tab Bài tập. Mục lục đủ tên năm bước nằm trên trang làm bài, không trên phiếu. Nút trên điện thoại: `Kiểm tra` cạnh `Cần gợi ý?`.

**Quy tắc chữ và UI (khóa):** mỗi câu, mỗi chữ, mỗi ô một việc — không việc thì bỏ. Áp dụng **học sinh và giáo viên**. Tiếng lớp 12 / phòng giáo viên, không calque LMS/NCKH (`ngân bài`, `ngân hàng`, `hàng đợi`, `sinh biến thể`, `cổng`, `kho` trừ khi là kho thật, `phát hành`, `duyệt cổng`, `ngưỡng`, `nấc`, `Em` trên chrome, Bloom, mã `T12`/`DH12` trên UI). Không câu giải thích UI. Không lặp cùng một số ở hai chỗ. Tên mục = việc của trang.

**Chữ học sinh:** tiêu đề phiếu `Bài tiếp theo`. Tab `Bài tập`. CTA khóa: `Làm bước tiếp`. Heading khóa: `Chào An`. `/hs/kho`: một letterhead + tab `Công thức` / `Tài liệu`. Hàng công thức = KaTeX rồi tên; tiếng Việt trong `\text{}`; không hộp xám, không câu nói lại công thức, không cột 5 bước trên mặt. `/hs/lich`: **bảng tuần** T2–CN × giờ (thời khóa biểu). Một câu yếu nhất trên đầu. Ô buổi = việc, bấm về `/hs` — không dòng nhắc dưới ô. Không danh sách giả lịch, không hàng «Buổi tối nay» giữa các thứ. Gia sư HS: không URL localhost, không «không gọi API» trên chrome.

**Chữ giáo viên:** ray `Lớp` / `Duyệt` / `Đề bài` / `Tạo đề` / `Tài liệu` / `Công thức` / `Mức` / `Gia sư` / `Cài lớp` (testid `nav-gv-*` giữ nguyên). Heading khóa: `Lớp 12A1 thử`, `Cài đặt lớp`, `Kết nối ChatGPT`. `/gv` = letterhead + hàng việc (Gia sư, kẹt, chờ, chặn, đã mở) — không nút trùng ray. Hàng kẹt dẫn `Mức`. Gia sư: dán khóa ChatGPT / OpenRouter / Z.AI (coding) — không nhập URL; khối đầu ghi `ChatGPT`, không «Lớp»; ba nhà cùng chữ «Tạo một khóa, sao chép.»; **Cài lớp không dán khóa**, không lặp «Dán ở Gia sư», không điểm 9/9 trên mặt, mô hình trong `details`. Duyệt: không liệt kê công thức khi Không kiểm được; một căn cứ khi Đạt/Sai; lý do thôi «có trích dẫn». Trạng thái bài: `Đã mở` / `Chờ duyệt` / `Bị chặn`. Cổng 3 tầng giữ `Đạt` / `Sai` / `Không kiểm được`; lý do không calque «máy tự kiểm». Banner GV vào bằng `phieu-vao`. 3 mức CV 7991 chỉ ở `/gv/tien-do?muc=3`. Mức trên điện thoại = danh sách từng em; `md+` mới bảng.

**Trang công khai:** `/` chia như một trang mở: chữ trái, hình `y = x^3 - 3x` phải (kéo `x`, tiếp tuyến và `y′` đổi theo). Heading khóa `Học toán với AI` ở đầu trang. Dưới là ba việc có thật (năm bước, một chủ đề, không đáp án) và mục lục năm bước — không thẻ icon giống nhau, không số học sinh giả, không ảnh kiến trúc. Dải cuối trên nền `board`: một câu và Vào học. `data-testid=vao-hoc` chỉ một nút trên header. Không khung trình duyệt, không SymPy, không email, không gắn nhãn cực trị trên hình. `/dang-nhap`: không câu «tài khoản thử» dưới tiêu đề; mật khẩu thử chỉ khi sai.

**Trang làm bài** (`/hs/luyen`): từ `sm` mục lục trái — số mono, tên bước, vạch trái 2 px ở bước đang làm (bút đỏ nếu sai). Dưới `sm` cùng mục lục thành một dòng phía trên, vạch dưới, tên ngắn, để công thức lấy hết chiều ngang. Không nền mực cho cả hàng. Trang: `02 / Đạo hàm`, một câu việc của bước, công thức lớn căn trái, chỗ viết ngay dưới. Phản hồi một câu ngay dưới chỗ viết (vạch `mark` hoặc `pass`), không thẻ màu. Nút chính `Kiểm tra` / `Kiểm tra lại`; `Cần gợi ý?` là phụ và mới mở tờ gia sư. Không đồ thị bài đang chấm. Không cột gia sư khi chưa hỏi.

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

Không lấy viên thuốc Material Expressive. Chữ nhật bo nhẹ như Wiii: bán kính **8 px** cho nút, **12 px** cho thẻ.

| Trục | Giá trị | Nguồn |
| --- | --- | --- |
| Cao thị giác | 40 px | Material 3 default button; Carbon productive |
| Cao chạm (thô / AAA) | 44 px | [WCAG 2.2 SC 2.5.5](https://www.w3.org/WAI/WCAG22/Understanding/target-size-enhanced.html) 44×44; [Apple HIG](https://developer.apple.com/design/human-interface-guidelines/buttons) 44×44 pt (vẫn sau Liquid Glass, 12/2025) |
| Tối thiểu AA | 24×24 | [WCAG 2.2 SC 2.5.8](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html) |
| Đệm ngang | 16 px | Material 3 Expressive (5/2025) khuyến 16 dp, không 24 |
| Chỉ biểu tượng | 44×44 | Cùng HIG / 2.5.5 — không `p-1.5` |
| Ô xét dấu trong bảng | 32×32 chuột; 44×44 `pointer: coarse` | Dày phiếu trên desktop; điện thoại đủ HIG 44 |

Nút `md` = `min-h-10 px-4`. `@media (pointer: coarse)` → `min-h-11`. Dùng `Button` / `buttonClasses`.

Thanh công cụ điện thoại 48 px (`h-12`) — Material touch 48 dp, cao hơn 44 một nấc lưới.

## Thành phần

- Nút primary = nền `action` (đất nung), chữ trắng, bán kính 8 px, giải phẫu trên. Nút phụ: nền `raise`, viền `line-strong` (≥ 3 : 1; `line` chỉ là kẻ chia).
- Bước 5 bước = mục lục tên + số mono, vạch 2 px ở bước hiện tại. Mỗi hàng `min-h-11`. Không chip viên thuốc, không tô nền đặc cả hàng.
- Sai = vạch `mark` trên mục và dòng phản hồi ngay dưới chỗ viết. Ô sai trong bảng vẫn `cell-bad`.
- Composer gia sư: chỉ khi bấm `Cần gợi ý?`. Dưới `lg` là tờ full màn; `lg+` là tờ phải 24 rem, có Đóng. Không chiếm cột khi đóng. Thanh đáy điện thoại: **Kiểm tra + Cần gợi ý?** Chip «Sai chỗ nào?» (gửi vẫn «Em sai chỗ nào?»). `visualViewport` khi bàn phím, composer đáy, Đóng 44. `textarea` tối thiểu 44, nút Gửi **luôn** 44×44, Enter gửi / Shift+Enter dòng / Escape Dừng hoặc đóng tờ. Ô vẫn gõ được lúc đang nghĩ. Cuộn theo đáy (Open WebUI); kéo lên thì giữ chỗ, có «Xuống». Lỗi: «Hỏi lại» đổ câu vào ô — không tự gửi. SSE `trang_thai` kho/gọi/lọc rồi `xong` — **không** xả token. Chờ = 3 ô CSS (bước đang làm nhịp, scale 0,85↔1) + chữ `Đang nghĩ…` / `Đang mở công thức…` / `Đang hỏi gia sư…` / `Đang kiểm lời…`. Câu mới và dòng chấm vào bằng `phieu-vao` (180 ms, 6 px); tờ dưới `lg` dùng `to-len` (200 ms, 16 px). Không khối SVG trang trí, không bong bóng gradient, không gọi lại model khi SSE lỗi. Hình trang chủ là đồ thị của hàm minh họa, không phải họa tiết.
- Lời gia sư: Markdown + KaTeX (nhịp Claude / assistant-ui / Open WebUI — flush trái trên `canvas`, học sinh mới có bong bóng `wash` (sáng) / `raise` (tối) như Wiii). Cột hẹp: đoạn ngắn, danh sách, công thức căn trái cuộn ngang. Chuẩn hóa `\[ \]`, `\(...\)`, `align` trần, hàng rào `latex`. KaTeX lỗi thì chữ mờ, không hộp đỏ. Tiêu đề `#` thành chữ đậm cùng cỡ. Không HTML thô; lời gia sư không có bong bóng. Không tự giới thiệu trên mặt («Chào em», «Mình là AI…») — lời mở chỉ việc: đọc công thức, không đáp án. Trích dẫn: số `[n]` trong lời là badge (không lẫn số danh sách). Bấm số / chip = xem đúng đoạn trên phiếu. **Mở công thức** / **Mở tài liệu** mới về `#ct-` / `#tl-`. Một đoạn, không chồng 5–6.
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

Skip link, `:focus-visible` viền 2 px màu `focus` cách 2 px (mọi điều khiển trong thanh trên 48 px: vào trong 2 px), `prefers-reduced-motion`, `aria-live` khi chấm. Giữ `data-testid`. `touch-action: manipulation`.

## Nguồn khoảng cách (không chép thương hiệu)

- W3C WCAG 2.2 Understanding 2.5.5 / 2.5.8 (2023, vẫn hiệu lực 2026).
- Apple Human Interface Guidelines — Buttons, *iOS/iPadOS*, cập nhật Liquid Glass 12/2025.
- Google Material 3 — Buttons; Material 3 Expressive (Google I/O / 5/2025) horizontal padding.
- IBM Carbon — 8 px spacing, productive button 40 / 48.
- Fitts, P. M. (1954). *The information capacity of the human motor system*.
- Smashing Magazine (2024). *Designing Better Target Sizes*.
