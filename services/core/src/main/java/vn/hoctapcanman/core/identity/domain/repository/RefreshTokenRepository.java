package vn.hoctapcanman.core.identity.domain.repository;

import java.time.Instant;
import java.util.Optional;
import vn.hoctapcanman.core.identity.domain.model.RefreshToken;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken token);

    /**
     * Thu hồi token nếu nó còn hiệu lực, nguyên tử với các yêu cầu đồng thời: chỉ một lời gọi nhận {@code true} cho
     * một token.
     */
    boolean revokeIfActive(String tokenHash, Instant now);

    /** Xóa token đã hết hạn trước mốc; trả số dòng đã xóa. */
    int deleteExpiredBefore(Instant cutoff);
}
