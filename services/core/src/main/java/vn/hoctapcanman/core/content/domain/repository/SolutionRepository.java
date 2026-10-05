package vn.hoctapcanman.core.content.domain.repository;

import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.domain.model.Solution;

/**
 * Lời giải mẫu và dữ kiện bảo vệ (FR-006, ADR 003). Chỉ phần chấm, phần duyệt của giáo viên và phần mở lời giải sau khi
 * nộp (khi lớp bật cờ) được đọc; cổng đọc bài cho học sinh không bao giờ dùng port này.
 */
public interface SolutionRepository {

    /** Ghi mới hoặc ghi đè theo bài. */
    void save(Solution solution);

    Optional<Solution> findByProblemId(UUID problemId);

    /**
     * Lời giải của bài khi bài đang phát hành ở lớp {@code classId} và phiên bản nội dung hiện tại là {@code contentVersion}
     * (mở lời giải sau khi nộp). Lời giải, phiên bản và phát hành đọc trong một câu lệnh: sửa lời giải tăng phiên bản và rút
     * phát hành trong cùng giao dịch (V5), nên không bao giờ trả lời giải mới kèm phiên bản cũ.
     */
    Optional<Solution> findReleased(UUID classId, UUID problemId, int contentVersion);
}
