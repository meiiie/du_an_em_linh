# Stack và hệ sinh thái — trạng thái tại 2026-10-01

Câu hỏi: phiên bản nào là hiện hành và đủ ổn định cho một dự án bắt đầu tháng 10/2026? Phục vụ ADR 011.

## Kết luận

| Thành phần | Chọn | Căn cứ | Độ tin |
| --- | --- | --- | --- |
| Backend | **Spring Boot 4.1.x** (4.1.0 ngày 10/06/2026; 4.1.1 ngày 21/08/2026) | Spring Boot 4.0 phát hành 20/11/2025; 4.1 là mục tiêu khuyến nghị cho dự án mới; nhánh 3.x đã có bản vá cuối 3.5.16 (25/06/2026) | Cao |
| AI trên JVM | **Spring AI 2.0.x** (GA 12/06/2026, yêu cầu Boot 4.x, Spring Framework 7) | Blog Spring chính thức; 2.1.0-M1 ra 25/09/2026 — chưa dùng milestone | Cao |
| Java | **Java 25 LTS** | LTS sau 21; Spring Boot 4.1 hỗ trợ Java 17–26 | Cao |
| Frontend | **Angular 22** (03/06/2026) | Signal Forms, zoneless, `resource()`/`httpResource` ổn định; OnPush mặc định cho component mới; `@angular/aria` ổn định; Vitest | Cao |
| CSDL | **PostgreSQL 18** (22/09/2025) + **pgvector 0.8.x** | pgvector 0.8 cải thiện truy vấn có lọc và HNSW | Cao |
| Dịch vụ toán | Giữ **Python 3.12+ / SymPy 1.14** của v0 | Đã kiểm định; không có lý do nâng ngay | Cao |
| Node cho công cụ FE | Node LTS mà Angular 22 hỗ trợ (kiểm lại lúc dựng khung) | Máy dựng đang có Node 25 (lẻ, không LTS) — Angular CLI cảnh báo | Trung bình |

## Hệ quả cho repo LMS khi tái sử dụng

LMS ghim Spring Boot 3.2.6 và Angular 20.3. Module chép sang phải nâng trong lúc chép:

- Boot 3 → 4: Spring Framework 7, Jakarta EE 11, Hibernate 7. Kiểm API bảo mật và cấu hình `spring.jpa.*`. LMS đã gặp lỗi thư viện không đọc thuộc tính Spring (hypersistence-utils, `docs/LESSONS_LEARNED_2026-04-27.md`) — kiểm lại khi nâng.
- Angular 20 → 22: bỏ `CommonModule` thừa, dùng Signal Forms thay Reactive Forms cho form mới, dùng `@angular/aria` cho widget truy cập được.

## Nguồn (truy cập 2026-10-01)

- Spring AI 2.0.0 GA: https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/
- Spring Boot 4.0.8 / 4.1.1, Spring AI 2.0.1: https://javarubberduck.com/java/news-2026-08-22-spring/
- Vòng đời Spring Boot: https://versionlog.com/spring-boot/ · https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026
- Angular 22: https://angular.love/angular-22-key-features-and-changes · https://blog.codewithahsan.dev/whats-new-angular-v22/
- Java 25 LTS: https://codefarm.in/blog/java/java-25-lts-what-matters
- pgvector 0.8.0: https://www.postgresql.org/about/news/pgvector-080-released-2952/
- PostgreSQL vòng đời: https://versionlog.com/postgresql/
