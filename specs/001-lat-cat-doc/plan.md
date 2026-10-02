# Implementation Plan: P2 — Lát cắt dọc một chủ đề

**Branch**: `001-lat-cat-doc` | **Date**: 2026-10-02 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-lat-cat-doc/spec.md`

## Summary

Chạy hết vòng của sơ đồ trên v2 cho Toán 12 «đơn điệu và cực trị», dùng lại nội dung và dịch vụ toán đã kiểm định của v0. `services/core` có thêm 6 module nghiệp vụ: lớp, nội dung + cổng, làm bài, gia sư, mức hiểu, lịch. `apps/frontend` có 5 màn học sinh và 9 màn giáo viên tương đương v0. `services/math` giữ hợp đồng `/v1` và thêm đúng một job: kiểm lời giảng của gia sư (FR-015, [ADR 013](../../docs/adr/013-cong-ba-tang-cho-loi-gia-su.md)). Logic sư phạm của v0 (thang gợi ý, luật xin đáp án, mức hiểu, chọn bài kế, lịch) được chép sang Java theo đúng tham số, kèm bộ đối chiếu với v0.

## Technical Context

**Language/Version**: Java 25 (`services/core`), TypeScript 6 (`apps/frontend`), Python 3.12 (`services/math`)
**Primary Dependencies**: Spring Boot 4.1.1, Spring AI 2.0.1 (GA 2026-09-24, dựng trên Boot 4.1.1), Spring Security 7; Angular 22.2 (zoneless, Signal Forms), KaTeX 0.16, MathLive 0.107 (cùng bản v0); FastAPI + SymPy (giữ nguyên)
**Storage**: PostgreSQL 18, Flyway chỉ thêm (`V3__` trở đi)
**Testing**: JUnit 5 + Testcontainers PostgreSQL 18 + ArchUnit (core); Vitest + jsdom (frontend); Playwright trên compose v2 (e2e, 390 + 1280 px); pytest (math); các bộ kiểm của v0 chạy lại qua luồng mới
**Target Platform**: Docker Compose (`compose.v2.yaml`); trình duyệt điện thoại và máy tính
**Project Type**: Ứng dụng web 3 dịch vụ: SPA + dịch vụ nghiệp vụ + dịch vụ toán không trạng thái
**Performance Goals**: trạng thái đầu của lượt gia sư ≤ 1 s; offline ≤ 3 s (p95, máy dev); chấm một bước ≤ dịch vụ toán (trần 12 s, như v0)
**Constraints**: đóng mặc định ở mọi chỗ kiểm (chấm, lọc, cổng); không stream token; không tự chuyển nhà AI; không dữ liệu thật; khóa nhà AI chỉ ở biến môi trường
**Scale/Scope**: 1 lớp, 3 học sinh + 1 giáo viên tổng hợp; 1 chủ đề, 7 kỹ năng, 5 bước; ngân hàng bằng v0 (bài từ `data/supham/` + biến thể sinh có hạt giống); 14 màn

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Nguyên tắc | Trả lời | Cách đạt trong P2 |
| --- | --- | --- | --- |
| I | Không đưa đáp án | **CÓ** | Gia sư chỉ nhận đề, bước sai, loại lỗi, mã lỗi, gợi ý đã kiểm (FR-011); luật xin đáp án chạy trước mô hình (FR-013); mọi câu qua `/v1/filter` trên cả câu, đóng mặc định (FR-014); lời giải mẫu không rời `services/core` khi học sinh đang làm (FR-006) |
| II | Đúng toán trước hết | **CÓ** | Bài qua `/v1/verify` 3 tầng; công thức trong lời giảng qua job mới `kiem_loi_giang` 3 tầng (FR-015, ADR 013); lỗi hoặc hết giờ của dịch vụ toán = không đạt (FR-009) |
| III | Người học là trẻ vị thành niên | **CÓ** | Chỉ tài khoản `synthetic`; xóa định danh trước khi gửi nhà AI (FR-020); nhà thật chỉ cho tài khoản tổng hợp tới khi ADR 012 được duyệt; nhãn «Gia sư AI» (FR-021); giáo viên ghi đè được qua duyệt; khóa chỉ ở biến môi trường (FR-019) |
| IV | Bằng chứng | **CÓ** | Mỗi SC có lệnh và bộ ca (`quickstart.md` §Kiểm); ADR 013 cho quy tắc công thức trong lời giảng |
| V | Tiếng Việt đúng lứa tuổi | **CÓ** | Chữ giao diện chép từ v0 đã qua lab Thiết kế; câu mới qua skill `design-study` và rule `web-ui` |
| VI | Ranh giới kiến trúc | **CÓ** | `apps/frontend` không logic nghiệp vụ (không chấm, không giữ đáp án); `services/core` giữ trạng thái và quyền, 6 module mới đủ 3 tầng, ArchUnit xanh; `services/math` thuần hàm, job mới không trạng thái; Flyway chỉ thêm |
| VII | Thay đổi nhỏ | **CÓ** | Tách issue theo câu chuyện và theo dịch vụ (`tasks.md`), mỗi issue một PR; không trừu tượng hóa cho chủ đề khác trước khi có chủ đề thứ hai; giữ `data-testid` của v0 |
| VIII | Truy cập được | **CÓ** | e2e ở 390 + 1280 px; nút 40, chạm 44; bàn phím toán luôn có ô LaTeX dự phòng; trạng thái gia sư báo qua `aria-live` |

Kết quả cổng trước Phase 0: **đạt**. Kết quả sau Phase 1: **đạt**. Không có vi phạm cần ghi ở Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/001-lat-cat-doc/
├── spec.md
├── plan.md              # file này
├── research.md          # Phase 0: 12 quyết định
├── data-model.md        # Phase 1: thực thể, quan hệ, chuyển trạng thái
├── quickstart.md        # Phase 1: chạy và kiểm lát cắt
├── contracts/
│   ├── api-core.md      # REST + SSE giữa apps/frontend và services/core
│   └── math-v1.md       # services/core ↔ services/math: job cũ dùng lại + job kiem_loi_giang
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
services/core/src/main/java/vn/hoctoanai/core/
├── identity/            # có từ P1
├── classroom/           # lớp, ghi danh, cài lớp (mở lời giải, nhà AI được bật)
├── content/             # chủ đề, kỹ năng, mã lỗi, khung bước, bài, lời giải ẩn, thang gợi ý,
│                        # bảng công thức (phiên bản), tài liệu (đoạn trích), lượt kiểm 3 tầng, lượt duyệt
├── practice/            # giao bài, bài làm, bước nộp, kết quả chấm
├── tutor/               # phiên gia sư, lượt, luật xin đáp án, thang gợi ý, nhà AI (Spring AI), SSE trạng thái
├── mastery/             # cấu hình BKT, mức hiểu, sự kiện, bài kế, cảnh báo kẹt
├── planner/             # thời gian biểu tuần, lời khuyên, nhắc trong app
└── shared/              # Clock, client dịch vụ toán (hết giờ = lỗi, không bao giờ «đạt»)
services/core/src/main/resources/db/migration/
└── V3__classroom.sql … V8__planner.sql
services/core/src/test/java/…   # unit theo module, Testcontainers, đối chiếu v0, ArchUnit

services/math/app/
└── loi_giang.py         # job mới kiem_loi_giang (ADR 013); routers.py thêm POST /v1/kiem-loi-giang

apps/frontend/src/app/
├── api/                 # kiểu khớp DTO theo module
├── features/
│   ├── hoc-sinh/        # trang Học, Đề bài, Luyện (phiếu 5 bước + gia sư), Lịch, Công thức
│   └── giao-vien/       # Lớp, Duyệt, Đề bài, Tài liệu, Công thức, Mức, Học sinh, Cài lớp, Gia sư
└── shared/
    ├── toan/            # KaTeX hiển thị, ô MathLive làm control của Signal Forms + ô LaTeX dự phòng
    └── layout/          # khung có thanh bên (mo-sidebar, nav-*) như v0
apps/frontend/e2e/       # một vòng, duyệt, tới VDC, bản tương đương luong-hoc-sinh, gia-su-harness

data/supham/             # nguồn nội dung (lab Sư phạm sở hữu), không sửa
```

**Structure Decision**: giữ 3 dịch vụ của ADR 011. Mỗi nhóm khả năng của sơ đồ là một module DDD trong `services/core` (gói con `domain` → `application` → `infrastructure`, ArchUnit kiểm). Module giao tiếp qua port trong `application`, không gọi chéo repository của nhau. `services/math` chỉ thêm một job thuần hàm. `apps/frontend` chia theo vai trò rồi theo màn, giữ route và `data-testid` của v0.

## Complexity Tracking

Không có vi phạm hiến chương. Hai điểm cần theo dõi:

| Điểm | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Job mới `kiem_loi_giang` trong `services/math` | Tầng 1 cần SymPy; bộ lọc chỉ có một bản, trong `services/math` (ADR 011) | Viết bộ nhận công thức bằng Java thì có hai bản logic toán, lệch nhau theo thời gian |
| Chép logic sư phạm từ TypeScript sang Java | Gia sư, mức hiểu, bài kế phải nằm ở `services/core` (ADR 011) | Gọi ngược `apps/web` (v0) thì không gỡ được v0 |
