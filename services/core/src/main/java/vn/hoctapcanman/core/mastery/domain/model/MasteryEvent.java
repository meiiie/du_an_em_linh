package vn.hoctapcanman.core.mastery.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Một bài làm đã nộp được tính vào mức hiểu (một dòng {@code mastery_events}, nhiều nhất một dòng mỗi bài làm): kỹ năng được
 * tính và luật chọn nó, thay đổi mastery, bước sai, mã lỗi, độ tin cậy của lần chấm, cờ nghi đoán mò, mức trước và sau, phiên
 * bản tham số. Nộp lại phát lại từ đây, không tính lần hai.
 */
public record MasteryEvent(
        UUID id,
        UUID studentId,
        String skillCode,
        UUID submissionId,
        double delta,
        Rule rule,
        @Nullable String wrongStep,
        @Nullable String errorCode,
        @Nullable Double confidence,
        boolean guessSuspected,
        Level4 levelBefore,
        Level4 levelAfter,
        int configVersion,
        Instant createdAt) {

    public MasteryEvent {
        Objects.requireNonNull(rule, "rule");
        if (guessSuspected != (rule == Rule.NGHI_DOAN_MO)) {
            throw new IllegalArgumentException("Luật NGHI_DOAN_MO khi và chỉ khi nghi đoán mò");
        }
    }

    /** {@code ruleApplied} của v0: kỹ năng được tính lấy theo bài, theo mã lỗi đủ tin cậy, hay theo bước sai. */
    public enum Rule {
        THEO_KY_NANG_BAI,
        THEO_MA_LOI,
        THEO_BUOC,
        NGHI_DOAN_MO
    }
}
