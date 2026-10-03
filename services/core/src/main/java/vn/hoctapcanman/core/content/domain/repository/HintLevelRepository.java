package vn.hoctapcanman.core.content.domain.repository;

import java.util.List;
import java.util.UUID;
import vn.hoctapcanman.core.content.domain.model.HintLevel;

/** Thang gợi ý của bài theo bước. */
public interface HintLevelRepository {

    /** Thay toàn bộ thang gợi ý của bài; mọi cấp phải thuộc bài {@code problemId}. */
    void replaceForProblem(UUID problemId, List<HintLevel> levels);

    /** Thang của bài, theo bước rồi theo cấp. */
    List<HintLevel> findByProblemId(UUID problemId);
}
