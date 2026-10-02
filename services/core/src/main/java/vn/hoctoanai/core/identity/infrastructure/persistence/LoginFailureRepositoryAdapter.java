package vn.hoctoanai.core.identity.infrastructure.persistence;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.hoctoanai.core.identity.domain.model.LoginAttemptKey;
import vn.hoctoanai.core.identity.domain.repository.LoginFailureRepository;

/**
 * Bảng {@code login_failures} qua JDBC (không cần entity JPA). Chạy trong giao dịch của use case: JpaTransactionManager
 * cho JdbcClient dùng chung kết nối, nên khóa tư vấn {@code pg_advisory_xact_lock} giữ tới khi giao dịch xong.
 */
@Repository
public class LoginFailureRepositoryAdapter implements LoginFailureRepository {

    private final JdbcClient jdbc;

    public LoginFailureRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lock(LoginAttemptKey key) {
        // 8 byte đầu của băm làm khóa bigint; trùng khóa giữa hai email + IP khác nhau chỉ làm chúng chờ nhau.
        long khoa = ByteBuffer.wrap(HexFormat.of().parseHex(key.hash().substring(0, 16))).getLong();
        jdbc.sql("select pg_advisory_xact_lock(?)").param(khoa).query((rs, i) -> 1).list();
    }

    @Override
    public List<Instant> recentSince(LoginAttemptKey key, Instant since, int limit) {
        return jdbc.sql("select created_at from login_failures where key_hash = ? and created_at > ? order by created_at desc limit ?")
                .params(key.hash(), Timestamp.from(since), limit)
                .query((rs, i) -> rs.getTimestamp(1).toInstant())
                .list();
    }

    @Override
    public void record(LoginAttemptKey key, Instant at) {
        jdbc.sql("insert into login_failures (key_hash, created_at) values (?, ?)").params(key.hash(), Timestamp.from(at)).update();
    }

    @Override
    public void clear(LoginAttemptKey key) {
        jdbc.sql("delete from login_failures where key_hash = ?").param(key.hash()).update();
    }

    @Override
    public int deleteBefore(Instant cutoff) {
        return jdbc.sql("delete from login_failures where created_at < ?").param(Timestamp.from(cutoff)).update();
    }
}
