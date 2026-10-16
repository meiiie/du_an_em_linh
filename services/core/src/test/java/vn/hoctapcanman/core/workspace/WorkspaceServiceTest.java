package vn.hoctapcanman.core.workspace;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.service.WorkspaceService;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
class WorkspaceServiceTest {
 final WorkspaceStore store=mock(WorkspaceStore.class);final ClassMembership members=mock(ClassMembership.class);final ContentWorkflow gate=mock(ContentWorkflow.class);final PasswordHasher hasher=mock(PasswordHasher.class);
 final Instant now=Instant.parse("2026-10-13T00:00:00Z");final WorkspaceService service=new WorkspaceService(store,members,gate,hasher,Clock.fixed(now,ZoneOffset.UTC));
 final UUID actor=UUID.randomUUID(),cl=UUID.randomUUID(),student=UUID.randomUUID(),problem=UUID.randomUUID();
 @Test void deniesOtherClassBeforeReading(){doThrow(new WorkspaceException(403,"Không thuộc lớp")).when(members).kiemGiaoVien(actor,cl);assertThrows(WorkspaceException.class,()->service.dashboard(actor,cl));verifyNoInteractions(store);}
 @Test void assignmentDeniesForeignStudent(){when(members.hocSinhCuaLop(actor,cl)).thenReturn(List.of(student));Assign request=new Assign(cl,List.of(UUID.randomUUID()),List.of(problem),"KIEM_TRA",null,"");assertThrows(WorkspaceException.class,()->service.assign(actor,request));verifyNoInteractions(store);}
 @Test void assignmentRejectsPastDeadline(){assertThrows(WorkspaceException.class,()->service.assign(actor,new Assign(cl,List.of(),List.of(problem),"ON_TAP",now.minusSeconds(1),"")));verifyNoInteractions(store);}
 @Test void classAssignmentExpandsMembershipAndKeepsMode(){when(store.activeStudents(List.of(student))).thenReturn(List.of(student));when(members.hocSinhCuaLop(actor,cl)).thenReturn(List.of(student));when(store.bank(cl)).thenReturn(List.of(new Item(problem.toString(),"","","DA_PHAT_HANH",Map.of())));Assign r=new Assign(cl,List.of(),List.of(problem),"KIEM_TRA",now.plusSeconds(3600),"Ôn tập");assertEquals(1,service.assign(actor,r).count());verify(store).assign(actor,r,List.of(student),now);}
 @Test void overrideDeniesStudentOutsideClass(){assertThrows(WorkspaceException.class,()->service.override(actor,new MasteryOverride(cl,student,"K1","VAN_DUNG","Đã kiểm tra")));verifyNoInteractions(store);}
 @Test void overrideCarriesActorAndReason(){when(members.laHocSinh(student,cl)).thenReturn(true);MasteryOverride r=new MasteryOverride(cl,student,"K1","VAN_DUNG","Đã kiểm tra trên lớp");service.override(actor,r);verify(store).override(actor,r,now);}
 @Test void csvEscapesQuotesAndNewlines(){assertEquals("\"Đạo hàm \"tích\"\nquy tắc\"".replace("\"tích\"","\"\"tích\"\""),WorkspaceService.csvCell("Đạo hàm \"tích\"\nquy tắc"));}
 @Test void csvNeutralizesFormulaInjection(){assertEquals("\"'=1+1\"",WorkspaceService.csvCell("=1+1"));assertEquals("\"' @SUM(A1)\"",WorkspaceService.csvCell(" @SUM(A1)"));}
 @Test void csvNeutralizesMultilineFormula(){assertEquals("\"'=1+1\nX\"",WorkspaceService.csvCell("=1+1\nX"));}
 @Test void todaysGeneratedSessionPrecedesOtherReminders(){
  var at=Instant.parse("2026-10-14T08:00:00Z");
  var s=new WorkspaceService(store,members,gate,hasher,Clock.fixed(at,ZoneOffset.UTC));
  when(members.lopHoc(student)).thenReturn(Optional.of(cl));
  when(store.reminders(student,at)).thenReturn(List.of(new Item("other1","Hạn ngày mai","","ON_TAP",Map.of()),new Item("other2","Hạn khác","","ON_TAP",Map.of())));
  var rows=s.reminders(student);
  assertEquals("GOI_Y",rows.getFirst().status());
  assertEquals("2026-10-14T12:30:00Z",rows.getFirst().details().get("startsAt"));
  assertEquals(3,rows.size());
 }
 @Test void weeklyPlanNeedsOwnClassAndValidWeekRange(){
  assertThrows(WorkspaceException.class,()->service.weeklyPlan(student,53));
  verifyNoInteractions(store,members);
  assertThrows(WorkspaceException.class,()->service.weeklyPlan(student,0));
  verifyNoInteractions(store);
 }
 @Test void stuckAlertsCheckTeacherBeforeReading(){
  doThrow(new WorkspaceException(403,"Không thuộc lớp")).when(members).kiemGiaoVien(actor,cl);
  assertThrows(WorkspaceException.class,()->service.stuckAlerts(actor,cl));verifyNoInteractions(store);
 }
 @Test void scheduleRejectsSubMinuteAndOverEightHours(){assertThrows(WorkspaceException.class,()->service.schedule(student,new Schedule("Học",now,now.plusSeconds(30))));assertThrows(WorkspaceException.class,()->service.schedule(student,new Schedule("Học",now,now.plusSeconds(8*3600+1))));verifyNoInteractions(store);}
 @Test void schoolAdminCannotDisableOutsideClass(){when(store.accountRole(actor)).thenReturn("SCHOOL_ADMIN");assertThrows(WorkspaceException.class,()->service.bulk(actor,new Bulk(List.of(student),false)));verify(store,never()).bulk(any(),any(),any());}
 @Test void cannotDisableSelf(){assertThrows(WorkspaceException.class,()->service.bulk(actor,new Bulk(List.of(actor),false)));verifyNoInteractions(store);}
 @Test void rejectsRealStudentIntake(){assertThrows(WorkspaceException.class,()->service.createUser(actor,new NewUser("demo@example.test","Demo","long-password","STUDENT",false)));verifyNoInteractions(hasher,store);}
 @Test void scheduleChecksTimeRange(){assertThrows(WorkspaceException.class,()->service.schedule(student,new Schedule("Học",now,now.minusSeconds(1))));verifyNoInteractions(store);}

 @Test void archiveAssignmentChecksClassBeforeWriting(){
  doThrow(new WorkspaceException(403,"Không thuộc lớp")).when(members).kiemGiaoVien(actor,cl);
  assertThrows(WorkspaceException.class,()->service.archiveAssignments(actor,new ArchiveSet(cl,"bo-thu")));
  verifyNoInteractions(store);
 }
 @Test void archiveDocumentChecksClassAndMissingResource(){
  assertThrows(WorkspaceException.class,()->service.archiveDocument(actor,cl,problem));
  verify(members).kiemGiaoVien(actor,cl);verify(store).archiveDocument(actor,cl,problem,now);
 }
 @Test void archiveUserChecksScopeAndProtectsSelf(){
  when(store.accountRole(actor)).thenReturn("ADMIN");
  assertThrows(WorkspaceException.class,()->service.archiveUser(actor,actor));
  verify(store,never()).archiveUser(any(),any(),any());
  when(store.archiveUser(actor,student,now)).thenReturn(true);
  assertEquals(1,service.archiveUser(actor,student).count());
 }
 @Test void passwordResetHashesAndDoesNotLogRawPassword(){
  when(store.accountRole(actor)).thenReturn("ADMIN");when(hasher.hash("New-password-123")).thenReturn("{bcrypt}HASH");
  when(store.resetPassword(actor,student,"{bcrypt}HASH",now)).thenReturn(true);
  assertEquals(1,service.resetPassword(actor,student,new ResetPassword("New-password-123")).count());
  verify(store).resetPassword(actor,student,"{bcrypt}HASH",now);
  assertFalse(new ResetPassword("New-password-123").toString().contains("New-password"));
 }
 @Test void schoolAdminCannotResetOutsideScopeOrUseOversizedUtf8(){
  when(store.accountRole(actor)).thenReturn("SCHOOL_ADMIN");
  assertThrows(WorkspaceException.class,()->service.resetPassword(actor,student,new ResetPassword("New-password-123")));
  when(store.accountRole(actor)).thenReturn("ADMIN");
  assertThrows(WorkspaceException.class,()->service.resetPassword(actor,student,new ResetPassword("ấ".repeat(30))));
  verifyNoInteractions(hasher);verify(store,never()).resetPassword(any(),any(),any(),any());
 }

 @Test void bankPaginationRejectsInvalidSizeAndDelegatesNormalizedQuery(){
  assertThrows(WorkspaceException.class,()->service.bankPage(actor,cl,"","","","",1,1000));
  verify(store,never()).bankPage(any(),any(),any(),any(),any(),anyInt(),anyInt());
  when(store.bankPage(cl,"tich phan","","","",2,10)).thenReturn(new BankPage(List.of(),0));
  assertEquals(0,service.bankPage(actor,cl,"Tích phân","","","",2,10).total());
  verify(store).bankPage(cl,"tich phan","","","",2,10);
 }
}
