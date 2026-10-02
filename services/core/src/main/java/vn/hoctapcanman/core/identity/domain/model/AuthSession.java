package vn.hoctapcanman.core.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Một lần đăng nhập. Mọi refresh token xoay vòng thuộc cùng phiên; phiên bị thu hồi thì mọi token của nó vô hiệu. */
public record AuthSession(UUID id, UserId userId, Instant createdAt, @Nullable Instant revokedAt) {

    public AuthSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static AuthSession start(UserId userId, Instant now) {
        return new AuthSession(UUID.randomUUID(), userId, now, null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public AuthSession revoke(Instant now) {
        return isRevoked() ? this : new AuthSession(id, userId, createdAt, now);
    }
}
