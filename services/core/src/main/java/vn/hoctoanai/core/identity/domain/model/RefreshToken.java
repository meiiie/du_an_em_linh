package vn.hoctoanai.core.identity.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Refresh token. Chỉ lưu băm SHA-256; giá trị gốc trả cho người dùng một lần. Mỗi lần làm mới, token cũ bị thu hồi và
 * thay bằng token mới; đăng xuất thu hồi token.
 */
public record RefreshToken(
        UUID id, UUID sessionId, UserId userId, String tokenHash, Instant expiresAt, @Nullable Instant revokedAt, Instant createdAt) {

    public RefreshToken {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static RefreshToken issue(AuthSession session, String rawToken, Instant now, Duration ttl) {
        return new RefreshToken(UUID.randomUUID(), session.id(), session.userId(), hash(rawToken), now.plus(ttl), null, now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isActive(Instant now) {
        return !isRevoked() && now.isBefore(expiresAt);
    }

    public RefreshToken revoke(Instant now) {
        return isRevoked() ? this : new RefreshToken(id, sessionId, userId, tokenHash, expiresAt, now, createdAt);
    }

    /** SHA-256 dạng hex: token ngẫu nhiên 256 bit nên không cần muối. */
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK thiếu SHA-256", e);
        }
    }

    @Override
    public String toString() {
        return "RefreshToken[id=" + id + ", sessionId=" + sessionId + "]";
    }
}
