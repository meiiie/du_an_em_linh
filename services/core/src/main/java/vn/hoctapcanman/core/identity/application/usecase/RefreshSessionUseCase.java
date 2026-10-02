package vn.hoctapcanman.core.identity.application.usecase;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.identity.application.dto.AuthResponse;
import vn.hoctapcanman.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctapcanman.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctapcanman.core.identity.application.service.SessionIssuer;
import vn.hoctapcanman.core.identity.domain.model.AuthSession;
import vn.hoctapcanman.core.identity.domain.model.RefreshToken;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.repository.AuthSessionRepository;
import vn.hoctapcanman.core.identity.domain.repository.RefreshTokenRepository;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/**
 * Làm mới phiên: xoay vòng refresh token trong cùng phiên. Khóa dòng phiên trước mọi kiểm tra, nên làm mới và đăng xuất
 * đồng thời trên cùng phiên chạy nối tiếp. Token đã thu hồi mà bị dùng lại (kể cả thua cuộc đua với yêu cầu đồng thời)
 * là dấu hiệu lộ token: thu hồi mọi phiên của người dùng. Không rollback khi ném lỗi để việc thu hồi được giữ lại.
 * Frontend gọi làm mới một luồng một lúc (#57).
 */
@Service
public class RefreshSessionUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final AuthSessionRepository authSessions;
    private final UserRepository users;
    private final SessionIssuer sessions;
    private final Clock clock;

    public RefreshSessionUseCase(
            RefreshTokenRepository refreshTokens,
            AuthSessionRepository authSessions,
            UserRepository users,
            SessionIssuer sessions,
            Clock clock) {
        this.refreshTokens = refreshTokens;
        this.authSessions = authSessions;
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthResponse execute(RefreshTokenRequest request) {
        Instant now = clock.instant();
        RefreshToken token =
                refreshTokens.findByTokenHash(RefreshToken.hash(request.refreshToken())).orElseThrow(RefreshSessionUseCase::hetHan);
        AuthSession session = authSessions.findByIdForUpdate(token.sessionId()).orElseThrow(RefreshSessionUseCase::hetHan);
        if (session.isRevoked()) {
            throw hetHan();
        }
        if (token.isRevoked()) {
            authSessions.revokeAllForUser(token.userId(), now);
            throw hetHan();
        }
        if (!token.isActive(now)) {
            throw hetHan();
        }
        User user = users.findById(token.userId()).filter(User::enabled).orElseThrow(RefreshSessionUseCase::hetHan);
        if (!refreshTokens.revokeIfActive(token.tokenHash(), now)) {
            authSessions.revokeAllForUser(token.userId(), now);
            throw hetHan();
        }
        return sessions.issue(session, user, now);
    }

    private static AuthenticationFailedException hetHan() {
        return new AuthenticationFailedException(AuthenticationFailedException.PHIEN_HET_HAN);
    }
}
