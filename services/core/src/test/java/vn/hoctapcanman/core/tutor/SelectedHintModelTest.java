package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.HintModel;
import vn.hoctapcanman.core.tutor.infrastructure.ai.*;

class SelectedHintModelTest {
    final HintPrompt prompt=new HintPrompt("Chỉ dẫn","Em bị kẹt");
    @Test void unavailableGlmNeverSelectsWorkingOfflineModel() {
        var models=new SelectedHintModelAdapter(new UnavailableHintModelAdapter(),new OfflineHintModelAdapter());
        assertTrue(models.selected("glm").draft(prompt).isEmpty());assertTrue(models.selected("offline").draft(prompt).isPresent());
    }
    @Test void configuredGlmDoesNotPreventExplicitOfflineDevSelection() {
        HintModel glm=new HintModel(){public Optional<String> draft(HintPrompt p){return Optional.of("Em đã thử gì?");}public String provider(){return "GLM";}};
        var models=new SelectedHintModelAdapter(glm,new OfflineHintModelAdapter());
        assertSame(glm,models.selected("glm"));assertEquals("offline",models.selected("offline").provider());assertTrue(models.selected("unknown").draft(prompt).isEmpty());
    }
}
