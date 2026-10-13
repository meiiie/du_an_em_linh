package vn.hoctapcanman.core.identity.infrastructure.seed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
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
        String password;
        try {
            password = seedPassword();
        } catch (IOException e) {
            LOG.warn("Bỏ qua tạo tài khoản trường hư cấu: không lưu được tệp mật khẩu riêng tư.");
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

    private String seedPassword() throws IOException {
        Path file = Path.of(environment.getProperty("MATHL_DEV_SEED_PASSWORD_FILE", "/tmp/mathl-dev-seed-password"));
        var permissions = PosixFilePermissions.fromString("rw-------");
        boolean exists = Files.exists(file, LinkOption.NOFOLLOW_LINKS);
        if (exists && (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !Files.isWritable(file))) {
            throw new IOException("Tệp mật khẩu không thuộc người chạy core.");
        }
        String password = environment.getProperty("MATHL_DEV_SEED_PASSWORD");
        if (exists) Files.setPosixFilePermissions(file, permissions);
        if (password == null || password.isBlank()) {
            password = exists ? Files.readString(file) : "";
            if (password.isBlank()) {
                byte[] random = new byte[32];
                new SecureRandom().nextBytes(random);
                password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            }
        }
        if (!exists) Files.createFile(file, PosixFilePermissions.asFileAttribute(permissions));
        Files.writeString(file, password, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                LinkOption.NOFOLLOW_LINKS);
        return password;
    }
}
