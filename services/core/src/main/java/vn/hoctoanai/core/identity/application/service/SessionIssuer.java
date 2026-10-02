package vn.hoctoanai.core.identity.application.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.UserDto;
import vn.hoctoanai.core.identity.application.port.AccessTokenIssuer;
import vn.hoctoanai.core.identity.application.port.RefreshTokenGenerator;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.repository.RefreshTokenRepository;

/** Cấp một phiên: access token ngắn hạn + refresh token lưu băm. Dùng chung cho đăng nhập và làm mới. */
@Service
public class SessionIssuer {

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenGenerator generator;
    private final RefreshTokenRepository refreshTokens;
    private final Duration refreshTokenTtl;

    public SessionIssuer(
            AccessTokenIssuer accessTokens,
            RefreshTokenGenerator generator,
            RefreshTokenRepository refreshTokens,
            @Value("${app.identity.refresh-token-ttl:P30D}") Duration refreshTokenTtl) {
        this.accessTokens = accessTokens;
        this.generator = generator;
        this.refreshTokens = refreshTokens;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public AuthResponse issue(User user, Instant now) {
        AccessTokenIssuer.IssuedAccessToken access = accessTokens.issue(user, now);
        String raw = generator.newToken();
        RefreshToken refresh = refreshTokens.save(RefreshToken.issue(user.id(), raw, now, refreshTokenTtl));
        return new AuthResponse(access.value(), access.expiresAt(), raw, refresh.expiresAt(), UserDto.from(user));
    }
}
