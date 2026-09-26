# apps/web

Next.js 15 App Router, React 19, Tailwind, KaTeX, MathLive. Chữ UI tiếng Việt.

## Lệnh (chạy từ `apps/web` hoặc `pnpm --filter web`)

```bash
pnpm --filter web typecheck
pnpm --filter web lint
pnpm --filter web test:e2e
pnpm --filter web db:migrate
pnpm --filter web seed
pnpm --filter web dev
```

E2E cần `pnpm dev:math` + web + Postgres đã seed. Playwright ghi ảnh vào `/opt/cursor/artifacts/screenshots` khi chạy trên máy nguyên mẫu.

## Token

Nguồn: `docs/DESIGN.md`, biến CSS trong `app/globals.css`.

- Mực `#17181C`, giấy `#FFFFFF`, rửa `#F6F6F7`, bút đỏ `#C81E1E`.
- IBM Plex Sans 400/500/600/700 + IBM Plex Mono.
- Lưới 8 px: 4 / 8 / 12 / 16 / 24 / 32 / 48.
- Nút `md`: `min-h-10` (40) + `px-4` (16). Con trỏ thô: `min-h-11` (44). Chỉ biểu tượng: `size-11`.
- Ô nhập: cùng chiều cao. Nhãn → ô: `mb-2` (8).
- Bán kính 6 (`rounded-button`). Không viên thuốc M3.
- Ô xét dấu trong bảng: tối thiểu 32×32 (dày phiếu; WCAG 2.5.8 AA là 24).

Dùng `Button` / `buttonClasses` trong `components/ui/button.tsx`. Đừng tự `px-4 py-2.5` rời.

## Testid không đổi

`sidebar`, `mo-sidebar`, `dong-sidebar`, `nav-*`, `solve-screen`, `latex-txd`, `latex-dh`, `nop-buoc`, `cham-thong-bao`, `mo-gia-su`, `tutor-input`, `tutor-send`, `tutor-log`, `bai-DH12-03-VD-01`, `hang-doi`, `duyet-*`, `tien-do`, `toggle-muc`, `mo-loi-giai`, `email`, `password`.

## Cấu trúc

- Trang: `app/hs`, `app/gv`, `app/dang-nhap`.
- Nguyên thủy: `components/ui/*`. Vỏ: `components/app-shell.tsx`.
- Server action: `lib/actions/{hs,gv,auth}.ts`. Schema: `lib/db/schema.ts`.
- Gia sư: `lib/tutor.ts` + `lib/llm.ts`. Không đưa lời giải vào prompt.

## Skill

`.cursor/skills/frontend-design`, `.cursor/skills/web-design-guidelines`.
