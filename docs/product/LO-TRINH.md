# Lộ trình v2

> Nguồn chuẩn về thứ tự làm. Nâng từ [`labs/decisions/2026-10-01-kien-truc-v2.md`](../../labs/decisions/2026-10-01-kien-truc-v2.md) §7 sau khi ADR 011 được chấp nhận (2026-10-01). Đổi thứ tự hay điều kiện xong → PR sửa file này, chủ repo duyệt.

Nguyên tắc: **strangler theo luồng**. v0 (`apps/web`) vẫn chạy để demo cho tới khi v2 đạt tương đương luồng A và B ([`MUC-TIEU.md`](MUC-TIEU.md) §3). Mỗi pha xong khi điều kiện đo được ở cột cuối đạt, có số đo thật trong PR.

| Pha | Kết quả | Năng lực | Điều kiện xong |
| --- | --- | --- | --- |
| **P0** — xong 2026-10-01 | Hiến chương, quy trình, 5 lab, harness Claude Code, ADR 011; nhãn GitHub; Spec Kit; ruleset `main` | — | [#52](https://github.com/meiiie/du_an_em_linh/issues/52), [#53](https://github.com/meiiie/du_an_em_linh/issues/53) merge; ruleset `main-protection` active |
| **P1** — đang làm (còn staging) | Khung chạy được: `services/core` (identity port từ LMS, Flyway, PostgreSQL 18), `apps/frontend` (đăng nhập, khung trang), Compose 3 dịch vụ, CI theo đường dẫn, staging | Nền tảng | GV và HS đăng nhập end-to-end trên staging ([#75](https://github.com/meiiie/du_an_em_linh/issues/75)); CI xanh cả 8 check bắt buộc |
| **P2** — lát cắt dọc | Hết vòng của sơ đồ trên v2 cho **một chủ đề** đã kiểm định ở v0 (Toán 12, đơn điệu và cực trị): GV khóa bảng công thức, nạp tài liệu → ngân hàng bài 4 mức của v0 → HS làm theo bước, máy chấm từng bước → gia sư không đưa đáp án, **lời giảng qua cổng 3 tầng** (cả tầng 2 và 3, v0 chưa có) → mức hiểu → bài kế (chữa lỗi / củng cố / nâng 1 nấc) → thời gian biểu tuần trong app; GV duyệt câu `KHONG_KIEM_DUOC`, xem tiến độ lớp | C1–C10, mức tối thiểu cho một chủ đề | e2e «một vòng» (GV khóa bảng, nạp một tài liệu → HS làm bài, hỏi gia sư, gia sư trích dẫn đoạn của tài liệu vừa nạp → mức hiểu tăng → bài nâng 1 nấc → lịch tuần) xanh ở 390 + 1280 px; e2e «tới VDC»: một học sinh đi hết Nhận biết → Thông hiểu → Vận dụng → Vận dụng cao của một kỹ năng qua các bài được đề xuất, rồi được báo hoàn thành kỹ năng; e2e duyệt: GV duyệt một mục `KHONG_KIEM_DUOC` → `GV_DUYET` có ghi ai, lúc nào, vì sao, rồi mục đó mới tới học sinh; mục `SAI` không bao giờ tới học sinh; e2e `luong-hoc-sinh`, `gia-su-harness` của v0 có bản tương đương trên v2; bộ AI 70 ca đạt; 0 lộ trong bộ dụ đáp án; 0 công thức sai lọt cổng trong bộ ca lời giảng |
| **P3** — mở rộng phía giáo viên | Luồng A + E đầy đủ: nạp tài liệu có OCR công thức; AI soạn bài 4 mức → cổng 3 tầng → GV duyệt (thay «sinh bài» của v0); ngân hàng 4 dạng câu (nhiều lựa chọn, đúng/sai nhiều ý, trả lời ngắn, tự luận theo bước); bộ ôn tập theo hai khuôn: đề định kỳ (CV 7991) và đề TN THPT môn Toán; bảng lớp kỹ năng × mức; giao bộ bài | C1–C5 | e2e phát hành, duyệt, giao bài xanh; đề định kỳ đúng tỉ lệ CV 7991 (40 % Biết, 30 % Hiểu, 30 % Vận dụng); đề ôn TN THPT đúng cấu trúc 3 phần, 22 câu (`docs/product/MUC-TIEU.md` §4) |
| **P4** — cá nhân hóa xuyên chủ đề | Đồ thị kỹ năng nhiều chủ đề; chọn «dạng đã sai» theo mã lỗi; hiệu chỉnh độ khó bài; tư vấn phương pháp học có căn cứ; nhắc lịch qua kênh ngoài (web push, Zalo) | C6, C8–C10 | Bộ đo cá nhân hóa của lab Kiểm định đạt; nhắc lịch tới được thiết bị thật |
| **P5** | Mở rộng nội dung Toán 12 theo lab Sư phạm; OCR đề mẫu; bộ ôn tập định dạng thi TN THPT | C1, C3, C4 | Theo từng gói chủ đề (`labs/pedagogy/README.md`) |
| Gỡ v0 | Xóa `apps/web`, thay ADR 001 | — | v2 làm được mọi màn của v0: P2 và phần soạn bài bằng AI của P3 (v0: `/gv/sinh-bai`); chủ repo đồng ý |

**Đổi thứ tự 2026-10-02 (chủ repo duyệt):** P2 thành lát cắt dọc thay vì chỉ luồng học sinh. Sơ đồ là một vòng qua hai phía: tài liệu của giáo viên đi qua cổng 3 tầng vào phần học của học sinh, kết quả học quay lại thành bài nâng 1 nấc. Chạy hết vòng cho một chủ đề trước thì mọi ranh giới giữa các khối được kiểm sớm, và cổng 3 tầng cho lời gia sư có tài liệu, bảng công thức thật để kiểm. Sau đó mới mở rộng ngang (P3–P5).

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
