package vn.hoctapcanman.core.tutor.application.port;

import java.util.Optional;
import java.util.function.Consumer;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage;

/** Chỉ token nội dung; bên nhận phải kiểm trước khi phát. */
public interface HintModel {
    Optional<String> draft(HintPrompt prompt);
    String provider();
    default HintModel selected(String provider) { return this; }
    default ModelUsage stream(HintPrompt prompt, Consumer<String> tokens) {
        String text = draft(prompt).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalStateException("MODEL_UNAVAILABLE"));
        tokens.accept(text);
        return new ModelUsage(estimate(prompt.system() + prompt.user()), estimate(text), true);
    }
    static int estimate(String text) { return (text.length() + 3) / 4; }
}
