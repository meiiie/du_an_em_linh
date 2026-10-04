package vn.hoctapcanman.core.practice.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Một dòng của bước kiểu dòng ({@code cac_dong} của v0): số dòng 0-based, LaTeX như học sinh viết (được trống: v0 gửi dòng
 * trống khi học sinh chưa viết gì), nhãn {@code loai} khi bước cần (nghiệm hay điểm không xác định, ô kết luận), không có
 * thì {@code null} và không gửi khóa {@code loai}.
 */
public record StepLine(int lineNo, String latex, @Nullable String kind) {

    private static final Pattern NHAN = Pattern.compile("[A-Z][A-Z0-9_]{0,15}");

    public StepLine {
        Objects.requireNonNull(latex, "latex");
        if (lineNo < 0 || lineNo > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Số dòng phải từ 0");
        }
        if (latex.length() > 2000) {
            throw new IllegalArgumentException("Dòng quá dài (tối đa 2000 ký tự)");
        }
        if (kind != null && !NHAN.matcher(kind).matches()) {
            throw new IllegalArgumentException("Nhãn dòng không hợp lệ: " + kind);
        }
    }
}
