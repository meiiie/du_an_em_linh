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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.identity.application.port.AccessTokenIssuer;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/**
 * Vai trò tài khoản theo đường dẫn (#111): cổng {@code ClassMembership} chỉ xét vai trò trong lớp, nên tầng web chặn
 * {@code /api/gv/**} và {@code /api/hs/**} theo {@code users.role}. Đường dẫn ở đây chưa có handler: đúng vai trò thì
 * qua lớp quyền và nhận 404, sai vai trò thì 403 trước khi tới controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PhanQuyenDuongDanTest {

    // Các route /lop và /lich đã có handler trong spec 002; dùng route không có để chỉ kiểm tầng quyền.
    private static final String API_GV = "/api/gv/kiem-quyen-khong-co";
    private static final String API_HS = "/api/hs/kiem-quyen-khong-co";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AccessTokenIssuer tokens;

    @Autowired
    private UserRepository users;

    @Autowired
    private vn.hoctapcanman.core.workspace.application.port.WorkspaceStore workspace;

    @Autowired
    private jakarta.persistence.EntityManager entities;

    @Test
    void datLaiMatKhauThuHoiJwtNgayVaTokenMoiVanDungDuoc() {
        String token = bearer(Role.STUDENT);
        User user = users.findByEmail(new Email("thu.student@demo.local")).orElseThrow();
        entities.flush();
        assertThat(workspace.resetPassword(user.id().value(), user.id().value(), "mat-khau-bam-moi", Instant.now())).isTrue();
        entities.clear();
        assertThat(mvc.get().uri(API_HS).header("Authorization", token)).hasStatus(401);
        User updated = users.findById(user.id()).orElseThrow();
        assertThat(mvc.get().uri(API_HS).header("Authorization", "Bearer " + tokens.issue(updated, Instant.now()).value())).hasStatus(404);
    }

    @Test
    void moLaiTaiKhoanKhongKhoiPhucJwtTruocKhiKhoa() {
        String token = bearer(Role.STUDENT);
        User user = users.findByEmail(new Email("thu.student@demo.local")).orElseThrow();
        entities.flush();
        workspace.bulk(user.id().value(), new vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Bulk(java.util.List.of(user.id().value()), false), Instant.now());
        workspace.bulk(user.id().value(), new vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Bulk(java.util.List.of(user.id().value()), true), Instant.now());
        entities.clear();
        assertThat(mvc.get().uri(API_HS).header("Authorization", token)).hasStatus(401);
    }

    private String bearer(Role role) {
        Instant now = Instant.now();
        String email = "thu." + role.name().toLowerCase(Locale.ROOT).replace('_', '-') + "@demo.local";
        User user = User.create(new Email(email), "khong-dung", "Tài khoản thử", role, true, now);
        users.save(user); // JWT phải thuộc tài khoản đang hoạt động (FR-114), rollback sau mỗi test.
        return "Bearer " + tokens.issue(user, now).value();
    }

    @Test
    void hocSinhKhongVaoDuocApiGiaoVien() {
        assertThat(mvc.get().uri(API_GV).header("Authorization", bearer(Role.STUDENT))).hasStatus(403);
    }

    @Test
    void giaoVienVaQuanTriKhongVaoDuocApiHocSinh() {
        for (Role role : new Role[] {Role.TEACHER, Role.SCHOOL_ADMIN, Role.ADMIN}) {
            assertThat(mvc.get().uri(API_HS).header("Authorization", bearer(role))).as(role.name()).hasStatus(403);
        }
    }

    @Test
    void dungVaiTroThiQuaLopQuyen() {
        assertThat(mvc.get().uri(API_HS).header("Authorization", bearer(Role.STUDENT))).hasStatus(404);
        for (Role role : new Role[] {Role.TEACHER, Role.SCHOOL_ADMIN, Role.ADMIN}) {
            assertThat(mvc.get().uri(API_GV).header("Authorization", bearer(role))).as(role.name()).hasStatus(404);
        }
    }

    @Test
    void chuaDangNhapThi401() {
        assertThat(mvc.get().uri(API_GV)).hasStatus(401);
        assertThat(mvc.get().uri(API_HS)).hasStatus(401);
    }

    @Test
    void tokenCuaTaiKhoanKhongTonTaiThi401() {
        Instant now = Instant.now();
        User user = User.create(new Email("khong-co@demo.local"), "khong-dung", "Tài khoản thử", Role.STUDENT, true, now);
        assertThat(mvc.get().uri(API_HS).header("Authorization", "Bearer " + tokens.issue(user, now).value())).hasStatus(401);
    }

    @Test
    void khoaTaiKhoanThiTokenDaPhatKhongConHopLe() {
        String token = bearer(Role.STUDENT);
        User user = users.findByEmail(new Email("thu.student@demo.local")).orElseThrow();
        users.save(new User(user.id(), user.email(), user.passwordHash(), user.displayName(), user.role(), false,
            user.synthetic(), user.createdAt(), Instant.now(), user.sessionEpoch()));
        assertThat(mvc.get().uri(API_HS).header("Authorization", token)).hasStatus(401);
    }

    @Test
    void doiVaiTroThiTokenVaiTroCuKhongConHopLe() {
        String token = bearer(Role.STUDENT);
        User user = users.findByEmail(new Email("thu.student@demo.local")).orElseThrow();
        users.save(new User(user.id(), user.email(), user.passwordHash(), user.displayName(), Role.TEACHER, user.enabled(),
            user.synthetic(), user.createdAt(), Instant.now(), user.sessionEpoch()));
        assertThat(mvc.get().uri(API_HS).header("Authorization", token)).hasStatus(401);
    }
}
