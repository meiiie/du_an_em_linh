package vn.hoctapcanman.core.identity.application.usecase;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctapcanman.core.identity.domain.model.RefreshToken;
import vn.hoctapcanman.core.identity.domain.repository.AuthSessionRepository;
import vn.hoctapcanman.core.identity.domain.repository.RefreshTokenRepository;

/**
 * Đăng xuất: thu hồi cả phiên của refresh token, kể cả khi token đó đã bị xoay vòng. UPDATE trên dòng phiên chờ khóa
 * của lần làm mới đang chạy, nên token vừa cấp cũng vô hiệu. Lặp lại hay token lạ đều không báo lỗi. Access token đã
 * cấp còn hạn tối đa 15 phút (JWT không trạng thái).
 */
@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final AuthSessionRepository authSessions;
    private final Clock clock;

    public LogoutUseCase(RefreshTokenRepository refreshTokens, AuthSessionRepository authSessions, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.authSessions = authSessions;
        this.clock = clock;
    }

    @Transactional
    public void execute(RefreshTokenRequest request) {
        refreshTokens.findByTokenHash(RefreshToken.hash(request.refreshToken()))
                .ifPresent(token -> authSessions.revoke(token.sessionId(), clock.instant()));
    }
}
