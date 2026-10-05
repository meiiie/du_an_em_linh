# MathL+ — Nghiên cứu giao diện Wiii × 3b1b

Tạo bằng công cụ imagegen tích hợp. Phạm vi: 12 ảnh mô phỏng. Nội dung tương ứng giữ nhất quán giữa ba phương án.

Brief: [2026-10-06-wiii-3b1b.md](../../studies/2026-10-06-wiii-3b1b.md). Danh sách màn theo yêu cầu trực tiếp: Luyện bài 1280 × 800, Luyện bài 390 × 844, Học 1280 × 800, Đăng nhập 390 × 844 cho A, B, C.

Imagegen xuất ảnh desktop 1586 × 992 và điện thoại 853 × 1844. Từng ảnh được sao chép vào thư mục này rồi thu nhỏ toàn khung bằng nội suy bicubic để xuất đúng 1280 × 800 hoặc 390 × 844; không thêm chữ, vẽ lại thành phần hay cắt nội dung bằng mã. Các bản sửa thiết kế cũng do imagegen thực hiện.

Trong repo, ảnh lưu dạng WebP chất lượng 86 (Pillow 12.0, `method=6`): 12 ảnh PNG gốc 7 527 568 byte còn 325 370 byte; kích thước khung giữ nguyên. Các lời nhắc dưới đây nói «PNG» vì đó là định dạng lúc sinh.

Đã xem đủ 12 ảnh để kiểm tra chữ Việt, tên MathL+, nội dung màn và tính đúng của bảng dấu/điểm cực trị. Kích thước cùng định dạng PNG được kiểm tra sau khi xuất. Ảnh raster phục vụ so sánh thiết kế; chưa phải bằng chứng kiểm định WCAG hoặc giao diện chạy thật.

## Tệp và prompt nguyên văn

### A-luyen-1280.webp

[A-luyen-1280.webp](A-luyen-1280.webp) — 1280 × 800 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option A — “Phòng Wiii sáng, bảng phấn 3b1b”.
Warm light app chrome: page #FAF9F5, sidebar and secondary panels #F0EEE6, hairlines #D8D5CD, main ink #141413, secondary ink #3D3D3A, muted text #73726C. Primary button #C75B39. Very soft multi-layer shadows.
EVERY math area, including formula, sign table and cubic plot, is on its own dark chalkboard panel #1C1C1C, with white/cream Computer Modern serif math. Manim colors: curve blue #58C4DD, critical-point dots yellow #F4D345, positive signs and rising arrows teal #5CD0B3, negative signs and falling arrows red #FC6255. Precise smooth rounded curve strokes, subdued graph grid. Keep the rest of the UI light.

Canvas: exactly 1280 × 800 px, desktop viewport.
Layout: left sidebar exactly 260 px wide, full height. Text wordmark “MathL+” at top; navigation items “Học”, “Đề bài”, “Lịch”, “Công thức” with simple thin line icons; “Đề bài” is active. A small circular avatar with “A” and user name “An” at bottom.
Main practice column starts at x=292 and ends near x=1008; right collapsed tutor card is x=1032..1248. Header at y≈48: “Luyện bài” with generous hierarchy and a small theme toggle.
Problem copy at y≈108 is exactly “Xét tính đơn điệu và tìm cực trị của hàm số”. Immediately beneath, a dedicated wide math panel around y=152..232 holds the large centered formula “y = x³ − 3x² + 2.” Together these form the exact problem statement “Xét tính đơn điệu và tìm cực trị của hàm số y = x³ − 3x² + 2.”
At y≈264 a horizontal five-step progress rail has these five labels in order: “Tập xác định” with ✓, “Đạo hàm” with ✓, “Nghiệm của y′” with ✓, “Xét dấu y′” with a current-step ring and small “Đang làm”, “Kết luận” in the pending state. Exact readable labels; the current step is number 4.
Below, section title “Xét dấu y′”. A large sign table panel around x=292..1008, y=360..552. A proper Vietnamese sign/variation table with rows labelled serif “x”, “y′”, “y”. Across the x row, in left-to-right order, show “−∞”, “0”, “2”, “+∞”. Divide the y′ row into intervals and critical-point columns, showing exactly “+”, “0”, “−”, “0”, “+”. Align the zeros vertically beneath x = 0 and x = 2; the three signs occupy the open intervals. In the y row, show the variation −∞ ↗ 2 ↘ −2 ↗ +∞, aligned with the two critical points. Use colored rising and falling arrows. Clear thin table rules, generous spacing, no extra critical points.
Below the table, a small compact plot at x≈292..688, y≈568..744. The cubic graph is mathematically accurate for y = x³ − 3x² + 2: it comes from the lower left, rises to a local maximum at (0, 2), falls to a local minimum at (2, −2), then rises to the upper right. Mark ONLY those two critical points with colored dots and exact small labels “(0, 2)” and “(2, −2)”. Draw horizontal x and vertical y axes, a faint grid and an origin “0”. Do not confuse roots with critical points. The curve crosses the x-axis three times, approximately at −0.73, 1 and 2.73.
To the plot’s right, show a single primary button “Kiểm tra bước”, approximately 208 px wide, 40 px visual height, 44 px hit area, aligned comfortably in the lower workspace.
Right side: collapsed tutor card with title “Gia sư”, a chevron indicating collapse, and exactly one hint sentence wrapping naturally: “Quan sát dấu của y′ trên từng khoảng.” No chat conversation.
No invented scores, percentage counters, promotional copy or illustrations. Keep the formula, table and plot dominant.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

Prompt chỉnh sửa cuối, nguyên văn. Ảnh đầu vào là ảnh được tạo bởi prompt ngay phía trên:

```text
Use case: ui-mockup precise layout correction. Input image is the edit target, a finished MathL+ practice screenshot.
Make only these corrections to this image:
1. Widen the LEFT sidebar to exactly 20.3125% of the total canvas width (260 pixels in a 1280 × 800 logical viewport). In the supplied 1586 × 992 render this is about 322 image pixels. Keep the right tutor column at the same position and width. Reflow the main workspace so its left gutter is x=292 in the logical viewport (about x=362 in the supplied render), with a 32 logical-pixel gap after the sidebar. Preserve every current component, all copy, the existing typography, and the complete mathematics. Ensure the five-step rail and the sign table still fit in the main column.
2. Give the “Kiểm tra bước” primary button exactly 40 logical pixels visual height (49.6 pixels in this 1586-wide render), 44 logical pixels hit area, 6 logical pixels corner radius, a SOLID flat #C75B39 fill, and bold 20 logical-pixel white label.

Invariants: preserve wordmark “MathL+”; Vietnamese text with correct diacritics; formula y = x³ − 3x² + 2; critical points (0, 2) and (2, −2); correct table signs +, 0, −, 0, +; completed first three steps, fourth step current, last step pending; all other palette tokens, panel colors, exact labels, tutor hint, graph, nav and avatar remain unchanged. Do not add any content. No frame, mascot, Pi character or foreign logo. Keep canvas aspect ratio 1280:800. Return the single corrected full screen.
```

### A-luyen-390.webp

[A-luyen-390.webp](A-luyen-390.webp) — 390 × 844 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option A — “Phòng Wiii sáng, bảng phấn 3b1b”.
Warm light app chrome: page #FAF9F5, sidebar and secondary panels #F0EEE6, hairlines #D8D5CD, main ink #141413, secondary ink #3D3D3A, muted text #73726C. Primary button #C75B39. Very soft multi-layer shadows.
EVERY math area, including formula, sign table and cubic plot, is on its own dark chalkboard panel #1C1C1C, with white/cream Computer Modern serif math. Manim colors: curve blue #58C4DD, critical-point dots yellow #F4D345, positive signs and rising arrows teal #5CD0B3, negative signs and falling arrows red #FC6255. Precise smooth rounded curve strokes, subdued graph grid. Keep the rest of the UI light.

Canvas: exactly 390 × 844 px, mobile viewport, no phone hardware or browser frame.
This is the responsive version of the exact same desktop practice screen. Sidebar collapses to a 44 px menu button. Top bar at y=0..64: menu icon, text wordmark “MathL+”, small sun/moon theme toggle. 16 px side gutters.
At y≈80, heading “Luyện bài”. Under it, exact problem copy “Xét tính đơn điệu và tìm cực trị của hàm số” wraps into two or three clear lines. A separate math panel at y≈188..244 shows centered Computer Modern serif “y = x³ − 3x² + 2.” Together this is the exact same problem statement.
At y≈264..344, a compact horizontal five-step stepper with five equal-width columns and connecting line: “Tập xác định” ✓, “Đạo hàm” ✓, “Nghiệm của y′” ✓, “Xét dấu y′” with current ring number 4, “Kết luận” pending number 5. Labels wrap within columns, keep Vietnamese diacritics readable, and include “Đang làm” directly under the current-step label if needed.
At y≈360, title “Xét dấu y′”. Sign table in a horizontally scrollable 358 px card at y≈396..548; show a tiny unobtrusive horizontal scroll indicator at its base, keep the mathematical columns readable. A proper Vietnamese sign/variation table with rows labelled serif “x”, “y′”, “y”. Across the x row, in left-to-right order, show “−∞”, “0”, “2”, “+∞”. Divide the y′ row into intervals and critical-point columns, showing exactly “+”, “0”, “−”, “0”, “+”. Align the zeros vertically beneath x = 0 and x = 2; the three signs occupy the open intervals. In the y row, show the variation −∞ ↗ 2 ↘ −2 ↗ +∞, aligned with the two critical points. Use colored rising and falling arrows. Clear thin table rules, generous spacing, no extra critical points.
Small cubic plot card at y≈564..692, visible on this phone viewport. The cubic graph is mathematically accurate for y = x³ − 3x² + 2: it comes from the lower left, rises to a local maximum at (0, 2), falls to a local minimum at (2, −2), then rises to the upper right. Mark ONLY those two critical points with colored dots and exact small labels “(0, 2)” and “(2, −2)”. Draw horizontal x and vertical y axes, a faint grid and an origin “0”. Do not confuse roots with critical points. The curve crosses the x-axis three times, approximately at −0.73, 1 and 2.73.
Collapsed compact tutor row around y≈708..764, title “Gia sư”, chevron, and the single sentence “Quan sát dấu của y′ trên từng khoảng.” It wraps once if needed.
Sticky full-width bottom action area at y≈780..844, with a 40 px visual-height, at least 44 px touch-area terracotta button labelled “Kiểm tra bước”, 16 px side gutters. Do not cover any math or tutor copy. All content fits the stated viewport.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

### A-hoc-1280.webp

[A-hoc-1280.webp](A-hoc-1280.webp) — 1280 × 800 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option A — “Phòng Wiii sáng, bảng phấn 3b1b”.
Warm light app chrome: page #FAF9F5, sidebar and secondary panels #F0EEE6, hairlines #D8D5CD, main ink #141413, secondary ink #3D3D3A, muted text #73726C. Primary button #C75B39. Very soft multi-layer shadows.
EVERY math area, including formula, sign table and cubic plot, is on its own dark chalkboard panel #1C1C1C, with white/cream Computer Modern serif math. Manim colors: curve blue #58C4DD, critical-point dots yellow #F4D345, positive signs and rising arrows teal #5CD0B3, negative signs and falling arrows red #FC6255. Precise smooth rounded curve strokes, subdued graph grid. Keep the rest of the UI light.

Canvas: exactly 1280 × 800 px, desktop student home.
Sidebar exactly 260 px wide, full height, identical geometry to the practice screen: wordmark “MathL+” at top; items “Học”, “Đề bài”, “Lịch”, “Công thức”; “Học” is active; avatar “A” and name “An” at bottom.
Main workspace has 40 px padding and broad whitespace. Header heading “Chào An” at x≈300, y≈56, with a small theme toggle at the far right.
A wide, quiet next-assignment card around x=300..1224, y=128..288, title “Bài tiếp theo: Đơn điệu và cực trị” and subtitle “5 bước · hạn thứ Sáu”. Include one terracotta primary button “Vào học”. The title and subtitle together are exactly “Bài tiếp theo: Đơn điệu và cực trị · 5 bước · hạn thứ Sáu”. No decorative picture.
Below, section heading “Bài được giao”. Three clear horizontal assignment rows, all with matching spacing and a thin separator. Left titles and right textual statuses, EXACTLY:
“Bài 1 · Đơn điệu của hàm số” — “Chưa làm”
“Bài 2 · Đơn điệu và cực trị” — “Đang làm 3/5”
“Bài 3 · Cực trị của hàm số” — “Đạt”
Statuses must use readable text plus a small appropriate neutral, current, or completed marker; color never carries the state alone. Do not invent scores or extra problems.
Below at y≈600, a compact mastery strip headed “Mức độ”. Four equally weighted, quiet segments labelled exactly “Nhận biết”, “Thông hiểu”, “Vận dụng”, “Vận dụng cao”. No numbers, percentages, giant dashboard metrics, or fabricated attainment claims.
This home screen contains no formulas, sign tables or graphs; do not invent math panels. Harmonize with the same option’s practice screen.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

### A-dang-nhap-390.webp

[A-dang-nhap-390.webp](A-dang-nhap-390.webp) — 390 × 844 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option A — “Phòng Wiii sáng, bảng phấn 3b1b”.
Warm light app chrome: page #FAF9F5, sidebar and secondary panels #F0EEE6, hairlines #D8D5CD, main ink #141413, secondary ink #3D3D3A, muted text #73726C. Primary button #C75B39. Very soft multi-layer shadows.
EVERY math area, including formula, sign table and cubic plot, is on its own dark chalkboard panel #1C1C1C, with white/cream Computer Modern serif math. Manim colors: curve blue #58C4DD, critical-point dots yellow #F4D345, positive signs and rising arrows teal #5CD0B3, negative signs and falling arrows red #FC6255. Precise smooth rounded curve strokes, subdued graph grid. Keep the rest of the UI light.

Canvas: exactly 390 × 844 px, mobile login viewport. Edge-to-edge flat screen, no device frame.
Generous calm whitespace. Simple text wordmark “MathL+”, no icon or mascot. Small sun/moon theme-toggle icon near the top-right, with 44 px target.
Single restrained login composition, 32 px side gutters, optical center around 46% of viewport height, approximately 3:2 lower versus upper free space. Product mark near y≈216, heading “Đăng nhập” near y≈288.
One visible field with label “Email”, placeholder “Nhập email của bạn”, subtle hairline border and 44 px minimum target, near y≈360. One full-width terracotta primary button “Tiếp tục”, 40 px visual height, 44 px touch area, near y≈440.
Below around y≈512, two small secondary demo buttons placed side by side, both at least 44 px touch targets, exact labels “Học sinh An” and “Giáo viên”. No password field, no social provider, no extra navigation, no marketing slogan.
Footer near the lower part of the viewport, centred and comfortably legible, wraps into three short lines as needed, exact complete text: “Toán 12 · đơn điệu và cực trị · Tài khoản thử — không có học sinh thật”.
There are no formulas, sign tables or plots on this login screen; do not add chalkboard decoration. Keep the page aligned to the option’s application palette.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

### B-luyen-1280.webp

[B-luyen-1280.webp](B-luyen-1280.webp) — 1280 × 800 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option B — “Wiii tối toàn phần”.
Whole application is warm charcoal: page #1E1D1B, sidebar/cards #282724, raised surfaces #353330, thin hairlines #3A3935, text #E8E8E4, secondary text #A8A7A2. A very faint fine grid texture appears in the page background, as in a restrained warm charcoal brand card. Small section labels are letter-spaced Vietnamese uppercase in light blue #58C4DD. Primary button #C75B39. Restrained layered shadows and softly rounded panels with thin borders.
Math is cream Computer Modern serif. Manim colors only on mathematical strokes and status accents: curve #58C4DD, critical-point dots #F4D345, positive signs/rising arrows #5CD0B3, negative signs/falling arrows #FC6255. Plot grid faint; curve smooth and rounded.

Canvas: exactly 1280 × 800 px, desktop viewport.
Layout: left sidebar exactly 260 px wide, full height. Text wordmark “MathL+” at top; navigation items “Học”, “Đề bài”, “Lịch”, “Công thức” with simple thin line icons; “Đề bài” is active. A small circular avatar with “A” and user name “An” at bottom.
Main practice column starts at x=292 and ends near x=1008; right collapsed tutor card is x=1032..1248. Header at y≈48: “Luyện bài” with generous hierarchy and a small theme toggle.
Problem copy at y≈108 is exactly “Xét tính đơn điệu và tìm cực trị của hàm số”. Immediately beneath, a dedicated wide math panel around y=152..232 holds the large centered formula “y = x³ − 3x² + 2.” Together these form the exact problem statement “Xét tính đơn điệu và tìm cực trị của hàm số y = x³ − 3x² + 2.”
At y≈264 a horizontal five-step progress rail has these five labels in order: “Tập xác định” with ✓, “Đạo hàm” with ✓, “Nghiệm của y′” with ✓, “Xét dấu y′” with a current-step ring and small “Đang làm”, “Kết luận” in the pending state. Exact readable labels; the current step is number 4.
Below, section title “Xét dấu y′”. A large sign table panel around x=292..1008, y=360..552. A proper Vietnamese sign/variation table with rows labelled serif “x”, “y′”, “y”. Across the x row, in left-to-right order, show “−∞”, “0”, “2”, “+∞”. Divide the y′ row into intervals and critical-point columns, showing exactly “+”, “0”, “−”, “0”, “+”. Align the zeros vertically beneath x = 0 and x = 2; the three signs occupy the open intervals. In the y row, show the variation −∞ ↗ 2 ↘ −2 ↗ +∞, aligned with the two critical points. Use colored rising and falling arrows. Clear thin table rules, generous spacing, no extra critical points.
Below the table, a small compact plot at x≈292..688, y≈568..744. The cubic graph is mathematically accurate for y = x³ − 3x² + 2: it comes from the lower left, rises to a local maximum at (0, 2), falls to a local minimum at (2, −2), then rises to the upper right. Mark ONLY those two critical points with colored dots and exact small labels “(0, 2)” and “(2, −2)”. Draw horizontal x and vertical y axes, a faint grid and an origin “0”. Do not confuse roots with critical points. The curve crosses the x-axis three times, approximately at −0.73, 1 and 2.73.
To the plot’s right, show a single primary button “Kiểm tra bước”, approximately 208 px wide, 40 px visual height, 44 px hit area, aligned comfortably in the lower workspace.
Right side: collapsed tutor card with title “Gia sư”, a chevron indicating collapse, and exactly one hint sentence wrapping naturally: “Quan sát dấu của y′ trên từng khoảng.” No chat conversation.
No invented scores, percentage counters, promotional copy or illustrations. Keep the formula, table and plot dominant.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

Prompt chỉnh sửa cuối, nguyên văn. Ảnh đầu vào là ảnh được tạo bởi prompt ngay phía trên:

```text
Use case: ui-mockup precise layout correction. Input image is the edit target, a finished MathL+ practice screenshot.
Make only these corrections to this image:
1. Widen the LEFT sidebar to exactly 20.3125% of the total canvas width (260 pixels in a 1280 × 800 logical viewport). In the supplied 1586 × 992 render this is about 322 image pixels. Keep the right tutor column at the same position and width. Reflow the main workspace so its left gutter is x=292 in the logical viewport (about x=362 in the supplied render), with a 32 logical-pixel gap after the sidebar. Preserve every current component, all copy, the existing typography, and the complete mathematics. Ensure the five-step rail and the sign table still fit in the main column.
2. Give the “Kiểm tra bước” primary button exactly 40 logical pixels visual height (49.6 pixels in this 1586-wide render), 44 logical pixels hit area, 6 logical pixels corner radius, a SOLID flat #C75B39 fill, and bold 20 logical-pixel white label.
3. In the y′ row of the sign table, both positive “+” symbols must be #5CD0B3 and the negative “−” symbol must be #FC6255; keep the zero symbols cream. Keep arrows teal/red.
Invariants: preserve wordmark “MathL+”; Vietnamese text with correct diacritics; formula y = x³ − 3x² + 2; critical points (0, 2) and (2, −2); correct table signs +, 0, −, 0, +; completed first three steps, fourth step current, last step pending; all other palette tokens, panel colors, exact labels, tutor hint, graph, nav and avatar remain unchanged. Do not add any content. No frame, mascot, Pi character or foreign logo. Keep canvas aspect ratio 1280:800. Return the single corrected full screen.
```

### B-luyen-390.webp

[B-luyen-390.webp](B-luyen-390.webp) — 390 × 844 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option B — “Wiii tối toàn phần”.
Whole application is warm charcoal: page #1E1D1B, sidebar/cards #282724, raised surfaces #353330, thin hairlines #3A3935, text #E8E8E4, secondary text #A8A7A2. A very faint fine grid texture appears in the page background, as in a restrained warm charcoal brand card. Small section labels are letter-spaced Vietnamese uppercase in light blue #58C4DD. Primary button #C75B39. Restrained layered shadows and softly rounded panels with thin borders.
Math is cream Computer Modern serif. Manim colors only on mathematical strokes and status accents: curve #58C4DD, critical-point dots #F4D345, positive signs/rising arrows #5CD0B3, negative signs/falling arrows #FC6255. Plot grid faint; curve smooth and rounded.

Canvas: exactly 390 × 844 px, mobile viewport, no phone hardware or browser frame.
This is the responsive version of the exact same desktop practice screen. Sidebar collapses to a 44 px menu button. Top bar at y=0..64: menu icon, text wordmark “MathL+”, small sun/moon theme toggle. 16 px side gutters.
At y≈80, heading “Luyện bài”. Under it, exact problem copy “Xét tính đơn điệu và tìm cực trị của hàm số” wraps into two or three clear lines. A separate math panel at y≈188..244 shows centered Computer Modern serif “y = x³ − 3x² + 2.” Together this is the exact same problem statement.
At y≈264..344, a compact horizontal five-step stepper with five equal-width columns and connecting line: “Tập xác định” ✓, “Đạo hàm” ✓, “Nghiệm của y′” ✓, “Xét dấu y′” with current ring number 4, “Kết luận” pending number 5. Labels wrap within columns, keep Vietnamese diacritics readable, and include “Đang làm” directly under the current-step label if needed.
At y≈360, title “Xét dấu y′”. Sign table in a horizontally scrollable 358 px card at y≈396..548; show a tiny unobtrusive horizontal scroll indicator at its base, keep the mathematical columns readable. A proper Vietnamese sign/variation table with rows labelled serif “x”, “y′”, “y”. Across the x row, in left-to-right order, show “−∞”, “0”, “2”, “+∞”. Divide the y′ row into intervals and critical-point columns, showing exactly “+”, “0”, “−”, “0”, “+”. Align the zeros vertically beneath x = 0 and x = 2; the three signs occupy the open intervals. In the y row, show the variation −∞ ↗ 2 ↘ −2 ↗ +∞, aligned with the two critical points. Use colored rising and falling arrows. Clear thin table rules, generous spacing, no extra critical points.
Small cubic plot card at y≈564..692, visible on this phone viewport. The cubic graph is mathematically accurate for y = x³ − 3x² + 2: it comes from the lower left, rises to a local maximum at (0, 2), falls to a local minimum at (2, −2), then rises to the upper right. Mark ONLY those two critical points with colored dots and exact small labels “(0, 2)” and “(2, −2)”. Draw horizontal x and vertical y axes, a faint grid and an origin “0”. Do not confuse roots with critical points. The curve crosses the x-axis three times, approximately at −0.73, 1 and 2.73.
Collapsed compact tutor row around y≈708..764, title “Gia sư”, chevron, and the single sentence “Quan sát dấu của y′ trên từng khoảng.” It wraps once if needed.
Sticky full-width bottom action area at y≈780..844, with a 40 px visual-height, at least 44 px touch-area terracotta button labelled “Kiểm tra bước”, 16 px side gutters. Do not cover any math or tutor copy. All content fits the stated viewport.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

Prompt chỉnh sửa cuối, nguyên văn. Ảnh đầu vào là ảnh được tạo bởi prompt ngay phía trên:

```text
Use case: ui-mockup precise color correction.
Input image is the edit target: the finished B dark MathL+ mobile practice screenshot.
Change ONLY the sign-table y′ row’s mathematical sign colors: BOTH positive “+” symbols become exactly #5CD0B3 teal; the one negative “−” symbol becomes exactly #FC6255 red. Both critical-point zero symbols remain cream. The variation arrows stay teal/red.
Also flatten the existing “Kiểm tra bước” button fill to SOLID #C75B39, preserving its size, position, corner radius and white label.
Keep ALL content, canvas, component geometry, text, Vietnamese diacritics, MathL+ wordmark, warm charcoal background, faint grid, cream formula, horizontal five-step stepper, table values, cubic plot, the two yellow critical points at (0, 2) and (2, −2), hint and sticky bottom action unchanged. No new content, logo or mascot. Return the single corrected full mobile screen.
```

### B-hoc-1280.webp

[B-hoc-1280.webp](B-hoc-1280.webp) — 1280 × 800 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option B — “Wiii tối toàn phần”.
Whole application is warm charcoal: page #1E1D1B, sidebar/cards #282724, raised surfaces #353330, thin hairlines #3A3935, text #E8E8E4, secondary text #A8A7A2. A very faint fine grid texture appears in the page background, as in a restrained warm charcoal brand card. Small section labels are letter-spaced Vietnamese uppercase in light blue #58C4DD. Primary button #C75B39. Restrained layered shadows and softly rounded panels with thin borders.
Math is cream Computer Modern serif. Manim colors only on mathematical strokes and status accents: curve #58C4DD, critical-point dots #F4D345, positive signs/rising arrows #5CD0B3, negative signs/falling arrows #FC6255. Plot grid faint; curve smooth and rounded.

Canvas: exactly 1280 × 800 px, desktop student home.
Sidebar exactly 260 px wide, full height, identical geometry to the practice screen: wordmark “MathL+” at top; items “Học”, “Đề bài”, “Lịch”, “Công thức”; “Học” is active; avatar “A” and name “An” at bottom.
Main workspace has 40 px padding and broad whitespace. Header heading “Chào An” at x≈300, y≈56, with a small theme toggle at the far right.
A wide, quiet next-assignment card around x=300..1224, y=128..288, title “Bài tiếp theo: Đơn điệu và cực trị” and subtitle “5 bước · hạn thứ Sáu”. Include one terracotta primary button “Vào học”. The title and subtitle together are exactly “Bài tiếp theo: Đơn điệu và cực trị · 5 bước · hạn thứ Sáu”. No decorative picture.
Below, section heading “Bài được giao”. Three clear horizontal assignment rows, all with matching spacing and a thin separator. Left titles and right textual statuses, EXACTLY:
“Bài 1 · Đơn điệu của hàm số” — “Chưa làm”
“Bài 2 · Đơn điệu và cực trị” — “Đang làm 3/5”
“Bài 3 · Cực trị của hàm số” — “Đạt”
Statuses must use readable text plus a small appropriate neutral, current, or completed marker; color never carries the state alone. Do not invent scores or extra problems.
Below at y≈600, a compact mastery strip headed “Mức độ”. Four equally weighted, quiet segments labelled exactly “Nhận biết”, “Thông hiểu”, “Vận dụng”, “Vận dụng cao”. No numbers, percentages, giant dashboard metrics, or fabricated attainment claims.
This home screen contains no formulas, sign tables or graphs; do not invent math panels. Harmonize with the same option’s practice screen.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

### B-dang-nhap-390.webp

[B-dang-nhap-390.webp](B-dang-nhap-390.webp) — 390 × 844 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option B — “Wiii tối toàn phần”.
Whole application is warm charcoal: page #1E1D1B, sidebar/cards #282724, raised surfaces #353330, thin hairlines #3A3935, text #E8E8E4, secondary text #A8A7A2. A very faint fine grid texture appears in the page background, as in a restrained warm charcoal brand card. Small section labels are letter-spaced Vietnamese uppercase in light blue #58C4DD. Primary button #C75B39. Restrained layered shadows and softly rounded panels with thin borders.
Math is cream Computer Modern serif. Manim colors only on mathematical strokes and status accents: curve #58C4DD, critical-point dots #F4D345, positive signs/rising arrows #5CD0B3, negative signs/falling arrows #FC6255. Plot grid faint; curve smooth and rounded.

Canvas: exactly 390 × 844 px, mobile login viewport. Edge-to-edge flat screen, no device frame.
Generous calm whitespace. Simple text wordmark “MathL+”, no icon or mascot. Small sun/moon theme-toggle icon near the top-right, with 44 px target.
Single restrained login composition, 32 px side gutters, optical center around 46% of viewport height, approximately 3:2 lower versus upper free space. Product mark near y≈216, heading “Đăng nhập” near y≈288.
One visible field with label “Email”, placeholder “Nhập email của bạn”, subtle hairline border and 44 px minimum target, near y≈360. One full-width terracotta primary button “Tiếp tục”, 40 px visual height, 44 px touch area, near y≈440.
Below around y≈512, two small secondary demo buttons placed side by side, both at least 44 px touch targets, exact labels “Học sinh An” and “Giáo viên”. No password field, no social provider, no extra navigation, no marketing slogan.
Footer near the lower part of the viewport, centred and comfortably legible, wraps into three short lines as needed, exact complete text: “Toán 12 · đơn điệu và cực trị · Tài khoản thử — không có học sinh thật”.
There are no formulas, sign tables or plots on this login screen; do not add chalkboard decoration. Keep the page aligned to the option’s application palette.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

### C-luyen-1280.webp

[C-luyen-1280.webp](C-luyen-1280.webp) — 1280 × 800 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option C — “Wiii sáng, toán sáng”.
Light everywhere: page #FAF9F5, main cards and ALL math panels #FFFFFF, sidebar #F0EEE6, hairlines #D8D5CD, ink #141413, secondary ink #3D3D3A, muted #73726C. Primary button #C75B39. Very soft multi-layer shadows.
All mathematics uses ink Computer Modern serif on light surfaces. Darkened Manim hues: curve #1C758A, critical-point markers #C78D46, positive signs and rising arrows #49A88F, negative signs and falling arrows #CF5044. Use dark ink for point labels and thin dark edging on colored mathematical symbols or markers wherever needed to retain accessible contrast. Plot grid very faint. No dark chalkboard panels.

Canvas: exactly 1280 × 800 px, desktop viewport.
Layout: left sidebar exactly 260 px wide, full height. Text wordmark “MathL+” at top; navigation items “Học”, “Đề bài”, “Lịch”, “Công thức” with simple thin line icons; “Đề bài” is active. A small circular avatar with “A” and user name “An” at bottom.
Main practice column starts at x=292 and ends near x=1008; right collapsed tutor card is x=1032..1248. Header at y≈48: “Luyện bài” with generous hierarchy and a small theme toggle.
Problem copy at y≈108 is exactly “Xét tính đơn điệu và tìm cực trị của hàm số”. Immediately beneath, a dedicated wide math panel around y=152..232 holds the large centered formula “y = x³ − 3x² + 2.” Together these form the exact problem statement “Xét tính đơn điệu và tìm cực trị của hàm số y = x³ − 3x² + 2.”
At y≈264 a horizontal five-step progress rail has these five labels in order: “Tập xác định” with ✓, “Đạo hàm” with ✓, “Nghiệm của y′” with ✓, “Xét dấu y′” with a current-step ring and small “Đang làm”, “Kết luận” in the pending state. Exact readable labels; the current step is number 4.
Below, section title “Xét dấu y′”. A large sign table panel around x=292..1008, y=360..552. A proper Vietnamese sign/variation table with rows labelled serif “x”, “y′”, “y”. Across the x row, in left-to-right order, show “−∞”, “0”, “2”, “+∞”. Divide the y′ row into intervals and critical-point columns, showing exactly “+”, “0”, “−”, “0”, “+”. Align the zeros vertically beneath x = 0 and x = 2; the three signs occupy the open intervals. In the y row, show the variation −∞ ↗ 2 ↘ −2 ↗ +∞, aligned with the two critical points. Use colored rising and falling arrows. Clear thin table rules, generous spacing, no extra critical points.
Below the table, a small compact plot at x≈292..688, y≈568..744. The cubic graph is mathematically accurate for y = x³ − 3x² + 2: it comes from the lower left, rises to a local maximum at (0, 2), falls to a local minimum at (2, −2), then rises to the upper right. Mark ONLY those two critical points with colored dots and exact small labels “(0, 2)” and “(2, −2)”. Draw horizontal x and vertical y axes, a faint grid and an origin “0”. Do not confuse roots with critical points. The curve crosses the x-axis three times, approximately at −0.73, 1 and 2.73.
To the plot’s right, show a single primary button “Kiểm tra bước”, approximately 208 px wide, 40 px visual height, 44 px hit area, aligned comfortably in the lower workspace.
Right side: collapsed tutor card with title “Gia sư”, a chevron indicating collapse, and exactly one hint sentence wrapping naturally: “Quan sát dấu của y′ trên từng khoảng.” No chat conversation.
No invented scores, percentage counters, promotional copy or illustrations. Keep the formula, table and plot dominant.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

Prompt chỉnh sửa cuối, nguyên văn. Ảnh đầu vào là ảnh được tạo bởi prompt ngay phía trên:

```text
Use case: ui-mockup precise layout correction. Input image is the edit target, a finished MathL+ practice screenshot.
Make only these corrections to this image:
1. Widen the LEFT sidebar to exactly 20.3125% of the total canvas width (260 pixels in a 1280 × 800 logical viewport). In the supplied 1586 × 992 render this is about 322 image pixels. Keep the right tutor column at the same position and width. Reflow the main workspace so its left gutter is x=292 in the logical viewport (about x=362 in the supplied render), with a 32 logical-pixel gap after the sidebar. Preserve every current component, all copy, the existing typography, and the complete mathematics. Ensure the five-step rail and the sign table still fit in the main column.
2. Give the “Kiểm tra bước” primary button exactly 40 logical pixels visual height (49.6 pixels in this 1586-wide render), 44 logical pixels hit area, 6 logical pixels corner radius, a SOLID flat #C75B39 fill, and bold 20 logical-pixel white label.

Invariants: preserve wordmark “MathL+”; Vietnamese text with correct diacritics; formula y = x³ − 3x² + 2; critical points (0, 2) and (2, −2); correct table signs +, 0, −, 0, +; completed first three steps, fourth step current, last step pending; all other palette tokens, panel colors, exact labels, tutor hint, graph, nav and avatar remain unchanged. Do not add any content. No frame, mascot, Pi character or foreign logo. Keep canvas aspect ratio 1280:800. Return the single corrected full screen.
```

### C-luyen-390.webp

[C-luyen-390.webp](C-luyen-390.webp) — 390 × 844 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option C — “Wiii sáng, toán sáng”.
Light everywhere: page #FAF9F5, main cards and ALL math panels #FFFFFF, sidebar #F0EEE6, hairlines #D8D5CD, ink #141413, secondary ink #3D3D3A, muted #73726C. Primary button #C75B39. Very soft multi-layer shadows.
All mathematics uses ink Computer Modern serif on light surfaces. Darkened Manim hues: curve #1C758A, critical-point markers #C78D46, positive signs and rising arrows #49A88F, negative signs and falling arrows #CF5044. Use dark ink for point labels and thin dark edging on colored mathematical symbols or markers wherever needed to retain accessible contrast. Plot grid very faint. No dark chalkboard panels.

Canvas: exactly 390 × 844 px, mobile viewport, no phone hardware or browser frame.
This is the responsive version of the exact same desktop practice screen. Sidebar collapses to a 44 px menu button. Top bar at y=0..64: menu icon, text wordmark “MathL+”, small sun/moon theme toggle. 16 px side gutters.
At y≈80, heading “Luyện bài”. Under it, exact problem copy “Xét tính đơn điệu và tìm cực trị của hàm số” wraps into two or three clear lines. A separate math panel at y≈188..244 shows centered Computer Modern serif “y = x³ − 3x² + 2.” Together this is the exact same problem statement.
At y≈264..344, a compact horizontal five-step stepper with five equal-width columns and connecting line: “Tập xác định” ✓, “Đạo hàm” ✓, “Nghiệm của y′” ✓, “Xét dấu y′” with current ring number 4, “Kết luận” pending number 5. Labels wrap within columns, keep Vietnamese diacritics readable, and include “Đang làm” directly under the current-step label if needed.
At y≈360, title “Xét dấu y′”. Sign table in a horizontally scrollable 358 px card at y≈396..548; show a tiny unobtrusive horizontal scroll indicator at its base, keep the mathematical columns readable. A proper Vietnamese sign/variation table with rows labelled serif “x”, “y′”, “y”. Across the x row, in left-to-right order, show “−∞”, “0”, “2”, “+∞”. Divide the y′ row into intervals and critical-point columns, showing exactly “+”, “0”, “−”, “0”, “+”. Align the zeros vertically beneath x = 0 and x = 2; the three signs occupy the open intervals. In the y row, show the variation −∞ ↗ 2 ↘ −2 ↗ +∞, aligned with the two critical points. Use colored rising and falling arrows. Clear thin table rules, generous spacing, no extra critical points.
Small cubic plot card at y≈564..692, visible on this phone viewport. The cubic graph is mathematically accurate for y = x³ − 3x² + 2: it comes from the lower left, rises to a local maximum at (0, 2), falls to a local minimum at (2, −2), then rises to the upper right. Mark ONLY those two critical points with colored dots and exact small labels “(0, 2)” and “(2, −2)”. Draw horizontal x and vertical y axes, a faint grid and an origin “0”. Do not confuse roots with critical points. The curve crosses the x-axis three times, approximately at −0.73, 1 and 2.73.
Collapsed compact tutor row around y≈708..764, title “Gia sư”, chevron, and the single sentence “Quan sát dấu của y′ trên từng khoảng.” It wraps once if needed.
Sticky full-width bottom action area at y≈780..844, with a 40 px visual-height, at least 44 px touch-area terracotta button labelled “Kiểm tra bước”, 16 px side gutters. Do not cover any math or tutor copy. All content fits the stated viewport.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

### C-hoc-1280.webp

[C-hoc-1280.webp](C-hoc-1280.webp) — 1280 × 800 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option C — “Wiii sáng, toán sáng”.
Light everywhere: page #FAF9F5, main cards and ALL math panels #FFFFFF, sidebar #F0EEE6, hairlines #D8D5CD, ink #141413, secondary ink #3D3D3A, muted #73726C. Primary button #C75B39. Very soft multi-layer shadows.
All mathematics uses ink Computer Modern serif on light surfaces. Darkened Manim hues: curve #1C758A, critical-point markers #C78D46, positive signs and rising arrows #49A88F, negative signs and falling arrows #CF5044. Use dark ink for point labels and thin dark edging on colored mathematical symbols or markers wherever needed to retain accessible contrast. Plot grid very faint. No dark chalkboard panels.

Canvas: exactly 1280 × 800 px, desktop student home.
Sidebar exactly 260 px wide, full height, identical geometry to the practice screen: wordmark “MathL+” at top; items “Học”, “Đề bài”, “Lịch”, “Công thức”; “Học” is active; avatar “A” and name “An” at bottom.
Main workspace has 40 px padding and broad whitespace. Header heading “Chào An” at x≈300, y≈56, with a small theme toggle at the far right.
A wide, quiet next-assignment card around x=300..1224, y=128..288, title “Bài tiếp theo: Đơn điệu và cực trị” and subtitle “5 bước · hạn thứ Sáu”. Include one terracotta primary button “Vào học”. The title and subtitle together are exactly “Bài tiếp theo: Đơn điệu và cực trị · 5 bước · hạn thứ Sáu”. No decorative picture.
Below, section heading “Bài được giao”. Three clear horizontal assignment rows, all with matching spacing and a thin separator. Left titles and right textual statuses, EXACTLY:
“Bài 1 · Đơn điệu của hàm số” — “Chưa làm”
“Bài 2 · Đơn điệu và cực trị” — “Đang làm 3/5”
“Bài 3 · Cực trị của hàm số” — “Đạt”
Statuses must use readable text plus a small appropriate neutral, current, or completed marker; color never carries the state alone. Do not invent scores or extra problems.
Below at y≈600, a compact mastery strip headed “Mức độ”. Four equally weighted, quiet segments labelled exactly “Nhận biết”, “Thông hiểu”, “Vận dụng”, “Vận dụng cao”. No numbers, percentages, giant dashboard metrics, or fabricated attainment claims.
This home screen contains no formulas, sign tables or graphs; do not invent math panels. Harmonize with the same option’s practice screen.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```

### C-dang-nhap-390.webp

[C-dang-nhap-390.webp](C-dang-nhap-390.webp) — 390 × 844 px.

Prompt tạo ảnh, nguyên văn:

```text
Use case: ui-mockup.
Asset type: one high-fidelity flat UI design-study PNG for MathL+, a Vietnamese Angular math-learning app for high-school students.
Render the application straight-on, edge to edge, with no device frame, browser chrome, photograph, perspective, presentation margin, watermark, option letter, or explanatory caption. This is one complete screen, not a collage.
Typography: UI uses a clear modern sans-serif with complete Vietnamese diacritics, careful baseline and ample line height. Math alone uses Computer Modern / KaTeX-like serif typography with true superscripts, primes and a proper minus sign. Do not use Literata.
All on-screen language is real Vietnamese, copied exactly from the text specified below. Product wordmark is only “MathL+”, typeset as text; no mascot, no Wiii wordmark or logo, no 3Blue1Brown logo, no Pi character, no stock illustrations.
Design constraints: consistent 8 px spacing grid, generous whitespace, precise alignment, minimal thin line icons, soft 6 px button corners, buttons 40 px visual height with 44 px touch area and 16 px horizontal padding. Make every essential text label readable at WCAG AA contrast. The terracotta primary button uses bold 20 px white text, large enough for AA large-text contrast. Graph colors are strokes and markers; small graph labels use contrasting ink or cream. Avoid gradient-heavy dashboards and KPI cards. The mathematics is the visual focus.
Include a small sun/moon theme-toggle icon with a 44 px target. Preserve the same geometry and the exact same content as the other color options for this screen.

Option C — “Wiii sáng, toán sáng”.
Light everywhere: page #FAF9F5, main cards and ALL math panels #FFFFFF, sidebar #F0EEE6, hairlines #D8D5CD, ink #141413, secondary ink #3D3D3A, muted #73726C. Primary button #C75B39. Very soft multi-layer shadows.
All mathematics uses ink Computer Modern serif on light surfaces. Darkened Manim hues: curve #1C758A, critical-point markers #C78D46, positive signs and rising arrows #49A88F, negative signs and falling arrows #CF5044. Use dark ink for point labels and thin dark edging on colored mathematical symbols or markers wherever needed to retain accessible contrast. Plot grid very faint. No dark chalkboard panels.

Canvas: exactly 390 × 844 px, mobile login viewport. Edge-to-edge flat screen, no device frame.
Generous calm whitespace. Simple text wordmark “MathL+”, no icon or mascot. Small sun/moon theme-toggle icon near the top-right, with 44 px target.
Single restrained login composition, 32 px side gutters, optical center around 46% of viewport height, approximately 3:2 lower versus upper free space. Product mark near y≈216, heading “Đăng nhập” near y≈288.
One visible field with label “Email”, placeholder “Nhập email của bạn”, subtle hairline border and 44 px minimum target, near y≈360. One full-width terracotta primary button “Tiếp tục”, 40 px visual height, 44 px touch area, near y≈440.
Below around y≈512, two small secondary demo buttons placed side by side, both at least 44 px touch targets, exact labels “Học sinh An” and “Giáo viên”. No password field, no social provider, no extra navigation, no marketing slogan.
Footer near the lower part of the viewport, centred and comfortably legible, wraps into three short lines as needed, exact complete text: “Toán 12 · đơn điệu và cực trị · Tài khoản thử — không có học sinh thật”.
There are no formulas, sign tables or plots on this login screen; do not add chalkboard decoration. Keep the page aligned to the option’s application palette.

Output intent: deliver only this one finished UI screenshot as a PNG with the specified canvas dimensions. Carefully proofread every Vietnamese accent and mathematical value before finishing.
```
