package vn.hoctoanai.core.identity.domain.repository;

import java.time.Instant;
import java.util.List;
import vn.hoctoanai.core.identity.domain.model.LoginAttemptKey;

/** Nhật ký lần đăng nhập sai theo khóa đếm (F-10). */
public interface LoginFailureRepository {

    /**
     * Khóa khóa đếm tới hết giao dịch: các lần đăng nhập cùng email + IP chạy nối tiếp, nên gửi song song cũng không thử
     * được quá ngưỡng.
     */
    void lock(LoginAttemptKey key);

    /** Tối đa {@code limit} lần sai gần nhất sau mốc {@code since}, mới nhất trước. */
    List<Instant> recentSince(LoginAttemptKey key, Instant since, int limit);

    void record(LoginAttemptKey key, Instant at);

    void clear(LoginAttemptKey key);

    /** Xóa bản ghi cũ hơn mốc; trả số dòng đã xóa. */
    int deleteBefore(Instant cutoff);
}
