package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;
import vn.hoctapcanman.core.classroom.domain.model.EscalationKind;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.infrastructure.persistence.UserRepositoryAdapter;

/** Flyway V3 trên PostgreSQL 18 thật + ánh xạ entity ↔ domain (ddl-auto=validate) + các ràng buộc của lớp học. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    UserRepositoryAdapter.class,
    SchoolClassRepositoryAdapter.class,
    EnrollmentRepositoryAdapter.class,
    ClassSettingsRepositoryAdapter.class,
    EscalationRepositoryAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class ClassroomPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Autowired
    private UserRepositoryAdapter users;

    @Autowired
    private SchoolClassRepositoryAdapter classes;

    @Autowired
    private EnrollmentRepositoryAdapter enrollments;

    @Autowired
    private ClassSettingsRepositoryAdapter settings;

    @Autowired
    private EscalationRepositoryAdapter escalations;

    private UUID nguoi(String email, Role role) {
        return users.save(User.create(new Email(email), "{bcrypt}x", "Người thử", role, true, NOW)).id().value();
    }

    private SchoolClass lop(String ten) {
        return classes.save(SchoolClass.create(ten, 12, "2026-2027", NOW));
    }

    @Test
    void luuLopGhiDanhVaCaiDat() {
        SchoolClass a = lop("12A1 thử");
        UUID gv = nguoi("gv.p1@demo.local", Role.TEACHER);
        UUID hs = nguoi("hs.p1@demo.local", Role.STUDENT);
        enrollments.save(new Enrollment(a.id(), gv, ClassRole.TEACHER, NOW));
        enrollments.save(new Enrollment(a.id(), hs, ClassRole.STUDENT, NOW.plusSeconds(1)));
        settings.save(ClassSettings.macDinh(a.id(), NOW).capNhat(true, "openrouter", false, gv, NOW.plusSeconds(2)));

        assertThat(classes.findByNameAndSchoolYear("12A1 thử", "2026-2027")).contains(a);
        assertThat(enrollments.find(a.id(), gv)).hasValueSatisfying(e -> assertThat(e.role()).isEqualTo(ClassRole.TEACHER));
        assertThat(enrollments.findByClass(a.id(), ClassRole.STUDENT)).extracting(Enrollment::userId).containsExactly(hs);
        assertThat(settings.findByClassId(a.id())).hasValueSatisfying(s -> {
            assertThat(s.revealSolutionAfterSubmit()).isTrue();
            assertThat(s.aiProvider()).isEqualTo("openrouter");
            assertThat(s.updatedBy()).isEqualTo(gv);
        });
    }

    @Test
    void hocSinhChiThuocMotLop() {
        UUID hs = nguoi("hs.p2@demo.local", Role.STUDENT);
        enrollments.save(new Enrollment(lop("12A2 thử").id(), hs, ClassRole.STUDENT, NOW));
        enrollments.save(new Enrollment(lop("12A3 thử").id(), hs, ClassRole.STUDENT, NOW));
        assertThatThrownBy(() -> enrollments.findByUser(hs)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tenLopKhongTrungTrongMotNamHoc() {
        lop("12A4 thử");
        lop("12A4 thử");
        assertThatThrownBy(() -> classes.findByNameAndSchoolYear("12A4 thử", "2026-2027"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void canhBaoMoKhongGhiTrungXuLyXongThiGhiLaiDuoc() {
        ClassId a = lop("12A5 thử").id();
        UUID hs = nguoi("hs.p3@demo.local", Role.STUDENT);
        UUID gv = nguoi("gv.p3@demo.local", Role.TEACHER);
        Escalation dau = Escalation.open(a, hs, EscalationKind.NHO_GV, "T12.DH.03", "B12-01", "B.DH.XETDAU", "Em nhờ thầy cô.", NOW);

        assertThat(escalations.saveIfNoOpenDuplicate(dau)).isTrue();
        // cùng (lớp, học sinh, loại, kỹ năng, bài), khác bước: vẫn là cảnh báo đang mở đó
        assertThat(escalations.saveIfNoOpenDuplicate(
            Escalation.open(a, hs, EscalationKind.NHO_GV, "T12.DH.03", "B12-01", "B.DH.NGHIEM", "Em nhờ thầy cô.", NOW))).isFalse();
        // khác bài, khác loại: cảnh báo riêng; kẹt theo kỹ năng không gắn bài: một cảnh báo mở
        assertThat(escalations.saveIfNoOpenDuplicate(
            Escalation.open(a, hs, EscalationKind.NHO_GV, "T12.DH.03", "B12-02", null, "Em nhờ thầy cô.", NOW))).isTrue();
        assertThat(escalations.saveIfNoOpenDuplicate(
            Escalation.open(a, hs, EscalationKind.KET, "T12.DH.03", null, null, "Kẹt 3 lượt.", NOW))).isTrue();
        assertThat(escalations.saveIfNoOpenDuplicate(
            Escalation.open(a, hs, EscalationKind.KET, "T12.DH.03", null, null, "Kẹt 4 lượt.", NOW))).isFalse();
        assertThat(escalations.findByClass(a, true)).hasSize(3);

        assertThat(escalations.markHandled(dau.id(), gv, NOW.plusSeconds(60))).isTrue();
        assertThat(escalations.markHandled(dau.id(), gv, NOW.plusSeconds(120))).isFalse();
        assertThat(escalations.findById(dau.id())).hasValueSatisfying(e -> {
            assertThat(e.isOpen()).isFalse();
            assertThat(e.handledAt()).isEqualTo(NOW.plusSeconds(60));
            assertThat(e.handledBy()).isEqualTo(gv);
        });
        assertThat(escalations.findByClass(a, true)).hasSize(2);
        assertThat(escalations.findByClass(a, false)).hasSize(3);
        assertThat(escalations.saveIfNoOpenDuplicate(
            Escalation.open(a, hs, EscalationKind.NHO_GV, "T12.DH.03", "B12-01", null, "Em lại nhờ thầy cô.", NOW.plusSeconds(180))))
            .isTrue();
    }
}
