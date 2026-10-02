package vn.hoctoanai.core.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctoanai.core.identity.domain.model.AuthSession;
import vn.hoctoanai.core.identity.domain.model.UserId;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private @Nullable Instant revokedAt;

    @SuppressWarnings("NullAway.Init")
    protected AuthSessionJpaEntity() {}

    public static AuthSessionJpaEntity from(AuthSession session) {
        AuthSessionJpaEntity entity = new AuthSessionJpaEntity();
        entity.id = session.id();
        entity.userId = session.userId().value();
        entity.createdAt = session.createdAt();
        entity.revokedAt = session.revokedAt();
        return entity;
    }

    public AuthSession toDomain() {
        return new AuthSession(id, new UserId(userId), createdAt, revokedAt);
    }
}
