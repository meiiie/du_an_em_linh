package vn.hoctapcanman.core.content.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.domain.model.Problem;

/** Bài của ngân hàng (nội dung chung). Không đọc lời giải: lời giải ở {@link SolutionRepository} riêng (FR-006). */
public interface ProblemRepository {

    /** Ghi mới hoặc ghi đè theo id. Mã bài là duy nhất: bài khác id cùng mã thì lỗi ràng buộc. */
    void save(Problem problem);

    Optional<Problem> findById(UUID id);

    Optional<Problem> findByCode(String code);

    /** Các bài có id trong {@code ids}, theo mã bài. */
    List<Problem> findAllById(Collection<UUID> ids);
}
