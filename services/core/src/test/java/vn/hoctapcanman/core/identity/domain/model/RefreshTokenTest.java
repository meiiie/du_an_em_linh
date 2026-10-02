package vn.hoctapcanman.core.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Test
    void chiLuuBamKhongLuuGiaTriGoc() {
        AuthSession phien = AuthSession.start(UserId.newId(), NOW);
        RefreshToken token = RefreshToken.issue(phien, "gia-tri-goc", NOW, Duration.ofDays(30));
        assertThat(token.tokenHash()).hasSize(64).isEqualTo(RefreshToken.hash("gia-tri-goc")).doesNotContain("gia-tri-goc");
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(token.sessionId()).isEqualTo(phien.id());
        assertThat(token.userId()).isEqualTo(phien.userId());
        assertThat(token.toString()).doesNotContain(phien.userId().value().toString());
    }

    @Test
    void hetHanVaThuHoi() {
        RefreshToken token = RefreshToken.issue(AuthSession.start(UserId.newId(), NOW), "x", NOW, Duration.ofMinutes(5));
        assertThat(token.isActive(NOW)).isTrue();
        assertThat(token.isActive(NOW.plus(Duration.ofMinutes(5)))).isFalse();
        RefreshToken revoked = token.revoke(NOW);
        assertThat(revoked.isActive(NOW)).isFalse();
        assertThat(revoked.revoke(NOW.plusSeconds(1)).revokedAt()).isEqualTo(NOW);
    }
}
