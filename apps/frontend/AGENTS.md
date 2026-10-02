# apps/frontend

Frontend v2 (ADR 011): Angular 22, zoneless, signal-first, Signal Forms, Vitest. Chuẩn đầy đủ: skill `angular-frontend` (`.claude/skills/angular-frontend/SKILL.md`); thiết kế: `docs/DESIGN.md`.

## Lệnh (từ `apps/frontend` hoặc `pnpm --filter frontend`)

```bash
pnpm --filter frontend dev     # ng serve ở :4200
pnpm --filter frontend build   # bản production vào dist/frontend
pnpm --filter frontend test    # Vitest (jsdom) qua ng test, chạy một lần
```

Cần Node `^22.22.3 || ^24.15.0 || >=26` (Angular CLI 22 chặn bản lẻ như 25). Workspace pnpm của repo: cài từ gốc bằng `pnpm install`.

Cả hệ v2 trong Docker: `docker compose -f compose.v2.yaml up --build --wait` ở gốc repo → http://localhost:4200, `/api/` qua nginx tới `services/core` (cùng gốc, không cần CORS).

## Bản đồ

| Đường dẫn | Việc |
| --- | --- |
| `src/app/app.config.ts`, `app.routes.ts` | Provider, route (lazy theo tính năng); `/` và đường lạ → `/dang-nhap` cho tới khi có trang công khai |
| `src/app/core/` | Việc toàn ứng dụng: `TieuDeTrang` (tab `<trang> · Học toán với AI`), sau này auth, guard, interceptor |
| `src/app/features/<tính-năng>/` | Màn theo tính năng: `dang-nhap` (hai bước email → mật khẩu, chưa nối API — #57) |
| `src/app/shared/ui/` | Nguyên thủy: `appButton` (giải phẫu nút), `app-brand-mark` |
| `src/styles.css` | Token màu, khoảng, nút `.btn*`, skip link, `prefers-reduced-motion` |
| `Dockerfile`, `Dockerfile.dockerignore` | Ảnh production: ngữ cảnh build là gốc repo (cần lockfile workspace), chạy nginx không root ở :8080 |
| `nginx/default.conf.template` | `/api/` → `CORE_URL`; tệp có hash cache một năm, còn lại `no-cache`; đường lạ trả `index.html` |

## Gotcha

- Font IBM Plex tự host qua `@fontsource` (khai ở `angular.json` → `styles`), không CDN.
- Nút luôn qua `appButton` trên phần tử gốc (`<button appButton>`), không tự đặt lớp `.btn`.
- Route, `data-testid` và heading giữ như v0 (`apps/web/AGENTS.md`) cho màn tương đương.
- Test component zoneless: đổi giá trị ô bằng `input` event rồi `await fixture.whenStable()`.
