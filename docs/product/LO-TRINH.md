# Lộ trình v2

> Nguồn chuẩn về thứ tự làm. Nâng từ [`labs/decisions/2026-10-01-kien-truc-v2.md`](../../labs/decisions/2026-10-01-kien-truc-v2.md) §7 sau khi ADR 011 được chấp nhận (2026-10-01). Đổi thứ tự hay điều kiện xong → PR sửa file này, chủ repo duyệt.

Nguyên tắc: **strangler theo luồng**. v0 (`apps/web`) vẫn chạy để demo cho tới khi v2 đạt tương đương luồng A và B ([`MUC-TIEU.md`](MUC-TIEU.md) §3). Mỗi pha xong khi điều kiện đo được ở cột cuối đạt, có số đo thật trong PR.

| Pha | Kết quả | Năng lực | Điều kiện xong |
| --- | --- | --- | --- |
| **P0** — đang làm | Hiến chương, quy trình, 5 lab, harness Claude Code, ADR 011; nhãn GitHub; Spec Kit | — | PR harness và PR Spec Kit được merge |
| **P1** | Khung chạy được: `services/core` (identity port từ LMS, Flyway, PostgreSQL 18), `apps/frontend` (đăng nhập, khung trang), Compose 3 dịch vụ, CI theo đường dẫn, staging | Nền tảng | GV và HS đăng nhập end-to-end trên staging; CI xanh cả 4 job |
| **P2** | Luồng B trên v2: làm bài khung bước + gia sư + bộ lọc | C5, C7 | e2e `luong-hoc-sinh`, `gia-su-harness` xanh trên v2; bộ AI 70 ca đạt; 0 lộ trong bộ dụ đáp án |
| **P3** | Luồng A + E: kho tài liệu, bảng công thức, ngân hàng 4 dạng câu, cổng 3 tầng, bảng lớp, giao bài | C1–C5 | e2e phát hành, duyệt, giao bài xanh; đề ôn đúng tỉ lệ CV 7991 |
| **P4** | Cá nhân hóa + kế hoạch: mức hiểu, chọn bài (chữa lỗi / củng cố / nâng 1 nấc), lịch tuần, web push | C6, C8–C10 | Bộ đo cá nhân hóa của lab Kiểm định đạt; nhắc lịch tới được thiết bị thật |
| **P5** | Mở rộng nội dung Toán 12 theo lab Sư phạm; OCR đề mẫu; bộ ôn tập định dạng thi TN THPT | C1, C3, C4 | Theo từng gói chủ đề (`labs/pedagogy/README.md`) |
| Gỡ v0 | Xóa `apps/web`, thay ADR 001 | — | P2 + P3 tương đương, chủ repo đồng ý |

Trước khi pilot với học sinh thật (bất kỳ pha nào): ADR quyền riêng tư được duyệt và luồng đồng ý phụ huynh chạy được (hiến chương III).

## Issue đề xuất cho P1

Dạng sẵn để Codex hoặc chủ repo mở issue (`priority/p1`, `area/*`). Mỗi issue một PR.

| # | Tiêu đề | Tiêu chí nghiệm thu |
| --- | --- | --- |
| 1 | `chore(core): dựng khung services/core — Spring Boot 4.1, Java 25, Maven Wrapper` | `./mvnw test` xanh; `/actuator/health` UP; ArchUnit (chép từ LMS) chạy và xanh; JSpecify bật |
| 2 | `feat(core): port identity từ LMS — JWT, 4 vai trò` | Đăng nhập / làm mới token / đăng xuất; vai trò ADMIN, SCHOOL_ADMIN, TEACHER, STUDENT; test Testcontainers; nguồn `LMS_hohulili@<sha>` ghi trong PR |
| 3 | `chore(frontend): dựng khung apps/frontend — Angular 22 zoneless` | Build + Vitest xanh; trang `/dang-nhap` theo `docs/DESIGN.md` (tâm quang học 46 %); 390 / 1280 px |
| 4 | `feat(frontend): đăng nhập và khung trang HS / GV gọi services/core` | Route `/hs`, `/gv` có guard; `data-testid` khớp v0 cho màn tương đương |
| 5 | `build: docker compose cho frontend + core + math + PostgreSQL 18` | Một lệnh dựng cả hệ; health check từng dịch vụ; `.env.example` đủ biến |
| 6 | `ci: job theo đường dẫn cho services/core và apps/frontend` | Job chỉ chạy khi thư mục tương ứng đổi; harness, toán, web v0 giữ nguyên |
| 7 | `docs(adr): ADR quyền riêng tư cho dữ liệu học sinh` | Luồng đồng ý, tối thiểu hóa, chuyển dữ liệu ra nước ngoài khi gọi LLM, thời hạn lưu; mục cần luật sư xác nhận |
