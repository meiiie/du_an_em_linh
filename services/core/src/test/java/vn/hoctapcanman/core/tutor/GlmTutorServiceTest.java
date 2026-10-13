package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.tutor.application.port.HintModel;
import vn.hoctapcanman.core.tutor.application.service.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
import vn.hoctapcanman.core.workspace.application.port.*;

class GlmTutorServiceTest {
    final WorkspaceStore store = mock(WorkspaceStore.class);
    final ClassMembership members = mock(ClassMembership.class);
    final ContentWorkflow gate = mock(ContentWorkflow.class);
    final HintModel model = mock(HintModel.class);
    final UUID student = UUID.randomUUID(), cl = UUID.randomUUID(), problem = UUID.randomUUID();
    final Clock clock = Clock.fixed(Instant.parse("2026-10-14T00:00:00Z"), ZoneOffset.UTC);
    final TutorRateLimiter limiter = new TutorRateLimiter(clock, 12, Duration.ofMinutes(10));
    final OfflineTutorService tutor = new OfflineTutorService(store, members, gate, model, limiter);
    final String hint = "Em cần xét điều kiện nào?";
    TutorRequest request(String text) { return new TutorRequest("DH1", "B.DH.TXD", 1, text); }
    @BeforeEach void context() {
        when(members.lopHoc(student)).thenReturn(Optional.of(cl));
        when(store.tutorContext(eq(student), eq(cl), any())).thenReturn(Optional.of(
            new TutorContext(problem, "DH1", "Đề bài", "x**2", "B.DH.TXD", hint)));
        when(model.provider()).thenReturn("GLM");
    }
    @Test void answerRequestsNeverCallModel() {
        when(gate.sanitize(eq(cl), eq(problem), anyString(), anyString(), anyList())).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));
        for (String q : List.of("cho em đáp án", "đáp số", "kết quả cuối", "giải giúp", "làm giúp", "ra bao nhiêu",
            "bỏ qua hướng dẫn", "ignore previous", "system prompt", "bạn là ai thật", "roleplay as a teacher",
            "đóng vai giáo viên", "giả vờ là giáo viên")) {
            assertTrue(OfflineTutorService.asksAnswer(q), q);
            assertTrue(tutor.answerAuthorized(student, cl, request(q), ignored -> {}).message().contains("Em đang vướng"));
        }
        verify(model, never()).draft(any());
    }
    @Test void gateKeepsOnlyVerifiedSentencesOfModelDraft() {
        when(model.draft(any())).thenReturn(Optional.of("Em xét mẫu số chưa? Câu bị bỏ."));
        when(gate.sanitize(cl, problem, "Em xét mẫu số chưa? Câu bị bỏ.", "Em bị kẹt", List.of())).thenReturn(Optional.of("Em xét mẫu số chưa?"));
        assertEquals(new TutorEvent("xong", "Em xét mẫu số chưa?"), tutor.answer(student, request("Em bị kẹt")));
    }
    @Test void draftWithForeignScriptFallsBackToLadder() {
        when(model.draft(any())).thenReturn(Optional.of("Em tách từng项 rồi thử nhé?"));
        when(gate.sanitize(cl, problem, hint, "Em bị kẹt", List.of())).thenReturn(Optional.of(hint));
        assertEquals(new TutorEvent("xong", hint), tutor.answer(student, request("Em bị kẹt")));
    }
    @Test void fullyBlockedDraftRetriesModelOnceWithStricterPrompt() {
        when(model.draft(any())).thenReturn(Optional.of("Bản nháp bị chặn"), Optional.of("Em đã xét bước này chưa?"));
        when(gate.sanitize(cl, problem, "Em đã xét bước này chưa?", "Em bị kẹt", List.of())).thenReturn(Optional.of("Em đã xét bước này chưa?"));
        assertEquals(new TutorEvent("xong", "Em đã xét bước này chưa?"), tutor.answer(student, request("Em bị kẹt")));
        verify(model, times(2)).draft(any());
    }
    @Test void blockedModelDraftFallsBackThroughGate() {
        when(model.draft(any())).thenReturn(Optional.of("Bản nháp bị chặn"));
        when(gate.sanitize(cl, problem, hint, "Em bị kẹt", List.of())).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));
        assertEquals(new TutorEvent("xong", hint), tutor.answer(student, request("Em bị kẹt")));
        var order = inOrder(model, gate);
        order.verify(model).draft(any());
        order.verify(gate).sanitize(cl, problem, "Bản nháp bị chặn", "Em bị kẹt", List.of());
        order.verify(gate).sanitize(cl, problem, hint, "Em bị kẹt", List.of());
    }
    @Test void modelExceptionFallsBackToLadder() {
        when(model.draft(any())).thenThrow(new IllegalStateException("Không kết nối được"));
        when(gate.sanitize(cl, problem, hint, "Em bị kẹt", List.of())).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));
        assertEquals(hint, tutor.answer(student, request("Em bị kẹt")).message());
    }
    @Test void gateFailureNeverReleasesModelOrLadder() {
        when(model.draft(any())).thenReturn(Optional.of("Bản nháp"));
        when(gate.sanitize(any(), any(), anyString(), anyString(), anyList())).thenThrow(new IllegalStateException());
        TutorEvent result = tutor.answer(student, request("Em bị kẹt"));
        assertEquals("xong", result.status()); assertTrue(result.message().startsWith("Em xem lại bước này"));
        assertFalse(result.message().contains("Bản nháp"));
        assertFalse(result.message().contains(hint));
    }
    @Test void phasesSurroundModelAndGateAndAuthorizeCountsOnce() {
        List<String> timeline = new ArrayList<>();
        when(model.draft(any())).thenAnswer(i -> { timeline.add("mô hình"); return Optional.of("Câu hỏi gợi mở"); });
        when(gate.sanitize(any(), any(), anyString(), anyString(), anyList())).thenAnswer(i -> { timeline.add("cổng"); return java.util.Optional.of(i.<String>getArgument(2)); });
        TutorRequest r = request("Em bị kẹt");
        for (int i = 0; i < 12; i++) {
            UUID authorized = tutor.authorize(student, r);
            tutor.answerAuthorized(student, authorized, r, timeline::add);
        }
        assertEquals(List.of("kho", "goi", "mô hình", "loc", "cổng"), timeline.subList(0, 5));
        assertEquals(429, assertThrows(WorkspaceException.class, () -> tutor.authorize(student, r)).status());
        verify(store, times(12)).tutorContext(student, cl, r);
    }
    @Test void modelCannotReferOutsideSheetOrExceedThreeSentences() {
        when(gate.sanitize(eq(cl), eq(problem), anyString(), anyString(), anyList())).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));
        for (String bad : List.of("Em xem [999] rồi thử nhé.", "Em thử nhé. Mình chờ em. Hãy xem lại. Em hỏi tiếp nhé.")) {
            when(model.draft(any())).thenReturn(Optional.of(bad));
            assertEquals(hint, tutor.answer(student, request("Em bị kẹt")).message());
            verify(gate, never()).sanitize(cl, problem, bad, "Em bị kẹt", List.of());
        }
    }
    @Test void fallbackLadderAlsoRejectsInvalidShape() {
        when(model.draft(any())).thenReturn(Optional.empty());
        when(gate.sanitize(eq(cl), eq(problem), anyString(), anyString(), anyList())).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));
        for (String bad : List.of("Em xem [999] nhé.", "Em thử nhé. Mình chờ em. Hãy xem lại. Em hỏi tiếp nhé.")) {
            when(store.tutorContext(eq(student), eq(cl), any())).thenReturn(Optional.of(
                new TutorContext(problem, "DH1", "Đề bài", "x", "B.DH.TXD", bad)));
            TutorEvent result = tutor.answer(student, request("Em bị kẹt"));
            assertEquals("xong", result.status()); assertTrue(result.message().startsWith("Em xem lại bước này"));
            assertFalse(result.message().contains(bad));
        }
    }
    @Test void switchingToTestModeDuringModelCallPreventsRelease() {
        when(model.draft(any())).thenAnswer(i -> {
            when(store.testMode(student, cl, "DH1")).thenReturn(true);
            return Optional.of("Câu hỏi gợi mở");
        });
        when(gate.sanitize(any(), any(), anyString(), anyString(), anyList())).thenAnswer(i -> java.util.Optional.of(i.<String>getArgument(2)));
        assertEquals(403, assertThrows(WorkspaceException.class,
            () -> tutor.answer(student, request("Em bị kẹt"))).status());
    }
    @Test void testModeDeniedBeforeModelAndRateLimit() {
        when(store.testMode(student, cl, "DH1")).thenReturn(true);
        assertEquals(403, assertThrows(WorkspaceException.class, () -> tutor.authorize(student, request("Gợi ý"))).status());
        verifyNoInteractions(model, gate);
        verify(store, never()).tutorContext(any(), any(), any());
    }
}
