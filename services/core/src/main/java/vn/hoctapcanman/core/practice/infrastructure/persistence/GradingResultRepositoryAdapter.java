package vn.hoctapcanman.core.practice.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;
import vn.hoctapcanman.core.practice.domain.repository.GradingResultRepository;

/**
 * Kết quả chấm trên {@code grading_results} (chỉ thêm, V7). Chỉ mục duy nhất từng phần {@code (submission_id,
 * request_hash) WHERE result <> 'KHONG_CHAM_DUOC'}: hai tab ghi cùng yêu cầu thì lần sau chờ lần trước commit rồi không ghi,
 * đọc lại lần đã có.
 */
@Repository
public class GradingResultRepositoryAdapter implements GradingResultRepository {

    private static final String COT = """
            id, submission_id, step_code, request_hash, result, result_type, wrong_steps::text as wrong_steps, error_code,
            confidence, per_step::text as per_step, message, issues::text as issues, math_ok, unfinished, normalizer_version,
            normalization::text as normalization, graded_at""";

    private final JdbcClient jdbc;

    public GradingResultRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public GradingResult record(GradingResult k) {
        String trangThai = jdbc.sql("select status from submissions where id = :id for share").param("id", k.submissionId())
            .query(String.class).optional().orElse(null);
        if (!"DANG_LAM".equals(trangThai)) {
            throw new IllegalStateException("Bài làm đã nộp hoặc không có");
        }
        int ghi = jdbc.sql("""
                insert into grading_results (id, submission_id, step_code, request_hash, result, result_type, wrong_steps, error_code,
                    confidence, per_step, message, issues, math_ok, unfinished, normalizer_version, normalization, graded_at)
                values (:id, :bl, :buoc, :bam, :kq, :loai, cast(:sai as jsonb), :ma, :tin, cast(:tung as jsonb), :tb,
                    cast(:van as jsonb), :toan, :chua, :pbcn, cast(:cn as jsonb), :luc)
                on conflict (submission_id, request_hash) where result <> 'KHONG_CHAM_DUOC' do nothing""")
            .param("id", k.id()).param("bl", k.submissionId()).param("buoc", k.stepCode()).param("bam", k.requestHash())
            .param("kq", k.result().name()).param("loai", k.resultType()).param("sai", k.wrongStepsJson()).param("ma", k.errorCode())
            .param("tin", k.confidence()).param("tung", Cot.json(k.perStep())).param("tb", k.message()).param("van", k.issuesJson())
            .param("toan", k.mathOk()).param("chua", k.unfinished()).param("pbcn", k.normalizerVersion())
            .param("cn", k.normalizationJson()).param("luc", Cot.luc(k.gradedAt()))
            .update();
        if (ghi == 1) {
            return findById(k.id()).orElseThrow();
        }
        return findByRequest(k.submissionId(), k.requestHash())
            .orElseThrow(() -> new IllegalStateException("Không ghi được kết quả chấm"));
    }

    @Override
    public Optional<GradingResult> findByRequest(UUID submissionId, String requestHash) {
        return jdbc.sql("select " + COT + " from grading_results where submission_id = :bl and request_hash = :bam"
                + " and result <> 'KHONG_CHAM_DUOC'")
            .param("bl", submissionId).param("bam", requestHash)
            .query(GradingResultRepositoryAdapter::ketQua).optional();
    }

    @Override
    public List<GradingResult> bySubmission(UUID submissionId) {
        return jdbc.sql("select " + COT + " from grading_results where submission_id = :bl order by graded_at, id")
            .param("bl", submissionId).query(GradingResultRepositoryAdapter::ketQua).list();
    }

    private Optional<GradingResult> findById(UUID id) {
        return jdbc.sql("select " + COT + " from grading_results where id = :id").param("id", id)
            .query(GradingResultRepositoryAdapter::ketQua).optional();
    }

    private static GradingResult ketQua(ResultSet rs, int n) throws SQLException {
        return new GradingResult(Cot.uuid(rs, "id"), Cot.uuid(rs, "submission_id"), Cot.chu(rs, "step_code"),
            Cot.chu(rs, "request_hash"), GradeStatus.valueOf(Cot.chu(rs, "result")), rs.getString("result_type"),
            rs.getString("wrong_steps"), rs.getString("error_code"), Cot.realNeuCo(rs, "confidence"),
            Cot.bangChu(Cot.chu(rs, "per_step")), rs.getString("message"), rs.getString("issues"), Cot.dungSaiNeuCo(rs, "math_ok"),
            rs.getBoolean("unfinished"), rs.getString("normalizer_version"), rs.getString("normalization"),
            Cot.thoiDiem(rs, "graded_at"));
    }
}
