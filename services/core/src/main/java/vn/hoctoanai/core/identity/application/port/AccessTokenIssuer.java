package vn.hoctoanai.core.identity.application.port;

import java.time.Instant;
import vn.hoctoanai.core.identity.domain.model.User;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(User user, Instant now);

    record IssuedAccessToken(String value, Instant expiresAt) {

        @Override
        public String toString() {
            return "IssuedAccessToken[expiresAt=" + expiresAt + "]";
        }
    }
}
