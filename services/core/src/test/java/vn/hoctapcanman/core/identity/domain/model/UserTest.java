package vn.hoctapcanman.core.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Test
    void taoMoiBatSanVaDanhDauTongHop() {
        User user = User.create(new Email("hs.an@demo.local"), "{bcrypt}x", "  An ", Role.STUDENT, true, NOW);
        assertThat(user.enabled()).isTrue();
        assertThat(user.synthetic()).isTrue();
        assertThat(user.displayName()).isEqualTo("An");
    }

    @Test
    void tuChoiTenRong() {
        assertThatThrownBy(() -> User.create(new Email("a@b.vn"), "{bcrypt}x", " ", Role.TEACHER, false, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void khongInEmailHayMatKhauBamRaLog() {
        User user = User.create(new Email("hs.an@demo.local"), "{bcrypt}bi-mat", "An", Role.STUDENT, true, NOW);
        assertThat(user.toString()).doesNotContain("hs.an").doesNotContain("bi-mat");
    }
}
