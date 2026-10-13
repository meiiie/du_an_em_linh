package vn.hoctapcanman.core.classroom.infrastructure.seed;

import java.time.Clock;
import java.time.Instant;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;
import vn.hoctapcanman.core.classroom.domain.repository.EnrollmentRepository;
import vn.hoctapcanman.core.classroom.domain.repository.SchoolClassRepository;
import vn.hoctapcanman.core.identity.application.port.UserDirectory;

/** Lớp hư cấu dev, định danh ổn định; không chạm lớp trùng tên hoặc có thành viên thật. */
@Component
@Profile("dev")
@Order(2)
public class LopNguyenTraiSeeder implements ApplicationRunner {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LopNguyenTraiSeeder.class);

    private final SchoolClassRepository classes;
    private final EnrollmentRepository enrollments;
    private final ClassSettingsRepository settings;
    /** Tra tài khoản qua cổng của identity, không đọc kho người dùng của module khác (#111). */
    private final UserDirectory users;
    private final Clock clock;

    public LopNguyenTraiSeeder(SchoolClassRepository classes, EnrollmentRepository enrollments, ClassSettingsRepository settings,
            UserDirectory users, Clock clock) {
        this.classes = classes;
        this.enrollments = enrollments;
        this.settings = settings;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        for (String ten : TruongNguyenTrai.LOP) {
            ClassId expected = new ClassId(TruongNguyenTrai.id("lop:" + ten));
            Optional<SchoolClass> daCo = classes.findByNameAndSchoolYear(ten, TruongNguyenTrai.NAM_HOC);
            if (daCo.isPresent() && (!daCo.get().id().equals(expected) || coThanhVienThat(expected))) {
                LOG.warn("Bỏ qua lớp trùng tên hoặc không còn là lớp tổng hợp.");
                continue;
            }
            boolean coGiaoVien = TruongNguyenTrai.taiKhoan().stream()
                .filter(tk -> tk.lop().contains(ten) && tk.vaiTro().equals("TEACHER"))
                .anyMatch(tk -> users.findByEmail(tk.email()).filter(u -> u.synthetic() && u.role().equals("TEACHER")).isPresent());
            if (!coGiaoVien) continue;
            SchoolClass lop = daCo.orElseGet(() -> classes.save(new SchoolClass(expected, ten,
                Integer.parseInt(ten.substring(0, 2)), TruongNguyenTrai.NAM_HOC, now)));
            TruongNguyenTrai.taiKhoan().stream().filter(tk -> tk.lop().contains(ten))
                .forEach(tk -> ghiDanh(lop.id(), tk.email(), ClassRole.valueOf(tk.vaiTro()), now));
            if (settings.findByClassId(lop.id()).isEmpty()) // Trường hư cấu (dev): bật gia sư GLM; máy chủ không có khóa thì vẫn chạy thang offline.
                settings.save(new ClassSettings(lop.id(), false, "glm", false, now, null));
        }
    }

    private boolean coThanhVienThat(ClassId lop) {
        return Stream.of(ClassRole.values())
                .flatMap(role -> enrollments.findByClass(lop, role).stream())
                .anyMatch(e -> users.findById(e.userId()).map(u -> !u.synthetic()).orElse(false));
    }

    private void ghiDanh(ClassId lop, String email, ClassRole role, Instant now) {
        users.findByEmail(email).filter(u -> u.synthetic() && u.role().equals(role.name())).ifPresent(user -> {
            UUID id = user.id();
            boolean daGhiDanh = enrollments.find(lop, id).isPresent();
            boolean daHocLopKhac = role == ClassRole.STUDENT
                    && enrollments.findByUser(id).stream().anyMatch(e -> e.role() == ClassRole.STUDENT);
            if (!daGhiDanh && !daHocLopKhac) {
                enrollments.save(new Enrollment(lop, id, role, now));
            }
        });
    }
}
