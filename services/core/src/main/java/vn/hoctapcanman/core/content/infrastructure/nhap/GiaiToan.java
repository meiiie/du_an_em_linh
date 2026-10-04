package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Hai job của {@code services/math} mà importer dùng: {@code /v1/solve} (máy giải một hàm thành lời giải 5 bước, dữ kiện
 * bảo vệ, thang gợi ý) và {@code /v1/generate} (biến thể có hạt giống cố định). Rỗng khi dịch vụ toán lỗi, hết giờ hay trả
 * phong bì lỗi: không bao giờ bịa lời giải.
 */
public interface GiaiToan {

    Optional<Map<String, @Nullable Object>> giai(Map<String, ?> yeuCau);

    Optional<Map<String, @Nullable Object>> sinh(Map<String, ?> yeuCau);
}
