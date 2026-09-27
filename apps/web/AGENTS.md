# apps/web

Next.js 15 App Router, React 19, Tailwind, KaTeX, MathLive. Chữ UI tiếng Việt.

## Lệnh (chạy từ `apps/web` hoặc `pnpm --filter web`)

```bash
pnpm --filter web typecheck
pnpm --filter web lint
pnpm --filter web test:unit
pnpm --filter web test:e2e
pnpm --filter web db:migrate
pnpm --filter web seed
pnpm --filter web dev
```

E2E cần `pnpm dev:math` + web + Postgres đã seed. Ảnh: `/opt/cursor/artifacts/screenshots` trên máy nguyên mẫu, hoặc `PLAYWRIGHT_SHOTS`. CI: `.github/workflows/ci.yml`.

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

`/dang-nhap`: tâm quang học ≈ 46% `dvh`, dư 3:2 (`docs/DESIGN.md`). Một logo giữa form. Không header trùng, không `-Nvh`.

`/hs`: phiếu + sổ tab. Desktop `lg`: cột phiếu | kẻ 1 px | cột sổ (`Kỹ năng` / `?so=giao`). Tab gạch chân mực, không viên thuốc. Title tab ngắn. Đề = `statementLatex`. Không Bloom, không mã bài/kỹ năng trên mặt học sinh. Chữ: tiếng lớp 12 (`Bài tiếp theo`, `Bài tập`), không phát hành/duyệt/ngưỡng. Giữ heading «Chào An» và CTA «Làm bước tiếp».

## Testid không đổi

`sidebar`, `mo-sidebar`, `dong-sidebar`, `nav-*`, `solve-screen`, `latex-txd`, `latex-dh`, `nop-buoc`, `cham-thong-bao`, `mo-gia-su`, `tutor-input`, `tutor-send`, `tutor-log`, `tutor-provider`, `tutor-composer`, `bai-DH12-03-VD-01`, `hang-doi`, `duyet-*`, `tien-do`, `toggle-muc`, `mo-loi-giai`, `ai-provider`, `ket-noi-chatgpt`, `kho-cong-thuc`, `kho-theo-buoc`, `san-sang-ai`, `email`, `password`.

## Cấu trúc

- Trang: `app/hs`, `app/gv`, `app/dang-nhap`. Ping giữ thức: `app/api/suc-khoe`.
- Nguyên thủy: `components/ui/*`. Vỏ: `components/app-shell.tsx`.
- Server action: `lib/actions/{hs,gv,auth}.ts`. Schema: `lib/db/schema.ts`.
- Gia sư: `lib/tutor.ts` + `lib/ai-harness.ts` + `lib/llm.ts` + `components/tutor-panel.tsx`. Không đưa lời giải vào prompt. Không fallback thầm.

## Skill

`.cursor/skills/frontend-design`, `.cursor/skills/web-design-guidelines`.
