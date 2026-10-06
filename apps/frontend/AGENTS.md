# apps/frontend

Frontend v2 (ADR 011): Angular 22, zoneless, signal-first, Signal Forms, Vitest. Chuẩn đầy đủ: skill `angular-frontend` (`.claude/skills/angular-frontend/SKILL.md`); thiết kế: `docs/DESIGN.md`.

## Lệnh (từ `apps/frontend` hoặc `pnpm --filter frontend`)

```bash
pnpm --filter frontend dev     # ng serve ở :4200
pnpm --filter frontend build   # bản production vào dist/frontend
pnpm --filter frontend test    # Vitest (jsdom) qua ng test, chạy một lần
pnpm --filter frontend e2e     # Playwright trên hệ compose v2 đang chạy (http://127.0.0.1:4200), 390 + 1280 px
```

`ng serve` chuyển `/api` tới `http://localhost:8080` (`proxy.conf.json`): chạy kèm `services/core` profile `dev`.

Cần Node `^22.22.3 || ^24.15.0 || >=26` (Angular CLI 22 chặn bản lẻ như 25). Workspace pnpm của repo: cài từ gốc bằng `pnpm install`.

Cả hệ v2 trong Docker: `docker compose -f compose.v2.yaml up --build --wait` ở gốc repo → http://localhost:4200, `/api/` qua nginx tới `services/core` (cùng gốc, không cần CORS).

## Bản đồ

| Đường dẫn | Việc |
| --- | --- |
| `src/app/app.config.ts`, `app.routes.ts` | Provider, route (lazy theo tính năng). `/hs`, `/gv` là route cha với khung `KhungTrang`; trang con theo bảng phụ lục của `specs/001-lat-cat-doc/spec.md` (màn chưa làm dùng `TrangCho`: đúng heading, câu mô tả). `/` và đường lạ → `/dang-nhap` cho tới khi có trang công khai |
| `src/app/api/` | Kiểu khớp DTO của `services/core`: `auth.ts` (người dùng, phiên, endpoint, header chống CSRF); `hoc-sinh.ts`, `giao-vien.ts` theo `contracts/api-core.md` (core chưa có các endpoint này; trường hợp đồng chưa định nghĩa để `unknown`) |
| `src/app/core/` | Việc toàn ứng dụng: `TieuDeTrang` (tab `<trang> · MathL+`); `GiaoDien` (chế độ sáng / tối: chưa chọn thì theo `prefers-color-scheme`, bấm thì nhớ ở `localStorage` khóa `mathl-giao-dien`; ghi `data-theme` lên `<html>`) |
| `src/app/core/auth/` | `Phien` (access token trong bộ nhớ, refresh token trong cookie HttpOnly, làm mới một luồng qua Web Locks), `xacThucInterceptor` (Bearer, 401 → làm mới một lần), guard `chiVaiTro`, `chuaDangNhap`; `phien.testing.ts` là `PhienGia` cho test |
| `src/app/features/<tính-năng>/` | Màn theo tính năng: `trang-chu` (`/` công khai theo trang Wiii: nền ảnh vẽ riêng, từ xoay một vòng, thẻ cảnh, đồ thị kéo được, dải Fourier vẽ một lần, thẻ học sinh / giáo viên có ảnh; ảnh ở `public/anh/`, SEO ở `src/index.html` và `public/`), `dang-nhap` (hai bước email → mật khẩu), `hoc-sinh` (`/hs` «Chào <tên>»), `giao-vien` (`/gv`, chưa có API lớp nên «Chưa có lớp») |
| `src/app/shared/ui/` | Nguyên thủy: `appButton` (giải phẫu nút), `app-brand-mark` (chữ hiệu «MathL+» đậm 800, «+» theo `--brand-plus`; cỡ `sm` / `md` / `lg`), `app-bieu-tuong` (nét Lucide chép nguyên văn), `app-doi-giao-dien` (nút bật tắt giao diện tối, `aria-pressed`, `doi-giao-dien`) |
| `src/app/shared/layout/` | `app-khung-trang`: desktop là thanh bên 260 px nền `--wash` (mục đang mở: nền nhạt `--accent`, vạch trái 3 px; chân: chữ đầu tên, tên, Đăng xuất), nút đổi giao diện ở góc phải trên; dưới `lg` là thanh trên (menu, chữ hiệu, đổi giao diện, Đăng xuất) + ngăn kéo (`mo-sidebar`, `dong-sidebar`). Ngăn kéo là hộp thoại: đóng thì `inert`; mở thì phần còn lại của trang `inert` và khóa cuộn; Esc, nền, nút đóng trả tiêu điểm về nút mở; chọn mục thì đóng ngay và đưa tiêu điểm vào `#noi-dung`. Vùng `aria-live` (`thong-bao-trang`) đọc tiêu đề trang mới. Một nút `dang-xuat` trong DOM; phiên mất thì về `/dang-nhap`. `dieu-huong.ts`: mục ray và `nav-*` chép từ v0. `TrangCho`: trang con chưa có dữ liệu |
| `src/app/shared/toan/` | `app-katex` (`throwOnError: false` với chữ lỗi màu `--muted`, `trust: false`; `khoi` căn trái, `bang` là tấm bảng tối 3b1b căn giữa; đang cuộn ngang thì nhận Tab); `app-o-cong-thuc`: ô công thức cho Signal Forms như `MathInput` của v0 — `math-field` (MathLive nạp lười, tắt menu) và ô gõ bằng bàn phím luôn có, dòng «Máy hiểu là»; `data-testid` `<testId>` / `mf-<testId>` / `hieu-<testId>` |
| `e2e/`, `playwright.config.ts` | e2e qua nginx → core: đăng nhập HS / GV, guard vai trò, tải lại giữ phiên, cookie HttpOnly; `khung.spec.ts`: ray dẫn tới mọi màn, ngăn kéo, không tràn ngang ở 390 / 1280 px |
| `src/styles.css` | Token «Phòng Wiii, bảng phấn 3b1b», giá trị theo bảng của `docs/DESIGN.md` (`node scripts/tuong-phan-token.mjs` ở CI đỏ khi lệch): sáng ở `:root`, tối ở `:root[data-theme='dark']`; khoảng, nút `.btn*`, `.nut-icon`, nhãn `.nhan-muc`, trạng thái rỗng `.trong`, nền vệt góc + lưới tối, skip link, `prefers-reduced-motion` |
| `Dockerfile`, `Dockerfile.dockerignore` | Ảnh production: ngữ cảnh build là gốc repo (cần lockfile workspace), chạy nginx không root ở :8080 |
| `nginx/default.conf.template` | `/api/` → `CORE_URL`; tệp có hash cache một năm, còn lại `no-cache`; đường lạ trả `index.html` |

## Gotcha

- Không bọc `app-o-cong-thuc` trong `<label>` (ô tự vẽ nhãn): bọc `<label>` làm mất phím đầu trên máy chạm (v0 phải vá). Không gán `value` vào `<math-field>` trước khi MathLive nạp xong: thuộc tính riêng che getter của MathLive.
- `KhungTrang` đặt lớp trên `<html>`: `co-thanh-tren` (màn hẹp, `styles.css` chừa `scroll-padding-top` cho thanh dính) và `khoa-cuon` (ngăn kéo mở).
- Phông giao diện là chồng phông hệ thống (`--font-sans`, như Wiii): không tải phông, không CDN. KaTeX tự host qua `katex.min.css` trong `angular.json` → `styles`. Phông MathLive cũng tự host: `angular.json` chép `node_modules/mathlive/fonts` ra `/mathlive/fonts`, `napMathLive()` đặt `fontsDirectory` về đó và tắt âm thanh. Test thay cách nạp qua token `NAP_MATHLIVE`.
- Nút luôn qua `appButton` trên phần tử gốc (`<button appButton>`), không tự đặt lớp `.btn`; trang công khai `/` (`features/trang-chu/`) dùng biến thể viên `variant="vien-dam" | "vien-kem" | "vien-vien"` theo trang Wiii. Nút chỉ có biểu tượng dùng lớp `.nut-icon` (44 px) và luôn có `aria-label`. Vòng tiêu điểm vẽ ra ngoài cách 2 px, trừ trong thanh trên `.thanh-tren` và nút trong ô nhập `.nut-trong-o`: hai chỗ đó vẽ vào trong.
- `data-theme` trên `<html>` luôn có giá trị (`light` / `dark`): script đầu `src/index.html` đặt trước lần vẽ đầu, `GiaoDien` giữ đồng bộ. Đổi khóa hay luật thì sửa cả hai chỗ. Nếu sau này CSP chặn script nội tuyến, trang chớp chế độ sáng tới khi Angular chạy. CSS toàn cục chặn vẽ (`inlineCritical: false` trong `angular.json`): CSS chậm thì trang chưa vẽ gì cho tới khi CSS về, khung đầu đã đúng chế độ. Đừng bật lại CSS trọng yếu nội tuyến: phần nội tuyến chỉ có token sáng nên chế độ tối sẽ chớp sáng.
- Vùng toán luôn là bảng tối (`--board*`) ở cả hai chế độ; màu Manim `--m-*` chỉ dùng trên bảng. Chữ trên nền trang dùng `--ink` / `--ink-2` / `--muted` / `--label`; viền ô nhập, nút phụ là `--line-strong` (`--line` chỉ để kẻ chia, dưới 3 : 1). Vòng focus là `--focus`, không `--accent` (trên `--raise` tối chỉ 2,99 : 1). Kẻ mang nghĩa trên bảng (bảng xét dấu) là `--board-rule`; `--board-line` chỉ trang trí.
- Style của `khung-trang.ts` sát ngân sách 4 kB mỗi component (`angular.json`): thêm style cho khung thì đưa nguyên thủy dùng chung sang `styles.css`.
- Route, `data-testid` và heading giữ như v0 (`apps/web/AGENTS.md`) cho màn tương đương.
- Test component zoneless: đổi giá trị ô bằng `input` event rồi `await fixture.whenStable()`. Chuỗi `await` tự viết (gọi service rồi điều hướng) không được Angular theo dõi: chờ thêm `setTimeout(0)`.
- Không giữ token trong `localStorage` / `sessionStorage`. Gọi API bằng đường tương đối `/api/…`: interceptor chỉ gắn token cho đường cùng gốc.
- Hai tab cùng làm mới phiên bằng một cookie cũ thì core coi là token bị lộ và thu hồi mọi phiên: luôn làm mới qua `Phien.lamMoi()` (một luồng, Web Locks).
