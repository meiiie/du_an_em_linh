package vn.hoctapcanman.core.practice.infrastructure.client;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import vn.hoctapcanman.core.tutor.application.port.HintModel;
import vn.hoctapcanman.core.practice.application.port.SolutionRevealEvents;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.workspace.application.port.WorkspaceStore;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Library;
import vn.hoctapcanman.core.shared.infrastructure.math.*;
class FullSolutionExplanationTest {
 final UUID cl=UUID.randomUUID();final HintModel model=mock(HintModel.class);final WorkspaceStore workspace=mock(WorkspaceStore.class);final MathServiceClient math=mock(MathServiceClient.class);final SolutionRevealEvents events=mock(SolutionRevealEvents.class);
 final FullSolutionExplanationAdapter adapter=new FullSolutionExplanationAdapter(model,workspace,math,events,Clock.systemUTC());
 final BaiChoLamBai problem=new BaiChoLamBai(UUID.randomUUID(),"P1",1,"K1","Kỹ năng","NHAN_BIET","Đề công khai","x^2","TU_LUAN_5_BUOC","x**2",List.of(),List.of(),null);
 final String json="{\"TXD\":\"Tập xác định R.\",\"DAOHAM\":\"Đạo hàm 2x.\",\"NGHIEM\":\"Nghiệm 0.\",\"XETDAU\":\"Xét dấu.\",\"KETLUAN\":\"Kết luận.\"}";
 final String full="Tập xác định R.\nĐạo hàm 2x.\nNghiệm 0.\nXét dấu.\nKết luận.";
 @BeforeEach void setup(){when(model.provider()).thenReturn("GLM");when(workspace.library(cl,false)).thenReturn(new Library(List.of(),List.of()));when(model.draft(any())).thenReturn(Optional.of(json));}
 @Test void offlineDoesNotCallModelOrMath(){when(model.provider()).thenReturn("offline");assertThat(adapter.explain(cl,problem)).isEmpty();verify(model,never()).draft(any());verifyNoInteractions(math,events);}
 @Test void providerFailureAndIncompleteStepsFallBack(){when(model.draft(any())).thenReturn(Optional.empty(),Optional.of("{\"TXD\":\"R\"}"));assertThat(adapter.explain(cl,problem)).isEmpty();assertThat(adapter.explain(cl,problem)).isEmpty();verifyNoInteractions(math);}
 @Test void uncheckedOrModifiedTextNeverLeavesGate(){when(math.call(eq(MathJob.KIEM_LOI_GIANG),any())).thenReturn(new MathResult.Failed(MathResult.Reason.UNAVAILABLE,"Unavailable"),new MathResult.Ok(Map.of("thay_bang_goi_y",false,"cau_sach","Xét dấu.")));assertThat(adapter.explain(cl,problem)).isEmpty();assertThat(adapter.explain(cl,problem)).isEmpty();}
 @Test void verifiedFullTextUsesOnlyPublishedProblemAndMetersRequest(){when(math.call(eq(MathJob.KIEM_LOI_GIANG),any())).thenReturn(new MathResult.Ok(Map.of("loi",false,"thay_bang_goi_y",false,"cau_sach",full)));assertThat(adapter.explain(cl,problem)).contains(full);verify(model).draft(argThat(p->p.user().contains("Đề công khai")&&!p.user().contains(cl.toString())&&!p.user().contains("Lời giải giáo viên")));verify(events).modelCall(eq(cl),eq("GLM"),any());}
}
