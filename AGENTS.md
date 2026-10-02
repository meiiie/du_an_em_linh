# Hướng dẫn cho agent

Nguồn chuẩn cho mọi agent (Codex, Cursor, Claude Code…). Claude Code đọc `CLAUDE.md`, tệp đó nhập `@AGENTS.md` rồi thêm ghi chú riêng. Giữ tệp này dưới 200 dòng: kiến thức dài để ở skill, ràng buộc theo vùng để ở `.claude/rules/`.

## Đây là gì

**Phần mềm học toán với AI** — hợp đồng dài hạn. Mục tiêu sản phẩm (đọc từ sơ đồ khách): [`docs/product/MUC-TIEU.md`](docs/product/MUC-TIEU.md).

- **v0 (mã hiện có):** nguyên mẫu NCKH, **một** chủ đề Toán 12 — đơn điệu và cực trị. `apps/web` (Next.js) + `services/math` (FastAPI + SymPy). Mọi chữ trên màn hình là tiếng Việt. Demo mặc định nhà `offline` — chạy không cần `LLM_API_KEY`.
- **v2 (chấp nhận 2026-10-01, [ADR 011](docs/adr/011-kien-truc-v2.md)):** `apps/frontend` (Angular 22) + `services/core` (Spring Boot 4.1, Java 25) + giữ `services/math`. Làm theo pha trong [`docs/product/LO-TRINH.md`](docs/product/LO-TRINH.md); `apps/web` (v0) đóng băng, chỉ sửa lỗi, gỡ khi v2 tương đương.

## Thứ bậc và bản đồ

Hiến chương [`docs/HIEN-CHUONG.md`](docs/HIEN-CHUONG.md) > ADR đã chấp nhận (`docs/adr/`) > tệp này > rule, skill.

| Cần | Đọc |
| --- | --- |
| Quy trình, lệnh kích hoạt («check đi», «check #N», «lab …»), chuẩn GitHub | [`docs/QUY-TRINH.md`](docs/QUY-TRINH.md) |
| Bản đồ mã | [`docs/CODEMAP.md`](docs/CODEMAP.md) — đọc trước khi grep cả repo |
| Thiết kế giao diện | [`docs/DESIGN.md`](docs/DESIGN.md) |
| Gia sư AI | [`docs/AI-HARNESS.md`](docs/AI-HARNESS.md) |
| Số đo kiểm thử thật | [`docs/KIEM-THU.md`](docs/KIEM-THU.md) |
| Lab: thiết kế, sư phạm, kiểm định, nghiên cứu, quyết định | [`labs/README.md`](labs/README.md) |

## Không mở lại (v0 — ADR 002–010)

- Khung 5 bước `B.DH.TXD` / `B.DH.DAOHAM` / `B.DH.NGHIEM` / `B.DH.XETDAU` / `B.DH.KETLUAN`. Chấm cả bước, không tô từng ô khi gõ.
- Cổng 3 tầng: `DAT` | `SAI` | `KHONG_KIEM_DUOC` + `GV_DUYET`.
- Gia sư **không** đọc lời giải chuẩn. Thứ tự: luật xin đáp án → thang 3 cấp (không bottom-out) → kho lớp (tài liệu + công thức) → nhà đã chọn → lọc SymPy. Kết nối = khóa chính thức một lần: ChatGPT, OpenRouter (lập trình) hoặc Z.AI (coding) — hoặc OAuth định danh nếu có `client_id` OpenAI cấp. Không device-OAuth Codex. Không nhập URL nhà. Chi tiết: `docs/AI-HARNESS.md`.
- 4 mức cho học sinh; 3 mức CV 7991 chỉ lúc **xem**.
- Dữ liệu tổng hợp. Không neko-core. v0 không mở rộng chủ đề lớp 10–12 (mở rộng nội dung thuộc v2, qua lab Sư phạm).
- `k` ô bảng sản phẩm 0-based; YAML kiểm định 1-based. Xem `docs/chi-so-o-bang.md`.
- Giữ `data-testid` và heading e2e: «Chào An», «Lớp 12A1 thử», `/3 mức/`, «Vào học».

## Lệnh

```bash
pnpm dev:math
pnpm dev:web
pnpm db:migrate && pnpm seed
pnpm test:math
pnpm test:web                       # typecheck + lint + unit
pnpm --filter web test:e2e
pnpm test:khoa                      # quét khóa trong git
(cd services/core && ./mvnw verify)  # v2 core: build + test + ArchUnit (JDK 25)
pnpm --filter frontend build && pnpm --filter frontend test  # v2 frontend (Node 24)
node --test .claude/hooks/*.test.mjs  # test hook của harness
```

Lệnh cục bộ: `apps/web/AGENTS.md`, `services/math/AGENTS.md`, `services/core/AGENTS.md`, `apps/frontend/AGENTS.md`. Chạy test của đúng thư mục đã đụng, không chạy cả repo khi không cần.

## Cách làm

1. Việc đến từ GitHub Issue có tiêu chí nghiệm thu. Một việc → một nhánh `feat/` `fix/` `docs/` `chore/` → một PR vào `main`. Không chồng PR, không đẩy thẳng `main`, agent không merge.
2. Đọc bản đồ rồi sửa đúng tầng: UI v0 → `apps/web`; UI v2 → `apps/frontend`; nghiệp vụ v2 → `services/core`; CAS → `services/math` (sandbox). Diff tối thiểu, đúng phạm vi.
3. Commit Conventional Commits (tiêu đề tiếng Việt được), trailer `Co-Authored-By` khi agent viết. Stage từng đường dẫn.
4. Trước PR: chạy cổng của vùng đã đụng, ghi số đo thật (lệnh + kết quả + SHA) vào PR. Không viết «đã test» chung chung.
5. Quyết định khó đảo ngược → phân tích ở `labs/decisions/`, ADR «Đề xuất», chờ chủ repo duyệt rồi mới viết mã.
6. Dữ liệu sư phạm (`data/supham/`) và kiểm định (`services/math/kiemdinh/`) thuộc lab: chỉ sửa khi áp **bản vá nguyên văn** có mã.
7. Không bịa tính năng, số liệu, nguồn. Tra cứu phiên bản, luật, nghiên cứu mới — không dựa trí nhớ mô hình.

## Giao diện

Phiếu làm bài. Không kit Figma Edu*, không cream + Literata + terracotta, không chép hex Coursera `#0056D2`, Khan `#1865f2`, IBM `#0F62FE`, Canvas electric, Brilliant pear.

Lưới **8 px**. Nút: cao thị giác **40**, mục tiêu chạm **44**, đệm ngang **16**, bán kính **6**. `padding` nới hit; `margin` không. Khối đứng: **tâm quang học ≈ 46 %** từ đỉnh, dư dưới : dư trên = **3 : 2**. Chi tiết và nguồn: `docs/DESIGN.md`.

Skill nạp khi cần: Claude Code `.claude/skills/` (`design-study`, …); Cursor `.cursor/skills/frontend-design`, `.cursor/skills/web-design-guidelines`, `.cursor/skills/fastapi-routers`.

## Bảo mật

Không đọc, không ghi `.env`, `.env.local`, khóa, `*.pem` (Claude Code: `permissions.deny` + hook chặn). Không commit `zaiapikey.txt`, `ZAI_API_KEY`, khóa lớp. Mẫu biến: `.env.example`. Không dữ liệu học sinh thật ở bất kỳ đâu trong repo, issue, prompt.

## Tài khoản thử

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Giáo viên | `gv@demo.local` | `giaovien123` |
| Học sinh | `hs.an@demo.local` | `hocsinh123` |
