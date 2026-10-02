package vn.hoctoanai.core.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
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
    public boolean revokeIfActive(String tokenHash, Instant now) {
        return jpa.revokeIfActive(tokenHash, now) == 1;
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return jpa.deleteExpiredBefore(cutoff);
    }
}
