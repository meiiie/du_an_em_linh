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

    /**
     * Các bài đang phát hành ({@code DA_PHAT_HANH}) ở lớp {@code classId}, theo mã bài. Bài và trạng thái phát hành đọc trong
     * một câu lệnh (một ảnh chụp CSDL): sửa nội dung bài rút phát hành trong cùng giao dịch (V5), nên không bao giờ trả nội
     * dung vừa sửa, chưa kiểm, kèm phát hành cũ.
     */
    List<Problem> findReleasedInClass(UUID classId);

    /** Bài mã {@code code} nếu đang phát hành ở lớp {@code classId}; đọc trong một câu lệnh như {@link #findReleasedInClass(UUID)}. */
    Optional<Problem> findReleasedInClass(UUID classId, String code);

    /**
     * Phiên bản nội dung hiện tại của bài ({@code problems.content_version}, CSDL tăng mỗi khi đề, lời giải hay thang gợi ý
     * đổi). Lượt kiểm bài ghi phiên bản này ({@code VerificationRun.forProblem}).
     */
    Optional<Integer> findContentVersion(UUID problemId);
}
