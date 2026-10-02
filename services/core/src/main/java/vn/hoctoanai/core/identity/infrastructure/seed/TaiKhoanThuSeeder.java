package vn.hoctoanai.core.identity.infrastructure.seed;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctoanai.core.identity.application.port.PasswordHasher;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.Role;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;

/**
 * Tài khoản tổng hợp cho dev / demo (AGENTS.md «Tài khoản thử», ADR 006): chỉ chạy với profile {@code dev}, tạo khi
 * chưa có, đánh dấu {@code synthetic}. Không có dữ liệu học sinh thật.
 */
@Component
@Profile("dev")
public class TaiKhoanThuSeeder implements ApplicationRunner {

    private record TaiKhoan(String email, String ten, Role vaiTro, String matKhau) {}

    private static final List<TaiKhoan> TAI_KHOAN = List.of(
            new TaiKhoan("hs.an@demo.local", "An", Role.STUDENT, "hocsinh123"),
            new TaiKhoan("hs.binh@demo.local", "Bình", Role.STUDENT, "hocsinh123"),
            new TaiKhoan("hs.chi@demo.local", "Chi", Role.STUDENT, "hocsinh123"),
            new TaiKhoan("gv@demo.local", "Giáo viên thử", Role.TEACHER, "giaovien123"));

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    public TaiKhoanThuSeeder(UserRepository users, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        for (TaiKhoan tk : TAI_KHOAN) {
            Email email = new Email(tk.email());
            if (users.findByEmail(email).isEmpty()) {
                users.save(User.create(email, hasher.hash(tk.matKhau()), tk.ten(), tk.vaiTro(), true, now));
            }
        }
    }
}
