package vn.hoctapcanman.core.content.domain.repository;

import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.domain.model.ContentReview;
import vn.hoctapcanman.core.content.domain.model.SubjectKind;
import vn.hoctapcanman.core.content.domain.model.VerificationRun;

/**
 * Lượt kiểm 3 tầng (chỉ thêm), căn cứ từng tầng, đoạn trích dẫn và bản ghi duyệt của giáo viên. Lượt đã ghi chỉ đổi theo
 * hai đường: đánh dấu cũ, hoặc được duyệt ({@code V4}, {@code V5}).
 */
public interface VerificationRunRepository {

    /**
     * Ghi một lượt mới cùng các tầng và đoạn trích dẫn. Lượt kiểm bài chỉ ghi được cho phiên bản nội dung hiện tại của bài
     * (CSDL từ chối lượt trên nội dung cũ: lỗi ràng buộc).
     */
    void save(VerificationRun run);

    Optional<VerificationRun> findById(UUID id);

    /** Lượt mới nhất của (lớp, đối tượng), xếp theo thời điểm rồi id. */
    Optional<VerificationRun> findLatest(UUID classId, SubjectKind kind, UUID subjectId);

    /** Đánh dấu lượt là cũ (không còn là căn cứ phát hành). */
    void markStale(UUID runId);

    /**
     * Bảng công thức của lớp vừa đổi: mọi lượt của lớp kiểm với bảng khác {@code currentSheetId} (hay không có bảng) thành
     * cũ. Trả số lượt vừa đánh dấu.
     */
    int markStaleExceptSheet(UUID classId, UUID currentSheetId);

    /**
     * Ghi kết quả giáo viên duyệt ({@link VerificationRun#approve}): bản ghi duyệt và lượt chuyển sang {@code GV_DUYET},
     * cùng một giao dịch. Bản phát hành gắn với lượt này theo sang {@code DA_PHAT_HANH} (khóa ngoại {@code ON UPDATE
     * CASCADE} của V4).
     */
    void saveApproval(VerificationRun.Approval approval);

    Optional<ContentReview> findReview(UUID runId);
}
