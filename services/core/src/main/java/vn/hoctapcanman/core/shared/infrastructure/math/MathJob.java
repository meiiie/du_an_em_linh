package vn.hoctapcanman.core.shared.infrastructure.math;

import java.time.Duration;

/**
 * Job của {@code services/math} mà core gọi, kèm hết giờ phía core theo
 * {@code specs/001-lat-cat-doc/contracts/math-v1.md}. Dịch vụ toán tự chặn trần 20 s cho mỗi job.
 */
public enum MathJob {
    GRADE("/v1/grade", Duration.ofSeconds(12)),
    VERIFY("/v1/verify", Duration.ofSeconds(20)),
    FILTER("/v1/filter", Duration.ofSeconds(12)),
    GOI_Y("/v1/goi-y", Duration.ofSeconds(12)),
    GENERATE("/v1/generate", Duration.ofSeconds(20)),
    KIEM_LOI_GIANG("/v1/kiem-loi-giang", Duration.ofSeconds(12)),
    KIEM_DONG_CONG_THUC("/v1/kiem-dong-cong-thuc", Duration.ofSeconds(20));

    private final String path;
    private final Duration timeout;

    MathJob(String path, Duration timeout) {
        this.path = path;
        this.timeout = timeout;
    }

    public String path() {
        return path;
    }

    public Duration timeout() {
        return timeout;
    }
}
