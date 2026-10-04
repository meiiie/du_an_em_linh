package vn.hoctapcanman.core.practice.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.InputEvent;
import vn.hoctapcanman.core.practice.domain.model.SignTable;
import vn.hoctapcanman.core.practice.domain.model.StepLine;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.model.SubmissionStatus;
import vn.hoctapcanman.core.practice.domain.model.TableCell;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;

/**
 * Bài làm trên {@code submissions}, nội dung bước trên {@code submission_steps}, {@code submission_tables},
 * {@code submission_table_cells}, sự kiện trên {@code input_events}. Mọi lần ghi phần con khóa dòng bài làm
 * {@code FOR NO KEY UPDATE} và kiểm còn {@code DANG_LAM} trước: hai tab nộp cùng bước nối tiếp nhau, và lần nộp bài
 * đồng thời chờ (trigger của V7 còn kiểm lại).
 */
@Repository
public class SubmissionRepositoryAdapter implements SubmissionRepository {

    private static final String COT = """
            id, class_id, student_id, problem_id, content_version, status, guess_suspected, guess_reason, result, started_at,
            submitted_at""";

    private final JdbcClient jdbc;

    public SubmissionRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Submission openOrGet(Submission moi) {
        if (!moi.isOpen()) {
            throw new IllegalArgumentException("Chỉ mở được bài làm đang làm");
        }
        // Hai tab mở cùng lúc: lần ghi sau chờ lần trước commit rồi trùng chỉ mục từng phần, không ghi; đọc lại thấy bài làm đó.
        jdbc.sql("""
                insert into submissions (id, class_id, student_id, problem_id, content_version, status, guess_suspected, started_at)
                values (:id, :lop, :hs, :bai, :pb, 'DANG_LAM', false, :luc)
                on conflict (student_id, class_id, problem_id, content_version) where status = 'DANG_LAM' do nothing""")
            .param("id", moi.id()).param("lop", moi.classId()).param("hs", moi.studentId()).param("bai", moi.problemId())
            .param("pb", moi.contentVersion()).param("luc", Cot.luc(moi.startedAt()))
            .update();
        return findOpen(moi.studentId(), moi.classId(), moi.problemId(), moi.contentVersion())
            .orElseThrow(() -> new IllegalStateException("Không mở được bài làm"));
    }

    @Override
    public Optional<Submission> findOpen(UUID studentId, UUID classId, UUID problemId, int contentVersion) {
        return jdbc.sql("select " + COT + " from submissions where student_id = :hs and class_id = :lop and problem_id = :bai"
                + " and content_version = :pb and status = 'DANG_LAM'")
            .param("hs", studentId).param("lop", classId).param("bai", problemId).param("pb", contentVersion)
            .query(SubmissionRepositoryAdapter::baiLam).optional();
    }

    @Override
    public Optional<Submission> findLatest(UUID studentId, UUID classId, UUID problemId) {
        return jdbc.sql("select " + COT + " from submissions where student_id = :hs and class_id = :lop and problem_id = :bai"
                + " order by started_at desc, id desc limit 1")
            .param("hs", studentId).param("lop", classId).param("bai", problemId)
            .query(SubmissionRepositoryAdapter::baiLam).optional();
    }

    @Override
    public Optional<Submission> findById(UUID id) {
        return jdbc.sql("select " + COT + " from submissions where id = :id").param("id", id)
            .query(SubmissionRepositoryAdapter::baiLam).optional();
    }

    @Override
    @Transactional
    public void saveStep(UUID submissionId, StepWork buoc) {
        khoaDangLam(submissionId);
        jdbc.sql("delete from submission_steps where submission_id = :bl and step_code = :buoc")
            .param("bl", submissionId).param("buoc", buoc.stepCode()).update();
        jdbc.sql("delete from submission_tables where submission_id = :bl and step_code = :buoc")
            .param("bl", submissionId).param("buoc", buoc.stepCode()).update();
        for (StepLine d : buoc.lines()) {
            jdbc.sql("""
                    insert into submission_steps (submission_id, step_code, line_no, latex, line_kind)
                    values (:bl, :buoc, :dong, :latex, :loai)""")
                .param("bl", submissionId).param("buoc", buoc.stepCode()).param("dong", d.lineNo()).param("latex", d.latex())
                .param("loai", d.kind())
                .update();
        }
        SignTable bang = buoc.table();
        if (bang != null) {
            jdbc.sql("insert into submission_tables (submission_id, step_code, table_kind) values (:bl, :buoc, :loai)")
                .param("bl", submissionId).param("buoc", buoc.stepCode()).param("loai", bang.kind()).update();
            int thuTu = 0;
            for (TableCell o : bang.cells()) {
                jdbc.sql("""
                        insert into submission_table_cells (submission_id, step_code, ordinal, row_code, k, value)
                        values (:bl, :buoc, :thuTu, :hang, :k, :giaTri)""")
                    .param("bl", submissionId).param("buoc", buoc.stepCode()).param("thuTu", thuTu++).param("hang", o.row())
                    .param("k", o.k()).param("giaTri", o.value())
                    .update();
            }
        }
    }

    @Override
    public List<StepWork> steps(UUID submissionId) {
        // Thứ tự bước của khung (step_templates.ordinal), rồi số dòng / thứ tự ô.
        Map<Integer, String> buocTheoThuTu = new TreeMap<>();
        Map<String, List<StepLine>> dong = new LinkedHashMap<>();
        jdbc.sql("""
                select s.step_code, t.ordinal, s.line_no, s.latex, s.line_kind from submission_steps s
                join step_templates t on t.step_code = s.step_code where s.submission_id = :bl order by t.ordinal, s.line_no""")
            .param("bl", submissionId)
            .query((ResultSet rs) -> {
                String ma = Cot.chu(rs, "step_code");
                buocTheoThuTu.put(rs.getInt("ordinal"), ma);
                dong.computeIfAbsent(ma, k -> new ArrayList<>())
                    .add(new StepLine(rs.getInt("line_no"), Cot.chu(rs, "latex"), rs.getString("line_kind")));
            });
        Map<String, String> loaiBang = new LinkedHashMap<>();
        Map<String, List<TableCell>> o = new LinkedHashMap<>();
        jdbc.sql("""
                select b.step_code, t.ordinal, b.table_kind, c.row_code, c.k, c.value from submission_tables b
                join step_templates t on t.step_code = b.step_code
                left join submission_table_cells c on c.submission_id = b.submission_id and c.step_code = b.step_code
                where b.submission_id = :bl order by t.ordinal, c.ordinal""")
            .param("bl", submissionId)
            .query((ResultSet rs) -> {
                String ma = Cot.chu(rs, "step_code");
                buocTheoThuTu.put(rs.getInt("ordinal"), ma);
                loaiBang.put(ma, Cot.chu(rs, "table_kind"));
                List<TableCell> cua = o.computeIfAbsent(ma, k -> new ArrayList<>());
                String hang = rs.getString("row_code");
                if (hang != null) {
                    cua.add(new TableCell(hang, Cot.soNeuCo(rs, "k"), Cot.chu(rs, "value")));
                }
            });
        List<StepWork> ra = new ArrayList<>();
        for (String ma : buocTheoThuTu.values()) {
            String loai = loaiBang.get(ma);
            SignTable bang = loai == null ? null : new SignTable(loai, o.getOrDefault(ma, List.of()));
            ra.add(new StepWork(ma, dong.getOrDefault(ma, List.of()), bang));
        }
        return ra;
    }

    @Override
    @Transactional
    public void addEvents(UUID submissionId, List<InputEvent> suKien) {
        if (suKien.isEmpty()) {
            return;
        }
        khoaDangLam(submissionId);
        for (InputEvent e : suKien) {
            jdbc.sql("""
                    insert into input_events (id, submission_id, step_code, cell_row, cell_k, old_value, new_value, at)
                    values (:id, :bl, :buoc, :hang, :k, :cu, :moi, :luc)""")
                .param("id", UUID.randomUUID()).param("bl", submissionId).param("buoc", e.stepCode()).param("hang", e.cellRow())
                .param("k", e.cellK()).param("cu", e.oldValue()).param("moi", e.newValue()).param("luc", Cot.luc(e.at()))
                .update();
        }
    }

    @Override
    public List<InputEvent> events(UUID submissionId) {
        return jdbc.sql("""
                select step_code, cell_row, cell_k, old_value, new_value, at from input_events where submission_id = :bl
                order by at, id""")
            .param("bl", submissionId)
            .query((rs, n) -> new InputEvent(Cot.chu(rs, "step_code"), rs.getString("cell_row"), Cot.soNeuCo(rs, "cell_k"),
                rs.getString("old_value"), Cot.chu(rs, "new_value"), Cot.thoiDiem(rs, "at")))
            .list();
    }

    @Override
    @Transactional
    public void update(Submission baiLam) {
        khoaDangLam(baiLam.id());
        int dong = jdbc.sql("""
                update submissions set status = :st, guess_suspected = :nghi, guess_reason = :lyDo, result = :kq, submitted_at = :nop
                where id = :id and status = 'DANG_LAM'""")
            .param("st", baiLam.status().name()).param("nghi", baiLam.guessSuspected()).param("lyDo", baiLam.guessReason())
            .param("kq", tenNeuCo(baiLam.result())).param("nop", Cot.lucNeuCo(baiLam.submittedAt())).param("id", baiLam.id())
            .update();
        if (dong != 1) {
            throw new IllegalStateException("Bài làm đã nộp hoặc không có");
        }
    }

    /**
     * Khóa dòng bài làm ({@code FOR NO KEY UPDATE}) và dòng bài ({@code FOR SHARE}) cho tới hết giao dịch; bài làm đã nộp,
     * không có, hay là của phiên bản nội dung cũ thì {@link IllegalStateException}. Dùng chung cho mọi lần ghi phần con
     * (cả kết quả chấm), để trigger của V7 không phải nâng khóa.
     */
    static void khoaDangLam(JdbcClient jdbc, UUID submissionId) {
        boolean ghiDuoc = jdbc.sql("""
                select s.status = 'DANG_LAM' and s.content_version = p.content_version from submissions s
                join problems p on p.id = s.problem_id where s.id = :id for no key update of s for share of p""")
            .param("id", submissionId).query(Boolean.class).optional().orElse(false);
        if (!ghiDuoc) {
            throw new IllegalStateException("Bài làm đã nộp, không có, hay là của phiên bản nội dung cũ");
        }
    }

    private void khoaDangLam(UUID submissionId) {
        khoaDangLam(jdbc, submissionId);
    }

    private static @Nullable String tenNeuCo(@Nullable GradeStatus s) {
        return s == null ? null : s.name();
    }

    private static Submission baiLam(ResultSet rs, int n) throws SQLException {
        String kq = rs.getString("result");
        return new Submission(Cot.uuid(rs, "id"), Cot.uuid(rs, "class_id"), Cot.uuid(rs, "student_id"), Cot.uuid(rs, "problem_id"),
            rs.getInt("content_version"), SubmissionStatus.valueOf(Cot.chu(rs, "status")), rs.getBoolean("guess_suspected"),
            rs.getString("guess_reason"), kq == null ? null : GradeStatus.valueOf(kq), Cot.thoiDiem(rs, "started_at"),
            Cot.thoiDiemNeuCo(rs, "submitted_at"));
    }
}
