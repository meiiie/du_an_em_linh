package vn.hoctoanai.core.identity.domain.repository;

import java.time.Instant;
import java.util.Optional;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.UserId;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken token);

    /** Thu hồi mọi token còn hiệu lực của người dùng (khi phát hiện token đã thu hồi bị dùng lại). */
    void revokeAllActive(UserId userId, Instant now);
}
