package vn.hoctoanai.core.identity.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.port.PasswordHasher;
import vn.hoctoanai.core.identity.application.service.SessionIssuer;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;

/**
 * Đăng nhập bằng email + mật khẩu. Port từ {@code LMS_hohulili@34c3f0f2:backend/.../identity/application/usecase/AuthenticateUserUseCaseV2.java}
 * (MIT), sửa ba điểm: luôn so mật khẩu (kể cả khi email không tồn tại, bằng băm giả) để thời gian không lộ tài khoản;
 * kiểm tài khoản khóa sau mật khẩu, cùng một thông điệp; không ghi email vào log.
 */
@Service
public class LoginUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final SessionIssuer sessions;
    private final Clock clock;

    public LoginUseCase(UserRepository users, PasswordHasher hasher, SessionIssuer sessions, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse execute(LoginRequest request) {
        Optional<User> found = email(request.email()).flatMap(users::findByEmail);
        boolean matches = hasher.matches(request.password(), found.map(User::passwordHash).orElseGet(hasher::dummyHash));
        User user = found.filter(u -> matches && u.enabled())
                .orElseThrow(() -> new AuthenticationFailedException(AuthenticationFailedException.THONG_BAO));
        Instant now = clock.instant();
        return sessions.start(user, now);
    }

    private static Optional<Email> email(String raw) {
        try {
            return Optional.of(new Email(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
