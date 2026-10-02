package vn.hoctoanai.core.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.Role;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.model.UserId;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @SuppressWarnings("NullAway.Init")
    protected UserJpaEntity() {}

    public static UserJpaEntity from(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.id().value();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash();
        entity.displayName = user.displayName();
        entity.role = user.role();
        entity.enabled = user.enabled();
        entity.synthetic = user.synthetic();
        entity.createdAt = user.createdAt();
        entity.updatedAt = user.updatedAt();
        return entity;
    }

    public User toDomain() {
        return new User(new UserId(id), new Email(email), passwordHash, displayName, role, enabled, synthetic, createdAt, updatedAt);
    }
}
