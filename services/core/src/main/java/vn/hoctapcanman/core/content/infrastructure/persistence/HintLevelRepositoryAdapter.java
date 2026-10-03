package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.domain.model.HintLevel;
import vn.hoctapcanman.core.content.domain.repository.HintLevelRepository;

/** Thang gợi ý trên bảng {@code hint_levels}. */
@Repository
public class HintLevelRepositoryAdapter implements HintLevelRepository {

    private final JdbcClient jdbc;

    public HintLevelRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void replaceForProblem(UUID problemId, List<HintLevel> levels) {
        if (levels.stream().anyMatch(h -> !h.problemId().equals(problemId))) {
            throw new IllegalArgumentException("Cấp gợi ý phải của đúng bài " + problemId);
        }
        jdbc.sql("delete from hint_levels where problem_id = :id").param("id", problemId).update();
        for (HintLevel h : levels) {
            jdbc.sql("insert into hint_levels (problem_id, step_code, level, text) values (:id, :step, :level, :text)")
                .param("id", h.problemId()).param("step", h.stepCode()).param("level", h.level()).param("text", h.text())
                .update();
        }
    }

    /** Bước của khung theo thứ tự khung; bước ngoài khung (dạng bài khác) xếp sau, theo mã. */
    @Override
    public List<HintLevel> findByProblemId(UUID problemId) {
        return jdbc.sql("""
                select h.problem_id, h.step_code, h.level, h.text from hint_levels h
                left join step_templates s on s.step_code = h.step_code
                where h.problem_id = :id order by s.ordinal nulls last, h.step_code, h.level""")
            .param("id", problemId)
            .query((rs, n) -> new HintLevel(Cot.uuid(rs, "problem_id"), Cot.chu(rs, "step_code"), rs.getInt("level"), Cot.chu(rs, "text")))
            .list();
    }
}
