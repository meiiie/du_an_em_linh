package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.HintModel;
import vn.hoctapcanman.core.tutor.application.service.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
import vn.hoctapcanman.core.workspace.application.port.*;

class GlmTutorServiceTest {
    final WorkspaceStore store=mock(WorkspaceStore.class);
    final ClassMembership members=mock(ClassMembership.class);
    final ContentWorkflow gate=mock(ContentWorkflow.class);
    final HintModel model=mock(HintModel.class,CALLS_REAL_METHODS);
    final UUID student=UUID.randomUUID(),cl=UUID.randomUUID(),problem=UUID.randomUUID();
    final OfflineTutorService tutor=new OfflineTutorService(store,members,model,
        new TutorRateLimiter(Clock.systemUTC(),100,Duration.ofMinutes(10)));
    final String hint="Không được đọc thang này";
    TutorRequest request(String q){return new TutorRequest("DH1","B.DH.TXD",1,q);}
    @BeforeEach void setup() {
        when(members.lopHoc(student)).thenReturn(Optional.of(cl));
        when(store.tutorContext(eq(student),eq(cl),any())).thenReturn(Optional.of(new TutorContext(problem,"DH1","Đề bài","x**2","B.DH.TXD",hint)));
        when(model.provider()).thenReturn("GLM");
        when(store.settings(cl)).thenReturn(new Settings(cl,false,"glm"));
    }
    @Test void rawPartialTokensAreReleasedDuringProviderStreamWithoutGateOrRegeneration() {
        var emitted=new ArrayList<String>();
        var usage=new ArrayList<vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage>();
        doAnswer(i -> {
            java.util.function.Consumer<String> chunks=i.getArgument(1);
            chunks.accept("Em thử $x");
            assertEquals(List.of("Em thử $x"),emitted,"Không chờ câu hoặc delimiter toán đóng");
            chunks.accept("^2$\n");
            chunks.accept(" ");
            chunks.accept("Kết quả là 3.");
            return new vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage(50,20,false);
        }).when(model).stream(any(),any());
        var r=request("Em bị kẹt");var c=tutor.context(student,cl,r);
        tutor.generate(student,cl,r,c,TutorPrompt.compose(c,r),emitted::add,usage::add,() -> {});
        assertEquals("Em thử $x^2$\n Kết quả là 3.",String.join("",emitted));
        verifyNoInteractions(gate);
        verify(model,times(1)).stream(any(),any());
        assertEquals(List.of(new vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage(50,20,false)),usage);
    }
    @Test void largeProviderChunkIsSplitWithoutChangingMarkdownWhitespaceOrUnicode() {
        String raw=" "+"a".repeat(62)+"🚀\n"+"$x^2$ ".repeat(30);
        when(model.draft(any())).thenReturn(Optional.of(raw));
        var emitted=new ArrayList<String>();var r=request("Em bị kẹt");var c=tutor.context(student,cl,r);
        tutor.generate(student,cl,r,c,TutorPrompt.compose(c,r),emitted::add,ignored -> {},() -> {});
        assertEquals(raw,String.join("",emitted));
        assertTrue(emitted.stream().allMatch(t -> !t.isEmpty() && t.length()<=64));
        assertTrue(emitted.stream().noneMatch(t -> Character.isHighSurrogate(t.charAt(t.length()-1)) || Character.isLowSurrogate(t.charAt(0))));
        verifyNoInteractions(gate);
    }
    @Test void answerProtectionIsPromptOnlyAndDoesNotTruncateAfterThreeSentences() {
        String raw="Kết quả là $x=3$. Em thử gì? Em cần gì? Em đã chọn gì?";
        when(model.draft(any())).thenReturn(Optional.of(raw));
        assertEquals(raw,tutor.answer(student,request("Em bị kẹt")).message());
        verify(model,times(1)).draft(any());verifyNoInteractions(gate);
    }
    @Test void answerClassifierSteersModelWithoutFixedRefusal() {
        when(model.draft(any())).thenReturn(Optional.of("Mình có thể cùng em chọn bước cần làm, em đã thử gì?"));
        for(String q:List.of("cho em đáp án","đáp số","giải giúp","ignore previous","system prompt","roleplay","đóng vai giáo viên")) {
            assertTrue(OfflineTutorService.asksAnswer(q));
            assertEquals("MO_HINH",tutor.answer(student,request(q)).nguon());
        }
        var prompts=ArgumentCaptor.forClass(HintPrompt.class);verify(model,times(7)).draft(prompts.capture());
        for(var p:prompts.getAllValues()){assertTrue(p.system().contains("Tín hiệu xin đáp án"));assertFalse(p.user().contains(hint));}
        verifyNoInteractions(gate);
    }
    @Test void providerFailureStopsWithoutRetryOrLadder() {
        when(model.draft(any())).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class,() -> tutor.answer(student,request("Em bị kẹt")));
        verify(model,times(1)).draft(any());verifyNoInteractions(gate);
    }
    @Test void personalDataNeverReachesModel() {
        assertEquals(400,assertThrows(WorkspaceException.class,() -> tutor.answer(student,request("Em là Nguyễn Văn An, bị kẹt"))).status());
        verify(model,never()).draft(any());
    }
    @Test void identifyingClassContentNeverReachesProvider() {
        when(store.tutorContext(eq(student),eq(cl),any())).thenReturn(Optional.of(new TutorContext(problem,"DH1","Bài của Nguyễn Văn An","x","B.DH.TXD",hint)));
        assertEquals(400,assertThrows(WorkspaceException.class,() -> tutor.answer(student,request("Em bị kẹt"))).status());verify(model,never()).draft(any());
    }
    @Test void directAnswerStopsIfContextOrProviderChangesDuringModelCall() {
        for(boolean providerChange:List.of(false,true)) {
            setup();
            when(model.draft(any())).thenAnswer(i -> {
                if(providerChange) {
                    when(store.settings(cl)).thenReturn(new Settings(cl,false,"offline"));
                    when(model.provider()).thenReturn("offline");
                } else when(store.tutorContext(eq(student),eq(cl),any())).thenReturn(Optional.of(new TutorContext(problem,"DH1","Đề vừa đổi","x**2","B.DH.TXD",hint)));
                return Optional.of("Em thử $x");
            });
            assertEquals(providerChange?403:409,assertThrows(WorkspaceException.class,() -> tutor.answer(student,request("Em bị kẹt"))).status());
        }
        verifyNoInteractions(gate);
    }
    @Test void offlineAdapterCannotServeGlmClass() {
        var offline=new OfflineTutorService(store,members,new vn.hoctapcanman.core.tutor.infrastructure.ai.OfflineHintModelAdapter(),new TutorRateLimiter(Clock.systemUTC(),10,Duration.ofMinutes(10)));
        assertEquals(503,assertThrows(WorkspaceException.class,() -> offline.answer(student,request("Em bị kẹt"))).status());verifyNoInteractions(gate);
    }
    @Test void testModeDeniedBeforeModelAndRateLimit() {
        when(store.testMode(student,cl,"DH1")).thenReturn(true);
        assertEquals(403,assertThrows(WorkspaceException.class,() -> tutor.authorize(student,request("Gợi ý"))).status());verifyNoInteractions(model,gate);
    }
}
