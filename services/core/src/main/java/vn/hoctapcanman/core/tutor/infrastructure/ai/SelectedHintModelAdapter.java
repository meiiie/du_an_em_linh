package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.util.Optional;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** Chọn đúng cấu hình lớp; không chuyển nhà khi model lỗi. */
public final class SelectedHintModelAdapter implements HintModel {
    private final HintModel glm;
    private final HintModel offline;
    private final HintModel unavailable=new UnavailableHintModelAdapter();
    public SelectedHintModelAdapter(HintModel glm,HintModel offline){this.glm=glm;this.offline=offline;}
    @Override public HintModel selected(String provider) {
        return switch(provider){case "glm" -> glm;case "offline" -> offline;default -> unavailable;};
    }
    @Override public Optional<String> draft(HintPrompt prompt){throw new IllegalStateException("MODEL_NOT_SELECTED");}
    @Override public String provider(){return "unselected";}
}
