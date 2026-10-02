package vn.hoctoanai.core.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import vn.hoctoanai.core.identity.application.port.AccessTokenIssuer;
import vn.hoctoanai.core.identity.application.port.PasswordHasher;
import vn.hoctoanai.core.identity.application.port.RefreshTokenGenerator;
import vn.hoctoanai.core.identity.application.service.SessionIssuer;
import vn.hoctoanai.core.identity.application.usecase.LoginUseCase;
import vn.hoctoanai.core.identity.application.usecase.LogoutUseCase;
import vn.hoctoanai.core.identity.application.usecase.RefreshSessionUseCase;
import vn.hoctoanai.core.identity.domain.model.AuthSession;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.Role;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.model.UserId;
import vn.hoctoanai.core.identity.domain.repository.AuthSessionRepository;
import vn.hoctoanai.core.identity.domain.repository.RefreshTokenRepository;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;

/** Bộ giả lập trong bộ nhớ cho test use case định danh; đồng hồ cố định, đếm số lần so mật khẩu. */
public final class GiaLapDinhDanh {

    public static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    public final Map<UUID, User> users = new HashMap<>();
    public final Map<String, RefreshToken> tokens = new HashMap<>();
    public final Map<UUID, AuthSession> sessions = new HashMap<>();
    public final AtomicInteger soLanSoMatKhau = new AtomicInteger();
    public final AtomicInteger soTokenDaSinh = new AtomicInteger();
    public Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    /** Giả lập một yêu cầu đồng thời đã xoay vòng token trước (UPDATE có điều kiện không khớp dòng nào). */
    public boolean thuaCuocDua;

    public final UserRepository userRepository = new UserRepository() {
        @Override
        public Optional<User> findById(UserId id) {
            return Optional.ofNullable(users.get(id.value()));
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return users.values().stream().filter(u -> u.email().equals(email)).findFirst();
        }

        @Override
        public User save(User user) {
            users.put(user.id().value(), user);
            return user;
        }
    };

    public final RefreshTokenRepository refreshTokenRepository = new RefreshTokenRepository() {
        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(tokens.get(tokenHash));
        }

        @Override
        public RefreshToken save(RefreshToken token) {
            tokens.put(token.tokenHash(), token);
            return token;
        }

        @Override
        public boolean revokeIfActive(String tokenHash, Instant now) {
            if (thuaCuocDua) {
                return false;
            }
            RefreshToken token = tokens.get(tokenHash);
            if (token == null || !token.isActive(now)) {
                return false;
            }
            tokens.put(tokenHash, token.revoke(now));
            return true;
        }

        @Override
        public int deleteExpiredBefore(Instant cutoff) {
            int truoc = tokens.size();
            tokens.values().removeIf(t -> t.expiresAt().isBefore(cutoff));
            return truoc - tokens.size();
        }
    };

    public final AuthSessionRepository authSessionRepository = new AuthSessionRepository() {
        @Override
        public AuthSession save(AuthSession session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<AuthSession> findByIdForUpdate(UUID id) {
            return Optional.ofNullable(sessions.get(id));
        }

        @Override
        public boolean revoke(UUID id, Instant now) {
            AuthSession session = sessions.get(id);
            if (session == null || session.isRevoked()) {
                return false;
            }
            sessions.put(id, session.revoke(now));
            return true;
        }

        @Override
        public void revokeAllForUser(UserId userId, Instant now) {
            sessions.replaceAll((id, s) -> s.userId().equals(userId) ? s.revoke(now) : s);
        }

        @Override
        public int deleteWithoutTokens() {
            int truoc = sessions.size();
            sessions.keySet().removeIf(id -> tokens.values().stream().noneMatch(t -> t.sessionId().equals(id)));
            return truoc - sessions.size();
        }
    };

    public final PasswordHasher hasher = new PasswordHasher() {
        @Override
        public String hash(String rawPassword) {
            return "bam:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            soLanSoMatKhau.incrementAndGet();
            return passwordHash.equals("bam:" + rawPassword);
        }

        @Override
        public String dummyHash() {
            return "bam:" + UUID.randomUUID();
        }
    };

    public final AccessTokenIssuer accessTokenIssuer =
        (user, now) -> new AccessTokenIssuer.IssuedAccessToken("access-" + user.id().value(), now.plus(Duration.ofMinutes(15)));

    public final RefreshTokenGenerator generator = () -> "refresh-" + soTokenDaSinh.incrementAndGet();

    public SessionIssuer sessionIssuer() {
        return new SessionIssuer(accessTokenIssuer, generator, refreshTokenRepository, authSessionRepository, Duration.ofDays(30));
    }

    public LoginUseCase login() {
        return new LoginUseCase(userRepository, hasher, sessionIssuer(), clock);
    }

    public RefreshSessionUseCase refresh() {
        return new RefreshSessionUseCase(refreshTokenRepository, authSessionRepository, userRepository, sessionIssuer(), clock);
    }

    public LogoutUseCase logout() {
        return new LogoutUseCase(refreshTokenRepository, authSessionRepository, clock);
    }

    public AuthSession phienCua(String refreshToken) {
        return sessions.get(tokens.get(RefreshToken.hash(refreshToken)).sessionId());
    }

    public void khoa(User user) {
        users.put(user.id().value(), new User(
            user.id(), user.email(), user.passwordHash(), user.displayName(), user.role(), false, user.synthetic(), NOW, NOW));
    }

    public User themNguoiDung(String email, String matKhau, Role role, boolean enabled) {
        User user = User.create(new Email(email), hasher.hash(matKhau), "Người thử", role, true, NOW);
        if (!enabled) {
            user = new User(user.id(), user.email(), user.passwordHash(), user.displayName(), role, false, true, NOW, NOW);
        }
        return userRepository.save(user);
    }
}
