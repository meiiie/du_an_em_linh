# Hệ thống thiết kế — Xanh–trắng, bảng toán navy

**Hướng hiện hành 2026-10-13 (chủ repo, lane THEME):** giao diện v2 (`apps/frontend`, Angular) dùng khung xanh–trắng: nền slate rất nhạt, bề mặt trắng, hành động xanh tự tin; tối theo máy hoặc nút đổi bằng nền navy và điểm nhấn xanh sáng. Vùng toán luôn là bảng tối, giữ cấu trúc và cách biểu đạt của 3Blue1Brown. Quyết định màu: [ADR 018](adr/018-bang-mau-xanh-trang.md). Nghiên cứu Wiii ngày 2026-10-06 là lịch sử tham khảo bố cục; bảng màu ấm đã được thay thế. `apps/web` đã gỡ theo yêu cầu chủ repo ngày 2026-10-13.

Không dùng kit Figma EduPlus / Eduva / EduTracker / EduFlow (Poppins, thẻ KPI, gradient). Đó chính là slop. Nghiên cứu **sản phẩm đang chạy**, lấy cấu trúc chứ không lấy thương hiệu.

## FLAT — chỉ đạo ưu tiên của chủ repo ngày 2026-10-13

Không bóng đổ, kể cả inset, token `--shadow-*` hay utility shadow. Không viền nhấn trái/inline-start, không dải màu ở mép hoặc khối 3D. Phân cấp bằng nền `canvas`/`wash`/`raise`, viền 1 px đều bốn cạnh, khoảng trắng và chữ. Focus dùng `outline` 2 px. Luật này thay mọi chỉ dẫn bóng/vạch trái cũ trong docs, rule, skill và nguyên mẫu; quyết định được ghi tại [ADR 014](adr/014-giao-dien-phang.md).

Cổng `node scripts/check-flat.mjs` quét CSS, HTML và `styles`/`template` nội tuyến TypeScript ở `apps/frontend/src`; báo tệp/dòng, thất bại khi phát hiện vi phạm. Scanner là kiểm tĩnh, nên rà giao diện vẫn cần kiểm dải màu tạo bằng hình ảnh hoặc CSS động. Test: `node --test scripts/check-flat.test.mjs`.

## Nguồn đã xem

| Sản phẩm | Việc lấy | Việc không lấy |
| --- | --- | --- |
| [Khan Academy Wonder Blocks](https://khan.github.io/wonder-blocks/) + [learner dashboard 2025](https://qasimbrown.design/projects/ai-powered-learner-dashboard) | Bài tiếp theo đứng trước catalog; mức vững = hàng ô; mật độ chữ cao; ít kiểu chữ | Xanh `#1865f2`, Lato, logo |
| [Brilliant solvables](https://www.paigeormiston.com/brilliant) | Một luồng bước; sai = banner ngay dưới bài; một CTA | Pear / CoFo / logo |
| [Canvas InstUI](https://instructure.design/) | Ray điều hướng đặc, việc là danh sách/bảng | Electric brand, widget KPI |
| [Google Classroom](https://support.google.com/chrome/a/answer/15210733) | Hàng việc 2 dòng (tên + meta), không lưới thẻ khóa học | Material purple |
| Linear / Stripe Dashboard | Kẻ 1 px, không bóng mềm dưới mọi thẻ | — |
| Wiii desktop (`github.com/meiiie/wiii`, `wiii-desktop/src/styles/globals.css`, commit `6b1c30b`; đọc mã 2026-10-06, độ tin cao cho giá trị CSS; phần thẻ thương hiệu là nhận định từ ảnh, độ tin trung bình) | Cấu trúc sáng / tối theo biến CSS (bảng màu ấm chỉ là tham chiếu lịch sử), easing `cubic-bezier(0.165, 0.85, 0.45, 1)`, thanh bên 260 px, vệt màu mờ ở góc nền; thẻ thương hiệu: lưới mờ trên nền than, nhãn chữ hoa giãn chữ màu xanh nhạt | Mascot Neko, logo, chữ «Wiii» |
| 3Blue1Brown / Manim (stencil `github.com/ghanemja/stencil`, `pptx-math-3b1b/scripts/style.py`, commit `8f5c5a2`; mã màu đối chiếu 3b1b/manim `fafa083` và ManimCE `8dbfd3b` ngày 2026-10-06, độ tin cao; nền `#1C1C1C` là lựa chọn của stencil, Manim mặc định `#333333`) | Nền bảng `#1C1C1C`, màu nhấn BLUE_C `#58C4DD`, TEAL_C `#5CD0B3`, YELLOW_D `#F4D345`, RED_C `#FC6255`, GOLD_C `#F0AC5F`, GREEN_C `#83C167`, GREY_C `#888888` cho kẻ mang nghĩa; nét cong dày bo tròn; công thức serif Computer Modern (KaTeX) | Video, nhân vật Pi |
| Desmos (`www.desmos.com/calculator`; xem bằng Chromium 2026-10-06, một hàm, độ tin trung bình) | Đồ thị là đối tượng chính, lưới mờ, điểm đặc biệt có chấm, bấm thì hiện tọa độ | Màu, logo |

Coursera `#0056D2`, IBM `#0F62FE`, Khan blue, Canvas electric: không chép. Từ 2026-10-13, bảng màu xanh–trắng của ADR 018 thay bảng màu ấm; Literata vẫn không dùng.

## Brief

Sản phẩm là **một trang toán**: công thức là điểm nhìn, năm bước là mục lục, gia sư chỉ mở khi được hỏi. Công thức, bảng xét dấu và đồ thị nằm trên tấm bảng tối kiểu 3b1b, nên toán nổi lên khỏi khung xanh–trắng. Chữ giao diện tiết chế; biểu thức mang sự biểu đạt. Không landing phần mềm (khẩu hiệu lớn, khung trình duyệt, ba thẻ tính năng).

## Token

Chữ đạt ≥ 4,5 : 1, thành phần giao diện ≥ 3 : 1. **Nguồn duy nhất của giá trị thực thi là `apps/frontend/src/styles.css`: `:root` và khối dark.** Bảng dưới là bản đối chiếu tài liệu được cổng kiểm tra xác nhận; component chỉ dùng `var(...)`, không định nghĩa lại màu. Script `node scripts/tuong-phan-token.mjs` đo trực tiếp CSS ở cả hai theme, kiểm bảng này khớp CSS, token được đo và các đối chứng trang trí vẫn dưới ngưỡng. Tỉ lệ là đầu ra script, không chép tay vào bảng.

Chế độ tối dùng `prefers-color-scheme` hoặc `data-theme` trên `<html>` (nhớ lựa chọn trong `localStorage`). Viền slate-300 dùng trang trí; viền điều khiển slate-500 để đạt tương phản. Màu trạng thái được làm dịu bằng nền tint (`color-mix` với bề mặt), giữ chữ đủ đậm ở sáng và đủ sáng ở tối. Bốn mức có tên đi kèm; màu không thay nhãn. Các màu `m-*` chỉ dùng trên bảng tối.

| Tên | Sáng | Tối | Việc |
| --- | --- | --- | --- |
| `canvas` | `#F8FAFC` | `#0B1220` | Nền trang |
| `wash` | `#EFF6FF` | `#111A2E` | Thanh bên, khối phụ xanh nhạt |
| `raise` | `#FFFFFF` | `#17233A` | Bề mặt và ô nhập |
| `line` | `#CBD5E1` | `#334155` | Viền trang trí slate; dưới 3 : 1 trên canvas |
| `line-strong` | `#64748B` | `#64748B` | Viền điều khiển; ≥ 3 : 1 trên cả ba bề mặt |
| `ink` | `#0F172A` | `#E2E8F0` | Chữ chính |
| `ink-2` | `#334155` | `#CBD5E1` | Chữ phụ đậm |
| `muted` | `#475569` | `#94A3B8` | Chữ phụ |
| `accent` | `#1D4ED8` | `#60A5FA` | Chữ / biểu tượng mục đang chọn |
| `focus` | `#1D4ED8` | `#60A5FA` | Outline 2 px, cách 2 px; ≥ 3 : 1 trên bề mặt và selection |
| `action` | `#1D4ED8` | `#60A5FA` | Nền nút chính |
| `action-ink` | `#FFFFFF` | `#0B1220` | Chữ nút chính; ≥ 4,5 : 1 trên action |
| `action-hover` | `#1E40AF` | `#3B82F6` | Nền nút chính khi trỏ (xanh đậm hơn) |
| `action-hover-ink` | `#FFFFFF` | `#0B1220` | Chữ nút khi trỏ; ≥ 4,5 : 1 trên action-hover |
| `label` | `#1D4ED8` | `#60A5FA` | Nhãn khu vực và liên kết |
| `pass` | `#166534` | `#86EFAC` | Đạt; xanh lá tiết chế, không dùng làm màu mức học |
| `mark` | `#B91C1C` | `#FCA5A5` | Sai / lỗi; đỏ dành cho lỗi và dấu âm trên bảng |
| `wait` | `#92400E` | `#FCD34D` | Chờ / cảnh báo; amber |
| `selection` | `#DBEAFE` | `#1E3656` | Nền xanh nhạt cho chọn / điều hướng hiện hành |
| `level-0` | `#1D4ED8` | `#60A5FA` | Nhận biết — xanh |
| `level-1` | `#0F766E` | `#5EEAD4` | Thông hiểu — teal |
| `level-2` | `#92400E` | `#FCD34D` | Vận dụng — amber |
| `level-3` | `#4338CA` | `#A5B4FC` | Vận dụng cao — indigo |
| `board` | `#0B1220` | `#0B1220` | Bảng toán, luôn tối |
| `board-line` | `#263449` | `#263449` | Lưới trang trí; dưới 3 : 1 trên board |
| `board-rule` | `#94A3B8` | `#94A3B8` | Kẻ mang nghĩa / trục; ≥ 3 : 1 trên board |
| `board-ink` | `#E2E8F0` | `#E2E8F0` | Chữ / công thức trên bảng |
| `m-blue` | `#60A5FA` | `#60A5FA` | Đường cong trên bảng |
| `m-teal` | `#5EEAD4` | `#5EEAD4` | Dấu dương trên bảng |
| `m-yellow` | `#FDE047` | `#FDE047` | Điểm đặc biệt trên bảng |
| `m-red` | `#FCA5A5` | `#FCA5A5` | Dấu âm trên bảng |
| `m-gold` | `#FCD34D` | `#FCD34D` | Nhấn amber trên bảng |
| `m-green` | `#86EFAC` | `#86EFAC` | Nhấn xanh lá trên bảng |

Token phụ: `brand-plus` tham chiếu `accent`; `mask` là màu đen chỉ dùng cho alpha mask; `grid-line` và `scrim` là nét lưới / lớp phủ trong suốt, tất cả khai báo cùng hai khối CSS. Không dùng chúng làm chữ hay ranh giới điều khiển. Focus luôn có outline 2 px; nền nút trong thanh trên phải đủ tương phản với focus khi outline vẽ vào trong.

## Chữ

- Giao diện: chồng phông hệ thống như Wiii — `system-ui, -apple-system, "Segoe UI", "Noto Sans", sans-serif`. Chạy offline, không tải phông, tiếng Việt đủ dấu trên Windows, macOS, Android, iOS. Bỏ IBM Plex.
- Công thức: KaTeX (họ Computer Modern, đúng chất 3b1b). Mã bài và số: `ui-monospace, "Cascadia Mono", Consolas, monospace`.
- Chữ hiệu «MathL+»: đậm 800, giãn −0,02 em; dấu «+» màu `accent` (sáng) ở cả hai theme.
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

Desktop: thanh bên 260 px. Điện thoại: thanh điều hướng đáy 4–5 mục, có safe-area; mục bổ sung trong ngăn kéo. Mục đang chọn dùng nền `selection` xanh nhạt, chữ `accent` đậm và `aria-current`, không vạch mép. Nền trang có vệt màu mờ ở góc như Wiii; chế độ tối thêm lưới mờ 32 px. Mọi thẻ và ngăn kéo phẳng, không bóng. Skip `#noi-dung`.

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

Desktop (`lg`): cột phiếu `1fr` | kẻ 1 px | cột sổ `18–24rem`. Điện thoại / máy tính bảng: phiếu rồi sổ. Sổ là **tab gạch chân `accent`** (Carbon / InstUI) — `Kỹ năng` mặc định, `?so=giao` cho bài tập. Không viên thuốc. Tab trình duyệt ngắn: `Học` / `Đề bài` / `Lịch` / `Công thức` (template `· MathL+`, #124). Ray HS cùng bốn chữ đó (testid `nav-hs-*` giữ nguyên).

Đề = KaTeX từ `statementLatex`. Một thang 4 mức — không Bloom trên mặt học sinh (3 mức CV 7991 chỉ ở cổng GV). Sổ kỹ năng: tên + mức 4 + ô vững; không câu giải thích UI. Hàng yếu lên trên, hàng đang gợi tô `wash`. Tab Bài tập ≠ Đề bài: chỉ bài thầy cô giao chưa đạt. Số chưa làm chỉ trên tab Bài tập. Mục lục đủ tên năm bước nằm trên trang làm bài, không trên phiếu. Nút trên điện thoại: `Kiểm tra` cạnh `Cần gợi ý?`.

**Quy tắc chữ và UI (khóa):** mỗi câu, mỗi chữ, mỗi ô một việc — không việc thì bỏ. Áp dụng **học sinh và giáo viên**. Tiếng lớp 12 / phòng giáo viên, không calque LMS/NCKH (`ngân bài`, `ngân hàng`, `hàng đợi`, `sinh biến thể`, `cổng`, `kho` trừ khi là kho thật, `phát hành`, `duyệt cổng`, `ngưỡng`, `nấc`, `Em` trên chrome, Bloom, mã `T12`/`DH12` trên UI). Không câu giải thích UI. Không lặp cùng một số ở hai chỗ. Tên mục = việc của trang.

**Chữ học sinh:** tiêu đề phiếu `Bài tiếp theo`. Tab `Bài tập`. CTA khóa: `Làm bước tiếp`. Heading khóa: `Chào An`. `/hs/kho`: một letterhead + tab `Công thức` / `Tài liệu`. Hàng công thức = KaTeX rồi tên; tiếng Việt trong `\text{}`; không hộp xám, không câu nói lại công thức, không cột 5 bước trên mặt. `/hs/lich`: **bảng tuần** T2–CN × giờ (thời khóa biểu). Một câu yếu nhất trên đầu. Ô buổi = việc, bấm về `/hs` — không dòng nhắc dưới ô. Không danh sách giả lịch, không hàng «Buổi tối nay» giữa các thứ. Gia sư HS: không URL localhost, không «không gọi API» trên chrome.

**Chữ giáo viên:** ray `Lớp` / `Duyệt` / `Đề bài` / `Tạo đề` / `Tài liệu` / `Công thức` / `Mức` / `Gia sư` / `Cài lớp` (testid `nav-gv-*` giữ nguyên). Heading khóa: `Lớp 12A1 thử`, `Cài đặt lớp`, `Kết nối ChatGPT`. `/gv` = letterhead + hàng việc (Gia sư, kẹt, chờ, chặn, đã mở) — không nút trùng ray. Hàng kẹt dẫn `Mức`. Gia sư: dán khóa ChatGPT / OpenRouter / Z.AI (coding) — không nhập URL; khối đầu ghi `ChatGPT`, không «Lớp»; ba nhà cùng chữ «Tạo một khóa, sao chép.»; **Cài lớp không dán khóa**, không lặp «Dán ở Gia sư», không điểm 9/9 trên mặt, mô hình trong `details`. Duyệt: không liệt kê công thức khi Không kiểm được; một căn cứ khi Đạt/Sai; lý do thôi «có trích dẫn». Trạng thái bài: `Đã mở` / `Chờ duyệt` / `Bị chặn`. Cổng 3 tầng giữ `Đạt` / `Sai` / `Không kiểm được`; lý do không calque «máy tự kiểm». Banner GV vào bằng `phieu-vao`. 3 mức CV 7991 chỉ ở `/gv/tien-do?muc=3`. Mức trên điện thoại = danh sách từng em; `md+` mới bảng.

**Trang công khai:** `/` theo trang Wiii trong meiiie-design-kit (chủ repo, 2026-10-06; đối chiếu: `labs/design/audits/2026-10-06-trang-chu.md`). Nền `raise` (tối: `canvas`), thẻ `canvas` / `wash`, nút viên `.vien-dam` (mực) và `.vien-kem` (kem) chỉ ở trang này. Mở đầu căn giữa: nhãn «Toán 12 · Đơn điệu và cực trị», tiêu đề hai dòng nét thường «Học toán theo từng bước. Tự mình hiểu ra.» với từ gạch chân màu Manim đổi lần lượt (trình đọc màn hình nghe câu cố định), hai nút viên, một dòng giới hạn của bản thử. «Xem MathL+ làm việc»: thẻ cảnh minh họa việc có thật (phiếu năm bước, gia sư, bảng toán kéo được, bảng công thức, mức hiểu), ghi «Minh họa». Rồi «Một bài. Năm bước.», dải bảng 3b1b (tổng riêng Fourier), «Chọn cách bắt đầu.» (học sinh / giáo viên, ảnh minh họa vẽ riêng), «Trước khi dùng», chân trang. Nền phần mở đầu là ảnh vẽ riêng (sáng / tối), giữa để trống cho chữ. Mọi chuyển động tự dừng trong 5 giây (từ xoay một vòng, đổi lần cuối ở 4 s, chuyển tiếp xong ở 4,56 s; dải vẽ một lần khi vào tầm nhìn, xong trong 2,4 s) nên không cần nút tạm dừng (WCAG 2.2.2); giảm chuyển động thì đứng yên. SEO ở `index.html`: mô tả, canonical, Open Graph / Twitter với ảnh 1200 × 630, JSON-LD, manifest, `robots.txt`, `sitemap.xml`; URL tuyệt đối là `__SITE_URL__`, nginx thay bằng biến `SITE_URL`. Hàm minh họa `y = 2x/(x² + 1)` (SP-10: không dùng hàm có trong ngân hàng bài). `data-testid=vao-hoc` chỉ một nút trên header. Không số học sinh giả, không lời khen giả, không khung trình duyệt, không SymPy, không gắn nhãn cực trị trên hình. `/dang-nhap`: không câu «tài khoản thử» dưới tiêu đề; mật khẩu thử chỉ khi sai.

**Trang làm bài** (`/hs/luyen`): từ `sm` mục lục trái — số mono, tên bước, nền nhạt và chữ đậm ở bước đang làm (chữ bút đỏ nếu sai). Dưới `sm` cùng mục lục thành một dòng phía trên, nền nhạt cho mục đang chọn, tên ngắn, để công thức lấy hết chiều ngang. Không nền mực cho cả hàng. Trang: `02 / Đạo hàm`, một câu việc của bước, công thức lớn căn trái, chỗ viết ngay dưới. Phản hồi một câu ngay dưới chỗ viết (biểu tượng và chữ `mark` hoặc `pass`), không thẻ màu. Nút chính `Kiểm tra` / `Kiểm tra lại`; `Cần gợi ý?` là phụ và mới mở tờ gia sư. Không đồ thị bài đang chấm. Không cột gia sư khi chưa hỏi.

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

Không lấy viên thuốc Material Expressive. Chữ nhật bo nhẹ như Wiii: bán kính **8 px** cho nút, **12 px** cho thẻ. Ngoại lệ: trang công khai `/` dùng nút viên và thẻ bo 16 px như trang Wiii, vẫn qua `appButton` (`variant="vien-dam" | "vien-kem" | "vien-vien"`, kiểu ở `.vien*` trong `styles.css`).

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
- Bước 5 bước = mục lục tên + số mono, nền nhạt và chữ đậm ở bước hiện tại. Mỗi hàng `min-h-11`. Không chip viên thuốc, không tô nền đặc cả hàng.
- Sai = chữ và biểu tượng `mark` trên mục và dòng phản hồi ngay dưới chỗ viết. Ô sai trong bảng vẫn `cell-bad`.
- Composer gia sư: chỉ khi bấm `Cần gợi ý?`. Ở ≤640 px là bottom sheet; từ đó tới `lg` là tờ đọc thích ứng; `lg+` là cột phải 24 rem khi mở, có Đóng. Không chiếm cột khi đóng. Thanh đáy điện thoại: **Kiểm tra + Cần gợi ý?** Chip «Sai chỗ nào?» (gửi vẫn «Em sai chỗ nào?»). `visualViewport` khi bàn phím, composer đáy, Đóng 44. `textarea` tối thiểu 44, nút Gửi **luôn** 44×44, Enter gửi / Shift+Enter dòng / Escape Dừng hoặc đóng tờ. Ô vẫn gõ được lúc đang nghĩ. Cuộn theo đáy (Open WebUI); kéo lên thì giữ chỗ, có «Xuống». Lỗi: «Hỏi lại» đổ câu vào ô — không tự gửi. SSE theo `docs/API_UX.md`: `kho`, `goi`, `loc` rồi `xong`/`loi` — **không** xả token. Chờ = 3 ô CSS (bước đang làm nhịp, đổi độ đậm) + chữ `Đang nghĩ…` / `Đang mở công thức…` / `Đang hỏi gia sư…` / `Đang kiểm lời…`. Câu mới và dòng chấm vào bằng `phieu-vao` (180 ms, 6 px); tờ dưới `lg` dùng `to-len` (200 ms, 16 px). Không khối SVG trang trí, không bong bóng gradient, không gọi lại model khi SSE lỗi. Hình trang chủ là đồ thị của hàm minh họa, không phải họa tiết.
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

Skip link, `:focus-visible` viền 2 px màu `focus` cách 2 px (vào trong 2 px ở mọi điều khiển của thanh trên 48 px và ở nút nằm trong ô nhập), `prefers-reduced-motion`, `aria-live` khi chấm. Giữ `data-testid`. `touch-action: manipulation`.

## Nguồn khoảng cách (không chép thương hiệu)

- W3C WCAG 2.2 Understanding 2.5.5 / 2.5.8 (2023, vẫn hiệu lực 2026).
- Apple Human Interface Guidelines — Buttons, *iOS/iPadOS*, cập nhật Liquid Glass 12/2025.
- Google Material 3 — Buttons; Material 3 Expressive (Google I/O / 5/2025) horizontal padding.
- IBM Carbon — 8 px spacing, productive button 40 / 48.
- Fitts, P. M. (1954). *The information capacity of the human motor system*.
- Smashing Magazine (2024). *Designing Better Target Sizes*.

## Hoàn thiện U0–U15 và chuyển động

[Spec 002](../specs/002-hoan-thien-v2/spec.md) là hợp đồng nghiệm thu U0–U15; màn Học lấy mức/bài kế từ server, nội dung Kho theo lớp, giao Ôn tập/Kiểm tra và hạn được server cưỡng chế. Mục mới theo vai trò có thể mở rộng cấu hình điều hướng, giữ heading/testid đã khóa. Trạng thái luôn có chữ/biểu tượng, không chỉ màu.

Chuyển trạng thái 120–200 ms; phiếu vào tối đa 6 px, tờ mở tối đa 16 px, không nâng thẻ khi hover, không khối 3D/perspective/rotateY. Flashcard đổi mặt bằng nội dung hoặc fade 2D, Enter/Space dùng được và focus giữ nguyên. `prefers-reduced-motion` tắt dịch chuyển/chuyển động lặp; chỉ báo chờ dùng hình tĩnh kèm chữ. Chuyển động tự chạy dừng trong 5 giây hoặc có nút dừng. Không animation thưởng/pháo giấy khi làm đúng.

Primitive dùng chung: rỗng, skeleton khớp bố cục, lỗi có «Thử lại», toast gộp trùng (3/4/5 giây) và hộp thoại phẳng. Lỗi cần hành động còn trong vùng tác vụ sau khi toast tắt. Hộp thoại/bottom sheet giữ focus, khóa cuộn và trả focus; bottom nav có safe-area, không che CTA hay ô nhập khi bàn phím mở. Tab hỗ trợ mũi tên/Home/End; progress có tên/giá trị ARIA.

Copy tiếng Việt đủ dấu, viết hoa đầu câu; nhãn cố gắng ≤5 từ, gợi ý ≤2 câu trừ nội dung toán cần thiết. Lỗi nói việc tiếp theo, không stacktrace/thuật ngữ kỹ thuật/emoji. Ảnh nghiệm thu 390/1280/1440 px sáng/tối; axe và kiểm bàn phím/reduced-motion bổ sung cổng FLAT tĩnh.
