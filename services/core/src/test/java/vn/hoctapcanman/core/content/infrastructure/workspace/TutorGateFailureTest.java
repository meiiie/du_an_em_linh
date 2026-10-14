package vn.hoctapcanman.core.content.infrastructure.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.hoctapcanman.core.content.domain.model.Solution;
import vn.hoctapcanman.core.content.domain.repository.*;
import vn.hoctapcanman.core.shared.infrastructure.math.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

class TutorGateFailureTest {
    final UUID cl=UUID.randomUUID(),problem=UUID.randomUUID();
    final SolutionRepository solutions=mock(SolutionRepository.class);
    final MathServiceClient math=mock(MathServiceClient.class);
    final JdbcClient db=mock(JdbcClient.class,RETURNS_DEEP_STUBS);
    final ContentWorkflowAdapter gate=new ContentWorkflowAdapter(db,mock(VerificationRunRepository.class),
        mock(ProblemRepository.class),mock(ProblemReleaseRepository.class),mock(FormulaSheetRepository.class),mock(DocumentRepository.class),solutions,math,Clock.systemUTC());
    @BeforeEach void setup(){when(solutions.findByProblemId(problem)).thenReturn(Optional.of(new Solution(problem,null,"[\"dữ kiện tổng hợp\"]",null)));}
    Optional<String> check(){return gate.sanitize(cl,problem,"Em cần tìm điều gì?","Em bị kẹt",List.of());}
    @Test void contentRejectionIsEmptyAndStopsBeforeSecondGate(){when(math.call(eq(MathJob.FILTER),anyMap())).thenReturn(new MathResult.Ok(Map.of("loi",false,"cho_phep",false)));assertTrue(check().isEmpty());verify(math,never()).call(eq(MathJob.KIEM_LOI_GIANG),anyMap());}
    @Test void missingEvidenceIsAnErrorNotAnInstructionToRegenerate(){when(solutions.findByProblemId(problem)).thenReturn(Optional.empty());assertEquals(503,assertThrows(WorkspaceException.class,this::check).status());verifyNoInteractions(math);}
    @Test void malformedFilterResponseIsInfrastructureError(){when(math.call(eq(MathJob.FILTER),anyMap())).thenReturn(new MathResult.Ok(Map.of("loi",false)));assertEquals(503,assertThrows(WorkspaceException.class,this::check).status());}
    @Test void failedOrMalformedExplanationIsInfrastructureError(){
        when(math.call(eq(MathJob.FILTER),anyMap())).thenReturn(new MathResult.Ok(Map.of("loi",false,"cho_phep",true)));
        for(Map<String,Object> response:List.<Map<String,Object>>of(Map.of(),Map.of("loi","KIEM_LOI","thay_bang_goi_y",true,"cau_sach",""))) {
            when(math.call(eq(MathJob.KIEM_LOI_GIANG),anyMap())).thenReturn(new MathResult.Ok(response));assertEquals(503,assertThrows(WorkspaceException.class,this::check).status());
        }
    }
    @Test void obsoleteGateNoLongerProducesReviewItems() {
        when(math.call(eq(MathJob.FILTER),anyMap())).thenReturn(new MathResult.Ok(Map.of("loi",false,"cho_phep",true)));
        when(math.call(eq(MathJob.KIEM_LOI_GIANG),anyMap())).thenReturn(new MathResult.Ok(Map.of("loi",false,"thay_bang_goi_y",true,"cau_sach","")));
        assertTrue(check().isEmpty());verifyNoInteractions(db);
    }
    @Test void validWholeSentenceReachesBothGates(){
        when(math.call(eq(MathJob.FILTER),anyMap())).thenReturn(new MathResult.Ok(Map.of("loi",false,"cho_phep",true)));
        when(math.call(eq(MathJob.KIEM_LOI_GIANG),anyMap())).thenReturn(new MathResult.Ok(Map.of("thay_bang_goi_y",false,"cau_sach","Em cần tìm điều gì?")));
        assertEquals(Optional.of("Em cần tìm điều gì?"),check());verify(math).call(eq(MathJob.FILTER),anyMap());verify(math).call(eq(MathJob.KIEM_LOI_GIANG),anyMap());
    }
}
