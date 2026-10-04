package vn.hoctapcanman.core.content.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.domain.model.ProblemRelease;

/** Trạng thái phát hành của bài theo lớp. */
public interface ProblemReleaseRepository {

    /**
     * Ghi đè theo (lớp, bài). Gắn vào một lượt kiểm thì lượt phải còn mới và đúng phiên bản nội dung hiện tại của bài
     * (CSDL từ chối: lỗi ràng buộc).
     */
    void save(ProblemRelease release);

    Optional<ProblemRelease> find(UUID classId, UUID problemId);

    /** Mọi bài đã có trạng thái ở lớp, theo mã bài. */
    List<ProblemRelease> findByClass(UUID classId);
}
