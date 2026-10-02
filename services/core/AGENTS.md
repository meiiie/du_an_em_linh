# services/core

Dịch vụ nghiệp vụ v2 (ADR 011): Spring Boot 4.1, Java 25, Maven Wrapper. Gói gốc `vn.hoctoanai.core`. Chuẩn đầy đủ: skill `spring-core` (`.claude/skills/spring-core/SKILL.md`).

## Lệnh (từ `services/core`)

```bash
./mvnw verify                 # build + test (ArchUnit; test CSDL cần Docker cho Testcontainers)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # :8080, PostgreSQL cục bộ hoc_toan_core, tài khoản thử
docker build -t hoc-toan-core .
```

Windows: `mvnw.cmd verify`. Cần JDK 25 (`JAVA_HOME`); wrapper tự tải Maven 3.9.16. Thiếu Docker thì các test CSDL tự bỏ qua (`disabledWithoutDocker`); CI luôn chạy.

## Bản đồ

| Đường dẫn | Việc |
| --- | --- |
| `src/main/java/vn/hoctoanai/core/CoreApplication.java` | Điểm vào |
| `src/main/java/vn/hoctoanai/core/<module>/` | Module nghiệp vụ: `domain` → `application` → `infrastructure` |
| `.../identity/` | Đăng nhập (#55): `/api/auth/login`, `/refresh`, `/logout`, `/api/me`; access token JWT HS256 15 phút, refresh token ngẫu nhiên lưu băm, xoay vòng, thu hồi khi đăng xuất |
| `.../shared/infrastructure/` | Dùng chung: `Clock` (UTC) |
| `src/main/resources/db/migration/` | Flyway, chỉ thêm: `V1__identity.sql` (`users`, `auth_sessions`, `refresh_tokens`) |
| `src/main/resources/application-dev.yaml` | Profile `dev`: CSDL cục bộ; `TaiKhoanThuSeeder` tạo 4 tài khoản tổng hợp |
| `src/main/resources/application.yaml` | Cấu hình; luồng ảo; JPA `validate` theo Flyway; problem+json; chỉ mở `health` (+ liveness / readiness); `app.identity.*` |
| `src/test/java/.../architecture/` | ArchUnit: luật ở `KienTrucRules`; `CleanArchitectureTest`, `DddArchitectureTest` (gốc LMS) chạy luật trên mã thật; `KienTrucRulesTuKiemTest` chạy luật trên lớp mẫu `vn.hoctoanai.mau`; `NullMarkedPackagesTest` |
| `Dockerfile` | Nhiều tầng, jar tách lớp, chạy UID 1001 |

## Gotcha

- Mỗi gói mới cần `package-info.java` có `@NullMarked`: JSpecify không lan sang gói con, `NullMarkedPackagesTest` chặn gói thiếu.
- Thêm luật kiến trúc → thêm lớp vi phạm mẫu ở `src/test/java/vn/hoctoanai/mau/xau/` và đăng ký trong `VI_PHAM` của `KienTrucRulesTuKiemTest`; test `moiLuatDeuCoMau` chặn nếu quên. Module mẫu đúng ở `mau/tot/` phải qua mọi luật.
- `archunit.properties`: `failOnEmptyShould=true` (từ #55): luật không khớp lớp nào là gõ sai gói, phải đỏ.
- Test CSDL dùng `TestcontainersConfiguration` (PostgreSQL 18, `@ServiceConnection`); không mock `JpaRepository`.
- Không ghi email, mật khẩu, token vào log: `toString()` của `User`, `Email`, DTO đăng nhập đã che.
- Khóa JWT: `APP_IDENTITY_JWT_SECRET` (base64 ≥ 32 byte); trống thì dùng khóa tạm, token mất hiệu lực khi khởi động lại.
- Spring Boot 4: test starter tách theo công nghệ (`spring-boot-starter-webmvc-test`, …); `@AutoConfigureMockMvc` ở `org.springframework.boot.webmvc.test.autoconfigure`; dùng `MockMvcTester`.
