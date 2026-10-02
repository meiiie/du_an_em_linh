package vn.hoctoanai.core.identity.application.usecase;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.service.SessionIssuer;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.repository.RefreshTokenRepository;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;

/**
 * Làm mới phiên: xoay vòng refresh token (thu hồi token cũ, cấp token mới). Token đã thu hồi mà bị dùng lại là dấu hiệu
 * lộ token: thu hồi mọi phiên của người dùng đó. Không rollback khi ném lỗi để việc thu hồi được giữ lại.
 */
@Service
public class RefreshSessionUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final SessionIssuer sessions;
    private final Clock clock;

    public RefreshSessionUseCase(RefreshTokenRepository refreshTokens, UserRepository users, SessionIssuer sessions, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthResponse execute(RefreshTokenRequest request) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByTokenHash(RefreshToken.hash(request.refreshToken())).orElseThrow(RefreshSessionUseCase::hetHan);
        if (token.isRevoked()) {
            refreshTokens.revokeAllActive(token.userId(), now);
            throw hetHan();
        }
        if (!token.isActive(now)) {
            throw hetHan();
        }
        User user = users.findById(token.userId()).filter(User::enabled).orElseThrow(RefreshSessionUseCase::hetHan);
        refreshTokens.save(token.revoke(now));
        return sessions.issue(user, now);
    }

    private static AuthenticationFailedException hetHan() {
        return new AuthenticationFailedException(AuthenticationFailedException.PHIEN_HET_HAN);
    }
}
