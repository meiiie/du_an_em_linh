---
name: spring-core
description: Chuẩn backend v2 trong services/core — Spring Boot 4.1, Java 25, DDD/Clean Architecture có ArchUnit, Flyway, Testcontainers, Spring AI 2.0 cho gia sư, gọi dịch vụ toán; cách chép module từ LMS và nâng từ Boot 3. Dùng khi tạo hoặc sửa mã trong services/core, port module LMS, hoặc hiện thực luồng gia sư phía máy chủ.
paths:
  - "services/core/**"
---

# services/core — Spring Boot 4.1 · Java 25

Quyết định: ADR 011. Ràng buộc ngắn: `.claude/rules/spring-core.md`. Gói gốc `vn.hoctapcanman.core`; lệnh `cd services/core && ./mvnw verify` (Maven Wrapper 3.9.16 — LMS gọi `mvn` cài sẵn, wrapper giữ máy dev và CI cùng phiên bản). Lệnh và gotcha: `services/core/AGENTS.md`.

## Kiến trúc (giữ chuẩn LMS — Clean Architecture + DDD)

```text
<module>/
  domain/          model (POJO thuần, không JPA), repository (port), valueobject, event, service
  application/     usecase (một hành vi một lớp), dto (record), port (dịch vụ ngoài: toán, LLM, thông báo)
  infrastructure/  persistence/{entity (*JpaEntity), mapper, *Adapter}, web (controller), client (HTTP ra ngoài)
```

Module dự kiến: `identity`, `school` (trường, lớp, ghi danh), `content` (tài liệu, bảng công thức, ngân hàng bài), `assessment` (bài làm, chấm qua dịch vụ toán), `learning` (mức hiểu, chọn bài, lịch), `tutor`, `notification`, `shared`.

Luật tầng — ArchUnit (`CleanArchitectureTest`, `DddArchitectureTest` chép từ LMS) chặn khi vi phạm:

- `domain` không phụ thuộc `application`, `infrastructure`, Spring, JPA.
- `JpaRepository<XJpaEntity, UUID>` — **không bao giờ** `JpaRepository<DomainModel, …>` (lỗi khởi động «Not a managed type» của LMS).
- Controller nhận và trả `record` DTO, không trả domain hay entity.
- Transaction ở use case. Kiểm quyền theo **lớp** trong use case (chống IDOR), không chỉ `@PreAuthorize` theo vai trò.

## Spring Boot 4 — khác Boot 3 khi chép từ LMS

| Boot 3 (LMS) | Boot 4.1 |
| --- | --- |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `flyway-core` trực tiếp | `spring-boot-starter-flyway` |
| Jackson 2 `com.fasterxml.jackson` | Jackson 3 `tools.jackson`; `@JacksonComponent` thay `@JsonComponent` |
| `@MockBean`, `@SpyBean` | `@MockitoBean`, `@MockitoSpyBean` |
| `@SpringBootTest` có sẵn MockMvc | Thêm `@AutoConfigureMockMvc` |
| `org.springframework.lang.Nullable` | JSpecify (`@NullMarked` ở `package-info.java`, `@Nullable`) |

Khi chép cấu hình: thêm tạm `spring-boot-properties-migrator`, sửa hết cảnh báo, rồi gỡ. Thư viện Hibernate bên thứ ba (hypersistence-utils…) có thể không đọc `spring.jpa.properties.*` — kiểm bằng log khởi động (bài học LMS 2026-04-27).

## Java 25

`record` cho DTO, command, kết quả; `sealed interface` cho sự kiện miền và kết quả chấm; `switch` theo mẫu; luồng ảo (`spring.threads.virtual.enabled=true`) cho việc chờ I/O như gọi dịch vụ toán, gọi LLM.

## Gọi dịch vụ toán (`services/math`)

- Port `MathEnginePort` ở `application/port`; adapter HTTP ở `infrastructure/client` gọi `/v1/grade`, `/v1/verify`, `/v1/filter`, `/v1/generate`, `/v1/solve`, `/v1/goi-y`, `/v1/extract`.
- Timeout ngắn. Lỗi hay hết giờ → `KHONG_KIEM_DUOC` hoặc «không hiển thị», **không bao giờ** coi là đạt (hiến chương II, fail-closed).
- Không tự phán đúng/sai bằng Java khi đã có endpoint toán.
- Test hợp đồng từ JSON mẫu theo schema Pydantic của `services/math/app/schemas.py`.

## Gia sư với Spring AI 2.0 (ADR 011, quyết định con C-1)

- Dùng `ChatClient`. Hàm dựng prompt là hàm thuần nhận một `record` chỉ có: đề, bước sai, loại kết quả, mã lỗi (đủ tin cậy), gợi ý đã kiểm của bước đó. Kiểu dữ liệu **không có trường** cho lời giải hay đáp án (ADR 003 thành ràng buộc biên dịch).
- Một nhà chính được chọn tường minh; không fallback hay retry tự động sang nhà khác (ADR 007). Lỗi nhà → lỗi rõ cho học sinh.
- Câu trả lời → `/v1/filter` → chỉ hiện khi qua; bộ lọc lỗi thì không hiện.
- Không đăng ký tool cho gia sư nếu chưa có ADR. Structured output chỉ cho việc phân loại nội bộ.
- Quan sát qua Micrometer; log không chứa prompt đầy đủ hay định danh học sinh. Khóa nhà từ biến môi trường hoặc kho bí mật, không trả về client.
- SSE trạng thái `kho` → `goi` → `loc` → `xong`, không stream token (ADR 010).

## Dữ liệu

- PostgreSQL 18; Flyway `V<n>__<viec>.sql`, chỉ thêm. Schema do Flyway quản; `ddl-auto: none` ở production.
- Bảng dữ liệu học sinh: kiểm quyền theo lớp ở use case và RLS nơi được (ADR 006).
- pgvector chỉ khi có ADR cho tầng 2 tìm theo nghĩa.

## Test

| Tầng | Cách |
| --- | --- |
| Domain | JUnit 5 thuần, không mock |
| Use case | Mock port (Mockito) |
| Persistence | Testcontainers PostgreSQL — **không** mock `JpaRepository` (bài học LMS: mock che lỗi migration) |
| Web | `@WebMvcTest` hoặc `@SpringBootTest` + `@AutoConfigureMockMvc` |
| Kiến trúc | ArchUnit luôn xanh |

## Chép từ LMS

Nguồn: `LMS_hohulili/backend/src/main/java/com/example/lms/<module>` (workspace cạnh repo, chỉ đọc). Ghi `LMS_hohulili@<sha>:<đường dẫn>` trong PR. Thứ tự: `identity` → `shared` → `config` (security, CORS, rate limit) → `assessment` (QuestionBank, GradingStrategy; thêm đúng/sai nhiều ý, trả lời ngắn dạng số) → `communication` (thông báo). Không chép: `competency_mapping`, `academic` (VMU), payment, video.
