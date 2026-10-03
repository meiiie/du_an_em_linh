package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.content.domain.model.Solution;
import vn.hoctapcanman.core.content.domain.repository.SolutionRepository;

/**
 * Lời giải trên bảng {@code solutions}. Cột {@code jsonb} không giữ cách viết: đọc lại là JSON tương đương, khóa có thể
 * đổi thứ tự, khoảng trắng chuẩn hóa. Dấu vân tay nội dung vì vậy không tính trên chuỗi đọc từ CSDL.
 */
@Repository
public class SolutionRepositoryAdapter implements SolutionRepository {

    private final JdbcClient jdbc;

    public SolutionRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Solution s) {
        jdbc.sql("""
                insert into solutions (problem_id, worked_solution, protected_facts, final_answer)
                values (:id, cast(:worked as jsonb), cast(:facts as jsonb), :answer)
                on conflict (problem_id) do update set worked_solution = excluded.worked_solution,
                    protected_facts = excluded.protected_facts, final_answer = excluded.final_answer""")
            .param("id", s.problemId()).param("worked", s.workedSolutionJson()).param("facts", s.protectedFactsJson())
            .param("answer", s.finalAnswer())
            .update();
    }

    @Override
    public Optional<Solution> findByProblemId(UUID problemId) {
        return jdbc.sql("select problem_id, worked_solution, protected_facts, final_answer from solutions where problem_id = :id")
            .param("id", problemId)
            .query((rs, n) -> new Solution(Cot.uuid(rs, "problem_id"), rs.getString("worked_solution"),
                Cot.chu(rs, "protected_facts"), rs.getString("final_answer")))
            .optional();
    }
}
