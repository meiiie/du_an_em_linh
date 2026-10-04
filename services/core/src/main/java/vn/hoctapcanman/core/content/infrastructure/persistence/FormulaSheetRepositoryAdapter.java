package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.domain.model.CheckStatus;
import vn.hoctapcanman.core.content.domain.model.Formula;
import vn.hoctapcanman.core.content.domain.model.FormulaKind;
import vn.hoctapcanman.core.content.domain.model.FormulaSheet;
import vn.hoctapcanman.core.content.domain.model.SheetStatus;
import vn.hoctapcanman.core.content.domain.repository.FormulaSheetRepository;

/**
 * Bảng công thức trên {@code formula_sheets}, {@code formulas}, {@code formula_citations}. Bảng khóa được ghi theo thứ tự
 * trigger của V4 đòi: bảng {@code NHAP} và các dòng, rồi đổi sang {@code KHOA}; CSDL tự kiểm lần nữa lúc đổi (bảng có dòng,
 * mọi dòng {@code DAT} có trích dẫn), và mọi trích dẫn khóa căn cứ {@code FOR SHARE} (V5).
 *
 * <p>Đọc một bảng có thể là nháp (đang sửa) thì đọc trong một giao dịch, khóa dòng bảng {@code FOR SHARE} trước khi đọc dòng
 * và trích dẫn: lần ghi luôn khóa dòng bảng trước khi đụng tới dòng con, nên lần đọc chờ lần ghi đang dở commit rồi mới đọc,
 * không trộn thông tin bảng cũ với dòng mới. Bảng đã khóa không đổi được (V4), nên {@link #findCurrent} không cần khóa.
 */
@Repository
public class FormulaSheetRepositoryAdapter implements FormulaSheetRepository {

    private static final String COT_BANG = "id, class_id, version, status, note, fingerprint, locked_at, locked_by, created_at";
    private static final String COT_DONG = """
            id, ordinal, code, skill_code, title, latex, statement, kind, tier1_status, tier2_status, tier1_detail,
            tier2_detail, citation_passage_id, checked_fingerprint""";

    private final JdbcClient jdbc;

    public FormulaSheetRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void save(FormulaSheet s) {
        // Bảng đã khóa ở CSDL: chỉ nhận ghi lại đúng bảng đó (cùng dấu vân tay các dòng, importer chạy lại), không gì khác.
        Optional<String> dauKhoa = jdbc.sql("select fingerprint from formula_sheets where id = :id and status = 'KHOA' for update")
            .param("id", s.id()).query(String.class).optional();
        if (dauKhoa.isPresent()) {
            if (s.status() != SheetStatus.KHOA || !dauKhoa.get().equals(s.fingerprint())) {
                throw new IllegalStateException("Bảng " + s.id() + " đã khóa, không ghi khác đi được; tạo bảng nháp phiên bản mới");
            }
            return;
        }
        jdbc.sql("""
                insert into formula_sheets (id, class_id, version, status, note, created_at)
                values (:id, :lop, :version, 'NHAP', :note, :created)
                on conflict (id) do update set version = excluded.version, note = excluded.note""")
            .param("id", s.id()).param("lop", s.classId()).param("version", s.version()).param("note", s.note())
            .param("created", Cot.luc(s.createdAt()))
            .update();
        jdbc.sql("delete from formulas where formula_sheet_id = :id").param("id", s.id()).update();
        for (Formula f : s.rows()) {
            ghiDong(s.id(), f);
        }
        if (s.status() == SheetStatus.KHOA) {
            jdbc.sql("""
                    update formula_sheets set status = 'KHOA', fingerprint = :fp, locked_at = :at, locked_by = :by
                    where id = :id""")
                .param("fp", s.fingerprint()).param("at", Cot.luc(Objects.requireNonNull(s.lockedAt())))
                .param("by", s.lockedBy()).param("id", s.id())
                .update();
        }
    }

    private void ghiDong(UUID bang, Formula f) {
        jdbc.sql("insert into formulas (formula_sheet_id, " + COT_DONG + """
                ) values (:bang, :id, :ordinal, :code, :skill, :title, :latex, :statement, :kind, :t1, :t2,
                    cast(:d1 as jsonb), cast(:d2 as jsonb), :cite, :fp)""")
            .param("bang", bang).param("id", f.id()).param("ordinal", f.ordinal()).param("code", f.code())
            .param("skill", f.skillCode()).param("title", f.title()).param("latex", f.latex()).param("statement", f.statement())
            .param("kind", ten(f.kind())).param("t1", ten(f.tier1Status())).param("t2", ten(f.tier2Status()))
            .param("d1", f.tier1DetailJson()).param("d2", f.tier2DetailJson()).param("cite", f.citationPassageId())
            .param("fp", f.checkedFingerprint())
            .update();
        for (UUID doan : f.extraCitationPassageIds()) {
            jdbc.sql("insert into formula_citations (formula_id, passage_id) values (:f, :p)").param("f", f.id()).param("p", doan).update();
        }
    }

    @Override
    @Transactional
    public Optional<FormulaSheet> findById(UUID id) {
        return jdbc.sql("select " + COT_BANG + " from formula_sheets where id = :id for share").param("id", id)
            .query((rs, n) -> new BangCho(rs)).optional().map(this::dung);
    }

    @Override
    public Optional<FormulaSheet> findCurrent(UUID classId) {
        return jdbc.sql("select " + COT_BANG + " from formula_sheets where class_id = :lop and status = 'KHOA' order by version desc limit 1")
            .param("lop", classId).query((rs, n) -> new BangCho(rs)).optional().map(this::dung);
    }

    @Override
    @Transactional
    public Optional<FormulaSheet> findDraft(UUID classId) {
        return jdbc.sql("select " + COT_BANG + " from formula_sheets where class_id = :lop and status = 'NHAP' for share")
            .param("lop", classId).query((rs, n) -> new BangCho(rs)).optional().map(this::dung);
    }

    private FormulaSheet dung(BangCho b) {
        Map<UUID, List<UUID>> trichThem = jdbc.sql("""
                select c.formula_id, c.passage_id from formula_citations c join formulas f on f.id = c.formula_id
                where f.formula_sheet_id = :id order by c.passage_id""")
            .param("id", b.id)
            .query((rs, n) -> Map.entry(Cot.uuid(rs, "formula_id"), Cot.uuid(rs, "passage_id"))).list().stream()
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        List<Formula> dong = jdbc.sql("select " + COT_DONG + " from formulas where formula_sheet_id = :id order by ordinal")
            .param("id", b.id)
            .query((rs, n) -> dongCongThuc(rs, trichThem.getOrDefault(Cot.uuid(rs, "id"), List.of())))
            .list();
        return new FormulaSheet(b.id, b.classId, b.version, b.status, b.note, b.fingerprint, b.lockedAt, b.lockedBy, b.createdAt, dong);
    }

    private static Formula dongCongThuc(ResultSet rs, List<UUID> trichThem) throws SQLException {
        String kind = rs.getString("kind");
        return new Formula(Cot.uuid(rs, "id"), rs.getInt("ordinal"), Cot.chu(rs, "code"), rs.getString("skill_code"),
            Cot.chu(rs, "title"), Cot.chu(rs, "latex"), Cot.chu(rs, "statement"), kind == null ? null : FormulaKind.valueOf(kind),
            trangThai(rs.getString("tier1_status")), trangThai(rs.getString("tier2_status")), rs.getString("tier1_detail"),
            rs.getString("tier2_detail"), Cot.uuidNeuCo(rs, "citation_passage_id"), trichThem, rs.getString("checked_fingerprint"));
    }

    private static @Nullable CheckStatus trangThai(@Nullable String ten) {
        return ten == null ? null : CheckStatus.valueOf(ten);
    }

    private static @Nullable String ten(@Nullable Enum<?> e) {
        return e == null ? null : e.name();
    }

    /** Dòng của {@code formula_sheets}, trước khi nạp các dòng công thức. */
    private static final class BangCho {
        final UUID id;
        final UUID classId;
        final int version;
        final SheetStatus status;
        final @Nullable String note;
        final @Nullable String fingerprint;
        final @Nullable Instant lockedAt;
        final @Nullable UUID lockedBy;
        final Instant createdAt;

        BangCho(ResultSet rs) throws SQLException {
            id = Cot.uuid(rs, "id");
            classId = Cot.uuid(rs, "class_id");
            version = rs.getInt("version");
            status = SheetStatus.valueOf(Cot.chu(rs, "status"));
            note = rs.getString("note");
            fingerprint = rs.getString("fingerprint");
            Timestamp khoaLuc = rs.getTimestamp("locked_at");
            lockedAt = khoaLuc == null ? null : khoaLuc.toInstant();
            lockedBy = Cot.uuidNeuCo(rs, "locked_by");
            createdAt = Cot.thoiDiem(rs, "created_at");
        }
    }
}
