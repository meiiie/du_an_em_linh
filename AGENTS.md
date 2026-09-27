# Hướng dẫn cho agent

Tệp này là nguồn chuẩn cho Cursor, Claude Code và các agent khác. Claude Code đọc thêm `CLAUDE.md` — tệp đó chỉ nhập `@AGENTS.md` rồi ghi chú riêng Claude.

Đọc `docs/CODEMAP.md` trước khi grep cả repo. Thiết kế: `docs/DESIGN.md`. Sư phạm: `docs/doi-chieu-thiet-ke.md`. ADR: `docs/adr/`.

## Đây là gì

Nguyên mẫu NCKH: phần mềm học toán THPT, **một** chủ đề Toán 12 — đơn điệu và cực trị. Mọi chữ trên màn hình là tiếng Việt. Demo mặc định nhà `offline` — chạy khi không có `LLM_API_KEY`.

## Không mở lại

- Khung 5 bước `B.DH.TXD` / `B.DH.DAOHAM` / `B.DH.NGHIEM` / `B.DH.XETDAU` / `B.DH.KETLUAN`. Chấm cả bước, không tô từng ô khi gõ.
- Cổng 3 tầng: `DAT` | `SAI` | `KHONG_KIEM_DUOC` + `GV_DUYET`.
- Gia sư **không** đọc lời giải chuẩn. Thứ tự: luật xin đáp án → thang 3 cấp (không bottom-out) → kho lớp (tài liệu + công thức) → nhà đã chọn → lọc SymPy. Kết nối = khóa chính thức một lần: ChatGPT, OpenRouter (lập trình) hoặc Z.AI (coding) — hoặc OAuth định danh nếu có `client_id` OpenAI cấp. Không device-OAuth Codex. Không nhập URL nhà. Chi tiết: `docs/AI-HARNESS.md`.
- 4 mức cho học sinh; 3 mức CV 7991 chỉ lúc **xem**.
- Dữ liệu tổng hợp. Không neko-core. Không mở rộng chủ đề lớp 10–12.
- `k` ô bảng sản phẩm 0-based; YAML kiểm định 1-based. Xem `docs/chi-so-o-bang.md`.
- Giữ `data-testid` và heading e2e: «Chào An», «Lớp 12A1 thử», `/3 mức/`, «Vào học».

## Lệnh

```bash
pnpm dev:math
pnpm dev:web
pnpm db:migrate && pnpm seed
pnpm test:math
pnpm test:web           # typecheck + lint + unit
pnpm --filter web test:e2e
```

Quy ước và lệnh cục bộ nằm ở `apps/web/AGENTS.md` và `services/math/AGENTS.md`. Nhánh: `CONTRIBUTING.md` (GitHub Flow, PR vào `main`, không chồng). Phiên bản: `docs/PHIEN-BAN.md` (một SemVer, Conventional Commits). Deploy: `docs/TRIEN-KHAI.md`. CI: `.github/workflows/ci.yml`.

## Giao diện

Phiếu làm bài. Không kit Figma Edu*, không cream + Literata + terracotta, không chép hex Coursera `#0056D2`, Khan `#1865f2`, IBM `#0F62FE`, Canvas electric, Brilliant pear.

Lưới **8 px**. Nút: cao thị giác **40**, mục tiêu chạm **44**, đệm ngang **16**, bán kính **6**. `padding` nới hit; `margin` không.

Khối đứng (login, hero giữa trang): **tâm quang học ≈ 46%** từ đỉnh, không giữa hình học 50%. Dư dưới : dư trên = **3 : 2**. Không `translateY(-Nvh)`. Chi tiết và nguồn: `docs/DESIGN.md`.

Skill khi đụng UI/API (nạp khi cần, không nhét vào mọi phiên): `.cursor/skills/frontend-design`, `.cursor/skills/web-design-guidelines`, `.cursor/skills/fastapi-routers`.

## Bảo mật

Không đọc `.env`, `.env.local`, khóa, `*.pem`. Không commit hay đẩy `zaiapikey.txt`, `ZAI_API_KEY`, khóa lớp. Mẫu biến: `.env.example`. CI `scripts/kiem-khoa.mjs`. Deny theo phiên: `.claude/settings.json`.

## Tài khoản thử

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Giáo viên | `gv@demo.local` | `giaovien123` |
| Học sinh | `hs.an@demo.local` | `hocsinh123` |

## Cách làm

1. Đọc bản đồ rồi sửa đúng tầng: UI → `apps/web`; CAS → `services/math` (sandbox).
2. Không bịa tính năng sản phẩm. Không POST skill ra ngoài repo.
3. Giữ harness gầy: kiến thức tái sử dụng để ở skill, không nhồi `AGENTS.md`.
