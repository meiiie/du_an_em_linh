package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.util.Optional;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** Model tất định chỉ cho test/demo offline, không dự phòng cho lớp chọn GLM. */
public final class OfflineHintModelAdapter implements HintModel {
    @Override public Optional<String> draft(HintPrompt prompt) {
        if(prompt.system().startsWith("Tóm tắt")) {
            var matcher=java.util.regex.Pattern.compile("Nguồn lượt cần nén: (\\[[0-9, ]+\\])").matcher(prompt.user());
            if(!matcher.find())return Optional.empty();
            return Optional.of("{\"mucTieuBuoc\":\"Em đang tìm cách thực hiện bước hiện tại.\",\"cachDaThu\":\"\",\"loiDaDuocCham\":\"\",\"cauHoiConMo\":\"\",\"goiYDaDung\":\"\",\"dieuDaSua\":\"\",\"luotNguon\":"+matcher.group(1)+"}");
        }
        return Optional.of("Em đã thử cách nào ở bước hiện tại?");
    }
    @Override public String provider() { return "offline"; }
}
