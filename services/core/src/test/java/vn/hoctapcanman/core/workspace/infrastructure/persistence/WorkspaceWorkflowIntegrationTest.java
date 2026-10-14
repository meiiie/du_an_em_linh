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
}
