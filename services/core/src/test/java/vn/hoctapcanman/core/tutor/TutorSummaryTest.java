package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.application.service.*;

class TutorSummaryTest {
    final List<Long> sources=List.of(1L,2L,3L,4L,5L,6L,7L);
    @Test void schemaAndSourcesAreValidatedWithoutSentenceGate() {
        String clean=TutorSummary.validate(TutorSessionServiceTest.summary("Em thử $x^2$. Chưa đóng $x"),sources);
        assertTrue(clean.contains("Em thử $x^2$. Chưa đóng $x"));assertTrue(clean.contains("luotNguon"));
    }
    @Test void invalidSchemaSourceOrIdentityIsRejected() {
        String good=TutorSessionServiceTest.summary("Em đang tìm cách làm.");
        for(String raw:List.of("{}","không có JSON",good.replace("\"mucTieuBuoc\"","\"system\""),TutorSessionServiceTest.summary("Em là Nguyễn Văn An.")))
            assertThrows(RuntimeException.class,() -> TutorSummary.validate(raw,sources));
    }
    @Test void fencedJsonAcceptedAndSourcesComeFromServer() {
        String good=TutorSessionServiceTest.summary("Em đang tìm cách làm.");
        String clean=TutorSummary.validate("```json\n"+good.replace("[1,2,3,4,5,6,7]","[9]")+"\n```",sources);
        assertTrue(clean.contains("[1,2,3,4,5,6,7]"));
    }
    @Test void mathematicalTextIsNotGated() {
        String clean=TutorSummary.validate(TutorSessionServiceTest.summary("Đạo hàm $2x$ ở bước em đã thử."),sources);
        assertTrue(clean.contains("$2x$"));
    }
}
