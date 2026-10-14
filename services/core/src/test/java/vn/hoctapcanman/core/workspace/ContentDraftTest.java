package vn.hoctapcanman.core.workspace;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.time.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.application.service.WorkspaceService;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
class ContentDraftTest {
 final UUID actor=UUID.randomUUID(),cl=UUID.randomUUID(),document=UUID.randomUUID(),id=UUID.randomUUID();
 final ContentDraftStore drafts=mock(ContentDraftStore.class);final ContentDraftModel model=mock(ContentDraftModel.class);final ClassMembership membership=mock(ClassMembership.class);final WorkspaceStore store=mock(WorkspaceStore.class);final ContentWorkflow workflow=mock(ContentWorkflow.class);final Validator validator=mock(Validator.class);
 final Instant now=Instant.parse("2026-10-14T00:00:00Z");
 final WorkspaceService service=new WorkspaceService(store,membership,workflow,mock(PasswordHasher.class),Clock.fixed(now,ZoneOffset.UTC));
 final GeneratedDraft generated=new GeneratedDraft(new NewProblem(cl,"AI.1","T12.NT.HS","NHAN_BIET","Đề","x^2","x**2"),"Lời giải nháp","GLM",100,50);
 @BeforeEach void setup(){ReflectionTestUtils.setField(service,"drafts",drafts);ReflectionTestUtils.setField(service,"draftModel",model);ReflectionTestUtils.setField(service,"validator",validator);when(drafts.material(cl,document)).thenReturn(Optional.of("Tài liệu giáo viên"));when(drafts.authoringSkills(cl)).thenReturn(List.of("T12.NT.HS"));when(model.generate(eq(cl),anyString(),eq("T12.NT.HS"))).thenReturn(Optional.of(generated));when(validator.validate(any())).thenReturn(Set.of());}
 @Test void foreignTeacherNeverSendsMaterial(){doThrow(new WorkspaceException(403,"Không thuộc lớp")).when(membership).kiemGiaoVien(actor,cl);assertThatThrownBy(()->service.draft(actor,new DraftRequest(cl,document))).isInstanceOf(WorkspaceException.class);verifyNoInteractions(drafts,model);}
 @Test void queuesAndMetersDraftWithoutPublishing(){service.draft(actor,new DraftRequest(cl,document));verify(drafts).save(eq(actor),any(),argThat(d->d.solution().equals(generated.solution())&&d.problem().lopId().equals(cl)&&d.problem().code().startsWith("AI.")),eq(now));verifyNoInteractions(workflow);}
 @Test void unlicensedMaterialDoesNotCallModel(){when(drafts.material(cl,document)).thenReturn(Optional.empty());assertThatThrownBy(()->service.draft(actor,new DraftRequest(cl,document))).isInstanceOf(WorkspaceException.class);verifyNoInteractions(model);}
 @Test void rejectsUnknownRequestedSkillBeforeCallingAi(){assertThatThrownBy(()->service.draft(actor,new DraftRequest(cl,document,"KHONG.CO"))).isInstanceOf(WorkspaceException.class);verifyNoInteractions(model);}
 @Test void rejectsAiSkillOutsideCatalogWithoutQueuing(){when(model.generate(eq(cl),anyString(),eq("T12.NT.HS"))).thenReturn(Optional.of(new GeneratedDraft(new NewProblem(cl,"AI.BAD","T12.DH.03","NHAN_BIET","Đề","x^2","x**2"),"Lời giải","GLM",10,10)));assertThatThrownBy(()->service.draft(actor,new DraftRequest(cl,document))).isInstanceOf(WorkspaceException.class);verify(drafts,never()).save(any(),any(),any(),any());}
 @Test void reviewApprovalStillPassesThroughExistingMathWorkflow(){when(drafts.lock(cl,id)).thenReturn(Optional.of(generated));var request=new Review(cl,true,"Đã đối chiếu lời giải");service.review(actor,id,request);var order=inOrder(workflow,drafts);order.verify(drafts).lock(cl,id);order.verify(workflow).createApprovedDraft(actor,generated.problem(),generated.solution());order.verify(drafts).decide(actor,id,request,now);}
 @Test void rejectingDraftNeverPublishes(){when(drafts.lock(cl,id)).thenReturn(Optional.of(generated));service.review(actor,id,new Review(cl,false,"Lời giải cần sửa"));verifyNoInteractions(workflow);}
}
