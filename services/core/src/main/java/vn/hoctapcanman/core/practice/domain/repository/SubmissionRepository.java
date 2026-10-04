package vn.hoctapcanman.core.practice.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.practice.domain.model.InputEvent;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.Submission;

/**
 * Bài làm và nội dung từng bước. CSDL giữ các bất biến (V7): chỉ học sinh của lớp, bài đã phát hành, đúng phiên bản nội
 * dung hiện tại; một bài làm đang làm mỗi (học sinh, lớp, bài, phiên bản nội dung); bài làm đã nộp thì không ghi thêm
 * được.
 */
public interface SubmissionRepository {

    /**
     * Bài làm đang làm của (học sinh, lớp, bài) ở phiên bản nội dung của {@code moi}; chưa có thì ghi {@code moi} rồi trả
     * nó. Hai tab mở cùng lúc nhận cùng một bài làm. Bài làm dở của phiên bản cũ không bị đụng tới.
     */
    Submission openOrGet(Submission moi);

    /** Bài làm đang làm của (học sinh, lớp, bài) ở đúng phiên bản nội dung này. */
    Optional<Submission> findOpen(UUID studentId, UUID classId, UUID problemId, int contentVersion);

    /** Bài làm mới nhất (đang làm hay đã nộp) của (học sinh, lớp, bài). */
    Optional<Submission> findLatest(UUID studentId, UUID classId, UUID problemId);

    Optional<Submission> findById(UUID id);

    /**
     * Thay toàn bộ nội dung của một bước (dòng và bảng) bằng {@code buoc}, trong một giao dịch khóa dòng bài làm: hai tab
     * nộp cùng bước nối tiếp nhau, không trộn dòng. Bài làm đã nộp thì {@link IllegalStateException}.
     */
    void saveStep(UUID submissionId, StepWork buoc);

    /** Nội dung các bước của bài làm, theo thứ tự bước của khung. */
    List<StepWork> steps(UUID submissionId);

    /** Thêm sự kiện nhập (chỉ thêm). Bài làm đã nộp thì {@link IllegalStateException}. */
    void addEvents(UUID submissionId, List<InputEvent> suKien);

    /** Sự kiện nhập của bài làm, theo thời điểm. */
    List<InputEvent> events(UUID submissionId);

    /**
     * Ghi cờ nghi đoán mò hay kết quả nộp của bài làm ({@link Submission#suspectGuess}, {@link Submission#submit}). Chỉ ghi
     * được khi bài làm còn đang làm; không thì {@link IllegalStateException}.
     */
    void update(Submission baiLam);
}
