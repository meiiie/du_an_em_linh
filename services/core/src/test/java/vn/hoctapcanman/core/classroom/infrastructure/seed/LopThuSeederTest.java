package vn.hoctapcanman.core.classroom.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;
import vn.hoctapcanman.core.classroom.domain.repository.SchoolClassRepository;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/** Profile dev: lớp «12A1 thử» với giáo viên thử, An, Bình, Chi và cài lớp mặc định; chạy lại không nhân bản. */
@SpringBootTest
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class LopThuSeederTest {

    @Autowired
    private LopThuSeeder seeder;

    @Autowired
    private SchoolClassRepository classes;

    @Autowired
    private ClassSettingsRepository settings;

    @Autowired
    private ClassMembership membership;

    @Autowired
    private UserRepository users;

    @Test
    void lopThuCoGiaoVienVaBaHocSinhChayLaiKhongNhanBan() {
        seeder.run(new DefaultApplicationArguments()); // khởi động đã chạy một lần (ApplicationRunner); lần hai không nhân bản

        SchoolClass lop = classes.findByNameAndSchoolYear(LopThuSeeder.TEN_LOP, LopThuSeeder.NAM_HOC).orElseThrow();
        UUID lopId = lop.id().value();
        assertThat(lop.grade()).isEqualTo(12);
        assertThat(membership.laGiaoVien(id(LopThuSeeder.GIAO_VIEN), lopId)).isTrue();
        List<UUID> hocSinh = LopThuSeeder.HOC_SINH.stream().map(this::id).toList();
        assertThat(membership.hocSinhCuaLop(id(LopThuSeeder.GIAO_VIEN), lopId)).containsExactlyInAnyOrderElementsOf(hocSinh);
        hocSinh.forEach(hs -> assertThat(membership.lopHoc(hs)).contains(lopId));
        assertThat(settings.findByClassId(lop.id())).hasValueSatisfying(s -> {
            assertThat(s.revealSolutionAfterSubmit()).isFalse();
            assertThat(s.aiProvider()).isEqualTo("offline");
        });
    }

    private UUID id(String email) {
        return users.findByEmail(new Email(email)).orElseThrow().id().value();
    }
}
