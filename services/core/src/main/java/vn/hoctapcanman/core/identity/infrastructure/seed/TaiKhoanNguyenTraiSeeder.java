package vn.hoctapcanman.core.identity.infrastructure.seed;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;

/** Tạo tài khoản trường hư cấu, chỉ dev; không cập nhật tài khoản trùng hoặc lưu mật khẩu trong repo. */
@Component
@Profile("dev")
@Order(1)
public class TaiKhoanNguyenTraiSeeder implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(TaiKhoanNguyenTraiSeeder.class);
    private final Environment environment;

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    public TaiKhoanNguyenTraiSeeder(UserRepository users, PasswordHasher hasher, Clock clock, Environment environment) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String password = environment.getProperty("MATHL_DEV_SEED_PASSWORD");
        if (password == null || password.isBlank()) {
            LOG.warn("Bỏ qua tạo tài khoản trường hư cấu: chưa đặt MATHL_DEV_SEED_PASSWORD.");
            return;
        }
        Instant now = clock.instant();
        for (TruongNguyenTrai.TaiKhoan tk : TruongNguyenTrai.taiKhoan()) {
            Email email = new Email(tk.email());
            if (users.findByEmail(email).isEmpty()) {
                users.save(User.create(email, hasher.hash(password), tk.ten(), Role.valueOf(tk.vaiTro()), true, now));
            }
        }
    }
}
