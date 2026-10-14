package vn.hoctapcanman.core.practice.application.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.port.*;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.practice.application.port.SolutionRevealEvents;
import vn.hoctapcanman.core.practice.domain.model.*;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
class SolutionRevealTest {
    final UUID student=UUID.randomUUID(),cl=UUID.randomUUID(),problem=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-14T00:00:00Z");
    final SubmissionRepository submissions=mock(SubmissionRepository.class);
    final ClassMembership membership=mock(ClassMembership.class);
    final LoiGiaiSauKhiNop solutions=mock(LoiGiaiSauKhiNop.class);
    final BaiDeLam bank=mock(BaiDeLam.class);
    final SolutionRevealEvents events=mock(SolutionRevealEvents.class);
    final vn.hoctapcanman.core.practice.application.port.FullSolutionExplanation explanation=mock(vn.hoctapcanman.core.practice.application.port.FullSolutionExplanation.class);
    final PlatformTransactionManager tx=mock(PlatformTransactionManager.class);
    final MoLoiGiai gate=new MoLoiGiai(submissions,membership,solutions,tx,bank,events,Clock.fixed(now,ZoneOffset.UTC));
    @BeforeEach void setup(){
        gate.explanation(Optional.of(explanation));
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(membership.laHocSinh(student,cl)).thenReturn(true);
        when(bank.bai(cl,"P1")).thenReturn(Optional.of(new BaiChoLamBai(problem,"P1",1,"K1","Kỹ năng","NHAN_BIET","Đề","x","TU_LUAN_5_BUOC","x",List.of(),List.of(),null)));
    }
    Submission submitted(){return new Submission(UUID.randomUUID(),cl,student,problem,1,SubmissionStatus.DA_NOP,false,null,GradeStatus.SAI,UUID.randomUUID(),now.minusSeconds(60),now,new SkillLevel("K1","NHAN_BIET"));}
    @Test void foreignClassNeverReadsSolution(){when(membership.laHocSinh(student,cl)).thenReturn(false);assertThatThrownBy(()->gate.reveal(student,cl,"P1")).isInstanceOf(WorkspaceException.class);verifyNoInteractions(bank,solutions,events);}
    @Test void testBeforeDeadlineNeverReadsSolutionOrHistory(){when(events.blocked(student,cl,problem,now)).thenReturn(true);assertThatThrownBy(()->gate.reveal(student,cl,"P1")).isInstanceOf(WorkspaceException.class);verifyNoInteractions(submissions,solutions,explanation);verify(events,never()).record(any(),any());}
    @Test void unfinishedAttemptNeverReadsSolution(){when(submissions.history(student,problem)).thenReturn(new SubmissionHistory(student,problem,List.of(Submission.open(cl,student,problem,1,now))));assertThatThrownBy(()->gate.reveal(student,cl,"P1")).isInstanceOf(WorkspaceException.class);verifyNoInteractions(solutions);}
    @Test void completedWrongAttemptCanRevealAndRecordsWithoutUpdatingMastery(){var s=submitted();when(submissions.history(student,problem)).thenReturn(new SubmissionHistory(student,problem,List.of(s)));when(solutions.vanBan(cl,problem,1)).thenReturn(Optional.of("Lời giải giáo viên"));assertThat(gate.reveal(student,cl,"P1").loiGiai()).isEqualTo("Lời giải giáo viên");verify(events).record(s.id(),now);}
    @Test void retryOfSameVersionInAnotherClassClosesGate(){var s=submitted();when(submissions.history(student,problem)).thenReturn(new SubmissionHistory(student,problem,List.of(s,Submission.open(UUID.randomUUID(),student,problem,1,now))));assertThatThrownBy(()->gate.reveal(student,cl,"P1")).isInstanceOf(WorkspaceException.class);verifyNoInteractions(solutions);}
    @Test void verifiedModelExplanationIsUsedOnlyAfterCompleteAttempt(){var s=submitted();when(submissions.history(student,problem)).thenReturn(new SubmissionHistory(student,problem,List.of(s)));when(solutions.vanBan(cl,problem,1)).thenReturn(Optional.of("Lời giải giáo viên"));when(explanation.explain(eq(cl),any())).thenReturn(Optional.of("Lời giải đã qua bộ kiểm"));var result=gate.reveal(student,cl,"P1");assertThat(result.nguon()).isEqualTo("MO_HINH");assertThat(result.loiGiai()).isEqualTo("Lời giải đã qua bộ kiểm");verify(events).record(s.id(),now);}
    @Test void changedOrMissingSolutionDoesNotRecordReveal(){var s=submitted();when(submissions.history(student,problem)).thenReturn(new SubmissionHistory(student,problem,List.of(s)));assertThatThrownBy(()->gate.reveal(student,cl,"P1")).isInstanceOf(WorkspaceException.class);verify(events,never()).record(any(),any());}
}
