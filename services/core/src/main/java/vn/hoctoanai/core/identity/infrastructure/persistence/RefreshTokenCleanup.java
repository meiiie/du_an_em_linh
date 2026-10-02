package vn.hoctoanai.core.identity.infrastructure.persistence;

import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctoanai.core.identity.domain.repository.RefreshTokenRepository;

/**
 * Dọn refresh token đã hết hạn quá một ngày, mỗi đêm. Token đã thu hồi mà còn hạn vẫn giữ để phát hiện dùng lại; sau
 * khi hết hạn, token không dùng được nữa nên xóa không mất gì.
 */
@Component
public class RefreshTokenCleanup {

    private static final Duration BIEN_AN_TOAN = Duration.ofDays(1);

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.identity.refresh-token-cleanup-cron:0 17 3 * * *}")
    @Transactional
    public int purgeExpired() {
        return refreshTokens.deleteExpiredBefore(clock.instant().minus(BIEN_AN_TOAN));
    }
}
