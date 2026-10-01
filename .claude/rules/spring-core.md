---
paths:
  - "services/core/**"
---

# services/core (v2 — Spring Boot 4.1, Java 25)

- Tầng: `domain` → `application` → `infrastructure`; tầng trong không import tầng ngoài. ArchUnit phải xanh.
- `JpaRepository<XJpaEntity, UUID>` — không bao giờ dùng domain model làm entity.
- Controller nhận / trả `record` DTO. Kiểm quyền theo lớp trong use case, không chỉ theo vai trò.
- Đúng / sai toán do `services/math` quyết định; lỗi hoặc hết giờ khi gọi → không coi là đạt (fail-closed).
- Gia sư: prompt chỉ từ `record` không có trường lời giải; không fallback / retry sang nhà khác; lọc trước khi hiện; không tool nếu chưa có ADR.
- Migration Flyway chỉ thêm. Test persistence bằng Testcontainers PostgreSQL, không mock `JpaRepository`.
- Spring Boot 4: `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, Jackson 3 (`tools.jackson`), `@MockitoBean`. Chi tiết: skill `spring-core`.
