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
}
