package vn.hoctapcanman.core.practice.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Bảng của bước kiểu bảng ({@code bang} của v0): loại bảng ({@code XET_DAU}) và các ô theo đúng thứ tự học sinh gửi, để
 * payload chấm dựng lại trùng v0. Không có hai ô cùng (hàng, k).
 */
public record SignTable(String kind, List<TableCell> cells) {

    private static final Pattern LOAI = Pattern.compile("[A-Z][A-Z0-9_]{0,15}");
    private static final int TOI_DA_O = 200;

    public SignTable {
        Objects.requireNonNull(kind, "kind");
        if (!LOAI.matcher(kind).matches()) {
            throw new IllegalArgumentException("Loại bảng không hợp lệ: " + kind);
        }
        cells = List.copyOf(cells);
        if (cells.size() > TOI_DA_O) {
            throw new IllegalArgumentException("Bảng quá lớn (tối đa " + TOI_DA_O + " ô)");
        }
        Set<String> o = new HashSet<>();
        for (TableCell c : cells) {
            if (!o.add(c.row() + "/" + c.k())) {
                throw new IllegalArgumentException("Ô trùng: hàng " + c.row() + ", k " + c.k());
            }
        }
    }
}
