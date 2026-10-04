package vn.hoctapcanman.core.practice.application.port;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Máy chấm từng bước ({@code POST /v1/grade} của dịch vụ toán, như v0). Đóng mặc định (FR-009): hết giờ, lỗi HTTP, JSON hỏng
 * hay job lỗi thì rỗng, không bao giờ là phán quyết. Hiện thực ở {@code practice.infrastructure.client}.
 */
public interface MayCham {

    /** Thân phản hồi chấm (khóa snake_case như v0) cho yêu cầu {@code yeuCau}; rỗng khi dịch vụ toán không trả lời được. */
    Optional<Map<String, @Nullable Object>> cham(Map<String, ?> yeuCau);
}
