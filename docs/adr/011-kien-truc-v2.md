# ADR 011 — Kiến trúc v2: Angular + Spring Boot, giữ dịch vụ toán Python

**Trạng thái:** Chấp nhận (2026-10-01) — chủ repo duyệt phương án C trong phiên thiết lập quy trình; ghi nhận chính thức khi PR thiết lập harness được merge.

## Bối cảnh

v0 là nguyên mẫu NCKH một chủ đề (Next.js + FastAPI/SymPy). Sản phẩm theo hợp đồng cần nền tảng đa vai trò, nội dung toàn chương trình, kho tài liệu có OCR, định dạng đề thi và kênh nhắc lịch ([`docs/product/MUC-TIEU.md`](../product/MUC-TIEU.md)). Stack chủ lực của đội là Angular + Spring Boot (LMS_hohulili). Phân tích đầy đủ, chấm điểm 4 phương án: [`labs/decisions/2026-10-01-kien-truc-v2.md`](../../labs/decisions/2026-10-01-kien-truc-v2.md).

## Quyết định

- `apps/frontend`: Angular 22. `services/core`: Spring Boot 4.1, Java 25, Spring AI 2.0, DDD/Clean Architecture có ArchUnit. PostgreSQL 18 + Flyway.
- `services/math` (FastAPI + SymPy) giữ nguyên làm dịch vụ không trạng thái. Hợp đồng `/v1/*` không đổi ở pha đầu.
- Điều phối gia sư nằm trong `services/core`. Bộ lọc lộ đáp án chỉ có một bản, trong `services/math`.
- Tiến hóa tại chỗ trong repo này. `apps/web` (v0) đóng băng, chỉ sửa lỗi; gỡ khi v2 đạt tương đương luồng A và B.
- Tái sử dụng LMS bằng cách chép module có chọn lọc, nâng phiên bản khi chép, ghi nguồn `LMS_hohulili@<sha>:<path>`.

## Hệ quả

- ADR 002–006 giữ hiệu lực. ADR 001 (monorepo Next.js + FastAPI) bị thay thế khi gỡ `apps/web`. ADR 007, 009, 010 được viết lại cho `services/core` khi port gia sư.
- CI tách job theo đường dẫn cho ba dịch vụ. Vận hành bằng Docker Compose.
- Khóa nhà cung cấp LLM do máy chủ quản lý; ADR quyền riêng tư (dữ liệu trẻ vị thành niên, chuyển dữ liệu ra nước ngoài) bắt buộc trước khi pilot với học sinh thật.
