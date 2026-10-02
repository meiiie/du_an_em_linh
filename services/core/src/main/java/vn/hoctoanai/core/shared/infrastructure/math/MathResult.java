package vn.hoctoanai.core.shared.infrastructure.math;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Kết quả một lần gọi dịch vụ toán. {@link Failed} không mang phán quyết nào, nên nơi gọi buộc phải hiểu nó là
 * «không chấm được» / «không kiểm được» (FR-009): hết giờ, lỗi HTTP, JSON hỏng hay job lỗi không bao giờ thành «đạt».
 */
public sealed interface MathResult permits MathResult.Ok, MathResult.Failed {

    /** Chỉ một {@link Ok} có trường {@code field} đúng bằng {@code "DAT"} mới là đạt. */
    default boolean isDat(String field) {
        return this instanceof Ok ok && "DAT".equals(ok.body().get(field));
    }

    /** Thân JSON của job (khóa snake_case tiếng Việt như v0), chỉ đọc. */
    record Ok(Map<String, @Nullable Object> body) implements MathResult {
        public Ok {
            body = Collections.unmodifiableMap(new LinkedHashMap<>(body));
        }
    }

    /** Thất bại; {@code detail} là lý do ngắn đã làm sạch (không ký tự điều khiển, tối đa 200 ký tự), không ghi vào log. */
    record Failed(Reason reason, String detail) implements MathResult {}

    enum Reason {
        /** Quá hết giờ phía core. */
        TIMEOUT,
        /** Không kết nối được. */
        UNAVAILABLE,
        /** HTTP 4xx / 5xx. */
        HTTP_ERROR,
        /** Thân không phải đối tượng JSON. */
        BAD_RESPONSE,
        /** Sandbox trả phong bì lỗi: job hết giờ, chết, không trả JSON hoặc máy bận. */
        JOB_FAILED
    }
}
