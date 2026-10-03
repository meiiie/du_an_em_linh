package vn.hoctapcanman.core.content.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Giáo viên duyệt một lượt kiểm bài {@code KHONG_KIEM_DUOC} (FR-005): ai, lúc nào, vì sao, trên đúng nội dung nào
 * ({@code contentHash}). Bắt buộc ghi chú. Chỉ tạo qua {@link VerificationRun#approve}, nơi kiểm lượt nào duyệt được.
 */
public record ContentReview(UUID id, UUID runId, String contentHash, UUID reviewerId, String note, Instant at) {

    private static final int GHI_CHU_DAI_TOI_DA = 1000;

    public ContentReview {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(contentHash, "contentHash");
        Objects.requireNonNull(reviewerId, "reviewerId");
        Objects.requireNonNull(note, "note");
        Objects.requireNonNull(at, "at");
        Kiem.sha256(contentHash, "Dấu vân tay nội dung");
        note = note.replace("\r\n", "\n").strip();
        Kiem.khongTrong(note, "Ghi chú duyệt");
        Kiem.hopLeUtf16(note, "Ghi chú duyệt");
        Kiem.toiDa(note, GHI_CHU_DAI_TOI_DA, "Ghi chú duyệt");
        // Ghi chú nhiều dòng thì được; ký tự điều khiển khác và ký tự định dạng (vd U+202E đảo chiều) thì không.
        if (note.codePoints().anyMatch(cp -> cp != '\n' && cp != '\t'
                && (Character.getType(cp) == Character.CONTROL || Character.getType(cp) == Character.FORMAT))) {
            throw new IllegalArgumentException("Ghi chú duyệt có ký tự điều khiển");
        }
    }

    /** Không in ghi chú của giáo viên vào log. */
    @Override
    public String toString() {
        return "ContentReview[id=" + id + ", runId=" + runId + "]";
    }
}
