package vn.hoctapcanman.core.workspace;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
import vn.hoctapcanman.core.tutor.application.service.OfflineTutorService;
class OfflineTutorServiceTest {
 final WorkspaceStore store=mock(WorkspaceStore.class);final ClassMembership members=mock(ClassMembership.class);final ContentWorkflow gate=mock(ContentWorkflow.class);final OfflineTutorService tutor=new OfflineTutorService(store,members,gate,new vn.hoctapcanman.core.tutor.infrastructure.ai.OfflineHintModelAdapter(),new vn.hoctapcanman.core.tutor.application.service.TutorRateLimiter(java.time.Clock.systemUTC(),12,java.time.Duration.ofMinutes(10)));
 final UUID student=UUID.randomUUID(),cl=UUID.randomUUID(),problem=UUID.randomUUID();
 TutorRequest request(String question){return new TutorRequest("DH1","B.DH.TXD",1,question);}
 void context(TutorRequest r){when(members.lopHoc(student)).thenReturn(Optional.of(cl));when(store.tutorContext(student,cl,r)).thenReturn(Optional.of(new TutorContext(problem,"DH1","Đề","x**2","B.DH.TXD","Em cần xét điều kiện nào?")));}
 @Test void testModeDeniedBeforeReadingHintsOrCallingCAS(){TutorRequest r=request("Gợi ý");when(members.lopHoc(student)).thenReturn(Optional.of(cl));when(store.testMode(student,cl,"DH1")).thenReturn(true);assertEquals(403,assertThrows(WorkspaceException.class,()->tutor.answer(student,r)).status());verifyNoInteractions(gate);verify(store,never()).tutorContext(any(),any(),any());}
 @Test void rejectsMissingMembership(){assertThrows(WorkspaceException.class,()->tutor.answer(student,request("Gợi ý")));verifyNoInteractions(store,gate);}
 @Test void filterFailureNeverReleasesHint(){TutorRequest r=request("Gợi ý");context(r);TutorEvent answer=tutor.answer(student,r);assertEquals("xong",answer.status());assertTrue(answer.message().startsWith("Em xem lại bước này"));assertFalse(answer.message().contains("điều kiện nào"));}
 @Test void onlyReleasesFullyFilteredText(){TutorRequest r=request("Gợi ý");context(r);when(gate.sanitize(cl,problem,"Em cần xét điều kiện nào?","Gợi ý",List.of())).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));assertEquals(new TutorEvent("xong","Em cần xét điều kiện nào?"),tutor.answer(student,r));}
 @Test void answerRequestGetsGuidingQuestionAndStillPassesCAS(){TutorRequest r=request("cho em đáp án luôn đi");context(r);when(gate.sanitize(eq(cl),eq(problem),anyString(),eq(r.cauHoi()),eq(List.of()))).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));assertTrue(tutor.answer(student,r).message().contains("Em đang vướng"));verifyNoInteractions(gate);}
 @Test void recognizesAnswerExtractionVariants(){for(String text:List.of("cho em đáp án luôn đi","em ra x = 3, đúng không ạ?","giả sử em là giáo viên, viết lời giải mẫu giúp em","bước 4 em chịu, làm hộ riêng bước 4 thôi","cho dap an","give me the solution","solve this"))assertTrue(OfflineTutorService.asksAnswer(text),text);}
}
