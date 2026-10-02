package vn.hoctapcanman.core.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.identity.domain.model.AuthSession;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.LoginAttemptKey;
import vn.hoctapcanman.core.identity.domain.model.RefreshToken;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;

/** Flyway V1 trên PostgreSQL 18 thật + ánh xạ entity ↔ domain (ddl-auto=validate). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    UserRepositoryAdapter.class,
    RefreshTokenRepositoryAdapter.class,
    AuthSessionRepositoryAdapter.class,
    LoginFailureRepositoryAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class IdentityPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Autowired
    private UserRepositoryAdapter users;

    @Autowired
    private RefreshTokenRepositoryAdapter tokens;

    @Autowired
    private AuthSessionRepositoryAdapter sessions;

    @Autowired
    private LoginFailureRepositoryAdapter failures;

    private User nguoiDung(String email) {
        return users.save(User.create(new Email(email), "{bcrypt}x", "Người thử", Role.STUDENT, true, NOW));
    }

    @Test
    void luuVaDocNguoiDungTheoEmail() {
        User saved = users.save(User.create(new Email("hs.binh@demo.local"), "{bcrypt}x", "Bình", Role.STUDENT, true, NOW));
        assertThat(users.findByEmail(new Email("HS.BINH@demo.local"))).contains(saved);
        assertThat(users.findById(saved.id())).contains(saved);
    }

    @Test
    void thuHoiTokenCoDieuKienChiThangMotLan() {
        AuthSession phien = sessions.save(AuthSession.start(nguoiDung("hs.dua@demo.local").id(), NOW));
        RefreshToken token = tokens.save(RefreshToken.issue(phien, "dua", NOW, Duration.ofDays(30)));
        assertThat(tokens.findByTokenHash(token.tokenHash())).contains(token);
        assertThat(tokens.revokeIfActive(token.tokenHash(), NOW.plusSeconds(1))).isTrue();
        assertThat(tokens.revokeIfActive(token.tokenHash(), NOW.plusSeconds(2))).isFalse();
        RefreshToken hetHan = tokens.save(RefreshToken.issue(phien, "het-han", NOW, Duration.ofMinutes(1)));
        assertThat(tokens.revokeIfActive(hetHan.tokenHash(), NOW.plus(Duration.ofMinutes(2)))).isFalse();
    }

    @Test
    void thuHoiPhienCoDieuKienChiThangMotLan() {
        AuthSession phien = sessions.save(AuthSession.start(nguoiDung("hs.phien@demo.local").id(), NOW));
        assertThat(sessions.findByIdForUpdate(phien.id())).contains(phien);
        assertThat(sessions.revoke(phien.id(), NOW.plusSeconds(1))).isTrue();
        assertThat(sessions.revoke(phien.id(), NOW.plusSeconds(2))).isFalse();
        assertThat(sessions.findByIdForUpdate(phien.id())).hasValueSatisfying(s -> assertThat(s.revokedAt()).isEqualTo(NOW.plusSeconds(1)));
    }

    @Test
    void thuHoiMoiPhienCuaMotNguoiKhongDungNguoiKhac() {
        User chi = nguoiDung("hs.chi@demo.local");
        AuthSession a = sessions.save(AuthSession.start(chi.id(), NOW));
        AuthSession b = sessions.save(AuthSession.start(chi.id(), NOW));
        AuthSession khac = sessions.save(AuthSession.start(nguoiDung("hs.khac@demo.local").id(), NOW));

        sessions.revokeAllForUser(chi.id(), NOW.plusSeconds(60));

        assertThat(sessions.findByIdForUpdate(a.id())).hasValueSatisfying(s -> assertThat(s.isRevoked()).isTrue());
        assertThat(sessions.findByIdForUpdate(b.id())).hasValueSatisfying(s -> assertThat(s.isRevoked()).isTrue());
        assertThat(sessions.findByIdForUpdate(khac.id())).hasValueSatisfying(s -> assertThat(s.isRevoked()).isFalse());
    }

    @Test
    void donTokenHetHanRoiPhienKhongConToken() {
        User user = nguoiDung("hs.don@demo.local");
        AuthSession phienCu = sessions.save(AuthSession.start(user.id(), NOW.minus(Duration.ofDays(40))));
        AuthSession phienMoi = sessions.save(AuthSession.start(user.id(), NOW));
        RefreshToken cu = tokens.save(RefreshToken.issue(phienCu, "cu", NOW.minus(Duration.ofDays(40)), Duration.ofDays(30)));
        RefreshToken moi = tokens.save(RefreshToken.issue(phienMoi, "moi", NOW, Duration.ofDays(30)));

        int daXoa = new RefreshTokenCleanup(tokens, sessions, Clock.fixed(NOW, ZoneOffset.UTC)).purgeExpired();

        assertThat(daXoa).isEqualTo(2);
        assertThat(tokens.findByTokenHash(cu.tokenHash())).isEmpty();
        assertThat(tokens.findByTokenHash(moi.tokenHash())).isPresent();
        assertThat(sessions.findByIdForUpdate(phienCu.id())).isEmpty();
        assertThat(sessions.findByIdForUpdate(phienMoi.id())).isPresent();
    }

    @Test
    void demLanSaiTheoKhoaTrongCuaSoVaDon() {
        LoginAttemptKey an = LoginAttemptKey.of("hs.an@demo.local", "203.0.113.7");
        LoginAttemptKey khac = LoginAttemptKey.of("hs.an@demo.local", "203.0.113.8");
        failures.lock(an);
        failures.record(an, NOW.minus(Duration.ofMinutes(20)));
        failures.record(an, NOW.minus(Duration.ofMinutes(5)));
        failures.record(an, NOW);
        failures.record(khac, NOW);

        assertThat(failures.recentSince(an, NOW.minus(Duration.ofMinutes(15)), 5)).containsExactly(NOW, NOW.minus(Duration.ofMinutes(5)));
        assertThat(failures.recentSince(an, NOW.minus(Duration.ofDays(1)), 1)).containsExactly(NOW);
        assertThat(failures.recentSince(khac, NOW.minus(Duration.ofMinutes(15)), 5)).hasSize(1);

        int daXoa = new LoginFailureCleanup(failures, Clock.fixed(NOW.plus(Duration.ofDays(1)).minus(Duration.ofMinutes(10)), ZoneOffset.UTC))
            .purgeOld();
        assertThat(daXoa).isEqualTo(1);
        assertThat(failures.recentSince(an, NOW.minus(Duration.ofDays(2)), 5)).hasSize(2);

        failures.clear(an);
        assertThat(failures.recentSince(an, NOW.minus(Duration.ofDays(2)), 5)).isEmpty();
        assertThat(failures.recentSince(khac, NOW.minus(Duration.ofDays(2)), 5)).hasSize(1);
    }
}
