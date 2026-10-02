package vn.hoctapcanman.core.classroom.infrastructure.seed;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.model.UserId;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/**
 * Lớp «12A1 thử» của v0 cho dev / demo (spec P2, ADR 006): giáo viên thử dạy An, Bình, Chi; cài lớp mặc định. Chạy sau
 * {@code TaiKhoanThuSeeder} (tài khoản phải có trước), chỉ với profile {@code dev}, tạo phần còn thiếu, chạy lại không
 * nhân bản. Chỉ ghi danh tài khoản tổng hợp ({@code synthetic}); lớp trùng tên mà đã có thành viên thật thì không đụng
 * tới, để tài khoản thử có mật khẩu công khai không thành giáo viên của một lớp thật. Học sinh đã thuộc lớp khác thì bỏ
 * qua (mỗi học sinh một lớp).
 */
@Component
@Profile("dev")
@Order(2)
public class LopThuSeeder implements ApplicationRunner {

    static final String TEN_LOP = "12A1 thử";
    static final String NAM_HOC = "2026-2027";
    static final String GIAO_VIEN = "gv@demo.local";
    static final List<String> HOC_SINH = List.of("hs.an@demo.local", "hs.binh@demo.local", "hs.chi@demo.local");

    private final SchoolClassRepository classes;
    private final EnrollmentRepository enrollments;
    private final ClassSettingsRepository settings;
    private final UserRepository users;
    private final Clock clock;

    public LopThuSeeder(SchoolClassRepository classes, EnrollmentRepository enrollments, ClassSettingsRepository settings,
            UserRepository users, Clock clock) {
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
        Optional<SchoolClass> daCo = classes.findByNameAndSchoolYear(TEN_LOP, NAM_HOC);
        if (daCo.isPresent() && coThanhVienThat(daCo.get().id())) {
            return;
        }
        SchoolClass lop = daCo.orElseGet(() -> classes.save(SchoolClass.create(TEN_LOP, 12, NAM_HOC, now)));
        ghiDanh(lop.id(), GIAO_VIEN, ClassRole.TEACHER, now);
        HOC_SINH.forEach(email -> ghiDanh(lop.id(), email, ClassRole.STUDENT, now));
        if (settings.findByClassId(lop.id()).isEmpty()) {
            settings.save(ClassSettings.macDinh(lop.id(), now));
        }
    }

    private boolean coThanhVienThat(ClassId lop) {
        return Stream.of(ClassRole.values())
                .flatMap(role -> enrollments.findByClass(lop, role).stream())
                .anyMatch(e -> users.findById(new UserId(e.userId())).map(u -> !u.synthetic()).orElse(false));
    }

    private void ghiDanh(ClassId lop, String email, ClassRole role, Instant now) {
        users.findByEmail(new Email(email)).filter(User::synthetic).ifPresent(user -> {
            UUID id = user.id().value();
            boolean daGhiDanh = enrollments.find(lop, id).isPresent();
            boolean daHocLopKhac = role == ClassRole.STUDENT
                    && enrollments.findByUser(id).stream().anyMatch(e -> e.role() == ClassRole.STUDENT);
            if (!daGhiDanh && !daHocLopKhac) {
                enrollments.save(new Enrollment(lop, id, role, now));
            }
        });
    }
}
