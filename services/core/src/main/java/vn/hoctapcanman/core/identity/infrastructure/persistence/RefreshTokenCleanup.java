package vn.hoctapcanman.core.identity.infrastructure.persistence;

import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.identity.domain.repository.AuthSessionRepository;
import vn.hoctapcanman.core.identity.domain.repository.RefreshTokenRepository;

/**
 * Dọn mỗi đêm: refresh token đã hết hạn quá một ngày, rồi các phiên không còn token nào. Token đã thu hồi mà còn hạn vẫn
 * giữ để phát hiện dùng lại; sau khi hết hạn, token không dùng được nữa nên xóa không mất gì.
 */
@Component
public class RefreshTokenCleanup {

    private static final Duration BIEN_AN_TOAN = Duration.ofDays(1);

    private final RefreshTokenRepository refreshTokens;
    private final AuthSessionRepository authSessions;
    private final Clock clock;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokens, AuthSessionRepository authSessions, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.authSessions = authSessions;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.identity.refresh-token-cleanup-cron:0 17 3 * * *}")
    @Transactional
    public int purgeExpired() {
        int tokens = refreshTokens.deleteExpiredBefore(clock.instant().minus(BIEN_AN_TOAN));
        return tokens + authSessions.deleteWithoutTokens();
    }
}
