package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Một cấp của thang gợi ý 3 cấp cho một bước của bài: cấp sau cụ thể hơn cấp trước, không bao giờ tới mức nêu đáp án
 * (lab Sư phạm). Chữ giữ nguyên văn như lab soạn.
 */
public record HintLevel(UUID problemId, String stepCode, int level, String text) {

    public HintLevel {
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(stepCode, "stepCode");
        Objects.requireNonNull(text, "text");
        Kiem.ma(stepCode, 32, "bước");
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Cấp gợi ý ngoài 1–3");
        }
        Kiem.khongTrong(text, "Câu gợi ý");
    }
}
