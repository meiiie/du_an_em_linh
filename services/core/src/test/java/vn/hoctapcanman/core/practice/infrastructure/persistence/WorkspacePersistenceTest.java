package vn.hoctapcanman.core.practice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.sql.Timestamp;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.shared.infrastructure.ClockConfig;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;
import vn.hoctapcanman.core.workspace.infrastructure.seed.WorkspaceNguyenTraiSeedAdapter;
import vn.hoctapcanman.core.workspace.application.port.ContentWorkflow;
import vn.hoctapcanman.core.tutor.application.service.OfflineTutorService;
import vn.hoctapcanman.core.tutor.infrastructure.web.OfflineTutorController;
import vn.hoctapcanman.core.workspace.infrastructure.web.WorkspaceExceptionHandler;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Assign;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.MasteryOverride;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.NextProblem;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Schedule;
import vn.hoctapcanman.core.workspace.infrastructure.persistence.WorkspaceStoreAdapter;

/** Kiểm tra workspace trên PostgreSQL thật, không giả lập kho dữ liệu. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ClockConfig.class, WorkspaceStoreAdapter.class, vn.hoctapcanman.core.content.infrastructure.persistence.FormulaSheetRepositoryAdapter.class})
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
    void nopBaiKiemTraVanTra403TruocKhiMoSse() throws Exception {
        store.assign(teacher, new Assign(cl, List.of(student), List.of(problem), "KIEM_TRA", now.plusSeconds(3600), "Kiểm tra"), List.of(student), now);
        UUID submission = UUID.randomUUID(), grading = UUID.randomUUID();
        db.sql("""
            INSERT INTO submissions(id,class_id,student_id,problem_id,content_version,status,started_at)
            VALUES(:id,:cl,:student,:problem,1,'DANG_LAM',:now)
            """).param("id",submission).param("cl",cl).param("student",student).param("problem",problem)
            .param("now",Timestamp.from(now)).update();
        db.sql("""
            INSERT INTO grading_results(id,submission_id,step_code,request_hash,result,graded_at)
            VALUES(:id,:submission,'B.DH.KETLUAN',:hash,'DAT',:now)
            """).param("id",grading).param("submission",submission).param("hash",DuLieuPractice.BAM)
            .param("now",Timestamp.from(now)).update();
        db.sql("""
            UPDATE submissions SET status='DA_NOP',submitted_at=:now,result='DAT',result_grading_id=:grading,
                skill_code='T12.DH.02',level4='THONG_HIEU' WHERE id=:id
            """).param("now",Timestamp.from(now.plusSeconds(1))).param("grading",grading).param("id",submission).update();
        assertThat(store.testMode(student,cl,code)).isTrue();

        var membership = mock(ClassMembership.class);
        when(membership.lopHoc(student)).thenReturn(java.util.Optional.of(cl));
        var gate = mock(ContentWorkflow.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OfflineTutorController(new OfflineTutorService(store,membership,gate)))
            .setControllerAdvice(new WorkspaceExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        var jwt = Jwt.withTokenValue("synthetic-token").header("alg","none").subject(student.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        try {
            mvc.perform(post("/api/hs/gia-su").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {"maBai":"%s","maBuoc":"B.DH.TXD","cap":1,"cauHoi":"Em cần gợi ý"}
                    """.formatted(code)))
                .andExpect(status().isForbidden()).andExpect(request().asyncNotStarted());
            verifyNoInteractions(gate);
        } finally {
            SecurityContextHolder.clearContext();
        }
        store.assign(teacher, new Assign(cl,List.of(student),List.of(problem),"KIEM_TRA",null,"Không có hạn"),List.of(student),now);
        assertThat(store.testMode(student,cl,code)).isTrue();
        store.assign(teacher, new Assign(cl,List.of(student),List.of(problem),"ON_TAP",now.plusSeconds(3600),"Ôn tập"),List.of(student),now);
        assertThat(store.testMode(student,cl,code)).isFalse();
    }

    @Test
    void kyNangVaPhanBoChiThuocKhoiVaChuDeCuaLopDaChon() {
        for (int grade : List.of(10,11,12)) {
            db.sql("INSERT INTO topics(code,name,grade) VALUES(:code,'Chủ đề thử',:grade)")
                .param("code","TEST"+grade).param("grade",grade).update();
            db.sql("INSERT INTO skills(code,topic_code,name,grade,is_core) VALUES(:code,:topic,'Kỹ năng thử',:grade,true)")
                .param("code","TEST."+grade).param("topic","TEST"+grade).param("grade",grade).update();
            db.sql("INSERT INTO mastery_states(student_id,skill_code,mastery,level4,attempts,stuck_counter) VALUES(:student,:skill,0.5,'THONG_HIEU',1,0)")
                .param("student",student).param("skill","TEST."+grade).update();
        }
        db.sql("INSERT INTO skills(code,topic_code,name,is_core) VALUES('TEST.10.NULL','TEST10','Kỹ năng chưa ghi khối',true)").update();
        db.sql("INSERT INTO skills(code,topic_code,name,grade,is_core) VALUES('TEST.10.EXTRA','TEST10','Kỹ năng phụ',10,false),('TEST.10.WRONG_TOPIC','TEST12','Sai chủ đề',10,true),('TEST.12.WRONG_GRADE','TEST10','Sai khối',12,true)").update();
        db.sql("UPDATE classes SET grade=10 WHERE id=:cl").param("cl",cl).update();
        UUID other = DuLieuPractice.lop(db);
        db.sql("UPDATE classes SET grade=11 WHERE id=:cl").param("cl",other).update();
        UUID otherStudent = DuLieuPractice.nguoi(db,"STUDENT");
        DuLieuPractice.ghiDanh(db,other,otherStudent,"STUDENT");
        for (String skill : List.of("TEST.10","TEST.11","TEST.12")) {
            db.sql("INSERT INTO mastery_states(student_id,skill_code,mastery,level4,attempts,stuck_counter) VALUES(:student,:skill,0.5,'THONG_HIEU',1,0)")
                .param("student",otherStudent).param("skill",skill).update();
        }

        assertThat(store.students(cl)).singleElement().satisfies(row -> {
            assertThat(row.details().get("skills")).isEqualTo(List.of(
                java.util.Map.of("code","TEST.10","name","Kỹ năng thử","level4","THONG_HIEU","level3","HIEU","mastery",0.5,"overridden",false),
                java.util.Map.of("code","TEST.10.NULL","name","Kỹ năng chưa ghi khối","level4","NHAN_BIET","level3","BIET","mastery",0.0,"overridden",false)));
        });
        assertThat(store.dashboard(cl).distribution()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
            "NHAN_BIET",1L,"THONG_HIEU",1L,"VAN_DUNG",0L,"VAN_DUNG_CAO",0L));
        assertThat(store.dashboard(other).distribution()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
            "NHAN_BIET",0L,"THONG_HIEU",1L,"VAN_DUNG",0L,"VAN_DUNG_CAO",0L));
        store.override(teacher,new MasteryOverride(cl,student,"TEST.10","VAN_DUNG","Quan sát trên lớp"),now);
        assertThat(store.dashboard(cl).distribution()).containsEntry("VAN_DUNG",1L).containsEntry("THONG_HIEU",0L);
    }

    @Test
    void hanSapToiLoaiHanCuVaKhongHanTruocKhiSapXepVaGioiHan() {
        Instant at = Instant.parse("2026-10-13T10:00:00Z");
        var fixedStore = new WorkspaceStoreAdapter(db,Clock.fixed(at,ZoneOffset.UTC));
        UUID expired = DuLieuPractice.bai(db,"CU");
        DuLieuPractice.phatHanh(db,cl,expired);
        store.assign(teacher,new Assign(cl,List.of(student),List.of(expired),"ON_TAP",at.minusSeconds(1),"Hạn cũ"),List.of(student),now);
        UUID noDeadline = DuLieuPractice.bai(db,"KHONG_HAN");
        DuLieuPractice.phatHanh(db,cl,noDeadline);
        store.assign(teacher,new Assign(cl,List.of(student),List.of(noDeadline),"ON_TAP",null,"Không có hạn"),List.of(student),now);
        for (int offset = 11; offset >= 0; offset--) {
            UUID p = DuLieuPractice.bai(db,"HAN_"+offset);
            DuLieuPractice.phatHanh(db,cl,p);
            store.assign(teacher,new Assign(cl,List.of(student),List.of(p),"ON_TAP",at.plusSeconds(offset),"Hạn sắp tới"),List.of(student),now);
        }
        assertThat(fixedStore.dashboard(cl).deadlines()).extracting(row -> row.details().get("dueAt"))
            .containsExactly("2026-10-13T10:00:00Z","2026-10-13T10:00:01Z","2026-10-13T10:00:02Z",
                "2026-10-13T10:00:03Z","2026-10-13T10:00:04Z","2026-10-13T10:00:05Z","2026-10-13T10:00:06Z",
                "2026-10-13T10:00:07Z","2026-10-13T10:00:08Z","2026-10-13T10:00:09Z");
    }

    @Test
    void lopSeedChiGomCacBoChuaQuaHan() {
        Instant at = Instant.parse("2026-10-13T10:00:00Z");
        cl = TruongNguyenTrai.id("lop:10A1");
        db.sql("INSERT INTO classes(id,name,grade,school_year,created_at) VALUES(:cl,'10A1',10,:year,:now)")
            .param("cl",cl).param("year",TruongNguyenTrai.NAM_HOC).param("now",Timestamp.from(now)).update();
        DuLieuPractice.ghiDanh(db,cl,teacher,"TEACHER");
        student = DuLieuPractice.nguoi(db,"STUDENT");
        DuLieuPractice.ghiDanh(db,cl,student,"STUDENT");
        UUID secondStudent = DuLieuPractice.nguoi(db,"STUDENT");
        DuLieuPractice.ghiDanh(db,cl,secondStudent,"STUDENT");
        for (Instant due : List.of(at.minusSeconds(1),at,at.plusSeconds(3600))) {
            for (int n = 0; n < 2; n++) {
                UUID p = DuLieuPractice.bai(db,"BO_"+n);
                DuLieuPractice.phatHanh(db,cl,p);
                store.assign(teacher,new Assign(cl,List.of(student,secondStudent),List.of(p),"ON_TAP",due,"Ôn tập"),List.of(student,secondStudent),now);
            }
        }
        DuLieuPractice.phatHanh(db,cl,problem);
        store.assign(teacher,new Assign(cl,List.of(student),List.of(problem),"ON_TAP",null,"Không có hạn"),List.of(student),now);
        var seedStore = new WorkspaceNguyenTraiSeedAdapter(db,Clock.fixed(at,ZoneOffset.UTC));
        assertThat(seedStore.dashboard(cl).deadlines()).hasSize(2).allSatisfy(row ->
            assertThat(row.details()).containsEntry("assignmentCount",4).containsEntry("problemCount",2).containsEntry("studentCount",2));
        assertThat(seedStore.dashboard(cl).deadlines()).extracting(row -> row.details().get("dueAt"))
            .containsExactly("2026-10-13T10:00:00Z","2026-10-13T11:00:00Z");
        assertThat(seedStore.assignments(cl)).hasSize(13);
        assertThat(seedStore.dashboard(cl).assignments()).isEqualTo(13);
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
            .isInstanceOf(org.springframework.dao.DataAccessException.class).hasMessageContaining("hết hạn nộp");
    }
}
