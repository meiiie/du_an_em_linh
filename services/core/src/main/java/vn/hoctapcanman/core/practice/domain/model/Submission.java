package vn.hoctapcanman.core.practice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bài làm của một học sinh cho một bài ở một lớp (data-model §practice). Lớp quyết định tài liệu, bảng công thức, cài
 * gia sư dùng cho bài làm. Mỗi (học sinh, lớp, bài, phiên bản nội dung) có tối đa một bài làm {@code DANG_LAM}; nộp
 * rồi thì không đổi:
 *
 * <pre>
 * mở ──▶ DANG_LAM ──nộp bước (chấm từng lần)──▶ DANG_LAM ──nộp bài──▶ DA_NOP (kết quả cố định)
 * </pre>
 *
 * {@code contentVersion} là phiên bản nội dung của bài lúc mở: nội dung đổi thì bài làm này thôi được chấm (bước đã viết
 * là cho đề cũ) và học sinh mở bài làm mới cho phiên bản mới. Nghi đoán mò (v0 {@code nghiDoanMo}) chỉ bật, không tắt.
 */
public record Submission(
        UUID id,
        UUID classId,
        UUID studentId,
        UUID problemId,
        int contentVersion,
        SubmissionStatus status,
        boolean guessSuspected,
        @Nullable String guessReason,
        @Nullable GradeStatus result,
        Instant startedAt,
        @Nullable Instant submittedAt) {

    public Submission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
        if (contentVersion < 1) {
            throw new IllegalArgumentException("contentVersion phải dương");
        }
        if (result == GradeStatus.KHONG_CHAM_DUOC) {
            throw new IllegalArgumentException("Kết quả nộp là một phán quyết, không phải KHONG_CHAM_DUOC");
        }
        boolean daNop = status == SubmissionStatus.DA_NOP;
        if (daNop != (submittedAt != null) || daNop != (result != null)) {
            throw new IllegalArgumentException("Chỉ bài làm đã nộp mới có lúc nộp và kết quả");
        }
        if (!guessSuspected && guessReason != null) {
            throw new IllegalArgumentException("Lý do nghi đoán mò chỉ đi kèm cờ nghi");
        }
        if (guessReason != null && (guessReason.isBlank() || guessReason.length() > 300)) {
            throw new IllegalArgumentException("Lý do nghi đoán mò phải có chữ, tối đa 300 ký tự");
        }
    }

    /** Bài làm mới, đang làm. */
    public static Submission open(UUID classId, UUID studentId, UUID problemId, int contentVersion, Instant now) {
        return new Submission(UUID.randomUUID(), classId, studentId, problemId, contentVersion, SubmissionStatus.DANG_LAM, false,
            null, null, now, null);
    }

    public boolean isOpen() {
        return status == SubmissionStatus.DANG_LAM;
    }

    /**
     * Nộp bài với kết quả cuối, là một phán quyết của bộ chấm. {@code KHONG_CHAM_DUOC} (dịch vụ toán lỗi) không đóng được
     * bài làm: {@link IllegalArgumentException}, bài làm vẫn mở để nộp lại. Bài làm đã nộp thì {@link IllegalStateException}.
     */
    public Submission submit(GradeStatus ketQua, Instant now) {
        Objects.requireNonNull(ketQua, "ketQua");
        if (ketQua == GradeStatus.KHONG_CHAM_DUOC) {
            throw new IllegalArgumentException("Không nộp bài với kết quả không chấm được");
        }
        if (!isOpen()) {
            throw new IllegalStateException("Bài làm đã nộp");
        }
        return new Submission(id, classId, studentId, problemId, contentVersion, SubmissionStatus.DA_NOP, guessSuspected, guessReason,
            ketQua, startedAt, now);
    }

    /** Bật nghi đoán mò; đã nghi thì giữ lý do đầu. Bài làm đã nộp thì {@link IllegalStateException}. */
    public Submission suspectGuess(String lyDo) {
        if (!isOpen()) {
            throw new IllegalStateException("Bài làm đã nộp");
        }
        if (guessSuspected) {
            return this;
        }
        return new Submission(id, classId, studentId, problemId, contentVersion, status, true, lyDo, null, startedAt, null);
    }
}
