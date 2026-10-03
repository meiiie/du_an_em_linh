package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Lời giải mẫu và dữ kiện bảo vệ của một bài (FR-006, ADR 003): không bao giờ vào DTO của học sinh khi đang làm, không
 * vào prompt của gia sư; chỉ mở sau khi nộp nếu lớp bật cờ. JSON giữ nguyên như lab soạn (cột {@code jsonb}).
 * {@link #toString()} không in nội dung, để log không bao giờ lộ đáp án.
 */
public record Solution(UUID problemId, @Nullable String workedSolutionJson, String protectedFactsJson, @Nullable String finalAnswer) {

    public Solution {
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(protectedFactsJson, "protectedFactsJson");
        Kiem.khongTrong(protectedFactsJson, "Dữ kiện bảo vệ");
    }

    @Override
    public String toString() {
        return "Solution[problemId=" + problemId + "]";
    }
}
