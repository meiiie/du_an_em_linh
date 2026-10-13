package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.util.Optional;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** Không có khóa: dùng thang gợi ý qua cổng toán, không gọi mạng. */
public final class OfflineHintModelAdapter implements HintModel {
    @Override public Optional<String> draft(HintPrompt prompt) { return Optional.empty(); }
    @Override public String provider() { return "offline"; }
}
