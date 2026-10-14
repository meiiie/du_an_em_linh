package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.application.service.*;

class TutorSummaryTest {
    final List<Long> sources=List.of(1L,2L,3L,4L,5L,6L,7L);
    @Test void schemaSourcesAndEveryTextFieldMustBeChecked() {
        var checked=new ArrayList<String>();
        String clean=TutorSummary.checked(TutorSessionServiceTest.summary("Em cần tìm điều gì?"),sources,s -> {checked.add(s);return s.strip();});
        assertEquals(List.of("Em cần tìm điều gì?"),checked);assertTrue(clean.contains("luotNguon"));
    }
    @Test void invalidSchemaSourceOrIdentityIsRejected() {
        String good=TutorSessionServiceTest.summary("Em đang tìm cách làm.");
        for(String raw:List.of("{}",good.replace("[1,2,3,4,5,6,7]","[7,6,5,4,3,2,1]"),good.replace("\"mucTieuBuoc\"","\"system\""),TutorSessionServiceTest.summary("Em là Nguyễn Văn An.")))
            assertThrows(RuntimeException.class,() -> TutorSummary.checked(raw,sources,s -> s));
    }
    @Test void mathematicalRejectionCannotBeSavedAsSummary() {
        assertThrows(OfflineTutorService.Rejected.class,() -> TutorSummary.checked(TutorSessionServiceTest.summary("Câu bị chặn."),sources,
            s -> {throw new OfflineTutorService.Rejected("GATE_REJECTED");}));
    }
}
