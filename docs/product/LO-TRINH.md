# Lộ trình v2

> Nguồn chuẩn về thứ tự làm. Nâng từ [`labs/decisions/2026-10-01-kien-truc-v2.md`](../../labs/decisions/2026-10-01-kien-truc-v2.md) §7 sau khi ADR 011 được chấp nhận (2026-10-01). Đổi thứ tự hay điều kiện xong → PR sửa file này, chủ repo duyệt.

Nguyên tắc: **strangler theo luồng**. v0 (`apps/web`) vẫn chạy để demo cho tới khi v2 đạt tương đương luồng A và B ([`MUC-TIEU.md`](MUC-TIEU.md) §3). Mỗi pha xong khi điều kiện đo được ở cột cuối đạt, có số đo thật trong PR.

| Pha | Kết quả | Năng lực | Điều kiện xong |
| --- | --- | --- | --- |
| **P0** — xong 2026-10-01 | Hiến chương, quy trình, 5 lab, harness Claude Code, ADR 011; nhãn GitHub; Spec Kit; ruleset `main` | — | [#52](https://github.com/meiiie/du_an_em_linh/issues/52), [#53](https://github.com/meiiie/du_an_em_linh/issues/53) merge; ruleset `main-protection` active |
| **P1** — đang làm (còn staging) | Khung chạy được: `services/core` (identity port từ LMS, Flyway, PostgreSQL 18), `apps/frontend` (đăng nhập, khung trang), Compose 3 dịch vụ, CI theo đường dẫn, staging | Nền tảng | GV và HS đăng nhập end-to-end trên staging ([#75](https://github.com/meiiie/du_an_em_linh/issues/75)); CI xanh cả 8 check bắt buộc |
| **P2** | Luồng B trên v2: làm bài khung bước + gia sư + bộ lọc | C5, C7 | e2e `luong-hoc-sinh`, `gia-su-harness` xanh trên v2; bộ AI 70 ca đạt; 0 lộ trong bộ dụ đáp án |
| **P3** | Luồng A + E: kho tài liệu, bảng công thức, ngân hàng 4 dạng câu, cổng 3 tầng, bảng lớp, giao bài | C1–C5 | e2e phát hành, duyệt, giao bài xanh; đề ôn đúng tỉ lệ CV 7991 |
| **P4** | Cá nhân hóa + kế hoạch: mức hiểu, chọn bài (chữa lỗi / củng cố / nâng 1 nấc), lịch tuần, web push | C6, C8–C10 | Bộ đo cá nhân hóa của lab Kiểm định đạt; nhắc lịch tới được thiết bị thật |
| **P5** | Mở rộng nội dung Toán 12 theo lab Sư phạm; OCR đề mẫu; bộ ôn tập định dạng thi TN THPT | C1, C3, C4 | Theo từng gói chủ đề (`labs/pedagogy/README.md`) |
| Gỡ v0 | Xóa `apps/web`, thay ADR 001 | — | P2 + P3 tương đương, chủ repo đồng ý |

Trước khi pilot với học sinh thật (bất kỳ pha nào): ADR quyền riêng tư được duyệt và luồng đồng ý phụ huynh chạy được (hiến chương III).

## Issue P1

Mở ngày 2026-10-01, mỗi issue một PR; tiêu chí nghiệm thu nằm trong issue. Cập nhật 2026-10-02: e2e đăng nhập GV / HS (390 + 1280 px) đã chạy trên compose trong CI; còn staging.

| Issue | Việc | Phụ thuộc | Trạng thái |
| --- | --- | --- | --- |
| [#54](https://github.com/meiiie/du_an_em_linh/issues/54) | Dựng khung `services/core` — Spring Boot 4.1, Java 25, Maven Wrapper | — | xong — [#65](https://github.com/meiiie/du_an_em_linh/pull/65) |
| [#55](https://github.com/meiiie/du_an_em_linh/issues/55) | Port identity từ LMS — JWT, 4 vai trò | [#54](https://github.com/meiiie/du_an_em_linh/issues/54) | xong — [#68](https://github.com/meiiie/du_an_em_linh/pull/68) |
| [#56](https://github.com/meiiie/du_an_em_linh/issues/56) | Dựng khung `apps/frontend` — Angular 22 zoneless | — | xong — [#67](https://github.com/meiiie/du_an_em_linh/pull/67) |
| [#57](https://github.com/meiiie/du_an_em_linh/issues/57) | Đăng nhập và khung trang HS / GV gọi `services/core` | [#55](https://github.com/meiiie/du_an_em_linh/issues/55), [#56](https://github.com/meiiie/du_an_em_linh/issues/56) | xong — [#72](https://github.com/meiiie/du_an_em_linh/pull/72) (cookie HttpOnly), [#73](https://github.com/meiiie/du_an_em_linh/pull/73) |
| [#58](https://github.com/meiiie/du_an_em_linh/issues/58) | Docker compose cho frontend + core + math + PostgreSQL 18 | [#54](https://github.com/meiiie/du_an_em_linh/issues/54), [#56](https://github.com/meiiie/du_an_em_linh/issues/56) | xong — [#71](https://github.com/meiiie/du_an_em_linh/pull/71) |
| [#59](https://github.com/meiiie/du_an_em_linh/issues/59) | CI theo đường dẫn cho `services/core` và `apps/frontend`; thêm check bắt buộc | [#54](https://github.com/meiiie/du_an_em_linh/issues/54), [#56](https://github.com/meiiie/du_an_em_linh/issues/56) | xong — [#70](https://github.com/meiiie/du_an_em_linh/pull/70), ruleset 8 check |
| [#60](https://github.com/meiiie/du_an_em_linh/issues/60) | ADR quyền riêng tư cho dữ liệu học sinh | — | ADR 012 «Đề xuất» — [#66](https://github.com/meiiie/du_an_em_linh/pull/66); chờ chủ repo và luật sư |
| [#69](https://github.com/meiiie/du_an_em_linh/issues/69) | Giới hạn đăng nhập sai (F-10) cho `services/core` | [#55](https://github.com/meiiie/du_an_em_linh/issues/55) | xong — [#74](https://github.com/meiiie/du_an_em_linh/pull/74) |
| [#75](https://github.com/meiiie/du_an_em_linh/issues/75) | Staging cho v2 | [#57](https://github.com/meiiie/du_an_em_linh/issues/57), [#58](https://github.com/meiiie/du_an_em_linh/issues/58) | chờ chủ repo chọn nơi chạy |

Câu hỏi mở cho khách (Q1–Q10, đang làm theo giả định mặc định): [#61](https://github.com/meiiie/du_an_em_linh/issues/61).
