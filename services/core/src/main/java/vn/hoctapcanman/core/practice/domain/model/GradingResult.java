package vn.hoctapcanman.core.practice.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Một lần chấm bài làm tới một bước ({@code nop_toi}), theo phản hồi {@code /v1/grade} của v0: kết quả, loại kết quả, bước
 * sai ({@code buoc_sai}, JSON), mã lỗi, độ tin cậy, kết quả từng bước, thông báo, các vấn đề ({@code cac_van_de}, JSON),
 * cờ dấu U ({@code toan_dung}), chưa xong, chuẩn hóa ({@code chuan_hoa}, JSON). {@code requestHash} là SHA-256 của payload
 * đã gửi, để chấm lại đúng yêu cầu đó không ghi lần hai. Các trường JSON giữ nguyên văn phản hồi (core không diễn giải).
 *
 * <p>{@link GradeStatus#KHONG_CHAM_DUOC} không mang phán quyết nào (không bước sai, mã lỗi, độ tin cậy, cờ dấu U), và
 * không bao giờ là đạt (FR-009).
 */
public record GradingResult(
        UUID id,
        UUID submissionId,
        String stepCode,
        String requestHash,
        GradeStatus result,
        @Nullable String resultType,
        @Nullable String wrongStepsJson,
        @Nullable String errorCode,
        @Nullable Double confidence,
        Map<String, String> perStep,
        @Nullable String message,
        @Nullable String issuesJson,
        @Nullable Boolean mathOk,
        boolean unfinished,
        @Nullable String normalizerVersion,
        @Nullable String normalizationJson,
        Instant gradedAt) {

    private static final Pattern BAM = Pattern.compile("[0-9a-f]{64}");

    public GradingResult {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(submissionId, "submissionId");
        Objects.requireNonNull(stepCode, "stepCode");
        Objects.requireNonNull(requestHash, "requestHash");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(gradedAt, "gradedAt");
        perStep = Map.copyOf(perStep);
        if (!BAM.matcher(requestHash).matches()) {
            throw new IllegalArgumentException("requestHash phải là SHA-256 hex chữ thường");
        }
        if (confidence != null && (confidence.isNaN() || confidence < 0 || confidence > 1)) {
            throw new IllegalArgumentException("Độ tin cậy phải trong [0, 1]");
        }
        if (result == GradeStatus.KHONG_CHAM_DUOC
                && (wrongStepsJson != null || errorCode != null || confidence != null || mathOk != null)) {
            throw new IllegalArgumentException("KHONG_CHAM_DUOC không mang phán quyết nào");
        }
    }

    /** Dịch vụ toán không trả lời được: không có phán quyết, thông báo chung cho học sinh (chi tiết lỗi chỉ ở log). */
    public static GradingResult notGraded(UUID submissionId, String stepCode, String requestHash, String message, Instant now) {
        return new GradingResult(UUID.randomUUID(), submissionId, stepCode, requestHash, GradeStatus.KHONG_CHAM_DUOC, null, null, null,
            null, Map.of(), message, null, null, false, null, null, now);
    }
}
