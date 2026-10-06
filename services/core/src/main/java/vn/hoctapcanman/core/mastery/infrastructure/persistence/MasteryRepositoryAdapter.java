package vn.hoctapcanman.core.mastery.infrastructure.persistence;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.mastery.domain.model.BktConfig;
import vn.hoctapcanman.core.mastery.domain.model.Level4;
import vn.hoctapcanman.core.mastery.domain.model.MasteryEvent;
import vn.hoctapcanman.core.mastery.domain.model.MasteryState;
import vn.hoctapcanman.core.mastery.domain.repository.MasteryRepository;

/**
 * Mức hiểu trên {@code mastery_config}, {@code mastery_states}, {@code mastery_events}. Cột {@code real} (mastery, thay đổi,
 * độ tin cậy) đọc theo chữ số ngắn nhất của số float, như postgres.js của v0 đọc: lượt sau tính trên đúng số v0 tính.
 */
@Repository
public class MasteryRepositoryAdapter implements MasteryRepository {

    private static final String COT_TRANG_THAI = """
            student_id, skill_code, mastery, level4, attempts, stuck_counter, last_error_codes, completed_at""";
    private static final String COT_SU_KIEN = """
            id, student_id, skill_code, submission_id, delta, rule_applied, wrong_step, error_code, confidence, guess_suspected,
            level4_before, level4_after, config_version, created_at""";

    private final JdbcClient jdbc;

    public MasteryRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BktConfig config() {
        return jdbc.sql("""
                select version, (value->>'p_t')::float8 as p_t, (value->>'p_g')::float8 as p_g, (value->>'p_s')::float8 as p_s,
                    (value->>'nguong_tin_cay_ma_loi')::float8 as tin_cay, (value->>'so_luot_ket')::int as so_luot_ket,
                    (value#>>'{nguong_muc,THONG_HIEU}')::float8 as th, (value#>>'{nguong_muc,VAN_DUNG}')::float8 as vd,
                    (value#>>'{nguong_muc,VAN_DUNG_CAO}')::float8 as vdc
                from mastery_config where key = 'bkt'""")
            .query((rs, n) -> new BktConfig(rs.getInt("version"), so(rs, "p_t"), so(rs, "p_g"), so(rs, "p_s"), so(rs, "tin_cay"),
                (int) so(rs, "so_luot_ket"), new BktConfig.Thresholds(so(rs, "th"), so(rs, "vd"), so(rs, "vdc"))))
            .optional().orElseThrow(() -> new IllegalStateException("Thiếu dòng bkt của mastery_config (V9)"));
    }

    @Override
    public Optional<MasteryEvent> eventOf(UUID submissionId) {
        return jdbc.sql("select " + COT_SU_KIEN + " from mastery_events where submission_id = :id").param("id", submissionId)
            .query(MasteryRepositoryAdapter::suKien).optional();
    }

    @Override
    public MasteryState lock(MasteryState initial) {
        jdbc.sql("""
                insert into mastery_states (%s) values (:student, :skill, :mastery, :level, :attempts, :stuck,
                    string_to_array(:codes, ','), :completed)
                on conflict do nothing""".formatted(COT_TRANG_THAI))
            .params(thamSo(initial)).update();
        return jdbc.sql("select " + COT_TRANG_THAI + " from mastery_states where student_id = :student and skill_code = :skill for update")
            .param("student", initial.studentId()).param("skill", initial.skillCode())
            .query(MasteryRepositoryAdapter::trangThai).single();
    }

    @Override
    public void save(MasteryState after, MasteryEvent e) {
        int doi = jdbc.sql("""
                update mastery_states set mastery = :mastery, level4 = :level, attempts = :attempts, stuck_counter = :stuck,
                    last_error_codes = string_to_array(:codes, ','), completed_at = :completed
                where student_id = :student and skill_code = :skill""")
            .params(thamSo(after)).update();
        if (doi != 1) {
            throw new IllegalStateException("Trạng thái mức hiểu chưa được khóa trước khi ghi");
        }
        jdbc.sql("insert into mastery_events (" + COT_SU_KIEN + """
                ) values (:id, :student, :skill, :submission, :delta, :rule, :step, :error, :confidence, :guess, :before, :after,
                    :version, :at)""")
            .param("id", e.id()).param("student", e.studentId()).param("skill", e.skillCode()).param("submission", e.submissionId())
            .param("delta", e.delta()).param("rule", e.rule().name()).param("step", e.wrongStep()).param("error", e.errorCode())
            .param("confidence", e.confidence()).param("guess", e.guessSuspected()).param("before", e.levelBefore().name())
            .param("after", e.levelAfter().name()).param("version", e.configVersion()).param("at", luc(e.createdAt()))
            .update();
    }

    @Override
    public List<MasteryState> statesOf(Collection<UUID> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + COT_TRANG_THAI + " from mastery_states where student_id in (:ids)").param("ids", studentIds)
            .query(MasteryRepositoryAdapter::trangThai).list();
    }

    private static Map<String, @Nullable Object> thamSo(MasteryState s) {
        Map<String, @Nullable Object> p = new HashMap<>();
        p.put("student", s.studentId());
        p.put("skill", s.skillCode());
        p.put("mastery", s.mastery());
        p.put("level", s.level().name());
        p.put("attempts", s.attempts());
        p.put("stuck", s.stuckCounter());
        p.put("codes", String.join(",", s.lastErrorCodes()));
        p.put("completed", s.completedAt() == null ? null : luc(s.completedAt()));
        return p;
    }

    private static MasteryState trangThai(ResultSet rs, int n) throws SQLException {
        Timestamp xong = rs.getTimestamp("completed_at");
        return new MasteryState(rs.getObject("student_id", UUID.class), rs.getString("skill_code"), real(rs, "mastery"),
            Level4.valueOf(rs.getString("level4")), rs.getInt("attempts"), rs.getInt("stuck_counter"), mang(rs.getArray("last_error_codes")),
            xong == null ? null : xong.toInstant());
    }

    private static MasteryEvent suKien(ResultSet rs, int n) throws SQLException {
        float tinCay = rs.getFloat("confidence");
        Double doTinCay = rs.wasNull() ? null : Double.valueOf(Float.toString(tinCay));
        return new MasteryEvent(rs.getObject("id", UUID.class), rs.getObject("student_id", UUID.class), rs.getString("skill_code"),
            rs.getObject("submission_id", UUID.class), real(rs, "delta"), MasteryEvent.Rule.valueOf(rs.getString("rule_applied")),
            rs.getString("wrong_step"), rs.getString("error_code"), doTinCay, rs.getBoolean("guess_suspected"),
            Level4.valueOf(rs.getString("level4_before")), Level4.valueOf(rs.getString("level4_after")), rs.getInt("config_version"),
            Objects.requireNonNull(rs.getTimestamp("created_at")).toInstant());
    }

    /** Cột {@code real}: chữ số ngắn nhất của số float, như postgres.js ({@code +text}) và {@code Cot.realNeuCo} của practice. */
    private static double real(ResultSet rs, String cot) throws SQLException {
        return Double.parseDouble(Float.toString(rs.getFloat(cot)));
    }

    private static double so(ResultSet rs, String cot) throws SQLException {
        double so = rs.getDouble(cot);
        if (rs.wasNull()) {
            throw new IllegalStateException("Thiếu tham số " + cot + " trong dòng bkt của mastery_config");
        }
        return so;
    }

    private static List<String> mang(Array a) throws SQLException {
        return List.of((String[]) a.getArray());
    }

    /** PostgreSQL giữ tới micro giây: cắt trước khi ghi để đọc lại đúng giá trị. */
    private static Timestamp luc(Instant at) {
        return Timestamp.from(at.truncatedTo(ChronoUnit.MICROS));
    }
}
