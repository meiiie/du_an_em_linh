package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.domain.model.ProblemRelease;
import vn.hoctapcanman.core.content.domain.model.ReleaseStatus;
import vn.hoctapcanman.core.content.domain.repository.ProblemReleaseRepository;

/**
 * Trạng thái phát hành trên {@code problem_releases}. Khóa ngoại bốn cột của V4 buộc trạng thái bằng trạng thái phát
 * hành của chính lượt kiểm; trigger của V5 buộc lượt còn mới và đúng phiên bản nội dung hiện tại của bài.
 */
@Repository
public class ProblemReleaseRepositoryAdapter implements ProblemReleaseRepository {

    private final JdbcClient jdbc;

    public ProblemReleaseRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void save(ProblemRelease r) {
        // Khóa lớp rồi bài trước khi đụng dòng phát hành: lần sửa bài khóa bài rồi mới rút phát hành này (KhoaThuTu).
        KhoaThuTu.lopRoiBai(jdbc, r.classId(), r.problemId());
        jdbc.sql("""
                insert into problem_releases (class_id, problem_id, status, run_id, updated_at)
                values (:lop, :bai, :status, :run, :at)
                on conflict (class_id, problem_id) do update set status = excluded.status, run_id = excluded.run_id,
                    updated_at = excluded.updated_at""")
            .param("lop", r.classId()).param("bai", r.problemId()).param("status", r.status().name()).param("run", r.runId())
            .param("at", Cot.luc(r.updatedAt()))
            .update();
    }

    @Override
    public Optional<ProblemRelease> find(UUID classId, UUID problemId) {
        return jdbc.sql("""
                select class_id, problem_id, status, run_id, updated_at from problem_releases
                where class_id = :lop and problem_id = :bai""")
            .param("lop", classId).param("bai", problemId).query(ProblemReleaseRepositoryAdapter::phatHanh).optional();
    }

    @Override
    public List<ProblemRelease> findByClass(UUID classId) {
        return jdbc.sql("""
                select r.class_id, r.problem_id, r.status, r.run_id, r.updated_at from problem_releases r
                join problems p on p.id = r.problem_id where r.class_id = :lop order by p.code""")
            .param("lop", classId).query(ProblemReleaseRepositoryAdapter::phatHanh).list();
    }

    private static ProblemRelease phatHanh(ResultSet rs, int n) throws SQLException {
        return new ProblemRelease(Cot.uuid(rs, "class_id"), Cot.uuid(rs, "problem_id"), ReleaseStatus.valueOf(Cot.chu(rs, "status")),
            Cot.uuidNeuCo(rs, "run_id"), Cot.thoiDiem(rs, "updated_at"));
    }
}
