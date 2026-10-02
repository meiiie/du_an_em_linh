package vn.hoctoanai.core.identity.application.usecase;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.repository.RefreshTokenRepository;

/** Đăng xuất: thu hồi refresh token. Lặp lại hay token lạ đều không báo lỗi. */
@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public LogoutUseCase(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Transactional
    public void execute(RefreshTokenRequest request) {
        refreshTokens.findByTokenHash(RefreshToken.hash(request.refreshToken()))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> refreshTokens.save(token.revoke(clock.instant())));
    }
}
