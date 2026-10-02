package vn.hoctapcanman.core.identity.infrastructure.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.identity.application.port.RefreshTokenGenerator;

@Component
public class SecureRefreshTokenGenerator implements RefreshTokenGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
