package vn.hoctapcanman.core.practice.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Đọc / ghi cột dùng chung của các adapter JDBC của practice. */
final class Cot {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<LinkedHashMap<String, String>> BANG_CHU = new TypeReference<>() {};

    private Cot() {}

    static UUID uuid(ResultSet rs, String cot) throws SQLException {
        return Objects.requireNonNull(rs.getObject(cot, UUID.class), cot);
    }

    static @Nullable UUID uuidNeuCo(ResultSet rs, String cot) throws SQLException {
        return rs.getObject(cot, UUID.class);
    }

    static String chu(ResultSet rs, String cot) throws SQLException {
        return Objects.requireNonNull(rs.getString(cot), cot);
    }

    static @Nullable Integer soNeuCo(ResultSet rs, String cot) throws SQLException {
        int so = rs.getInt(cot);
        return rs.wasNull() ? null : so;
    }

    static @Nullable Boolean dungSaiNeuCo(ResultSet rs, String cot) throws SQLException {
        boolean b = rs.getBoolean(cot);
        return rs.wasNull() ? null : b;
    }

    /** Cột {@code real}: đọc theo chữ số ngắn nhất của số float, để 0.35 ghi vào đọc lại đúng 0.35. */
    static @Nullable Double realNeuCo(ResultSet rs, String cot) throws SQLException {
        float so = rs.getFloat(cot);
        return rs.wasNull() ? null : Double.valueOf(Float.toString(so));
    }

    static Instant thoiDiem(ResultSet rs, String cot) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(cot), cot).toInstant();
    }

    static @Nullable Instant thoiDiemNeuCo(ResultSet rs, String cot) throws SQLException {
        Timestamp t = rs.getTimestamp(cot);
        return t == null ? null : t.toInstant();
    }

    /** PostgreSQL giữ tới micro giây và làm tròn phần lẻ hơn: cắt trước khi ghi để đọc lại đúng giá trị đã cắt. */
    static Timestamp luc(Instant instant) {
        return Timestamp.from(instant.truncatedTo(ChronoUnit.MICROS));
    }

    static @Nullable Timestamp lucNeuCo(@Nullable Instant instant) {
        return instant == null ? null : luc(instant);
    }

    static String json(Map<String, String> bang) {
        return JSON.writeValueAsString(bang);
    }

    static Map<String, String> bangChu(String json) {
        return JSON.readValue(json, BANG_CHU);
    }
}
