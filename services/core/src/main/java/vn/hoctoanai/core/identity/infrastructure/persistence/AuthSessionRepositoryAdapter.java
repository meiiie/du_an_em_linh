package vn.hoctoanai.core.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.hoctoanai.core.identity.domain.model.AuthSession;
import vn.hoctoanai.core.identity.domain.model.UserId;
import vn.hoctoanai.core.identity.domain.repository.AuthSessionRepository;
import vn.hoctoanai.core.identity.infrastructure.persistence.entity.AuthSessionJpaEntity;

@Repository
public class AuthSessionRepositoryAdapter implements AuthSessionRepository {

    private final AuthSessionJpaRepository jpa;

    public AuthSessionRepositoryAdapter(AuthSessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuthSession save(AuthSession session) {
        return jpa.save(AuthSessionJpaEntity.from(session)).toDomain();
    }

    @Override
    public Optional<AuthSession> findByIdForUpdate(UUID id) {
        return jpa.findByIdForUpdate(id).map(AuthSessionJpaEntity::toDomain);
    }

    @Override
    public boolean revoke(UUID id, Instant now) {
        return jpa.revoke(id, now) == 1;
    }

    @Override
    public void revokeAllForUser(UserId userId, Instant now) {
        jpa.revokeAllForUser(userId.value(), now);
    }

    @Override
    public int deleteWithoutTokens() {
        return jpa.deleteWithoutTokens();
    }
}
