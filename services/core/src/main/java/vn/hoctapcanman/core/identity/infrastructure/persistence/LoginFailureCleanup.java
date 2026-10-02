package vn.hoctapcanman.core.identity.infrastructure.persistence;

import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.identity.domain.repository.LoginFailureRepository;

/** Dọn lần đăng nhập sai quá một ngày, mỗi đêm: cửa sổ đếm chỉ 15 phút, giữ lâu hơn không có ích. */
@Component
public class LoginFailureCleanup {

    private static final Duration GIU_LAI = Duration.ofDays(1);

    private final LoginFailureRepository failures;
    private final Clock clock;

    public LoginFailureCleanup(LoginFailureRepository failures, Clock clock) {
        this.failures = failures;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.identity.login-failure-cleanup-cron:0 27 3 * * *}")
    @Transactional
    public int purgeOld() {
        return failures.deleteBefore(clock.instant().minus(GIU_LAI));
    }
}
