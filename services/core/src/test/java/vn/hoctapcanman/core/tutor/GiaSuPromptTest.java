package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.application.service.TutorPrompt;
import vn.hoctapcanman.core.tutor.infrastructure.ai.OfflineHintModelAdapter;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;

class GiaSuPromptTest {
    @Test void promptContainsOnlyProblemStepSheetAndStudentsOwnWork() {
        UUID problem = UUID.randomUUID();
        String solution = "Cực tiểu bí mật tại x=-987654321";
        TutorContext c = new TutorContext(problem, "DH1", "Cho hàm $y=x^2$", "x**2", "B.DH.DAOHAM", "Em xem [1] nhé.",
            "Đạo hàm", List.of(new TutorFormula(1, "F1", "Lũy thừa", "(x^n)'=nx^{n-1}", "Quy tắc lũy thừa")),
            List.of("f'(x)=3x"));
        var p = TutorPrompt.compose(c, new TutorRequest("DH1", "B.DH.DAOHAM", 8, "Em bị kẹt, an@example.test " + problem));
        assertAll(() -> assertTrue(p.user().contains(c.statement())),
            () -> assertTrue(p.user().contains("B.DH.DAOHAM — Đạo hàm")),
            () -> assertTrue(p.user().contains("Mức gợi ý: 3")),
            () -> assertTrue(p.user().contains("[1] Lũy thừa")),
            () -> assertTrue(p.user().contains("f'(x)=3x")),
            () -> assertTrue(p.user().contains(c.hint())),
            () -> assertFalse(p.user().contains(solution)),
            () -> assertFalse(p.user().contains("an@example.test")),
            () -> assertFalse(p.user().contains(problem.toString())),
            () -> assertTrue(p.system().contains("tối đa 3 câu")),
            () -> assertTrue(p.system().contains("$…$")));
        assertEquals(Set.of("problemId", "code", "statement", "function", "step", "hint", "stepName", "formulas", "submittedLines"),
            Arrays.stream(TutorContext.class.getRecordComponents()).map(x -> x.getName()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("system", "user"), Arrays.stream(p.getClass().getRecordComponents()).map(x -> x.getName()).collect(java.util.stream.Collectors.toSet()));
        assertFalse(p.toString().contains(c.statement()));
    }
    @Test void redactsLabeledIdentityInQuestionsAndSubmittedLines() {
        var c = new TutorContext(UUID.randomUUID(), "DH1", "Đề", "x", "B.DH.TXD", "Gợi ý", "Tập xác định", List.of(),
            List.of("Tên em là An Tổng Hợp, trường THPT Nguyễn Trãi, lớp 12A3."));
        var p = TutorPrompt.compose(c, new TutorRequest("DH1", "B.DH.TXD", 1,
            "Em tên là Bình Tổng Hợp, điện thoại 0912345678. Em bị kẹt."));
        assertFalse(p.user().contains("An Tổng Hợp"));
        assertFalse(p.user().contains("Bình Tổng Hợp"));
        assertFalse(p.user().contains("Nguyễn Trãi"));
        assertFalse(p.user().contains("12A3"));
        assertFalse(p.user().contains("0912345678"));
        assertTrue(p.user().contains("Em bị kẹt"));
    }
    @Test void offlineAdapterHasNoDraft() {
        var adapter = new OfflineHintModelAdapter();
        assertEquals("offline", adapter.provider());
        assertTrue(adapter.draft(TutorPrompt.compose(new TutorContext(UUID.randomUUID(), "DH1", "Đề", "x", "B.DH.TXD", "Gợi ý"),
            new TutorRequest("DH1", "B.DH.TXD", 1, "Em bị kẹt"))).isEmpty());
    }
}
