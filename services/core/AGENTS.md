# services/core

Dịch vụ nghiệp vụ v2 (ADR 011): Spring Boot 4.1, Java 25, Maven Wrapper. Gói gốc `vn.hoctapcanman.core` (đổi từ `vn.hoctoanai` ngày 2026-10-02: sản phẩm là nền tảng học tập, không chỉ môn toán). Chuẩn đầy đủ: skill `spring-core` (`.claude/skills/spring-core/SKILL.md`).

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
| `src/main/java/vn/hoctapcanman/core/CoreApplication.java` | Điểm vào |
| `src/main/java/vn/hoctapcanman/core/<module>/` | Module nghiệp vụ: `domain` → `application` → `infrastructure` |
| `.../identity/` | Đăng nhập (#55): `/api/auth/login`, `/refresh`, `/logout`, `/api/me`; access token JWT HS256 15 phút trong thân phản hồi; refresh token ngẫu nhiên lưu băm, xoay vòng trong phiên, thu hồi khi đăng xuất, chỉ đi trong cookie `hta_refresh` (HttpOnly, SameSite=Strict, Path=/api/auth); `/refresh` và `/logout` đòi header `X-Requested-With` (#57); sai mật khẩu 5 lần / 15 phút theo email + IP thì 429 (F-10, #69) |
| `.../classroom/` | Lớp học (#81): lớp, ghi danh (học sinh một lớp, giáo viên nhiều lớp), cài lớp, cảnh báo cho giáo viên. Cổng cho module khác ở `application/port`: `ClassMembership` (quyền theo lớp, F-08) và `CanhBaoGiaoVien` (`KET` của mastery, `NHO_GV` của tutor; không ghi trùng cảnh báo mở). Use case kiểm quyền theo lớp trước khi đọc / ghi; lớp khác hay học sinh → `KhongThuocLopException` (403), cảnh báo lớp khác → `CanhBaoKhongTimThayException` (404). Chưa có endpoint: API giáo viên ở T058 |
| `.../content/` | Nội dung chủ đề (#85, đang làm). `domain/model`: bài (`Problem`; lời giải ở `Solution` riêng, `toString` không in nội dung), thang gợi ý, trạng thái phát hành theo lớp (`ProblemRelease`), bảng công thức có phiên bản (`FormulaSheet`: khóa khi mọi dòng `DAT` tầng 1 và tầng 2 có trích dẫn, ADR 013), lượt kiểm 3 tầng gắn lớp và bảng (`VerificationRun`: trạng thái tổng đóng mặc định và phải khớp các tầng, cờ `stale`, điều kiện giáo viên duyệt của FR-005). `ProblemRelease.apply` chỉ nhận lượt còn mới. Chưa có persistence, importer, endpoint |
| `.../shared/infrastructure/` | Dùng chung: `Clock` (UTC); `math/`: `MathServiceClient` tới `services/math` (`app.math.base-url`, hết giờ theo `MathJob`), đóng mặc định: lỗi, hết giờ, JSON hỏng, phong bì lỗi của sandbox thành `MathResult.Failed`, không bao giờ đạt (#82). Module gọi qua port riêng ở `application/port`, adapter ở `infrastructure/client` |
| `src/main/resources/db/migration/` | Flyway, chỉ thêm: `V1__identity.sql` (`users`, `auth_sessions`, `refresh_tokens`), `V2__login_failures.sql` (lần đăng nhập sai, chỉ lưu băm email + IP), `V3__classroom.sql` (`classes`, `enrollments`, `class_settings`, `escalations`), `V4__content.sql` (18 bảng nội dung theo `specs/001-lat-cat-doc/data-model.md` §content; bất biến chéo bảng bằng khóa ngoại nhiều cột và trigger: bảng công thức đã khóa không đổi được, lưu bảng khóa theo thứ tự `NHAP` + dòng rồi `KHOA`) |
| `src/main/resources/application-dev.yaml` | Profile `dev`: CSDL cục bộ; `TaiKhoanThuSeeder` tạo 4 tài khoản tổng hợp, rồi `LopThuSeeder` tạo lớp «12A1 thử» (giáo viên thử, An, Bình, Chi) |
| `src/main/resources/application.yaml` | Cấu hình; luồng ảo; JPA `validate` theo Flyway; problem+json; chỉ mở `health` (+ liveness / readiness); `app.identity.*` |
| `src/test/java/.../architecture/` | ArchUnit: luật ở `KienTrucRules`; `CleanArchitectureTest`, `DddArchitectureTest` (gốc LMS) chạy luật trên mã thật; `KienTrucRulesTuKiemTest` chạy luật trên lớp mẫu `vn.hoctapcanman.mau`; `NullMarkedPackagesTest` |
| `Dockerfile` | Nhiều tầng, jar tách lớp, chạy UID 1001 |

## Gotcha

- Mỗi gói mới cần `package-info.java` có `@NullMarked`: JSpecify không lan sang gói con, `NullMarkedPackagesTest` chặn gói thiếu.
- Thêm luật kiến trúc → thêm lớp vi phạm mẫu ở `src/test/java/vn/hoctapcanman/mau/xau/` và đăng ký trong `VI_PHAM` của `KienTrucRulesTuKiemTest`; test `moiLuatDeuCoMau` chặn nếu quên. Module mẫu đúng ở `mau/tot/` phải qua mọi luật.
- `archunit.properties`: `failOnEmptyShould=true` (từ #55): luật không khớp lớp nào là gõ sai gói, phải đỏ.
- Test CSDL dùng `TestcontainersConfiguration` (PostgreSQL 18, `@ServiceConnection`); không mock `JpaRepository`.
- Không ghi email, mật khẩu, token vào log: `toString()` của `User`, `Email`, DTO đăng nhập đã che.
- Khóa JWT: `APP_IDENTITY_JWT_SECRET` (base64 ≥ 32 byte); trống thì dùng khóa tạm, token mất hiệu lực khi khởi động lại.
- IP máy khách: `server.forward-headers-strategy: native`, Tomcat chỉ tin `X-Forwarded-For` từ proxy nội bộ. Triển khai sau proxy khác dải mặc định thì đặt `server.tomcat.remoteip.internal-proxies`, nếu không mọi người dùng chung IP của proxy và khóa F-10 lan sang nhau.
- Spring Boot 4: test starter tách theo công nghệ (`spring-boot-starter-webmvc-test`, …); `@AutoConfigureMockMvc` ở `org.springframework.boot.webmvc.test.autoconfigure`; dùng `MockMvcTester`.
