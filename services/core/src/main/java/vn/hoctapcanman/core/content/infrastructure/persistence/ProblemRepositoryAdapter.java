package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.content.domain.model.BloomLevel;
import vn.hoctapcanman.core.content.domain.model.Level3;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ReleasedProblem;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;

/** Bài trên bảng {@code problems}. Không đọc bảng {@code solutions} (FR-006). */
@Repository
public class ProblemRepositoryAdapter implements ProblemRepository {

    private static final String COT = """
            id, code, skill_code, extra_skill_codes, level4, level3, bloom_level, difficulty, statement_text, statement_latex,
            function_sympy, answer_form, start_step, origin, content_hash, created_by, created_at, updated_at""";
    /** Bài đang phát hành ở lớp {@code :lop}, phiên bản nội dung và chủ đề: một câu lệnh, cùng một ảnh chụp với phát hành. */
    private static final String DANG_PHAT_HANH = "select " + COT
        + ", content_version, (select k.topic_code from skills k where k.code = p.skill_code) topic_code " + """
         from problems p where exists (
            select 1 from problem_releases r where r.problem_id = p.id and r.class_id = :lop and r.status = 'DA_PHAT_HANH')""";

    private final JdbcClient jdbc;

    public ProblemRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Problem p) {
        jdbc.sql("insert into problems (" + COT + """
                ) values (:id, :code, :skill, string_to_array(:extra, ','), :level4, :level3, :bloom, :difficulty, :text,
                    :latex, :sympy, :form, :start, :origin, :hash, :by, :created, :updated)
                on conflict (id) do update set code = excluded.code, skill_code = excluded.skill_code,
                    extra_skill_codes = excluded.extra_skill_codes, level4 = excluded.level4, level3 = excluded.level3,
                    bloom_level = excluded.bloom_level, difficulty = excluded.difficulty,
                    statement_text = excluded.statement_text, statement_latex = excluded.statement_latex,
                    function_sympy = excluded.function_sympy, answer_form = excluded.answer_form,
                    start_step = excluded.start_step, origin = excluded.origin, content_hash = excluded.content_hash,
                    created_by = excluded.created_by, created_at = excluded.created_at, updated_at = excluded.updated_at""")
            .param("id", p.id()).param("code", p.code()).param("skill", p.skillCode())
            .param("extra", Cot.noiMa(p.extraSkillCodes())).param("level4", p.level4().name())
            .param("level3", p.level3() == null ? null : p.level3().name())
            .param("bloom", p.bloomLevel() == null ? null : p.bloomLevel().name())
            .param("difficulty", p.difficulty()).param("text", p.statementText()).param("latex", p.statementLatex())
            .param("sympy", p.functionSympy()).param("form", p.answerForm()).param("start", p.startStep())
            .param("origin", p.origin()).param("hash", p.contentHash()).param("by", p.createdBy())
            .param("created", Cot.luc(p.createdAt())).param("updated", Cot.luc(p.updatedAt()))
            .update();
    }

    @Override
    public Optional<Problem> findById(UUID id) {
        return jdbc.sql("select " + COT + " from problems where id = :id").param("id", id).query(ProblemRepositoryAdapter::bai).optional();
    }

    @Override
    public Optional<Problem> findByCode(String code) {
        return jdbc.sql("select " + COT + " from problems where code = :code").param("code", code).query(ProblemRepositoryAdapter::bai).optional();
    }

    @Override
    public List<Problem> findAllById(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of(); // «in ()» là lỗi cú pháp của PostgreSQL
        }
        return jdbc.sql("select " + COT + " from problems where id in (:ids) order by code")
            .param("ids", List.copyOf(ids)).query(ProblemRepositoryAdapter::bai).list();
    }

    @Override
    public List<Problem> findReleasedInClass(UUID classId) {
        // Một câu lệnh: bài và phát hành cùng một ảnh chụp, không lẫn nội dung vừa sửa với phát hành chưa rút.
        return jdbc.sql("select " + COT + " " + """
                 from problems where id in (
                    select problem_id from problem_releases where class_id = :lop and status = 'DA_PHAT_HANH')
                order by code""")
            .param("lop", classId).query(ProblemRepositoryAdapter::bai).list();
    }

    @Override
    public Optional<Problem> findReleasedInClass(UUID classId, String code) {
        return jdbc.sql("select " + COT + " " + """
                 from problems p where code = :code and exists (
                    select 1 from problem_releases r where r.problem_id = p.id and r.class_id = :lop and r.status = 'DA_PHAT_HANH')""")
            .param("lop", classId).param("code", code).query(ProblemRepositoryAdapter::bai).optional();
    }

    @Override
    public Optional<ReleasedProblem> findReleasedForWork(UUID classId, String code) {
        // Một câu lệnh: bài, phiên bản nội dung, chủ đề và phát hành cùng một ảnh chụp.
        return jdbc.sql(DANG_PHAT_HANH + " and code = :code")
            .param("lop", classId).param("code", code)
            .query(ProblemRepositoryAdapter::dangPhatHanh).optional();
    }

    @Override
    public List<ReleasedProblem> findReleasedForWork(UUID classId) {
        return jdbc.sql(DANG_PHAT_HANH + " order by code").param("lop", classId).query(ProblemRepositoryAdapter::dangPhatHanh).list();
    }

    private static ReleasedProblem dangPhatHanh(ResultSet rs, int n) throws SQLException {
        return new ReleasedProblem(bai(rs, n), rs.getInt("content_version"), rs.getString("topic_code"));
    }

    @Override
    public Optional<Integer> findContentVersion(UUID problemId) {
        return jdbc.sql("select content_version from problems where id = :id").param("id", problemId).query(Integer.class).optional();
    }

    private static Problem bai(ResultSet rs, int n) throws SQLException {
        String level3 = rs.getString("level3");
        String bloom = rs.getString("bloom_level");
        return new Problem(Cot.uuid(rs, "id"), Cot.chu(rs, "code"), Cot.chu(rs, "skill_code"), Cot.mang(rs, "extra_skill_codes"),
            Level4.valueOf(Cot.chu(rs, "level4")), level3 == null ? null : Level3.valueOf(level3),
            bloom == null ? null : BloomLevel.valueOf(bloom), Cot.realNeuCo(rs, "difficulty"), Cot.chu(rs, "statement_text"),
            Cot.chu(rs, "statement_latex"), rs.getString("function_sympy"), Cot.chu(rs, "answer_form"), rs.getString("start_step"),
            Cot.chu(rs, "origin"), Cot.chu(rs, "content_hash"), Cot.uuidNeuCo(rs, "created_by"), Cot.thoiDiem(rs, "created_at"),
            Cot.thoiDiem(rs, "updated_at"));
    }
}
