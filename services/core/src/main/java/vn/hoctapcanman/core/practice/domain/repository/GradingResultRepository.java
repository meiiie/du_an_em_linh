package vn.hoctapcanman.core.practice.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;

/** Kết quả chấm, chỉ thêm (V7). */
public interface GradingResultRepository {

    /**
     * Ghi một lần chấm. Đã có lần chấm (không phải {@code KHONG_CHAM_DUOC}) cho đúng yêu cầu này của bài làm thì không ghi
     * lần hai mà trả lần đã có: hai tab nộp cùng bước ghi một lần. Bài làm đã nộp thì {@link IllegalStateException}.
     */
    GradingResult record(GradingResult ketQua);

    /** Lần chấm có phán quyết (không phải {@code KHONG_CHAM_DUOC}) của đúng yêu cầu này, nếu có. */
    Optional<GradingResult> findByRequest(UUID submissionId, String requestHash);

    /** Các lần chấm của bài làm, cũ trước. */
    List<GradingResult> bySubmission(UUID submissionId);
}
