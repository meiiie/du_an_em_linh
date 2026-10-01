# Lộ trình v2

> Nguồn chuẩn về thứ tự làm. Nâng từ [`labs/decisions/2026-10-01-kien-truc-v2.md`](../../labs/decisions/2026-10-01-kien-truc-v2.md) §7 sau khi ADR 011 được chấp nhận (2026-10-01). Đổi thứ tự hay điều kiện xong → PR sửa file này, chủ repo duyệt.

Nguyên tắc: **strangler theo luồng**. v0 (`apps/web`) vẫn chạy để demo cho tới khi v2 đạt tương đương luồng A và B ([`MUC-TIEU.md`](MUC-TIEU.md) §3). Mỗi pha xong khi điều kiện đo được ở cột cuối đạt, có số đo thật trong PR.

| Pha | Kết quả | Năng lực | Điều kiện xong |
| --- | --- | --- | --- |
| **P0** — xong 2026-10-01 | Hiến chương, quy trình, 5 lab, harness Claude Code, ADR 011; nhãn GitHub; Spec Kit; ruleset `main` | — | [#52](https://github.com/meiiie/du_an_em_linh/issues/52), [#53](https://github.com/meiiie/du_an_em_linh/issues/53) merge; ruleset `main-protection` active |
| **P1** — đang làm | Khung chạy được: `services/core` (identity port từ LMS, Flyway, PostgreSQL 18), `apps/frontend` (đăng nhập, khung trang), Compose 3 dịch vụ, CI theo đường dẫn, staging | Nền tảng | GV và HS đăng nhập end-to-end trên staging; CI xanh cả 4 job |
| **P2** | Luồng B trên v2: làm bài khung bước + gia sư + bộ lọc | C5, C7 | e2e `luong-hoc-sinh`, `gia-su-harness` xanh trên v2; bộ AI 70 ca đạt; 0 lộ trong bộ dụ đáp án |
| **P3** | Luồng A + E: kho tài liệu, bảng công thức, ngân hàng 4 dạng câu, cổng 3 tầng, bảng lớp, giao bài | C1–C5 | e2e phát hành, duyệt, giao bài xanh; đề ôn đúng tỉ lệ CV 7991 |
| **P4** | Cá nhân hóa + kế hoạch: mức hiểu, chọn bài (chữa lỗi / củng cố / nâng 1 nấc), lịch tuần, web push | C6, C8–C10 | Bộ đo cá nhân hóa của lab Kiểm định đạt; nhắc lịch tới được thiết bị thật |
| **P5** | Mở rộng nội dung Toán 12 theo lab Sư phạm; OCR đề mẫu; bộ ôn tập định dạng thi TN THPT | C1, C3, C4 | Theo từng gói chủ đề (`labs/pedagogy/README.md`) |
| Gỡ v0 | Xóa `apps/web`, thay ADR 001 | — | P2 + P3 tương đương, chủ repo đồng ý |

Trước khi pilot với học sinh thật (bất kỳ pha nào): ADR quyền riêng tư được duyệt và luồng đồng ý phụ huynh chạy được (hiến chương III).

## Issue P1

Mở ngày 2026-10-01, mỗi issue một PR; tiêu chí nghiệm thu nằm trong issue.

| Issue | Việc | Phụ thuộc |
| --- | --- | --- |
| [#54](https://github.com/meiiie/du_an_em_linh/issues/54) | Dựng khung `services/core` — Spring Boot 4.1, Java 25, Maven Wrapper | — |
| [#55](https://github.com/meiiie/du_an_em_linh/issues/55) | Port identity từ LMS — JWT, 4 vai trò | [#54](https://github.com/meiiie/du_an_em_linh/issues/54) |
| [#56](https://github.com/meiiie/du_an_em_linh/issues/56) | Dựng khung `apps/frontend` — Angular 22 zoneless | — |
| [#57](https://github.com/meiiie/du_an_em_linh/issues/57) | Đăng nhập và khung trang HS / GV gọi `services/core` | [#55](https://github.com/meiiie/du_an_em_linh/issues/55), [#56](https://github.com/meiiie/du_an_em_linh/issues/56) |
| [#58](https://github.com/meiiie/du_an_em_linh/issues/58) | Docker compose cho frontend + core + math + PostgreSQL 18 | [#54](https://github.com/meiiie/du_an_em_linh/issues/54), [#56](https://github.com/meiiie/du_an_em_linh/issues/56) |
| [#59](https://github.com/meiiie/du_an_em_linh/issues/59) | CI theo đường dẫn cho `services/core` và `apps/frontend`; thêm check bắt buộc | [#54](https://github.com/meiiie/du_an_em_linh/issues/54), [#56](https://github.com/meiiie/du_an_em_linh/issues/56) |
| [#60](https://github.com/meiiie/du_an_em_linh/issues/60) | ADR quyền riêng tư cho dữ liệu học sinh | — |

Câu hỏi mở cho khách (Q1–Q10, đang làm theo giả định mặc định): [#61](https://github.com/meiiie/du_an_em_linh/issues/61).
