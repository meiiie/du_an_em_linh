package vn.hoctapcanman.core.content.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Trạng thái phát hành của một bài cho một lớp (data-model §content). Học sinh chỉ thấy bài {@code DA_PHAT_HANH} của
 * lớp mình. Ngoài {@code NHAP}, trạng thái luôn lấy từ một lượt kiểm của đúng lớp và bài:
 *
 * <pre>
 * NHAP ──lượt kiểm──▶ DA_PHAT_HANH | BI_CHAN | CHO_GIAO_VIEN_DUYET
 * CHO_GIAO_VIEN_DUYET ──GV duyệt (lượt GV_DUYET)──▶ DA_PHAT_HANH
 * đổi nội dung bài ──▶ NHAP (chờ kiểm lại)
 * </pre>
 *
 * Kiểm lại một bài đã phát hành (ví dụ sau khi bảng công thức đổi) cũng áp kết quả mới, kể cả khi kết quả chặn bài:
 * đóng mặc định.
 */
public record ProblemRelease(UUID classId, UUID problemId, ReleaseStatus status, @Nullable UUID runId, Instant updatedAt) {

    public ProblemRelease {
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (status != ReleaseStatus.NHAP && runId == null) {
            throw new IllegalArgumentException("Trạng thái phát hành ngoài NHAP phải gắn một lượt kiểm");
        }
    }

    /** Bài vừa nạp cho lớp, chưa kiểm. */
    public static ProblemRelease draft(UUID classId, UUID problemId, Instant now) {
        return new ProblemRelease(classId, problemId, ReleaseStatus.NHAP, null, now);
    }

    /**
     * Áp kết quả của một lượt kiểm bài (kể cả lượt đã được giáo viên duyệt). Lượt của lớp hay bài khác thì
     * {@link IllegalArgumentException}; lượt đã {@code stale} thì {@link IllegalStateException}.
     */
    public ProblemRelease apply(VerificationRun run, Instant now) {
        if (run.subjectKind() != SubjectKind.PROBLEM || !run.classId().equals(classId) || !run.subjectId().equals(problemId)) {
            throw new IllegalArgumentException("Lượt kiểm không thuộc bài này của lớp này");
        }
        if (run.stale()) {
            throw new IllegalStateException("Lượt kiểm đã cũ: bảng công thức của lớp đã đổi");
        }
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
