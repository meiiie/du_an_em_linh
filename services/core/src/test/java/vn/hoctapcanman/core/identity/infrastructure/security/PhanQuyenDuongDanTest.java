package vn.hoctapcanman.core.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.identity.application.port.AccessTokenIssuer;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;

/**
 * Vai trò tài khoản theo đường dẫn (#111): cổng {@code ClassMembership} chỉ xét vai trò trong lớp, nên tầng web chặn
 * {@code /api/gv/**} và {@code /api/hs/**} theo {@code users.role}. Đường dẫn ở đây chưa có handler: đúng vai trò thì
 * qua lớp quyền và nhận 404, sai vai trò thì 403 trước khi tới controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PhanQuyenDuongDanTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AccessTokenIssuer tokens;

    private String bearer(Role role) {
        Instant now = Instant.now();
        String email = "thu." + role.name().toLowerCase(Locale.ROOT).replace('_', '-') + "@demo.local";
        User user = User.create(new Email(email), "khong-dung", "Tài khoản thử", role, true, now);
        return "Bearer " + tokens.issue(user, now).value();
    }

    @Test
    void hocSinhKhongVaoDuocApiGiaoVien() {
        assertThat(mvc.get().uri("/api/gv/lop").header("Authorization", bearer(Role.STUDENT))).hasStatus(403);
    }

    @Test
    void giaoVienVaQuanTriKhongVaoDuocApiHocSinh() {
        for (Role role : new Role[] {Role.TEACHER, Role.SCHOOL_ADMIN, Role.ADMIN}) {
            assertThat(mvc.get().uri("/api/hs/lich").header("Authorization", bearer(role))).as(role.name()).hasStatus(403);
        }
    }

    @Test
    void dungVaiTroThiQuaLopQuyen() {
        assertThat(mvc.get().uri("/api/hs/lich").header("Authorization", bearer(Role.STUDENT))).hasStatus(404);
        for (Role role : new Role[] {Role.TEACHER, Role.SCHOOL_ADMIN, Role.ADMIN}) {
            assertThat(mvc.get().uri("/api/gv/lop").header("Authorization", bearer(role))).as(role.name()).hasStatus(404);
        }
    }

    @Test
    void chuaDangNhapThi401() {
        assertThat(mvc.get().uri("/api/gv/lop")).hasStatus(401);
        assertThat(mvc.get().uri("/api/hs/lich")).hasStatus(401);
    }
}
