package vn.hoctoanai.core.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.UserId;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private @Nullable Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @SuppressWarnings("NullAway.Init")
    protected RefreshTokenJpaEntity() {}

    public static RefreshTokenJpaEntity from(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.id = token.id();
        entity.userId = token.userId().value();
        entity.tokenHash = token.tokenHash();
        entity.expiresAt = token.expiresAt();
        entity.revokedAt = token.revokedAt();
        entity.createdAt = token.createdAt();
        return entity;
    }

    public RefreshToken toDomain() {
        return new RefreshToken(id, new UserId(userId), tokenHash, expiresAt, revokedAt, createdAt);
    }
}
