package vn.hoctapcanman.core.practice.infrastructure.seed;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GiaoBaiStudyPoolTest {
    @Test void realSeedKeepsThreeStudyExercisesOutOfAutomaticAssignments()throws Exception {
        String source=Files.readString(Path.of("../../data/dev/nguyen-trai/bai-tap.json"));
        var study=GiaoBaiNguyenTraiSeeder.studyCodes(source);
        assertThat(study).containsExactlyInAnyOrder("NT-KG12-TUHOC-01","NT-TP12-TUHOC-01","NT-XS12-TUHOC-01");
        assertThat(GiaoBaiNguyenTraiSeeder.testCodes(source)).doesNotContainAnyElementsOf(study);
    }
}
