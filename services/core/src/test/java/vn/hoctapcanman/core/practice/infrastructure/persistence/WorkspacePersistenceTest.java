package vn.hoctapcanman.core.practice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Assign;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.MasteryOverride;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.NextProblem;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Schedule;
import vn.hoctapcanman.core.workspace.infrastructure.persistence.WorkspaceStoreAdapter;

/** Real PostgreSQL checks for V10 and workspace persistence, with no mocked repositories. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, WorkspaceStoreAdapter.class, vn.hoctapcanman.core.content.infrastructure.persistence.FormulaSheetRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
class WorkspacePersistenceTest {
    @Autowired JdbcClient db;
    @Autowired WorkspaceStoreAdapter store;
    @Autowired vn.hoctapcanman.core.classroom.infrastructure.persistence.EnrollmentJpaRepository enrollments;
    @Autowired vn.hoctapcanman.core.content.infrastructure.persistence.FormulaSheetRepositoryAdapter sheets;
    UUID teacher;
    UUID student;
    UUID cl;
    UUID problem;
    String code;
    Instant now;

    @BeforeEach
    void prepare() {
        now = Instant.now();
        DuLieuPractice.danhMuc(db);
        teacher = DuLieuPractice.nguoi(db, "TEACHER");
        student = DuLieuPractice.nguoi(db, "STUDENT");
        cl = DuLieuPractice.lop(db);
        DuLieuPractice.ghiDanh(db, cl, teacher, "TEACHER");
        DuLieuPractice.ghiDanh(db, cl, student, "STUDENT");
        problem = DuLieuPractice.bai(db, "UX");
        DuLieuPractice.phatHanh(db, cl, problem);
        code = db.sql("SELECT code FROM problems WHERE id=:id").param("id", problem).query(String.class).single();
    }

    @Test
    void assignmentsPersistModeDeadlineAndAudit() {
        store.assign(teacher, new Assign(cl, List.of(student), List.of(problem), "KIEM_TRA", now.plusSeconds(3600), "Kiểm tra"), List.of(student), now);
        assertThat(store.modes(student, cl)).singleElement().satisfies(i -> {
            assertThat(i.status()).isEqualTo("KIEM_TRA");
            assertThat(i.details().get("dueAt")).isNotEqualTo("");
        });
        assertThat(store.testMode(student, cl, code)).isTrue();
        assertThat(store.assignments(cl)).hasSize(1);
        assertThat(store.bank(cl)).hasSize(1);
        assertThat(store.dashboard(cl).students()).isEqualTo(1);
        assertThat(store.audit()).anyMatch(i -> i.title().equals("GIAO_BAI"));
    }

    @Test
    void activeTestBlocksTutorOnlyOnItsOwnProblems() {
        store.assign(teacher, new Assign(cl, List.of(student), List.of(problem), "KIEM_TRA", now.plusSeconds(3600), "Kiểm tra"), List.of(student), now);
        assertThat(store.testMode(student, cl, code)).isTrue();
        assertThat(store.testMode(student, cl, "BAI_KHAC")).isFalse();
        assertThat(store.testMode(teacher, cl, "BAI_KHAC")).isFalse();
        store.assign(teacher, new Assign(cl, List.of(student), List.of(problem), "KIEM_TRA", now.minusSeconds(1), "Đã hết hạn"), List.of(student), now);
        assertThat(store.testMode(student, cl, "BAI_KHAC")).isFalse();
        assertThat(store.testMode(student, cl, code)).isTrue();
    }

    @Test
    void classLockPreservesEnrollmentButClosesMembershipUntilUnlocked() {
        assertThat(enrollments.findActive(cl, student)).isPresent();
        assertThat(store.lockClass(teacher, cl, true, now)).isTrue();
        assertThat(enrollments.findActive(cl, student)).isEmpty();
        assertThat(enrollments.findByUserIdOrderByEnrolledAtAscClassIdAsc(student)).isEmpty();
        assertThat(enrollments.findByClassIdAndRoleOrderByEnrolledAtAscUserIdAsc(cl,
            vn.hoctapcanman.core.classroom.domain.model.ClassRole.STUDENT)).isEmpty();
        assertThat(store.classes(teacher, false)).singleElement().satisfies(i -> assertThat(i.status()).isEqualTo("KHOA"));
        assertThat(store.lockClass(teacher, cl, false, now.plusSeconds(1))).isTrue();
        assertThat(enrollments.findActive(cl, student)).isPresent();
        assertThat(store.audit()).anyMatch(i -> i.title().equals("KHOA_LOP"));
        assertThat(store.lockClass(teacher, UUID.randomUUID(), true, now)).isFalse();
    }

    @Test
    void nextProblemOverridePreservesExistingTestModeAndDeadline() {
        store.assign(teacher, new Assign(cl, List.of(student), List.of(problem), "KIEM_TRA", now.plusSeconds(3600), "Kiểm tra"), List.of(student), now);
        store.recommend(teacher, new NextProblem(cl, student, problem, "Củng cố kỹ năng"), now);
        assertThat(store.modes(student, cl)).singleElement().satisfies(i -> assertThat(i.status()).isEqualTo("KIEM_TRA"));
        assertThat(store.recommendations(student, cl)).singleElement().satisfies(i -> assertThat(i.id()).isEqualTo(code));
    }

    @Test
    void overridesRetainHistoryAndOnlyLatestIsActive() {
        store.override(teacher, new MasteryOverride(cl, student, "T12.DH.02", "THONG_HIEU", "Quan sát trên lớp"), now);
        store.override(teacher, new MasteryOverride(cl, student, "T12.DH.02", "VAN_DUNG", "Đã tự làm bài"), now.plusSeconds(1));
        assertThat(db.sql("SELECT count(*) FROM mastery_overrides WHERE student_id=:id").param("id", student).query(Integer.class).single()).isEqualTo(2);
        assertThat(db.sql("SELECT level4 FROM mastery_overrides WHERE student_id=:id AND removed_at IS NULL").param("id", student).query(String.class).single()).isEqualTo("VAN_DUNG");
    }

    @Test
    void scheduleOwnershipAndReminderPersist() {
        store.schedule(student, new Schedule("Ôn đạo hàm", now.plusSeconds(60), now.plusSeconds(1800)));
        UUID id = UUID.fromString(store.schedule(student).getFirst().id());
        assertThat(store.complete(teacher, id)).isFalse();
        assertThat(store.reminders(student, now)).hasSize(1);
        assertThat(store.complete(student, id)).isTrue();
        assertThat(store.reminders(student, now)).isEmpty();
    }

    @Test
    void overlappingStudySessionsAreRejected() {
        store.schedule(student, new Schedule("Ôn", now, now.plusSeconds(1800)));
        assertThatThrownBy(() -> store.schedule(student, new Schedule("Luyện", now.plusSeconds(900), now.plusSeconds(2700))))
            .isInstanceOf(vn.hoctapcanman.core.workspace.application.exception.WorkspaceException.class);
        assertThat(store.schedule(student)).hasSize(1);
    }

    @Test
    void unknownLicenseStaysOutsideStudentLibrary() {
        store.document(teacher, cl, "Bản nháp", "chua_ro", "Tài liệu tự soạn", now);
        assertThat(store.library(cl, false).documents()).isEmpty();
        assertThat(store.library(cl, true).documents()).hasSize(1);
    }

    @Test
    void addingFormulaCopiesLockedRowsAndClearsTheirChecks() {
        store.document(teacher, cl, "Căn cứ", "tu_soan", "Quy tắc đạo hàm tự soạn", now);
        UUID passage = db.sql("SELECT p.id FROM document_passages p JOIN documents d ON d.id=p.document_id WHERE d.class_id=:cl")
            .param("cl", cl).query(UUID.class).single();
        var row = vn.hoctapcanman.core.content.domain.model.Formula.unchecked(1, "d-1", null, "Đạo hàm", "(x^2)'=2x", "Đạo hàm của bình phương.");
        var check = vn.hoctapcanman.core.content.domain.model.FormulaCheck.of(row,
            vn.hoctapcanman.core.content.domain.model.FormulaKind.DANG_THUC,
            vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT,
            vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT, "{}", "{}", passage);
        var original = vn.hoctapcanman.core.content.domain.model.FormulaSheet.draft(cl, 1, null, List.of(row), now)
            .withCheckResults(java.util.Map.of(row.code(), check)).lock(teacher, now);
        sheets.save(original);
        store.formula(teacher, new vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Formula(cl,
            "Đạo hàm hằng", "c'=0", "Đạo hàm của hằng số."), now.plusSeconds(1));
        var draft = sheets.findDraft(cl).orElseThrow();
        assertThat(draft.rows()).hasSize(2);
        assertThat(draft.rows().getFirst().title()).isEqualTo(row.title());
        assertThat(draft.rows().getFirst().id()).isNotEqualTo(row.id());
        assertThat(draft.rows()).allMatch(f -> f.tier1Status() == null && f.tier2Status() == null && f.citationPassageId() == null);
        assertThat(store.library(cl, false).formulas()).hasSize(1);
    }

    @Test
    void directSubmissionAfterDeadlineIsRejected() {
        store.assign(teacher, new Assign(cl, List.of(student), List.of(problem), "ON_TAP", now.minusSeconds(3600), "Đã hết hạn"), List.of(student), now);
        assertThatThrownBy(() -> db.sql("INSERT INTO submissions(id,class_id,student_id,problem_id,content_version,status,started_at) VALUES(:id,:cl,:student,:problem,1,'DANG_LAM',now())")
            .param("id", UUID.randomUUID()).param("cl", cl).param("student", student).param("problem", problem).update())
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
