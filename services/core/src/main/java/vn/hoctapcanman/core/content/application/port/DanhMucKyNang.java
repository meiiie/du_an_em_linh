package vn.hoctapcanman.core.content.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import vn.hoctapcanman.core.content.application.dto.KyNang;

/**
 * Danh mục kỹ năng cho module khác (T050): kỹ năng gắn với mã lỗi và với bước của khung, như {@code applyMastery} của v0 đọc
 * {@code error_types}, {@code step_templates}; tên và chủ đề của kỹ năng. Dữ liệu chung của chủ đề, không theo lớp.
 */
public interface DanhMucKyNang {

    /** Kỹ năng chính của mã lỗi; rỗng khi mã lỗi không có trong danh mục hay không gắn kỹ năng. */
    Optional<String> kyNangCuaMaLoi(String maLoi);

    /** Kỹ năng của bước khung; rỗng khi bước không có trong khung hay không gắn kỹ năng. */
    Optional<String> kyNangCuaBuoc(String maBuoc);

    /** Mọi kỹ năng của các chủ đề có ít nhất một kỹ năng trong {@code maKyNang}, theo chủ đề rồi mã. */
    List<KyNang> cungChuDe(Collection<String> maKyNang);
}
