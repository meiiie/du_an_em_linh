package vn.hoctapcanman.core.practice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bài làm của một học sinh cho một bài ở một lớp (data-model §practice). Lớp quyết định tài liệu, bảng công thức, cài
 * gia sư dùng cho bài làm. Mỗi (học sinh, lớp, bài, phiên bản nội dung) có tối đa một bài làm {@code DANG_LAM}; nộp
 * rồi thì không đổi:
 *
 * <pre>
 * mở ──▶ DANG_LAM ──nộp bước (chấm từng lần)──▶ DANG_LAM ──nộp bài (căn cứ = một lần chấm của chính bài làm)──▶ DA_NOP
 * </pre>
 *
 * {@code contentVersion} là phiên bản nội dung của bài lúc mở: nội dung đổi thì bài làm này thôi được chấm (bước đã viết
 * là cho đề cũ) và học sinh mở bài làm mới cho phiên bản mới. Nghi đoán mò (v0 {@code nghiDoanMo}) chỉ bật, không tắt.
 *
 * <p>Bài làm đã nộp có kết quả, lúc nộp và căn cứ ({@code resultGradingId}): lần chấm của chính bài làm này mà kết quả là
 * {@code result}. Kết quả nộp vì vậy luôn là một phán quyết bộ chấm đã đưa (FR-009); CSDL kiểm lại (V8). Nơi cần biết chắc
 * bài làm đã nộp nhận {@link DaNop}, không nhận {@code Submission}.
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
        @Nullable UUID resultGradingId,
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
        if (daNop != (submittedAt != null) || daNop != (result != null) || daNop != (resultGradingId != null)) {
            throw new IllegalArgumentException("Chỉ bài làm đã nộp mới có lúc nộp, kết quả và căn cứ");
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
            null, null, null, now, null);
    }

    public boolean isOpen() {
        return status == SubmissionStatus.DANG_LAM;
    }

    /**
     * Nộp bài với căn cứ {@code canCu}, một lần chấm có phán quyết của chính bài làm này: kết quả nộp là kết quả của lần
     * chấm đó. Lần chấm của bài làm khác hay {@code KHONG_CHAM_DUOC} (dịch vụ toán lỗi) thì {@link IllegalArgumentException},
     * bài làm vẫn mở. Bài làm đã nộp thì {@link IllegalStateException}. Không kiểm căn cứ có phải lần chấm bước kết luận
     * trên nội dung hiện tại: việc đó của nơi tìm căn cứ ({@code NopBaiUseCase}).
     */
    public DaNop submit(GradingResult canCu, Instant now) {
        if (!canCu.submissionId().equals(id)) {
            throw new IllegalArgumentException("Căn cứ là lần chấm của bài làm khác");
        }
        if (canCu.result() == GradeStatus.KHONG_CHAM_DUOC) {
            throw new IllegalArgumentException("Không nộp bài với kết quả không chấm được");
        }
        if (!isOpen()) {
            throw new IllegalStateException("Bài làm đã nộp");
        }
        return new DaNop(new Submission(id, classId, studentId, problemId, contentVersion, SubmissionStatus.DA_NOP, guessSuspected,
            guessReason, canCu.result(), canCu.id(), startedAt, now));
    }

    /** Chứng nhận đã nộp của bài làm này; đang làm thì rỗng. */
    public Optional<DaNop> daNop() {
        return isOpen() ? Optional.empty() : Optional.of(new DaNop(this));
    }

    /** Bật nghi đoán mò; đã nghi thì giữ lý do đầu. Bài làm đã nộp thì {@link IllegalStateException}. */
    public Submission suspectGuess(String lyDo) {
        if (!isOpen()) {
            throw new IllegalStateException("Bài làm đã nộp");
        }
        if (guessSuspected) {
            return this;
        }
        return new Submission(id, classId, studentId, problemId, contentVersion, status, true, lyDo, null, null, startedAt, null);
    }

    /**
     * Chứng nhận bài làm đã nộp. Chỉ {@link Submission#submit} và {@link Submission#daNop} tạo được (constructor
     * {@code private}; record không giấu được constructor), nên hàm nhận {@code DaNop}, như cửa lời giải, không nhận được
     * bài làm đang làm.
     */
    public static final class DaNop {

        private final Submission baiLam;

        private DaNop(Submission baiLam) {
            this.baiLam = baiLam;
        }

        /** Bài làm ở trạng thái {@code DA_NOP}. */
        public Submission baiLam() {
            return baiLam;
        }

        /** {@code DAT}, {@code SAI} hay {@code KHONG_KIEM_DUOC}. */
        public GradeStatus ketQua() {
            return Objects.requireNonNull(baiLam.result());
        }

        /** Id của lần chấm làm căn cứ. */
        public UUID canCuId() {
            return Objects.requireNonNull(baiLam.resultGradingId());
        }

        public Instant nopLuc() {
            return Objects.requireNonNull(baiLam.submittedAt());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof DaNop d && d.baiLam.equals(baiLam);
        }

        @Override
        public int hashCode() {
            return baiLam.hashCode();
        }

        @Override
        public String toString() {
            return "DaNop[" + baiLam + "]";
        }
    }
}
