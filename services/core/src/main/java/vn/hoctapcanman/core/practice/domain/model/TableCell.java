package vn.hoctapcanman.core.practice.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Một ô của bảng xét dấu ({@code cac_o} của v0): hàng ({@code X}, {@code DAU_YPHAY}, {@code BIEN_THIEN}), chỉ số {@code k}
 * 0-based ({@code docs/chi-so-o-bang.md}), giá trị học sinh điền.
 */
public record TableCell(String row, @Nullable Integer k, String value) {

    private static final Pattern HANG = Pattern.compile("[A-Z][A-Z0-9_]{0,15}");

    public TableCell {
        Objects.requireNonNull(row, "row");
        Objects.requireNonNull(value, "value");
        if (!HANG.matcher(row).matches()) {
            throw new IllegalArgumentException("Hàng không hợp lệ: " + row);
        }
        if (k != null && (k < 0 || k > Short.MAX_VALUE)) {
            throw new IllegalArgumentException("k phải từ 0");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Giá trị ô quá dài (tối đa 200 ký tự)");
        }
    }
}
