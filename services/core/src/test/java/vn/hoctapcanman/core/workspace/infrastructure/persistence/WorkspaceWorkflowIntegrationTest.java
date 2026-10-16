package vn.hoctapcanman.core.workspace.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.service.ClassMembershipService;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.*;
import vn.hoctapcanman.core.content.domain.model.*;
import vn.hoctapcanman.core.content.infrastructure.persistence.*;
import vn.hoctapcanman.core.content.infrastructure.workspace.ContentWorkflowAdapter;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;
import vn.hoctapcanman.core.shared.infrastructure.ClockConfig;
import vn.hoctapcanman.core.shared.infrastructure.math.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.ContentDraftModel;
import vn.hoctapcanman.core.workspace.application.service.WorkspaceService;
import vn.hoctapcanman.core.workspace.infrastructure.seed.ObsoleteTutorReviewCleanup;
import vn.hoctapcanman.core.workspace.infrastructure.web.*;

/** Controllers → transactional service → PostgreSQL 18, with only external AI/CAS replaced. */
@DataJpaTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class,ClockConfig.class,WorkspaceService.class,WorkspaceStoreAdapter.class,
    ContentDraftStoreAdapter.class,ContentWorkflowAdapter.class,ProblemRepositoryAdapter.class,
    SolutionRepositoryAdapter.class,VerificationRunRepositoryAdapter.class,ProblemReleaseRepositoryAdapter.class,
    FormulaSheetRepositoryAdapter.class,DocumentRepositoryAdapter.class,ClassMembershipService.class,
    EnrollmentRepositoryAdapter.class,ClassSettingsRepositoryAdapter.class,WorkspaceWorkflowIntegrationTest.Validation.class})
@Testcontainers(disabledWithoutDocker=true)
class WorkspaceWorkflowIntegrationTest {
    @TestConfiguration static class Validation {
        @Bean jakarta.validation.Validator validator() { return jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator(); }
    }
    @Autowired JdbcClient db;
    @Autowired WorkspaceService service;
    @Autowired WorkspaceStoreAdapter store;
    @Autowired ContentDraftStoreAdapter drafts;
    @Autowired ProblemRepositoryAdapter problems;
    @Autowired SolutionRepositoryAdapter solutions;
    @Autowired FormulaSheetRepositoryAdapter sheets;
    @MockitoBean ContentDraftModel model;
    @MockitoBean MathServiceClient math;
    @MockitoBean PasswordHasher passwords;
    final JsonMapper json=JsonMapper.builder().build();
    UUID teacher,student,cl,problem,document;
    String code;
    MockMvc mvc;
    static final String SKILL="T12.NT.HS",TEXT="Đạo hàm của x bình phương là 2x.";
    @BeforeEach void fixture() throws Exception {
        DuLieuPractice.danhMuc(db);
        // Fresh current seed: no legacy T12.DH.* skill is present.
        db.sql("DELETE FROM skills WHERE code='T12.DH.02'").update();
        var seed=json.readTree(java.nio.file.Path.of("../../data/dev/nguyen-trai/ky-nang.json").toFile());
        for(var skill:seed)if("DH12".equals(skill.path("chu_de").asString()))
            db.sql("INSERT INTO skills(code,topic_code,name,grade,is_core) VALUES(:s,'DH12',:name,:grade,true)")
                .param("s",skill.path("ma").asString()).param("name",skill.path("ten").asString()).param("grade",skill.path("lop").asInt()).update();
        teacher=DuLieuPractice.nguoi(db,"TEACHER");student=DuLieuPractice.nguoi(db,"STUDENT");cl=DuLieuPractice.lop(db);
        DuLieuPractice.ghiDanh(db,cl,teacher,"TEACHER");DuLieuPractice.ghiDanh(db,cl,student,"STUDENT");
        service.upload(teacher,cl,"Tài liệu tự soạn","tu_soan","dao-ham.txt",TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        document=db.sql("SELECT id FROM documents WHERE class_id=:cl").param("cl",cl).query(UUID.class).single();
        UUID passage=db.sql("SELECT id FROM document_passages WHERE document_id=:doc").param("doc",document).query(UUID.class).single();
        var formula=vn.hoctapcanman.core.content.domain.model.Formula.unchecked(1,"dao-ham",SKILL,"Đạo hàm","(x^2)'=2x",TEXT);
        var sheet=FormulaSheet.draft(cl,1,null,List.of(formula),Instant.now()).withCheckResults(Map.of(formula.code(),
            FormulaCheck.of(formula,FormulaKind.DANG_THUC,CheckStatus.DAT,CheckStatus.DAT,null,null,passage)));
        sheets.save(sheet.lock(teacher,Instant.now()));
        var saved=DuLieuPractice.baiV0(db,"TU-HOC",SKILL,"NHAN_BIET","Xét đơn điệu của y=x².","x^2","x**2","TU_LUAN_5_BUOC",null,"SUPHAM");
        problem=saved.id();code=saved.ma();solutions.save(new Solution(problem,"{}","[]",null));DuLieuPractice.phatHanh(db,cl,problem);
        mvc=MockMvcBuilders.standaloneSetup(new WorkspaceController(service)).setControllerAdvice(new WorkspaceExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        when(math.call(eq(MathJob.SOLVE),anyMap())).thenReturn(new MathResult.Ok(Map.of("dat",true,"bai_lam",Map.of("dao_ham","2*x"),"su_kien",List.of())));
        when(math.call(eq(MathJob.VERIFY),anyMap())).thenReturn(new MathResult.Ok(Map.of("tang",List.of(
            Map.of("tang",1,"trang_thai","DAT"),Map.of("tang",2,"trang_thai","DAT","trich_dan",List.of(Map.of("document_id",document.toString(),"vi_tri",0,"trich",TEXT))),Map.of("tang",3,"trang_thai","DAT")))));
    }
    @AfterEach void clearPrincipal() { SecurityContextHolder.clearContext(); }
    void as(UUID actor) { SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("synthetic").header("alg","none").subject(actor.toString()).build())); }
    @Test void studentListsAndChoosesPublishedExerciseThroughHttp() throws Exception {
        as(student);
        mvc.perform(get("/api/hs/tu-hoc").param("chuDe","DH12")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(code)).andExpect(jsonPath("$[0].subtitle").value("Xét đơn điệu của y=x²."));
        mvc.perform(post("/api/hs/tu-hoc/{code}",code)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/api/hs/tu-hoc/{code}",code)).andExpect(status().isOk());
        assertThat(store.assignments(cl)).hasSize(1);
        assertThat(service.recommendations(student)).singleElement().satisfies(i->assertThat(i.id()).isEqualTo(code));
    }
    @Test void teacherCreatesSeededSkillAndApprovesAiDraftThenAssigns() throws Exception {
        as(teacher);
        var manual=new NewProblem(cl,"GV.SEED",SKILL,"NHAN_BIET","Xét đơn điệu y=x².","x^2","x**2");
        mvc.perform(post("/api/gv/ngan-hang").contentType("application/json").content(json.writeValueAsString(manual))).andExpect(status().isOk());
        when(model.generate(eq(cl),eq(TEXT),eq(SKILL))).thenReturn(Optional.of(new GeneratedDraft(manual,"Lời giải nháp giáo viên đọc.","GLM",10,20)));
        mvc.perform(post("/api/gv/soan-nhap").contentType("application/json").content(json.writeValueAsString(new DraftRequest(cl,document)))).andExpect(status().isOk());
        var pending=service.reviews(teacher,cl).stream().filter(i->"CONTENT_DRAFT".equals(i.details().get("kind"))).toList();
        assertThat(pending).hasSize(1);UUID id=UUID.fromString(pending.getFirst().id());
        mvc.perform(post("/api/gv/duyet/{id}",id).contentType("application/json").content(json.writeValueAsString(new Review(cl,true,"Đã đọc và đối chiếu.")))).andExpect(status().isOk());
        assertThat(drafts.pending(cl)).isEmpty();
        UUID created=UUID.fromString(service.bank(teacher,cl).stream().filter(i->i.title().startsWith("AI.")).findFirst().orElseThrow().id());
        service.assign(teacher,new Assign(cl,List.of(student),List.of(created),"ON_TAP",null,"AI đã duyệt"));
        assertThat(store.assignments(cl)).singleElement().satisfies(i->assertThat(i.details()).containsEntry("studentId",student.toString()));
    }
    @Test void emptyNewClassStillListsSeededSkillCatalogThroughHttp() throws Exception {
        UUID empty=DuLieuPractice.lop(db);DuLieuPractice.ghiDanh(db,empty,teacher,"TEACHER");
        as(teacher);
        mvc.perform(get("/api/gv/ky-nang").param("lopId",empty.toString())).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='"+SKILL+"')].title").isNotEmpty());
        as(student);
        assertThatThrownBy(()->mvc.perform(get("/api/gv/ky-nang").param("lopId",empty.toString())))
            .hasRootCauseInstanceOf(vn.hoctapcanman.core.classroom.application.exception.KhongThuocLopException.class);
    }
    @Test void invalidSkillIsRejectedBeforeModelOrMath() {
        assertThatThrownBy(()->service.createProblem(teacher,new NewProblem(cl,"BAD","T12.DH.03","NHAN_BIET","Đề","x^2","x**2")))
            .isInstanceOf(vn.hoctapcanman.core.workspace.application.exception.WorkspaceException.class);
        assertThatThrownBy(()->service.draft(teacher,new DraftRequest(cl,document,"T12.DH.03"))).isInstanceOf(vn.hoctapcanman.core.workspace.application.exception.WorkspaceException.class);
        verifyNoInteractions(model,math);
    }
    @Test void staleReleaseIsHiddenFromNewStudyAndRecommendationsButKeepsExistingWorkReadable() throws Exception {
        store.chooseStudy(student,cl,code,Instant.now());
        assertThat(problems.findReleasedForWork(cl,code)).isPresent();
        int version=problems.findContentVersion(problem).orElseThrow();
        assertThat(solutions.findReleased(cl,problem,version)).isPresent();
        db.sql("UPDATE verification_runs SET stale=true WHERE class_id=:cl AND subject_id=:p").param("cl",cl).param("p",problem).update();
        // ADR 005: bài đã giao và lời giải đúng phiên bản vẫn đọc theo trạng thái phát hành trong cùng ảnh chụp.
        assertThat(problems.findReleasedForWork(cl,code)).map(ReleasedProblem::contentVersion).contains(version);
        assertThat(problems.findReleasedInClass(cl)).extracting(Problem::id).containsExactly(problem);
        assertThat(problems.findReleasedInClass(cl,code)).map(Problem::id).contains(problem);
        assertThat(problems.findReleasedForWork(cl)).extracting(r->r.problem().id()).containsExactly(problem);
        assertThat(solutions.findReleased(cl,problem,version)).isPresent();
        assertThat(store.topics(student,cl)).isEmpty();
        assertThat(store.recommendations(student,cl)).singleElement().satisfies(i->assertThat(i.details()).containsEntry("noCandidate",true));
        as(student);mvc.perform(get("/api/hs/tu-hoc").param("chuDe","DH12")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(post("/api/hs/tu-hoc/{code}",code)).andExpect(status().isConflict());
    }
    @Test void changedContentVersionWithdrawsStudentVisibilityThroughRealSchemaTriggers() throws Exception {
        store.chooseStudy(student,cl,code,Instant.now());
        int before=problems.findContentVersion(problem).orElseThrow();
        db.sql("UPDATE problems SET content_hash=:hash,statement_text='Đề đã sửa.' WHERE id=:id")
            .param("hash","b".repeat(64)).param("id",problem).update();
        assertThat(problems.findContentVersion(problem)).contains(before+1);
        assertThat(problems.findReleasedForWork(cl,code)).isEmpty();
        assertThat(store.selfStudy(student,cl,"DH12")).isEmpty();
        assertThat(store.recommendations(student,cl)).singleElement().satisfies(i->assertThat(i.details()).containsEntry("noCandidate",true));
        as(student);mvc.perform(post("/api/hs/tu-hoc/{code}",code)).andExpect(status().isConflict());
    }
    @Test void obsoleteGateItemsAreHiddenAndDevCleanupIsIdempotentAndLimited() {
        for(String reason:List.of("Cổng công thức chưa xác minh được lời gợi ý; câu này chưa gửi cho học sinh.","Giáo viên yêu cầu đọc lại."))
            db.sql("INSERT INTO tutor_review_queue(id,class_id,problem_id,draft,reason,created_at) VALUES(:id,:cl,:p,:draft,:reason,now())")
                .param("id",UUID.randomUUID()).param("cl",cl).param("p",problem).param("draft",reason).param("reason",reason).update();
        assertThat(store.reviews(cl)).singleElement().satisfies(i->assertThat(i.subtitle()).isEqualTo("Giáo viên yêu cầu đọc lại."));
        var cleanup=new ObsoleteTutorReviewCleanup(db);cleanup.run(null);cleanup.run(null);
        assertThat(db.sql("SELECT reason FROM tutor_review_queue WHERE class_id=:cl").param("cl",cl).query(String.class).list()).containsExactly("Giáo viên yêu cầu đọc lại.");
        assertThat(ObsoleteTutorReviewCleanup.class.getAnnotation(Profile.class).value()).containsExactly("dev");
    }
    @Test void framedStudyIsAvailableAcrossPreviouslyEmptyTopicsAndSubmittedWorkIsExcluded() {
        for(String topic:List.of("TP12","KG12","XS12")) {
            String skill="T12.QA."+topic;
            db.sql("INSERT INTO topics(code,name,grade) VALUES(:t,:t,12) ON CONFLICT DO NOTHING").param("t",topic).update();
            db.sql("INSERT INTO skills(code,topic_code,name,grade,is_core) VALUES(:s,:t,'Kỹ năng thử',12,true) ON CONFLICT DO NOTHING").param("s",skill).param("t",topic).update();
            var exercise=DuLieuPractice.baiV0(db,"QA-"+topic,skill,"NHAN_BIET","Đề tổng hợp","x",null,"TU_LUAN",null,"SUPHAM");
            DuLieuPractice.phatHanh(db,cl,exercise.id());
            assertThat(store.selfStudy(student,cl,topic)).isEmpty();
            db.sql("INSERT INTO exercise_step_frames VALUES(:p,1,1,'[{},{},{},{},{}]'::jsonb,'{}'::jsonb)").param("p",exercise.id()).update();
            assertThat(store.selfStudy(student,cl,topic)).extracting(i->i.id()).containsExactly(exercise.ma());
            store.chooseStudy(student,cl,exercise.ma(),Instant.now());
            UUID submission=openWork(exercise.id());UUID grade=uncertainGrade(submission);
            submitUncertain(submission,grade);
            assertThat(store.selfStudy(student,cl,topic)).isEmpty();
            assertThat(store.recommendations(student,cl)).noneMatch(item->item.id().equals(exercise.ma()));
        }
    }
    @Test void freeResponseUsesRealTopicAndReferenceAnswerButMultipleChoiceStaysUnsupported() {
        var frames=new KhungChamRepositoryAdapter(db);
        for(String form:List.of("TU_LUAN","TN_NHIEU_LUA_CHON")) {
            var exercise=DuLieuPractice.baiV0(db,"QA-FREE",SKILL,"NHAN_BIET","Đề tổng hợp","x",null,form,null,"SUPHAM");
            solutions.save(new Solution(exercise.id(),"{}","[]","\"72\""));
            DuLieuPractice.phatHanh(db,cl,exercise.id());
            int version=problems.findContentVersion(exercise.id()).orElseThrow();
            if(form.equals("TU_LUAN")) {
                assertThat(frames.doc(exercise.id(),version)).get().satisfies(frame->{
                    assertThat(frame.cacBuoc()).extracting(step->step.maBuoc()).containsExactly("B.TD.TRALOI");
                    assertThat(frame.baiToan()).containsEntry("dap_so","72");
                });
                assertThat(store.selfStudy(student,cl,"DH12")).extracting(Item::id).contains(exercise.ma());
                store.chooseStudy(student,cl,exercise.ma(),Instant.now());
                assertThat(store.recommendations(student,cl)).extracting(Item::id).contains(exercise.ma());
            } else {
                assertThat(frames.doc(exercise.id(),version)).isEmpty();
                assertThat(store.selfStudy(student,cl,"DH12")).extracting(Item::id).doesNotContain(exercise.ma());
                assertThatThrownBy(()->store.chooseStudy(student,cl,exercise.ma(),Instant.now()))
                    .isInstanceOf(vn.hoctapcanman.core.workspace.application.exception.WorkspaceException.class);
            }
        }
        assertThat(db.sql("SELECT count(*) FROM topics WHERE code='TRALOI'").query(Integer.class).single()).isZero();
    }
    @Test void workOpenedExactlyAtNanosecondAssignmentTimeBelongsToThatAssignment() {
        Instant assigned=Instant.parse("2026-10-16T08:00:00.123456789Z");
        store.assign(teacher,new Assign(cl,List.of(student),List.of(problem),"ON_TAP",null,"Ôn tập"),List.of(student),assigned);
        var repo=new vn.hoctapcanman.core.practice.infrastructure.persistence.SubmissionRepositoryAdapter(db);
        var work=repo.openOrGet(vn.hoctapcanman.core.practice.domain.model.Submission.open(cl,student,problem,
            problems.findContentVersion(problem).orElseThrow(),assigned));
        assertThat(repo.findOpen(student,cl,problem,work.contentVersion())).contains(work);
        assertThat(db.sql("SELECT assignment_at=started_at FROM submissions WHERE id=:id")
            .param("id",work.id()).query(Boolean.class).single()).isTrue();
    }
    @Test void uncertainStepAppearsBeforeSubmissionAndTeacherAcknowledgementDoesNotChangeCasResult() {
        store.chooseStudy(student,cl,code,Instant.now());
        UUID submission=openWork(problem);UUID grade=uncertainGrade(submission);
        db.sql("INSERT INTO submission_steps(submission_id,step_code,line_no,latex) VALUES(:s,'B.DH.DAOHAM',0,'Biểu thức chưa đọc được')").param("s",submission).update();
        assertThat(store.reviews(cl)).singleElement().satisfies(item->{
            assertThat(item.id()).isEqualTo(grade.toString());assertThat(item.details()).containsEntry("kind","STEP").containsEntry("draft","Biểu thức chưa đọc được");
        });
        UUID other=DuLieuPractice.lop(db);DuLieuPractice.ghiDanh(db,other,teacher,"TEACHER");
        assertThatThrownBy(()->service.review(teacher,grade,new Review(other,true,"Đã xem.")))
            .isInstanceOf(vn.hoctapcanman.core.workspace.application.exception.WorkspaceException.class);
        service.review(teacher,grade,new Review(cl,true,"Đã xem, sẽ hướng dẫn em trên lớp."));
        assertThat(store.reviews(cl)).isEmpty();
        assertThat(db.sql("SELECT result FROM grading_results WHERE id=:g").param("g",grade).query(String.class).single()).isEqualTo("KHONG_KIEM_DUOC");
    }
    @Test void uncertainSignTableReviewIncludesOrderedCellsFromOnlyThatSubmissionAndStep() {
        UUID submission=openWork(problem);UUID grade=uncertainGrade(submission,"B.DH.XETDAU");
        db.sql("INSERT INTO submission_steps(submission_id,step_code,line_no,latex) VALUES(:s,'B.DH.DAOHAM',0,'Không phải bước xét dấu')")
            .param("s",submission).update();
        for(UUID work:List.of(submission,openWorkForOtherStudent())) {
            db.sql("INSERT INTO submission_tables(submission_id,step_code,table_kind) VALUES(:s,'B.DH.XETDAU','XET_DAU')").param("s",work).update();
            // Insert out of order: evidence must follow ordinal and retain the 0-based position.
            db.sql("""
                INSERT INTO submission_table_cells(submission_id,step_code,ordinal,row_code,k,value) VALUES
                (:s,'B.DH.XETDAU',2,'DAU_YPHAY',1,'?'),
                (:s,'B.DH.XETDAU',0,'X',NULL,'0'),
                (:s,'B.DH.XETDAU',1,'DAU_YPHAY',0,'-')
                """).param("s",work).update();
        }
        assertThat(store.reviews(cl)).singleElement().satisfies(item->{
            assertThat(item.id()).isEqualTo(grade.toString());
            assertThat(item.details()).containsEntry("kind","STEP")
                .containsEntry("draft","Bảng XET_DAU\nX = 0\nDAU_YPHAY[0] = -\nDAU_YPHAY[1] = ?");
        });
        service.review(teacher,grade,new Review(cl,true,"Đã xem bảng xét dấu."));
        assertThat(store.reviews(cl)).isEmpty();
        assertThat(db.sql("SELECT result FROM grading_results WHERE id=:g").param("g",grade).query(String.class).single()).isEqualTo("KHONG_KIEM_DUOC");
    }
    @Test void submissionForOldVersionDoesNotExcludeRepublishedExerciseFromSelfStudy() {
        store.chooseStudy(student,cl,code,Instant.now());
        UUID submission=openWork(problem);submitUncertain(submission,uncertainGrade(submission));
        int before=problems.findContentVersion(problem).orElseThrow();
        assertThat(store.selfStudy(student,cl,"DH12")).isEmpty();
        solutions.save(new Solution(problem,"{\"updated\":true}","[]",null));
        assertThat(problems.findContentVersion(problem)).contains(before+1);
        DuLieuPractice.phatHanh(db,cl,problem);
        assertThat(store.selfStudy(student,cl,"DH12")).extracting(Item::id).containsExactly(code);
        assertThat(store.recommendations(student,cl)).extracting(Item::id).containsExactly(code);
        UUID current=openWork(problem);submitUncertain(current,uncertainGrade(current));
        assertThat(store.selfStudy(student,cl,"DH12")).isEmpty();
        assertThat(store.recommendations(student,cl)).noneMatch(item->item.id().equals(code));
    }
    @Test void reassignedStudyIgnoresOldSubmissionAndExcludesCurrentSubmissionAtAssignmentTime() {
        Instant assigned=Instant.now();
        var request=new Assign(cl,List.of(student),List.of(problem),"ON_TAP",null,"Ôn tập");
        store.assign(teacher,request,List.of(student),assigned.minusSeconds(60));
        UUID old=openWork(problem);
        submitUncertain(old,uncertainGrade(old));
        assertThat(store.selfStudy(student,cl,"DH12")).isEmpty();
        assertThat(store.recommendations(student,cl)).noneMatch(item->item.id().equals(code));
        store.assign(teacher,request,List.of(student),assigned.plusSeconds(1));
        assertThat(store.selfStudy(student,cl,"DH12")).extracting(Item::id).containsExactly(code);
        assertThat(store.recommendations(student,cl)).extracting(Item::id).containsExactly(code);
        UUID current=openWork(problem);
        submitUncertain(current,uncertainGrade(current));
        assertThat(store.selfStudy(student,cl,"DH12")).isEmpty();
        assertThat(store.recommendations(student,cl)).noneMatch(item->item.id().equals(code));
    }
    @Test void selfStudyWithoutAssignmentExcludesOnlySubmittedCurrentVersion() {
        assertThat(store.selfStudy(student,cl,"DH12")).extracting(Item::id).containsExactly(code);
        UUID old=openWork(problem);
        submitUncertain(old,uncertainGrade(old));
        assertThat(store.selfStudy(student,cl,"DH12")).isEmpty();
        solutions.save(new Solution(problem,"{\"updated\":true}","[]",null));
        DuLieuPractice.phatHanh(db,cl,problem);
        assertThat(store.selfStudy(student,cl,"DH12")).extracting(Item::id).containsExactly(code);
        UUID current=openWork(problem);
        assertThat(store.selfStudy(student,cl,"DH12")).extracting(Item::id).containsExactly(code);
        submitUncertain(current,uncertainGrade(current));
        assertThat(store.selfStudy(student,cl,"DH12")).isEmpty();
        assertThat(store.assignments(cl)).isEmpty();
    }
    @Test void auditIdentifiesActualUploadedDocumentAndFormula() {
        store.formula(teacher,new vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Formula(cl,"Công thức thử","x=x","Đẳng thức."),Instant.now());
        var audit=store.audit();
        assertThat(audit).filteredOn(i->i.title().equals("NAP_TAI_LIEU")).singleElement().satisfies(i->{
            assertThat(i.status()).isEqualTo(document.toString());assertThat(i.details()).containsEntry("targetName","Tài liệu tự soạn");
        });
        assertThat(audit).filteredOn(i->i.title().equals("THEM_CONG_THUC")).singleElement().satisfies(i->{
            assertThat(i.status()).isNotEqualTo(cl.toString());assertThat(i.details()).containsEntry("targetName","Công thức thử");
        });
    }
    private UUID openWork(UUID p) {
        return openWork(p,student);
    }
    private UUID openWork(UUID p,UUID learner) {
        UUID id=UUID.randomUUID();
        // now() is the transaction start, which can precede chooseStudy's Java timestamp.
        db.sql("""
            INSERT INTO submissions(id,class_id,student_id,problem_id,content_version,status,started_at)
            SELECT :id,:cl,:s,id,content_version,'DANG_LAM',greatest(now(),
                (SELECT assigned_at FROM assignments WHERE class_id=:cl AND student_id=:s
                    AND problem_id=:p AND status='DA_GIAO'))
            FROM problems WHERE id=:p
            """)
            .param("id",id).param("cl",cl).param("s",learner).param("p",p).update();return id;
    }
    private UUID openWorkForOtherStudent() {
        UUID other=DuLieuPractice.nguoi(db,"STUDENT");DuLieuPractice.ghiDanh(db,cl,other,"STUDENT");
        return openWork(problem,other);
    }
    private void submitUncertain(UUID submission,UUID grade) {
        db.sql("""
            UPDATE submissions s SET status='DA_NOP',result='KHONG_KIEM_DUOC',result_grading_id=:g,
                submitted_at=now(),skill_code=p.skill_code,level4=p.level4
            FROM problems p WHERE s.id=:s AND p.id=s.problem_id
            """).param("g",grade).param("s",submission).update();
    }
    private UUID uncertainGrade(UUID submission) {
        return uncertainGrade(submission,"B.DH.DAOHAM");
    }
    private UUID uncertainGrade(UUID submission,String step) {
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO grading_results(id,submission_id,step_code,request_hash,result,message,graded_at) VALUES(:id,:s,:step,:hash,'KHONG_KIEM_DUOC','Chờ thầy cô xem.',now())")
            .param("id",id).param("s",submission).param("step",step).param("hash","c".repeat(64)).update();return id;
    }

}
