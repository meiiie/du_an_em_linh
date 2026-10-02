package vn.hoctoanai.core.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthSessionTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Test
    void thuHoiMotLanGiuMocDauTien() {
        AuthSession phien = AuthSession.start(UserId.newId(), NOW);
        assertThat(phien.isRevoked()).isFalse();
        AuthSession daThuHoi = phien.revoke(NOW.plusSeconds(5));
        assertThat(daThuHoi.isRevoked()).isTrue();
        assertThat(daThuHoi.revoke(NOW.plusSeconds(9)).revokedAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(daThuHoi.id()).isEqualTo(phien.id());
    }
}
