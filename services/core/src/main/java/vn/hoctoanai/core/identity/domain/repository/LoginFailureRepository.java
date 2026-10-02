package vn.hoctoanai.core.identity.domain.repository;

import java.time.Instant;
import vn.hoctoanai.core.identity.domain.model.LoginAttemptKey;

/** Nhật ký lần đăng nhập sai theo khóa đếm (F-10). */
public interface LoginFailureRepository {

    /**
     * Khóa khóa đếm tới hết giao dịch: các lần đăng nhập cùng email + IP chạy nối tiếp, nên gửi song song cũng không thử
     * được quá ngưỡng.
     */
    void lock(LoginAttemptKey key);

    int countSince(LoginAttemptKey key, Instant since);

    void record(LoginAttemptKey key, Instant at);

    void clear(LoginAttemptKey key);

    /** Xóa bản ghi cũ hơn mốc; trả số dòng đã xóa. */
    int deleteBefore(Instant cutoff);
}
