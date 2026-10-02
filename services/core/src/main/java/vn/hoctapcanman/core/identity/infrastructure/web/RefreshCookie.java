package vn.hoctapcanman.core.identity.infrastructure.web;

import java.time.Clock;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.identity.application.dto.AuthResponse;
import vn.hoctapcanman.core.identity.infrastructure.security.IdentityProperties;

/**
 * Cookie mang refresh token cho SPA (#57): {@code HttpOnly} nên JavaScript không đọc được, {@code SameSite=Strict},
 * chỉ gửi kèm {@code /api/auth/*}. Hết hạn cùng refresh token.
 */
@Component
public class RefreshCookie {

    public static final String TEN = "hta_refresh";
    static final String DUONG_DAN = "/api/auth";

    private final boolean secure;
    private final Clock clock;

    public RefreshCookie(IdentityProperties properties, Clock clock) {
        this.secure = properties.refreshCookieSecure();
        this.clock = clock;
    }

    public ResponseCookie cap(AuthResponse phien) {
        Duration conLai = Duration.between(clock.instant(), phien.refreshTokenExpiresAt());
        return goc(phien.refreshToken()).maxAge(conLai.isNegative() ? Duration.ZERO : conLai).build();
    }

    public ResponseCookie xoa() {
        return goc("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder goc(String giaTri) {
        return ResponseCookie.from(TEN, giaTri).httpOnly(true).secure(secure).sameSite("Strict").path(DUONG_DAN);
    }
}
