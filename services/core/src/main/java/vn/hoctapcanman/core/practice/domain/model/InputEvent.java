package vn.hoctapcanman.core.practice.domain.model;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Một lần học sinh sửa một ô hay dòng (v0 {@code events}: {@code ma_buoc}, {@code o}, giá trị cũ, giá trị mới, thời điểm),
 * cho nghi đoán mò. Ô không xác định ({@code o = null} ở v0) thì cả hàng và k đều trống.
 */
public record InputEvent(String stepCode, @Nullable String cellRow, @Nullable Integer cellK, @Nullable String oldValue, String newValue,
        Instant at) {

    public InputEvent {
        Objects.requireNonNull(stepCode, "stepCode");
        Objects.requireNonNull(newValue, "newValue");
        Objects.requireNonNull(at, "at");
        if ((cellRow == null) != (cellK == null)) {
            throw new IllegalArgumentException("Ô của sự kiện có cả hàng lẫn k, hoặc không có cả hai");
        }
        if (newValue.length() > 200 || (oldValue != null && oldValue.length() > 200)) {
            throw new IllegalArgumentException("Giá trị ô quá dài (tối đa 200 ký tự)");
        }
    }
}
