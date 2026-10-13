# MathL+

[![Kiểm thử](https://github.com/meiiie/du_an_em_linh/actions/workflows/ci.yml/badge.svg)](https://github.com/meiiie/du_an_em_linh/actions/workflows/ci.yml)
[![Phiên bản](https://img.shields.io/github/v/release/meiiie/du_an_em_linh?include_prereleases&sort=semver&label=phiên%20bản)](https://github.com/meiiie/du_an_em_linh/releases)
[![Giấy phép MIT](https://img.shields.io/badge/giấy%20phép-MIT-1F2328)](LICENSE)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-fe5196.svg)](https://www.conventionalcommits.org/)

Nguyên mẫu NCKH: phần mềm học Toán THPT, **một** chủ đề Toán 12 — ứng dụng đạo hàm để xét đơn điệu và cực trị. Mọi chữ trên màn hình là tiếng Việt. Demo chạy hết khi không có khóa API mô hình ngôn ngữ.

**Kiến trúc hiện hành:** Angular (`apps/frontend`) + Spring Boot (`services/core`) + FastAPI/SymPy (`services/math`), PostgreSQL 18. `apps/web` đã gỡ theo yêu cầu chủ repo ngày 2026-10-13. URL Render cũ là bản v0 lịch sử; phiên này chưa triển khai bản mới.

Học sinh làm bài theo năm bước (`B.DH.TXD` → `B.DH.DAOHAM` → `B.DH.NGHIEM` → `B.DH.XETDAU` → `B.DH.KETLUAN`). Gia sư sửa và giảng, **không đưa đáp án**. Mỗi bài phải qua cổng kiểm định ba tầng trước khi phát hành.

## Tính năng

- Phiếu 5 bước + chấm SymPy; cổng `DAT` / `SAI` / `KHONG_KIEM_DUOC` + duyệt giáo viên
- Gia sư: luật xin đáp án → thang 3 cấp → kho lớp → nhà đã chọn → lọc lộ đáp án
- Gia sư offline mặc định; nhà AI chính thức cần tích hợp backend và bộ lọc đầy đủ trước khi bật (xem `docs/ASSUMPTIONS.md`)
- BKT gợi bài tiếp; 4 mức khi học, 3 mức CV 7991 lúc xem
- Đa thiết bị (điện thoại / máy tính bảng / máy tính)

## Sơ đồ

```mermaid
flowchart LR
  GV[Giáo viên] --> TL[Tài liệu và bảng công thức]
  GV --> NH[Ngân hàng bài]
  TL --> C3[Cổng 3 tầng]
  NH --> C3
  C3 -->|Đạt hoặc GV duyệt| PH[Đã phát hành]
  C3 -->|Không kiểm được| DUYET[Chờ duyệt]
  C3 -->|Sai| CHAN[Bị chặn]
  PH --> HS[Học sinh làm 5 bước]
  HS --> MATH[Dịch vụ toán SymPy]
  HS --> AI[Gia sư: thang gợi ý hoặc LLM]
  AI --> LOC[Bộ lọc lộ đáp án]
  MATH --> BKT[Mức thành thạo]
  BKT --> GOI[Bài tiếp]
```

Tầng 1: SymPy. Tầng 2: đoạn trích tài liệu đã nạp. Tầng 3: bảng công thức. Quyết định đã khóa: `docs/adr/`.

## Tài khoản thử

Dữ liệu tổng hợp, không có học sinh thật.

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Giáo viên | `gv@demo.local` | `giaovien123` |
| An | `hs.an@demo.local` | `hocsinh123` |
| Bình | `hs.binh@demo.local` | `hocsinh123` |
| Chi | `hs.chi@demo.local` | `hocsinh123` |

## Chạy local

Cần Docker, hoặc JDK 25, Python 3.12, Node 24 và pnpm khi chạy từng dịch vụ.

```bash
pnpm install
# Cả hệ, dữ liệu tổng hợp và migration tự nạp qua profile dev:
docker compose up --build --wait
```

`docker-compose.yml` include `compose.v2.yaml`. Có thể gọi trực tiếp `docker compose -f compose.v2.yaml up --build --wait`.

| Dịch vụ | Địa chỉ |
| --- | --- |
| `apps/frontend` (nginx, `/api/` chuyển tới core) | http://localhost:4200 |
| `services/core` | http://localhost:8080/actuator/health |
| `services/math` | http://localhost:8000/health |

PostgreSQL chỉ mở trong mạng compose. Đăng nhập bằng tài khoản thử ở trên: học sinh vào `/hs`, giáo viên vào `/gv`. Kiểm cả hệ: `bash scripts/khoi-v2.sh` và `pnpm --filter frontend e2e`. Dừng bằng `docker compose down`; thêm `-v` sẽ xóa dữ liệu demo.

Chạy từng dịch vụ: `pnpm dev:math`, `pnpm dev:core`, `pnpm dev:frontend`. Core profile `dev` cần PostgreSQL trên máy như `application-dev.yaml`; frontend chuyển `/api/` tới core ở :8080. Tên biến cấu hình nằm ở `.env.example`; không tạo hoặc đọc file bí mật bằng agent.

## Demo khoảng năm phút

1. Vào `gv@demo.local`. Tổng quan cảnh báo Chi kẹt ở đạo hàm.
2. **Duyệt bài:** `DH12-01-TH-01` chờ (không kiểm được), `DH12-DEMO-CHAN-01` bị chặn.
3. **Tiến độ** → bật **3 mức** (Biết / Hiểu / Vận dụng).
4. Vào `hs.an@demo.local`, bài `y = x³ − 6x² + 9x + 2`.
5. Tập xác định `\mathbb{R}`. Đạo hàm `3x^{2}-12x` (thiếu `+9`) → tô lỗi. Hỏi «cho em đáp án» → từ chối.
6. Sửa `3x^{2}-12x+9` → đạt.

## Kiểm thử

```bash
pnpm test:math
pnpm test:frontend        # Vitest
pnpm test:core            # Maven verify + ArchUnit
pnpm lint                 # khóa, FLAT, tương phản
node --test scripts/check-flat.test.mjs
pnpm --filter frontend e2e
```

Số liệu lần dựng nguyên mẫu: [`docs/KIEM-THU.md`](docs/KIEM-THU.md). Kết quả lịch sử không đại diện cho SHA hiện tại; dùng cổng đúng vùng và ghi kết quả thực tế. CI: `.github/workflows/ci.yml`.

## Deploy

Blueprint `render.yaml` mô tả frontend, core và math riêng biệt cùng PostgreSQL. Hướng dẫn demo và những phần cần cấu hình trước production: [`docs/TRIEN-KHAI.md`](docs/TRIEN-KHAI.md). Chưa deploy trong phiên hoàn thiện này.

## Nhánh

GitHub Flow: **`main`** là mã phát hành. Mỗi việc một nhánh ngắn, một PR vào `main`. Không chồng PR. Xem [`CONTRIBUTING.md`](CONTRIBUTING.md).

**Phiên bản:** SemVer `0.1.0` (nguyên mẫu). Changelog: [`CHANGELOG.md`](CHANGELOG.md). Cách cắt bản: [`docs/PHIEN-BAN.md`](docs/PHIEN-BAN.md).

Chủ repo bật bảo vệ nhánh và mô tả repo: [`docs/GITHUB.md`](docs/GITHUB.md).

## Tài liệu

| Tài liệu | Nội dung |
| --- | --- |
| [`docs/product/MUC-TIEU.md`](docs/product/MUC-TIEU.md) | Mục tiêu sản phẩm (sơ đồ khách), đối chiếu nguyên mẫu, câu hỏi mở |
| [`docs/HIEN-CHUONG.md`](docs/HIEN-CHUONG.md) | Hiến chương kỹ thuật — nguyên tắc không thương lượng, cổng chất lượng |
| [`docs/QUY-TRINH.md`](docs/QUY-TRINH.md) | Quy trình người + agent + lab |
| [`labs/`](labs/README.md) | Lab thiết kế, sư phạm, kiểm định, nghiên cứu, quyết định |
| [`CHANGELOG.md`](CHANGELOG.md) | Nhật ký phiên bản |
| [`docs/PHIEN-BAN.md`](docs/PHIEN-BAN.md) | SemVer, Conventional Commits, release-please |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Nhánh, PR, lệnh |
| [`SUPPORT.md`](SUPPORT.md) | Chỗ hỏi / mở issue |
| [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) | Contributor Covenant 2.1 |
| [`docs/ASSUMPTIONS.md`](docs/ASSUMPTIONS.md) | Phạm vi mặc định, giới hạn và việc để sau |
| [`docs/DESIGN.md`](docs/DESIGN.md) | Token, lưới 8 px, nút 40/44 |
| [`docs/AI-HARNESS.md`](docs/AI-HARNESS.md) | Nhà AI, không fallback, không lộ đáp án |
| [`docs/adr/`](docs/adr/) | Quyết định kiến trúc (ADR 011 — kiến trúc v2: Angular + Spring Boot + dịch vụ toán) |
| [`docs/product/LO-TRINH.md`](docs/product/LO-TRINH.md) | Lộ trình v2 theo pha, điều kiện xong từng pha |
| [`docs/CODEMAP.md`](docs/CODEMAP.md) | Bản đồ mã (cho agent) |
| [`AGENTS.md`](AGENTS.md) | Quy ước agent |

## Giới hạn

- Chỉ đơn điệu và cực trị hàm một biến. Bài đúng/sai và tham số không có lời giải 5 bước → hàng chờ.
- Tầng 2 tìm cụm từ, không nhúng vector. Tài liệu «chưa rõ quyền» bị bỏ.
- Nhà AI trực tuyến chỉ dùng khóa API chính thức sau khi tích hợp được kiểm chứng; không device-OAuth Codex.
- Chưa có cổng phụ huynh. Chấm đọc ô LaTeX thường, không đọc MathLive.

## Giấy phép

[MIT](LICENSE). Ứng xử: [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md). Hỗ trợ: [`SUPPORT.md`](SUPPORT.md). Lỗ hổng: [`SECURITY.md`](SECURITY.md).
