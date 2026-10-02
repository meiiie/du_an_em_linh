package vn.hoctoanai.core.identity.application.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.exception.LoginLockedException;
import vn.hoctoanai.core.identity.application.port.PasswordHasher;
import vn.hoctoanai.core.identity.application.service.SessionIssuer;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.LoginAttemptKey;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.repository.LoginFailureRepository;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;

/**
 * Đăng nhập bằng email + mật khẩu. Port từ {@code LMS_hohulili@34c3f0f2:backend/.../identity/application/usecase/AuthenticateUserUseCaseV2.java}
 * (MIT), sửa ba điểm: luôn so mật khẩu (kể cả khi email không tồn tại, bằng băm giả) để thời gian không lộ tài khoản;
 * kiểm tài khoản khóa sau mật khẩu, cùng một thông điệp; không ghi email vào log.
 *
 * <p>Giới hạn đăng nhập sai như v0 (F-10, #69): {@value #NGUONG} lần sai trong {@code CUA_SO} (cửa sổ trượt) theo email + IP
 * thì tạm khóa, kể cả khi lần sau đúng mật khẩu; khóa mở khi lần sai cũ nhất trong số đó ra khỏi cửa sổ. Đăng nhập đúng
 * (dưới ngưỡng) xóa bộ đếm. Email không tồn tại cũng bị đếm và khóa như thường, nên không lộ tài khoản. Không rollback
 * khi ném lỗi để lần sai được ghi lại.
 */
@Service
public class LoginUseCase {

    static final int NGUONG = 5;
    static final Duration CUA_SO = Duration.ofMinutes(15);

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final SessionIssuer sessions;
    private final LoginFailureRepository failures;
    private final Clock clock;

    public LoginUseCase(
            UserRepository users, PasswordHasher hasher, SessionIssuer sessions, LoginFailureRepository failures, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.sessions = sessions;
        this.failures = failures;
        this.clock = clock;
    }

    /** {@code clientIp}: địa chỉ đã qua cấu hình proxy tin cậy ({@code server.forward-headers-strategy}). */
    @Transactional(noRollbackFor = {AuthenticationFailedException.class, LoginLockedException.class})
    public AuthResponse execute(LoginRequest request, String clientIp) {
        Instant now = clock.instant();
        LoginAttemptKey key = LoginAttemptKey.of(request.email(), clientIp);
        failures.lock(key);
        List<Instant> ganDay = failures.recentSince(key, now.minus(CUA_SO), NGUONG);
        if (ganDay.size() >= NGUONG) {
            Instant moKhoa = ganDay.get(NGUONG - 1).plus(CUA_SO);
            throw new LoginLockedException(Duration.between(now, moKhoa));
        }
        Optional<User> found = email(request.email()).flatMap(users::findByEmail);
        boolean matches = hasher.matches(request.password(), found.map(User::passwordHash).orElseGet(hasher::dummyHash));
        Optional<User> user = found.filter(u -> matches && u.enabled());
        if (user.isEmpty()) {
            failures.record(key, now);
            throw new AuthenticationFailedException(AuthenticationFailedException.THONG_BAO);
        }
        failures.clear(key);
        return sessions.start(user.get(), now);
    }

    private static Optional<Email> email(String raw) {
        try {
            return Optional.of(new Email(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
