package vn.hoctapcanman.core.practice.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Nội dung mới nhất học sinh nộp cho một bước: các dòng (bước kiểu dòng) và / hoặc bảng (bước kiểu bảng). Nộp lại cùng
 * bước thì thay toàn bộ nội dung bước đó. Dòng theo thứ tự số dòng, không trùng số dòng.
 */
public record StepWork(String stepCode, List<StepLine> lines, @Nullable SignTable table) {

    private static final Pattern MA_BUOC = Pattern.compile("[A-Z][A-Z0-9_.]{0,31}");
    private static final int TOI_DA_DONG = 50;

    public StepWork {
        Objects.requireNonNull(stepCode, "stepCode");
        if (!MA_BUOC.matcher(stepCode).matches()) {
            throw new IllegalArgumentException("Mã bước không hợp lệ: " + stepCode);
        }
        lines = List.copyOf(lines);
        if (lines.isEmpty() && table == null) {
            throw new IllegalArgumentException("Bước " + stepCode + " không có dòng hay bảng nào");
        }
        if (lines.size() > TOI_DA_DONG) {
            throw new IllegalArgumentException("Bước quá nhiều dòng (tối đa " + TOI_DA_DONG + ")");
        }
        Set<Integer> so = new HashSet<>();
        int truoc = -1;
        for (StepLine l : lines) {
            if (!so.add(l.lineNo()) || l.lineNo() < truoc) {
                throw new IllegalArgumentException("Dòng của bước phải theo thứ tự số dòng, không trùng");
            }
            truoc = l.lineNo();
        }
    }

    /**
     * Có ít nhất một dòng hay một ô có chữ: máy học sinh gửi dòng rỗng, ô rỗng khi em chưa viết gì vào bước. Khoảng trắng là
     * khoảng trắng của dịch vụ toán ({@link #khoangTrang}), không phải {@code String.isBlank} (bỏ sót {@code U+00A0},
     * {@code U+0085}).
     */
    public boolean coChu() {
        return lines.stream().anyMatch(l -> coChu(l.latex()))
            || (table != null && table.cells().stream().anyMatch(o -> coChu(o.value())));
    }

    private static boolean coChu(String s) {
        return s.codePoints().anyMatch(c -> !khoangTrang(c));
    }

    /**
     * Định nghĩa của {@code str.isspace()} trong Python, mà {@code str.strip()} của bộ chấm dùng: loại chung {@code Zs}, hay
     * hướng hai chiều {@code WS}, {@code B}, {@code S}. Test so với bảng do chính Python sinh trên mọi điểm mã.
     */
    static boolean khoangTrang(int c) {
        byte huong = Character.getDirectionality(c);
        return Character.getType(c) == Character.SPACE_SEPARATOR || huong == Character.DIRECTIONALITY_WHITESPACE
            || huong == Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR || huong == Character.DIRECTIONALITY_SEGMENT_SEPARATOR;
    }
}
