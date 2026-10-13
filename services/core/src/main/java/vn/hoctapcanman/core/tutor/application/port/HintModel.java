package vn.hoctapcanman.core.tutor.application.port;

import java.util.Optional;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;

/** Nhà cung cấp chỉ nhận ngữ cảnh không mang lời giải; bản nháp chưa được phép hiển thị. */
public interface HintModel {
    Optional<String> draft(HintPrompt prompt);
    String provider();
}
