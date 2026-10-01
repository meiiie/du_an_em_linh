# Hiến chương kỹ thuật — Học toán với AI

> **Phiên bản 1.0.0** · Đề xuất 2026-10-01 · Phê chuẩn: chờ chủ repo.
> Hiến chương đứng trên mọi quy ước khác. Thứ bậc: Hiến chương > ADR đã chấp nhận > `AGENTS.md` / `CLAUDE.md` > rule, skill > ý kiến review. Xung đột thì bậc trên thắng.

Mỗi nguyên tắc nêu **luật**, **lý do**, **cách kiểm**. Nguyên tắc I–III không thương lượng: reviewer được quyền chặn merge.

## I. Không đưa đáp án — không thương lượng

- Khi học sinh đang làm: không nêu kết quả cuối, không viết lời giải trọn, không làm hộ một bước. Chỉ được: chỉ ra bước sai và lý do (lấy từ bộ chấm), hỏi gợi mở, gợi ý theo thang không bottom-out.
- Gia sư không đọc lời giải chuẩn hay `protected_facts` (ADR 003).
- Mọi câu gia sư gửi học sinh đi qua bộ lọc lộ đáp án xác định bằng CAS, kiểu fail-closed: bộ lọc lỗi thì không gửi.
- Lời giải chỉ mở sau khi nộp đủ và lớp bật cờ (mặc định tắt).

Lý do: thiếu rào chắn thì điểm thi giảm 17 % khi bỏ AI (Bastani et al., *PNAS* 2025). Cách kiểm: bộ ca lộ đáp án và bộ AI của lab Đánh giá đạt 100 %.

## II. Đúng toán trước hết — không thương lượng

- Mọi nội dung toán tới học sinh (đề, đáp án, lời giải, gợi ý, công thức trong lời gia sư) qua cổng 3 tầng: (1) CAS, (2) trích dẫn từ tài liệu đã nạp, (3) bảng công thức đã khóa. `SAI` thì chặn. `KHONG_KIEM_DUOC` thì giáo viên duyệt, hệ thống ghi ai, lúc nào, vì sao (ADR 005).
- Đúng hay sai do bộ chấm CAS quyết định, không do LLM phán.
- Chuỗi người dùng không được eval trong tiến trình API. CAS chạy trong tiến trình con có hạn giờ (ADR 002).
- Đổi nội dung thì đổi hash và phải kiểm lại. Đổi bảng công thức thì kết quả cũ thành `stale`.

Lý do: gia sư toán bịa công thức là lỗi gây hại trực tiếp và khó phát hiện. Cách kiểm: `pnpm test:math`; bộ ca ác ý 0 đạt nhầm.

## III. Người học là trẻ vị thành niên — không thương lượng

- Không có dữ liệu học sinh thật trong repo, log, ảnh chụp, issue, prompt mẫu. Dữ liệu demo đánh dấu tổng hợp (ADR 006).
- Trước khi pilot với học sinh thật: có luồng đồng ý của người đại diện theo pháp luật (Luật 91/2025/QH15), có ADR quyền riêng tư được duyệt.
- Xóa định danh trước khi gửi bất kỳ nhà cung cấp AI nào. Chỉ dùng gói và điều khoản không cho nhà cung cấp dùng dữ liệu để huấn luyện.
- Học sinh luôn biết đang học cùng AI. Giáo viên xem và ghi đè được mọi phân loại mức, mọi gợi ý bài (Luật 134/2025/QH15).
- Khóa và bí mật chỉ nằm trong biến môi trường hoặc kho bí mật. Không bao giờ trong mã, log, trình duyệt.

Cách kiểm: `pnpm test:khoa`; hook chặn đọc / ghi file bí mật; reviewer kiểm luồng dữ liệu ra ngoài.

## IV. Bằng chứng thay vì khẳng định

- Số đo ghi kèm lệnh, SHA, ngày. Không viết «đã test» chung chung (`docs/KIEM-THU.md`).
- Đổi prompt, nhà cung cấp, mô hình hay bộ lọc thì chạy bộ eval của lab Đánh giá. Kết quả dán vào PR.
- Quyết định kiến trúc hoặc sư phạm có ADR. Nguồn ghi ngày truy cập, ưu tiên nguồn gốc (văn bản luật, bài báo, tài liệu chính thức).

## V. Tiếng Việt đúng lứa tuổi, mỗi chữ một việc

- Giao diện tiếng Việt có dấu. Giọng lớp 10–12 với học sinh, giọng phòng giáo viên với giáo viên. Thuật ngữ toán theo SGK CT GDPT 2018.
- Không đưa thuật ngữ kỹ thuật, LMS, NCKH lên mặt người dùng. Không câu giải thích UI thừa (`docs/DESIGN.md`).
- Thông báo lỗi nói việc cần làm tiếp.

## VI. Ranh giới kiến trúc rõ

- Mỗi dịch vụ một trách nhiệm (ADR 011): `apps/frontend` không chứa logic nghiệp vụ, không tính đúng / sai; `services/core` giữ trạng thái, quyền, kiểm toán, điều phối gia sư; `services/math` thuần hàm, không trạng thái.
- `services/core` theo Clean Architecture + DDD. ArchUnit là cổng chặn merge. Test persistence chạy trên PostgreSQL thật (Testcontainers), không mock `JpaRepository`.
- `apps/frontend` theo Angular signal-first (signals, Signal Forms, `@angular/aria`), không dùng mẫu cũ (`zone.js`, `@Input`, `*ngIf`).
- Hợp đồng giữa các dịch vụ có schema và test hợp đồng. Lỗi gọi dịch vụ toán không bao giờ được hiểu là «đạt».
- Migration CSDL chỉ thêm, không sửa migration đã gộp.
- Chi tiết theo stack: skill `spring-core`, `angular-frontend`, `math-engine`; rule tương ứng trong `.claude/rules/`.

## VII. Thay đổi nhỏ, đúng phạm vi, người duyệt cuối

- Mỗi PR một ý, gắn issue, diff tối thiểu, khớp phong cách sẵn có, không trừu tượng hóa suy đoán.
- Agent không tự merge, không đẩy thẳng `main`, không `--force`, không `--no-verify`. Hook trong `.claude/` chặn các thao tác này.
- Giữ `data-testid` và heading e2e đã khóa, trừ khi issue yêu cầu đổi.

## VIII. Truy cập được, điện thoại trước

- WCAG 2.2 AA. Mục tiêu chạm ≥ 44 px với con trỏ thô. Tôn trọng `prefers-reduced-motion`. Dùng `aria-live` khi chấm.
- Mọi thay đổi giao diện được kiểm ở 390 px và 1280 px.

## Cổng chất lượng

| Cổng | Lệnh / cách kiểm | Nguyên tắc |
| --- | --- | --- |
| Dịch vụ toán | `pnpm test:math` | II |
| Bộ kiểm định (ác ý, 5 bước, AI, thang gợi ý qua bộ lọc) | Theo `services/math/kiemdinh/` khi đụng CAS, cổng, bộ lọc | I, II |
| Web v0 | `pnpm test:web`; e2e khi đụng UI | VII, VIII |
| Eval gia sư | Bộ ca lab Đánh giá khi đụng prompt, nhà, bộ lọc | I, IV |
| Quét khóa | `pnpm test:khoa` | III |
| v2 `services/core` | Maven test gồm ArchUnit + Testcontainers (lệnh chốt khi dựng khung) | VI |
| v2 `apps/frontend` | Build + Vitest (lệnh chốt khi dựng khung) | VI, VIII |
| Harness | `node --test .claude/hooks/*.test.mjs` | VII |
| Review | CodeRabbit + chủ repo duyệt | VII |

## Sửa hiến chương

1. PR chỉ sửa file này, nêu lý do và tác động lên ADR, rule, skill.
2. Chủ repo duyệt. Phiên bản theo SemVer: MAJOR khi bỏ hoặc định nghĩa lại nguyên tắc; MINOR khi thêm nguyên tắc hoặc mở rộng đáng kể; PATCH khi sửa câu chữ.
3. Rà mỗi quý hoặc sau một bản mô hình lớn. Thực tế lệch văn bản thì sửa một trong hai, không để lệch.
