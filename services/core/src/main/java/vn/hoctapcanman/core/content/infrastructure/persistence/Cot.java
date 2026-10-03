package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Đọc / ghi cột dùng chung của các adapter JDBC nội dung. */
final class Cot {

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

    /**
     * Cột {@code real} (4 byte): đọc theo chữ số ngắn nhất của số float rồi đổi sang double, để 0.35 ghi vào đọc lại đúng
     * 0.35 chứ không phải 0.3499999940395355.
     */
    static @Nullable Double realNeuCo(ResultSet rs, String cot) throws SQLException {
        float so = rs.getFloat(cot);
        return rs.wasNull() ? null : Double.valueOf(Float.toString(so));
    }

    static Instant thoiDiem(ResultSet rs, String cot) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(cot), cot).toInstant();
    }

    static List<String> mang(ResultSet rs, String cot) throws SQLException {
        Array mang = rs.getArray(cot);
        return mang == null ? List.of() : Arrays.stream((Object[]) mang.getArray()).map(String::valueOf).toList();
    }

    /** Mảng mã, ghi bằng {@code string_to_array(:x, ',')}: mã đã qua {@code Kiem.ma} / {@code Kiem.maHoa}, không có dấu phẩy. */
    static String noiMa(List<String> ma) {
        return String.join(",", ma);
    }

    /** PostgreSQL giữ tới micro giây và làm tròn phần lẻ hơn: cắt trước khi ghi để đọc lại đúng giá trị đã cắt. */
    static Timestamp luc(Instant instant) {
        return Timestamp.from(instant.truncatedTo(ChronoUnit.MICROS));
    }
}
