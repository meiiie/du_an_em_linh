package vn.hoctoanai.core.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctoanai.core.TestcontainersConfiguration;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.Role;
import vn.hoctoanai.core.identity.domain.model.User;

/** Flyway V1 trên PostgreSQL 18 thật + ánh xạ entity ↔ domain (ddl-auto=validate). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserRepositoryAdapter.class, RefreshTokenRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
class IdentityPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Autowired
    private UserRepositoryAdapter users;

    @Autowired
    private RefreshTokenRepositoryAdapter tokens;

    @Test
    void luuVaDocNguoiDungTheoEmail() {
        User saved = users.save(User.create(new Email("hs.binh@demo.local"), "{bcrypt}x", "Bình", Role.STUDENT, true, NOW));
        assertThat(users.findByEmail(new Email("HS.BINH@demo.local"))).contains(saved);
        assertThat(users.findById(saved.id())).contains(saved);
    }

    @Test
    void thuHoiCoDieuKienChiThangMotLan() {
        User user = users.save(User.create(new Email("hs.dua@demo.local"), "{bcrypt}x", "Đua", Role.STUDENT, true, NOW));
        RefreshToken token = tokens.save(RefreshToken.issue(user.id(), "dua", NOW, Duration.ofDays(30)));
        assertThat(tokens.revokeIfActive(token.tokenHash(), NOW.plusSeconds(1))).isTrue();
        assertThat(tokens.revokeIfActive(token.tokenHash(), NOW.plusSeconds(2))).isFalse();
        RefreshToken hetHan = tokens.save(RefreshToken.issue(user.id(), "het-han", NOW, Duration.ofMinutes(1)));
        assertThat(tokens.revokeIfActive(hetHan.tokenHash(), NOW.plus(Duration.ofMinutes(2)))).isFalse();
    }

    @Test
    void donTokenHetHan() {
        User user = users.save(User.create(new Email("hs.don@demo.local"), "{bcrypt}x", "Dọn", Role.STUDENT, true, NOW));
        RefreshToken cu = tokens.save(RefreshToken.issue(user.id(), "cu", NOW.minus(Duration.ofDays(40)), Duration.ofDays(30)));
        RefreshToken moi = tokens.save(RefreshToken.issue(user.id(), "moi", NOW, Duration.ofDays(30)));
        assertThat(tokens.deleteExpiredBefore(NOW.minus(Duration.ofDays(1)))).isEqualTo(1);
        assertThat(tokens.findByTokenHash(cu.tokenHash())).isEmpty();
        assertThat(tokens.findByTokenHash(moi.tokenHash())).isPresent();
    }

    @Test
    void thuHoiMoiTokenConHieuLucCuaMotNguoi() {
        User user = users.save(User.create(new Email("hs.chi@demo.local"), "{bcrypt}x", "Chi", Role.STUDENT, true, NOW));
        RefreshToken a = tokens.save(RefreshToken.issue(user.id(), "a", NOW, Duration.ofDays(30)));
        RefreshToken b = tokens.save(RefreshToken.issue(user.id(), "b", NOW, Duration.ofDays(30)));

        tokens.revokeAllActive(user.id(), NOW.plusSeconds(60));

        assertThat(tokens.findByTokenHash(a.tokenHash())).hasValueSatisfying(t -> assertThat(t.isRevoked()).isTrue());
        assertThat(tokens.findByTokenHash(b.tokenHash())).hasValueSatisfying(t -> assertThat(t.isRevoked()).isTrue());
    }
}
