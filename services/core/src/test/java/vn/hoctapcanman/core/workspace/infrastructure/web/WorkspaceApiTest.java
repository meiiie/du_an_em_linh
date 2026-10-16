package vn.hoctapcanman.core.workspace.infrastructure.web;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.identity.infrastructure.security.SecurityConfig;
import vn.hoctapcanman.core.tutor.application.service.OfflineTutorService;
import vn.hoctapcanman.core.tutor.infrastructure.web.OfflineTutorController;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.ContentWorkflow;
import vn.hoctapcanman.core.workspace.application.port.WorkspaceStore;
import vn.hoctapcanman.core.workspace.application.service.WorkspaceService;

/** Real web/security/use cases; only external ports are replaced. */
@WebMvcTest(controllers = {WorkspaceController.class, OfflineTutorController.class, vn.hoctapcanman.core.tutor.infrastructure.web.TutorSessionController.class})
@Import({SecurityConfig.class, WorkspaceService.class, OfflineTutorService.class, vn.hoctapcanman.core.tutor.application.service.TutorRateLimiter.class, vn.hoctapcanman.core.tutor.application.service.TutorSessionService.class, WorkspaceApiTest.Time.class})
class WorkspaceApiTest {
    @TestConfiguration static class Time {
        @Bean Clock clock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }
    static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
    final UUID actor = UUID.randomUUID(), student = UUID.randomUUID(), cl = UUID.randomUUID(), problem = UUID.randomUUID();
    @Autowired MockMvc mvc;
    @MockitoBean vn.hoctapcanman.core.tutor.application.port.TermFeedback termFeedback;
    @MockitoBean WorkspaceStore store;
    @MockitoBean vn.hoctapcanman.core.tutor.application.port.TutorSessionStore sessions;
    @MockitoBean vn.hoctapcanman.core.tutor.application.port.HintModel hintModel;
    @MockitoBean ClassMembership membership;
    @MockitoBean ContentWorkflow content;
    @MockitoBean vn.hoctapcanman.core.workspace.application.port.ContentDraftStore drafts;
    @MockitoBean vn.hoctapcanman.core.workspace.application.port.ContentDraftModel draftModel;
    @MockitoBean PasswordHasher hasher;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean vn.hoctapcanman.core.identity.infrastructure.web.RefreshCookie refreshCookie;

    RequestPostProcessor as(UUID id, String role) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("role", role))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
    Item item(String id, String title, String text, String status, Map<String,Object> details) {
        return new Item(id, title, text, status, details);
    }
    @BeforeEach void prepare() {
        when(membership.lopHoc(student)).thenReturn(Optional.of(cl));
        when(store.accountRole(actor)).thenReturn("ADMIN");
        when(store.bank(cl)).thenReturn(List.of(
            item(problem.toString(), "DH1", "Đạo hàm của tích", "DA_PHAT_HANH", Map.of("level4","THONG_HIEU","skillCode","K1","gate","DAT")),
            item(UUID.randomUUID().toString(), "DH2", "Cực trị", "NHAP", Map.of("level4","VAN_DUNG","skillCode","K2","gate","KHONG_KIEM_DUOC"))));
        when(store.library(cl, true)).thenReturn(library());
        when(store.library(cl, false)).thenReturn(library());
    }
    Library library() {
        return new Library(List.of(
            item("f1", "Đạo hàm của tích", "(uv)'", "KHOA", Map.of("statement", "Quy tắc đạo hàm")),
            item("f2", "Cực trị", "f'(x)", "KHOA", Map.of())),
            List.of(item("d1", "Tài liệu đạo hàm", "Nội dung tự soạn", "tu_soan", Map.of())));
    }
    @Test void dashboardUsesTeacherClassAndReturnsCounts() throws Exception {
        when(store.dashboard(cl)).thenReturn(new Dashboard(3,1,2,4,1,Map.of("NHAN_BIET",3L),List.of()));
        mvc.perform(get("/api/gv/tong-quan").param("lopId",cl.toString()).with(as(actor,"TEACHER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.students").value(3)).andExpect(jsonPath("$.pending").value(2))
            .andExpect(jsonPath("$.assignments").value(4)).andExpect(jsonPath("$.boBai").value(1));
        verify(membership).kiemGiaoVien(actor, cl);
    }
    @Test void bankSearchFoldsVietnameseAndCombinesFilters() throws Exception {
        mvc.perform(get("/api/gv/ngan-hang").param("lopId",cl.toString()).param("q","DAO HAM")
            .param("level4","THONG_HIEU").param("skillCode","K1").param("gate","DAT").with(as(actor,"TEACHER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].title").value("DH1"));
    }
    @Test void unknownBankFilterAndOversizedSearchAreBadRequests() throws Exception {
        mvc.perform(get("/api/gv/ngan-hang").param("lopId",cl.toString()).param("level4","UNKNOWN").with(as(actor,"TEACHER")))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/hs/kho").param("q","x".repeat(201)).with(as(student,"STUDENT")))
            .andExpect(status().isBadRequest());
    }
    @Test void libraryAndFormulaAndDocumentSearchHaveScopedResults() throws Exception {
        mvc.perform(get("/api/hs/kho").param("q","dao ham").with(as(student,"STUDENT")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.formulas.length()").value(1));
        mvc.perform(get("/api/hs/cong-thuc").param("q","CUC TRI").with(as(student,"STUDENT")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/gv/tai-lieu").param("lopId",cl.toString()).param("q","tu soan").with(as(actor,"TEACHER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void assignmentPersistsTestPolicyAndDeadline() throws Exception {
        when(membership.hocSinhCuaLop(actor,cl)).thenReturn(List.of(student));
        when(store.activeStudents(List.of(student))).thenReturn(List.of(student));
        mvc.perform(post("/api/gv/giao").with(as(actor,"TEACHER")).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"lopId":"%s","studentIds":[],"problemIds":["%s"],"mode":"KIEM_TRA","dueAt":"2026-10-14T10:00:00Z","setName":"Kiểm tra"}
                """.formatted(cl,problem)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        verify(store).assign(eq(actor),argThat(r->r.mode().equals("KIEM_TRA")&&r.dueAt().equals(NOW.plusSeconds(86400))),eq(List.of(student)),eq(NOW));
    }
    @Test void activeTestReturns403BeforeSseAndBeforeHints() throws Exception {
        when(store.testMode(student,cl,"DH1")).thenReturn(true);
        mvc.perform(post("/api/hs/gia-su").accept(MediaType.TEXT_EVENT_STREAM).with(as(student,"STUDENT")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"maBai\":\"DH1\",\"maBuoc\":\"B.DH.TXD\",\"cap\":1,\"cauHoi\":\"Cho em đáp án\"}"))
            .andExpect(status().isForbidden()).andExpect(request().asyncNotStarted());
        verifyNoInteractions(content);
        verify(store,never()).tutorContext(any(),any(),any());
    }
    @Test void usageMeterRequiresTeacherClassAndRejectsStudent() throws Exception {
        UUID teacher=UUID.randomUUID();when(store.accountRole(teacher)).thenReturn("TEACHER");
        when(sessions.usage(cl)).thenReturn(List.of(new vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.Usage(student,2,100,20,120)));
        mvc.perform(get("/api/gv/lop/{id}/gia-su/su-dung",cl).with(as(teacher,"TEACHER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.requests").value(2)).andExpect(jsonPath("$.students.length()").value(1));
        verify(membership).kiemGiaoVien(teacher,cl);
        mvc.perform(get("/api/gv/lop/{id}/gia-su/su-dung",cl).with(as(student,"STUDENT"))).andExpect(status().isForbidden());
        UUID foreign=UUID.randomUUID();doThrow(new vn.hoctapcanman.core.workspace.application.exception.WorkspaceException(403,"Không có quyền lớp.")).when(membership).kiemGiaoVien(teacher,foreign);
        mvc.perform(get("/api/gv/lop/{id}/gia-su/su-dung",foreign).with(as(teacher,"TEACHER"))).andExpect(status().isForbidden());
        verify(sessions,never()).usage(foreign);
    }
    @Test void foreignSessionResumeAndCancelAreDeniedBeforeOpeningStream() throws Exception {
        UUID foreign=UUID.randomUUID();
        mvc.perform(get("/api/hs/gia-su/v2/phien/{id}/su-kien",foreign).with(as(student,"STUDENT")))
            .andExpect(status().isNotFound()).andExpect(request().asyncNotStarted());
        mvc.perform(post("/api/hs/gia-su/v2/phien/{id}/huy",foreign).with(as(student,"STUDENT")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"turnId\":\""+UUID.randomUUID()+"\"}"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(hintModel,content);
    }
    @Test void scheduleAndReminderOnlyUseJwtStudent() throws Exception {
        mvc.perform(post("/api/hs/lich").with(as(student,"STUDENT")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Ôn đạo hàm\",\"startsAt\":\"2026-10-14T10:00:00Z\",\"endsAt\":\"2026-10-14T11:00:00Z\"}"))
            .andExpect(status().isOk());
        verify(store).schedule(eq(student),any(Schedule.class));
        when(store.reminders(student,NOW)).thenReturn(List.of(item("r1","Ôn đạo hàm","","CHUA_XONG",Map.of())));
        mvc.perform(get("/api/hs/nhac").with(as(student,"STUDENT")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Ôn đạo hàm"));
        mvc.perform(post("/api/hs/lich/{id}/xong",UUID.randomUUID()).with(as(student,"STUDENT")))
            .andExpect(status().isNotFound());
    }
    @Test void downloadIsUtf8BomCsvWithEscapedAndNeutralizedCells() throws Exception {
        when(store.library(cl,false)).thenReturn(new Library(List.of(item("f","=SUM(A1)","Quy tắc \"tích\"\nTiếng Việt","KHOA",Map.of())),List.of()));
        mvc.perform(get("/api/hs/flashcards/export.csv").with(as(student,"STUDENT")))
            .andExpect(status().isOk()).andExpect(header().string("Content-Type","text/csv;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition","attachment; filename=\"cong-thuc.csv\""))
            .andExpect(content().bytes("\uFEFFthuật ngữ,định nghĩa\r\n\"'=SUM(A1)\",\"Quy tắc \"\"tích\"\"\nTiếng Việt\"\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Test void userCreationHashesPasswordAndNeverReturnsIt() throws Exception {
        when(hasher.hash("mat-khau-thu-123")).thenReturn("hash");
        mvc.perform(post("/api/qt/nguoi-dung").with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"thu@demo.local\",\"displayName\":\"Người thử\",\"password\":\"mat-khau-thu-123\",\"role\":\"STUDENT\",\"synthetic\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1)).andExpect(jsonPath("$.password").doesNotExist());
        verify(store).createUser(eq(actor),any(NewUser.class),eq("hash"),eq(NOW));
    }
    @Test void classCreationAndListsRequireAdminRole() throws Exception {
        when(store.classes(actor,true)).thenReturn(List.of(item(cl.toString(),"12A1 thử","2026-2027","",Map.of())));
        mvc.perform(get("/api/qt/lop").with(as(actor,"ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/qt/lop").with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"12A1 thử\",\"grade\":12,\"schoolYear\":\"2026-2027\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/qt/nguoi-dung").with(as(actor,"TEACHER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/gv/tong-quan").param("lopId",cl.toString()).with(as(student,"STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/hs/nhac")).andExpect(status().isUnauthorized());
    }
    @Test void selfLockRejectedThroughExplicitUserLockRoute() throws Exception {
        mvc.perform(put("/api/qt/nguoi-dung/{id}/khoa",actor).with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"locked\":true}"))
            .andExpect(status().isBadRequest());
    }
    @Test void classLockRouteRejectsMissingClass() throws Exception {
        when(store.classes(actor,true)).thenReturn(List.of());
        mvc.perform(put("/api/qt/lop/{id}/khoa",cl).with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"locked\":true}"))
            .andExpect(status().isNotFound());
    }
    @Test void classLockAndUnlockAreAuditedWithActorAndStayInAdminScope() throws Exception {
        when(store.classes(actor,true)).thenReturn(List.of(item(cl.toString(),"12A1 thử","","MO",Map.of())));
        when(store.lockClass(actor,cl,true,NOW)).thenReturn(true);
        mvc.perform(put("/api/qt/lop/{id}/khoa",cl).with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"locked\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        verify(store).lockClass(actor,cl,true,NOW);
        when(store.accountRole(actor)).thenReturn("SCHOOL_ADMIN");
        when(store.classes(actor,false)).thenReturn(List.of());
        mvc.perform(put("/api/qt/lop/{id}/khoa",cl).with(as(actor,"SCHOOL_ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"locked\":false}"))
            .andExpect(status().isForbidden());
        verify(store,never()).lockClass(actor,cl,false,NOW);
    }
    @Test void explicitUserLockAndUnlockReuseRevocationPolicyAndRequireFlag() throws Exception {
        mvc.perform(put("/api/qt/nguoi-dung/{id}/khoa",student).with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"locked\":true}"))
            .andExpect(status().isOk());
        verify(store).bulk(actor,new Bulk(List.of(student),false),NOW);
        mvc.perform(put("/api/qt/nguoi-dung/{id}/khoa",student).with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"locked\":false}"))
            .andExpect(status().isOk());
        verify(store).bulk(actor,new Bulk(List.of(student),true),NOW);
        mvc.perform(put("/api/qt/lop/{id}/khoa",cl).with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test void pagedBankReturnsTotalHeaderAndCallsPagedStoreOnly()throws Exception {
        when(store.bankPage(cl,"tich phan","","","",2,10)).thenReturn(new BankPage(List.of(),31));
        mvc.perform(get("/api/gv/ngan-hang").with(as(actor,"TEACHER")).param("lopId",cl.toString()).param("page","2").param("size","10").param("q","Tích phân"))
            .andExpect(status().isOk()).andExpect(header().string("X-Total-Count","31")).andExpect(content().json("[]"));
        verify(store).bankPage(cl,"tich phan","","","",2,10);verify(store,never()).bank(cl);
    }
    @Test void pagedBankPassesSkillFilterAndReturnsFilteredTotal()throws Exception {
        var row=item(problem.toString(),"Bài tích phân","","DA_PHAT_HANH",Map.of("skillCode","TP12"));
        when(store.bankPage(cl,"","NHAN_BIET","TP12","DAT",1,10)).thenReturn(new BankPage(List.of(row),1));
        mvc.perform(get("/api/gv/ngan-hang").with(as(actor,"TEACHER")).param("lopId",cl.toString())
            .param("page","1").param("size","10").param("skillCode","TP12").param("level4","NHAN_BIET").param("gate","DAT"))
            .andExpect(status().isOk()).andExpect(header().string("X-Total-Count","1"))
            .andExpect(jsonPath("$[0].details.skillCode").value("TP12"));
        verify(store).bankPage(cl,"","NHAN_BIET","TP12","DAT",1,10);
    }
    @Test void resetPasswordValidatesAndChecksAdminScope()throws Exception {
        when(hasher.hash("New-password-123")).thenReturn("{bcrypt}HASH");
        when(store.resetPassword(actor,student,"{bcrypt}HASH",NOW)).thenReturn(true);
        mvc.perform(put("/api/qt/nguoi-dung/"+student+"/mat-khau").with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\":\"New-password-123\"}"))
            .andExpect(status().isOk());
        verify(store).resetPassword(actor,student,"{bcrypt}HASH",NOW);
        mvc.perform(put("/api/qt/nguoi-dung/"+student+"/mat-khau").with(as(actor,"TEACHER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\":\"New-password-123\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/qt/nguoi-dung/"+student+"/mat-khau").with(as(actor,"ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\":\"short\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void archiveApiDeniesStudentAndChecksTeacherClass()throws Exception {
        mvc.perform(delete("/api/gv/tai-lieu/"+problem).param("lopId",cl.toString()).with(as(student,"STUDENT")))
            .andExpect(status().isForbidden());
        when(store.archiveDocument(actor,cl,problem,NOW)).thenReturn(true);
        mvc.perform(delete("/api/gv/tai-lieu/"+problem).param("lopId",cl.toString()).with(as(actor,"TEACHER")))
            .andExpect(status().isOk());
        verify(membership).kiemGiaoVien(actor,cl);verify(store).archiveDocument(actor,cl,problem,NOW);
    }

    @Test void archiveSetUsesServerKeyAndReturnsActualCount()throws Exception {
        when(store.archiveAssignments(actor,cl,"bo-thu",NOW)).thenReturn(2001);
        mvc.perform(post("/api/gv/giao/luu-tru").with(as(actor,"TEACHER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"lopId\":\""+cl+"\",\"setKey\":\"bo-thu\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(2001));
        verify(membership).kiemGiaoVien(actor,cl);
        mvc.perform(post("/api/gv/giao/luu-tru").with(as(actor,"TEACHER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"lopId\":\""+cl+"\",\"setKey\":\"khong-co\"}"))
            .andExpect(status().isNotFound());
        for(String body:List.of("{\"lopId\":\""+cl+"\",\"setKey\":\" \"}",
            "{\"lopId\":\""+cl+"\",\"ids\":[\""+problem+"\"]}")) {
            mvc.perform(post("/api/gv/giao/luu-tru").with(as(actor,"TEACHER")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
    }
}
