package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.util.Optional;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

public final class UnavailableHintModelAdapter implements HintModel {
    @Override public Optional<String> draft(HintPrompt prompt) { return Optional.empty(); }
    @Override public String provider() { return "unavailable"; }
}
