package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.domain.model.CheckStatus;
import vn.hoctapcanman.core.content.domain.model.ContentReview;
import vn.hoctapcanman.core.content.domain.model.ReleaseStatus;
import vn.hoctapcanman.core.content.domain.model.SubjectKind;
import vn.hoctapcanman.core.content.domain.model.TierResult;
import vn.hoctapcanman.core.content.domain.model.VerificationRun;
import vn.hoctapcanman.core.content.domain.repository.VerificationRunRepository;

/**
 * Lượt kiểm trên {@code verification_runs}, {@code verification_tier_results}, {@code verification_run_citations}; bản ghi
 * duyệt trên {@code content_reviews}. Các bất biến (chỉ thêm, đúng phiên bản nội dung, trích dẫn cùng lớp và không
 * {@code chua_ro}, {@code GV_DUYET} đi đôi bản ghi duyệt) do trigger và khóa ngoại của V4, V5 giữ; adapter chỉ ghi đúng
 * thứ tự và trong một giao dịch.
 */
@Repository
public class VerificationRunRepositoryAdapter implements VerificationRunRepository {

    private static final String COT = """
            id, class_id, subject_kind, subject_id, content_hash, content_version, formula_sheet_id, overall_status,
            publish_status, stale, created_at""";

    private final JdbcClient jdbc;

    public VerificationRunRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void save(VerificationRun r) {
        jdbc.sql("insert into verification_runs (" + COT + """
                ) values (:id, :lop, :kind, :subject, :hash, :version, :sheet, :overall, :publish, :stale, :created)""")
            .param("id", r.id()).param("lop", r.classId()).param("kind", r.subjectKind().name()).param("subject", r.subjectId())
            .param("hash", r.contentHash()).param("version", r.contentVersion()).param("sheet", r.formulaSheetId())
            .param("overall", r.overallStatus().name()).param("publish", r.publishStatus() == null ? null : r.publishStatus().name())
            .param("stale", r.stale()).param("created", Cot.luc(r.createdAt()))
            .update();
        for (TierResult t : r.tiers()) {
            jdbc.sql("""
                    insert into verification_tier_results (run_id, tier, status, result_type, wrong_steps, error_code,
                        confidence, reason, citation, raw)
                    values (:run, :tier, :status, :type, cast(:wrong as jsonb), :error, :confidence, :reason,
                        cast(:citation as jsonb), cast(:raw as jsonb))""")
                .param("run", r.id()).param("tier", t.tier()).param("status", t.status().name()).param("type", t.resultType())
                .param("wrong", t.wrongStepsJson()).param("error", t.errorCode()).param("confidence", t.confidence())
                .param("reason", t.reason()).param("citation", t.citationJson()).param("raw", t.rawJson())
                .update();
        }
        for (UUID doan : r.citationPassageIds()) {
            jdbc.sql("insert into verification_run_citations (run_id, passage_id) values (:run, :p)")
                .param("run", r.id()).param("p", doan).update();
        }
    }

    @Override
    public Optional<VerificationRun> findById(UUID id) {
        return jdbc.sql("select " + COT + " from verification_runs where id = :id").param("id", id)
            .query((rs, n) -> new LuotCho(rs)).optional().map(this::dung);
    }

    @Override
    public Optional<VerificationRun> findLatest(UUID classId, SubjectKind kind, UUID subjectId) {
        return jdbc.sql("select " + COT + " from verification_runs"
                + " where class_id = :lop and subject_kind = :kind and subject_id = :subject order by created_at desc, id desc limit 1")
            .param("lop", classId).param("kind", kind.name()).param("subject", subjectId)
            .query((rs, n) -> new LuotCho(rs)).optional().map(this::dung);
    }

    @Override
    @Transactional
    public void markStale(UUID runId) {
        // Lượt kiểm bài: khóa lớp rồi bài trước khi đụng dòng lượt (KhoaThuTu), như mọi đường ghi lượt hay phát hành. Lần ghi
        // phát hành đang dở (đã đọc lượt còn mới) commit trước, hoặc chạy sau và thấy lượt đã cũ; không gắn vào lượt vừa cũ.
        jdbc.sql("select class_id, subject_id from verification_runs where id = :id and subject_kind = 'PROBLEM'")
            .param("id", runId)
            .query((rs, n) -> Map.entry(Cot.uuid(rs, "class_id"), Cot.uuid(rs, "subject_id")))
            .optional()
            .ifPresent(lopBai -> KhoaThuTu.lopRoiBai(jdbc, lopBai.getKey(), lopBai.getValue()));
        jdbc.sql("update verification_runs set stale = true where id = :id and not stale").param("id", runId).update();
    }

    @Override
    @Transactional
    public void saveApproval(VerificationRun.Approval a) {
        ContentReview d = a.review();
        // Khóa lớp rồi bài trước khi đụng dòng lượt: lần sửa bài khóa bài rồi mới đánh dấu cũ lượt này (KhoaThuTu).
        KhoaThuTu.lopRoiBai(jdbc, a.run().classId(), a.run().subjectId());
        jdbc.sql("""
                insert into content_reviews (id, run_id, content_hash, reviewer_id, decision, note, at)
                values (:id, :run, :hash, :by, 'GV_DUYET', :note, :at)""")
            .param("id", d.id()).param("run", d.runId()).param("hash", d.contentHash()).param("by", d.reviewerId())
            .param("note", d.note()).param("at", Cot.luc(d.at()))
            .update();
        int doi = jdbc.sql("""
                update verification_runs set overall_status = 'GV_DUYET', publish_status = 'DA_PHAT_HANH'
                where id = :id and overall_status = 'KHONG_KIEM_DUOC' and not stale""")
            .param("id", a.run().id()).update();
        if (doi != 1) {
            throw new IllegalStateException("Lượt kiểm " + a.run().id() + " không còn chờ duyệt");
        }
    }

    @Override
    public Optional<ContentReview> findReview(UUID runId) {
        return jdbc.sql("select id, run_id, content_hash, reviewer_id, note, at from content_reviews where run_id = :run")
            .param("run", runId)
            .query((rs, n) -> new ContentReview(Cot.uuid(rs, "id"), Cot.uuid(rs, "run_id"), Cot.chu(rs, "content_hash"),
                Cot.uuid(rs, "reviewer_id"), Cot.chu(rs, "note"), Cot.thoiDiem(rs, "at")))
            .optional();
    }

    private VerificationRun dung(LuotCho l) {
        List<TierResult> tang = jdbc.sql("""
                select tier, status, result_type, wrong_steps, error_code, confidence, reason, citation, raw
                from verification_tier_results where run_id = :run order by tier""")
            .param("run", l.id)
            .query((rs, n) -> new TierResult(rs.getInt("tier"), CheckStatus.valueOf(Cot.chu(rs, "status")),
                rs.getString("result_type"), rs.getString("wrong_steps"), rs.getString("error_code"), Cot.realNeuCo(rs, "confidence"),
                rs.getString("reason"), rs.getString("citation"), rs.getString("raw")))
            .list();
        List<UUID> doan = jdbc.sql("select passage_id from verification_run_citations where run_id = :run order by passage_id")
            .param("run", l.id).query(UUID.class).list();
        return new VerificationRun(l.id, l.classId, l.kind, l.subjectId, l.contentHash, l.contentVersion, l.formulaSheetId,
            l.overall, l.publish, l.stale, l.createdAt, tang, doan);
    }

    /** Dòng của {@code verification_runs}, trước khi nạp các tầng và trích dẫn. */
    private static final class LuotCho {
        final UUID id;
        final UUID classId;
        final SubjectKind kind;
        final UUID subjectId;
        final String contentHash;
        final @Nullable Integer contentVersion;
        final @Nullable UUID formulaSheetId;
        final CheckStatus overall;
        final @Nullable ReleaseStatus publish;
        final boolean stale;
        final Instant createdAt;

        LuotCho(ResultSet rs) throws SQLException {
            id = Cot.uuid(rs, "id");
            classId = Cot.uuid(rs, "class_id");
            kind = SubjectKind.valueOf(Cot.chu(rs, "subject_kind"));
            subjectId = Cot.uuid(rs, "subject_id");
            contentHash = Cot.chu(rs, "content_hash");
            contentVersion = Cot.soNeuCo(rs, "content_version");
            formulaSheetId = Cot.uuidNeuCo(rs, "formula_sheet_id");
            overall = CheckStatus.valueOf(Cot.chu(rs, "overall_status"));
            String phatHanh = rs.getString("publish_status");
            publish = phatHanh == null ? null : ReleaseStatus.valueOf(phatHanh);
            stale = rs.getBoolean("stale");
            createdAt = Cot.thoiDiem(rs, "created_at");
        }
    }
}
