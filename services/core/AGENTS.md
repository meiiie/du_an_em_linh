# services/core

Dịch vụ nghiệp vụ v2 (ADR 011): Spring Boot 4.1, Java 25, Maven Wrapper. Gói gốc `vn.hoctoanai.core`. Chuẩn đầy đủ: skill `spring-core` (`.claude/skills/spring-core/SKILL.md`).

## Lệnh (từ `services/core`)

```bash
./mvnw verify                 # build + test (gồm ArchUnit)
./mvnw spring-boot:run        # chạy ở :8080; GET /actuator/health
docker build -t hoc-toan-core .
```

Windows: `mvnw.cmd verify`. Cần JDK 25 (`JAVA_HOME`); wrapper tự tải Maven 3.9.16.

## Bản đồ

| Đường dẫn | Việc |
| --- | --- |
| `src/main/java/vn/hoctoanai/core/CoreApplication.java` | Điểm vào |
| `src/main/java/vn/hoctoanai/core/<module>/` | Module nghiệp vụ: `domain` → `application` → `infrastructure` |
| `src/main/resources/application.yaml` | Cấu hình; luồng ảo bật; chỉ mở `health` (+ liveness / readiness) |
| `src/test/java/.../architecture/` | ArchUnit: luật ở `KienTrucRules`; `CleanArchitectureTest`, `DddArchitectureTest` (gốc LMS) chạy luật trên mã thật; `KienTrucRulesTuKiemTest` chạy luật trên lớp mẫu `vn.hoctoanai.mau`; `NullMarkedPackagesTest` |
| `Dockerfile` | Nhiều tầng, jar tách lớp, chạy UID 1001 |

## Gotcha

- Mỗi gói mới cần `package-info.java` có `@NullMarked`: JSpecify không lan sang gói con, `NullMarkedPackagesTest` chặn gói thiếu.
- Thêm luật kiến trúc → thêm lớp vi phạm mẫu ở `src/test/java/vn/hoctoanai/mau/xau/` và đăng ký trong `VI_PHAM` của `KienTrucRulesTuKiemTest`; test `moiLuatDeuCoMau` chặn nếu quên. Module mẫu đúng ở `mau/tot/` phải qua mọi luật.
- `archunit.properties` đang để `failOnEmptyShould=false` vì khung chưa có module; bật lại khi module đầu tiên đủ ba tầng ([#55](https://github.com/meiiie/du_an_em_linh/issues/55)).
- Spring Boot 4: test starter tách theo công nghệ (`spring-boot-starter-webmvc-test`, …); `@AutoConfigureMockMvc` ở `org.springframework.boot.webmvc.test.autoconfigure`; dùng `MockMvcTester`.
