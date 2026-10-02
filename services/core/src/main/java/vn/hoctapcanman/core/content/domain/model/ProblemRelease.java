package vn.hoctapcanman.core.content.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Trạng thái phát hành của một bài cho một lớp (data-model §content). Học sinh chỉ thấy bài {@code DA_PHAT_HANH} của
 * lớp mình. {@code NHAP} không gắn lượt kiểm; mọi trạng thái khác lấy từ một lượt kiểm của đúng lớp và bài, còn mới:
 *
 * <pre>
 * NHAP ──lượt kiểm──▶ DA_PHAT_HANH | BI_CHAN | CHO_GIAO_VIEN_DUYET
 * CHO_GIAO_VIEN_DUYET ──GV duyệt (lượt GV_DUYET)──▶ DA_PHAT_HANH
 * đổi nội dung bài ──▶ NHAP (chờ kiểm lại)
 * </pre>
 *
 * Kiểm lại một bài đã phát hành (ví dụ sau khi bảng công thức đổi) cũng áp kết quả mới, kể cả khi kết quả chặn bài:
 * đóng mặc định. Ở CSDL, khóa ngoại {@code (run_id, class_id, problem_id, status)} buộc trạng thái bằng trạng thái phát
 * hành của chính lượt đó ({@code V4__content.sql}).
 */
public record ProblemRelease(UUID classId, UUID problemId, ReleaseStatus status, @Nullable UUID runId, Instant updatedAt) {

    public ProblemRelease {
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if ((status == ReleaseStatus.NHAP) != (runId == null)) {
            throw new IllegalArgumentException("NHAP không gắn lượt kiểm; trạng thái khác phải gắn một lượt kiểm");
        }
    }

    /** Bài vừa nạp cho lớp, chưa kiểm. */
    public static ProblemRelease draft(UUID classId, UUID problemId, Instant now) {
        return new ProblemRelease(classId, problemId, ReleaseStatus.NHAP, null, now);
    }

    /**
     * Áp kết quả của một lượt kiểm bài (kể cả lượt đã được giáo viên duyệt). Lượt của lớp hay bài khác thì
     * {@link IllegalArgumentException}. Lượt không còn mới thì {@link IllegalStateException}: đã {@code stale}, không phải
     * lượt mới nhất, của nội dung cũ, hay kiểm với bảng khác bảng hiện tại ({@link VerificationRun#freshnessRefusal}).
     *
     * @param latest lượt này là lượt mới nhất của (lớp, bài)
     */
    public ProblemRelease apply(
            VerificationRun run, boolean latest, String currentContentHash, @Nullable UUID currentSheetId, Instant now) {
        if (run.subjectKind() != SubjectKind.PROBLEM || !run.classId().equals(classId) || !run.subjectId().equals(problemId)) {
            throw new IllegalArgumentException("Lượt kiểm không thuộc bài này của lớp này");
        }
        run.freshnessRefusal(latest, currentContentHash, currentSheetId).ifPresent(lyDo -> {
            throw new IllegalStateException("Lượt kiểm không còn là căn cứ: " + lyDo);
        });
        return new ProblemRelease(classId, problemId, Objects.requireNonNull(run.publishStatus()), run.id(), now);
    }

    /** Nội dung bài vừa đổi: về {@code NHAP}, chờ kiểm lại. */
    public ProblemRelease backToDraft(Instant now) {
        return new ProblemRelease(classId, problemId, ReleaseStatus.NHAP, null, now);
    }

    /** Học sinh của lớp thấy bài này. */
    public boolean visibleToStudents() {
        return status == ReleaseStatus.DA_PHAT_HANH;
    }
}
