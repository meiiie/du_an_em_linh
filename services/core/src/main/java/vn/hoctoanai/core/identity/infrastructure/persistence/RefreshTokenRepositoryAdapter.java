package vn.hoctoanai.core.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.UserId;
import vn.hoctoanai.core.identity.domain.repository.RefreshTokenRepository;
import vn.hoctoanai.core.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(RefreshTokenJpaEntity.from(token)).toDomain();
    }

    @Override
    public void revokeAllActive(UserId userId, Instant now) {
        jpa.revokeAllActive(userId.value(), now);
    }
}
