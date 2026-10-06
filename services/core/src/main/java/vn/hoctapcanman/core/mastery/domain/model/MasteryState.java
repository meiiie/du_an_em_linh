package vn.hoctapcanman.core.mastery.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Mức hiểu của một học sinh ở một kỹ năng (một dòng {@code mastery_states}): xác suất thành thạo, mức 4, số lượt được tính,
 * số lượt sai liền (kẹt), tối đa 8 mã lỗi gần đây không trùng, lúc lên Vận dụng cao. {@code completedAt} có khi và chỉ khi
 * đang ở Vận dụng cao (FR-026).
 */
public record MasteryState(
        UUID studentId,
        String skillCode,
        double mastery,
        Level4 level,
        int attempts,
        int stuckCounter,
        List<String> lastErrorCodes,
        @Nullable Instant completedAt) {

    static final int ERROR_CODES_KEPT = 8;

    public MasteryState {
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(skillCode, "skillCode");
        Objects.requireNonNull(level, "level");
        lastErrorCodes = List.copyOf(lastErrorCodes);
        if (attempts < 0 || stuckCounter < 0 || lastErrorCodes.size() > ERROR_CODES_KEPT) {
            throw new IllegalArgumentException("Trạng thái mức hiểu sai miền");
        }
        if ((level == Level4.VAN_DUNG_CAO) != (completedAt != null)) {
            throw new IllegalArgumentException("completedAt có khi và chỉ khi ở Vận dụng cao");
        }
    }

    /** Kỹ năng chưa có lượt nào, như v0 đọc khi chưa có dòng: mastery 0,3, mức suy từ mastery. */
    public static MasteryState initial(UUID studentId, String skillCode, BktConfig c, Instant at) {
        Level4 level = c.thresholds().levelOf(Bkt.INITIAL);
        return new MasteryState(studentId, skillCode, Bkt.INITIAL, level, 0, 0, List.of(), level == Level4.VAN_DUNG_CAO ? at : null);
    }

    /** Kẹt: sai liền từ {@code so_luot_ket} lượt (ngưỡng ghi cảnh báo cho giáo viên). */
    public boolean stuck(BktConfig c) {
        return stuckCounter >= c.stuckAfter();
    }

    /**
     * Một bài được tính ({@code applyMastery} của v0 sau khi chọn kỹ năng). Nghi đoán mò: mastery, mức, số lượt, đếm kẹt giữ
     * nguyên, mã lỗi vẫn ghi. Không nghi: BKT, mức sau bài, đúng thì đếm kẹt về 0, sai thì tăng; sai mà đủ ngưỡng kẹt thì báo
     * giáo viên. {@code next} trong kết quả chưa làm tròn: v0 tính mức trên số này rồi mới ghi vào cột {@code real}.
     */
    public Update apply(Evidence e, BktConfig c, Instant at) {
        boolean guess = e.guessSuspected();
        double next = guess ? mastery : Bkt.next(mastery, e.correct(), c);
        Level4 levelAfter = guess ? level : Bkt.levelAfter(level, next, e.correct(), e.problemLevel(), c.thresholds());
        int stuckAfter = guess ? stuckCounter : e.correct() ? 0 : stuckCounter + 1;
        Instant completed = levelAfter != Level4.VAN_DUNG_CAO ? null : completedAt != null ? completedAt : at;
        MasteryState after = new MasteryState(studentId, skillCode, next, levelAfter, attempts + (guess ? 0 : 1), stuckAfter,
            withErrorCode(e.errorCode()), completed);
        return new Update(this, after, guess ? 0 : next - mastery, !guess && !e.correct() && stuckAfter >= c.stuckAfter());
    }

    /** {@code [...new Set([...cũ, mã])].slice(-8)}: mã đã có giữ chỗ cũ, mã mới vào cuối, giữ 8 mã cuối. */
    private List<String> withErrorCode(@Nullable String code) {
        LinkedHashSet<String> codes = new LinkedHashSet<>(lastErrorCodes);
        if (code != null && !code.isEmpty()) {
            codes.add(code);
        }
        List<String> all = new ArrayList<>(codes);
        return all.subList(Math.max(0, all.size() - ERROR_CODES_KEPT), all.size());
    }

    /** Bằng chứng của bài được tính: đúng cả bài hay sai, mức của bài, mã lỗi của lần chấm, cờ nghi đoán mò. */
    public record Evidence(boolean correct, Level4 problemLevel, @Nullable String errorCode, boolean guessSuspected) {}

    /** Trước, sau, mức thay đổi mastery (0 khi nghi đoán mò) và có phải báo kẹt cho giáo viên. */
    public record Update(MasteryState before, MasteryState after, double delta, boolean stuckAlert) {}
}
