package vn.hoctapcanman.core.classroom.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatChoHocSinh;
import vn.hoctapcanman.core.classroom.application.exception.KhongThuocLopException;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;
import vn.hoctapcanman.core.classroom.domain.repository.EnrollmentRepository;

/** Ngữ nghĩa của cổng quyền theo lớp trên kho ghi danh trong bộ nhớ (persistence thật ở test tích hợp). */
class ClassMembershipServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    private final KhoGhiDanh kho = new KhoGhiDanh();
    private final Map<ClassId, ClassSettings> caiDat = new HashMap<>();
    private final ClassSettingsRepository khoCaiDat = new ClassSettingsRepository() {
        @Override
        public Optional<ClassSettings> findByClassId(ClassId classId) {
            return Optional.ofNullable(caiDat.get(classId));
        }

        @Override
        public ClassSettings save(ClassSettings settings) {
            caiDat.put(settings.classId(), settings);
            return settings;
        }
    };
    private final ClassMembershipService membership = new ClassMembershipService(kho, khoCaiDat);
    private final UUID lopA = UUID.randomUUID();
    private final UUID lopB = UUID.randomUUID();
    private final UUID gv = UUID.randomUUID();
    private final UUID hs = UUID.randomUUID();

    @Test
    void giaoVienChiDayLopCoGhiDanhVaiTroGiaoVien() {
        kho.save(new Enrollment(new ClassId(lopA), gv, ClassRole.TEACHER, NOW));
        kho.save(new Enrollment(new ClassId(lopB), gv, ClassRole.STUDENT, NOW.plusSeconds(1)));
        assertThat(membership.lopDay(gv)).containsExactly(lopA);
        assertThat(membership.laGiaoVien(gv, lopA)).isTrue();
        assertThat(membership.laGiaoVien(gv, lopB)).isFalse();
        assertThatThrownBy(() -> membership.kiemGiaoVien(gv, lopB)).isInstanceOf(KhongThuocLopException.class);
    }

    @Test
    void lopDangDayKhongLuiVeLopKhacKhiChonLopKhongDay() {
        kho.save(new Enrollment(new ClassId(lopA), gv, ClassRole.TEACHER, NOW));
        kho.save(new Enrollment(new ClassId(lopB), gv, ClassRole.TEACHER, NOW.plusSeconds(1)));
        assertThat(membership.lopDangDay(gv, null)).contains(lopA);
        assertThat(membership.lopDangDay(gv, lopB)).contains(lopB);
        assertThat(membership.lopDangDay(gv, UUID.randomUUID())).isEmpty();
        assertThat(membership.lopDangDay(UUID.randomUUID(), null)).isEmpty();
    }

    @Test
    void giaoVienDayHocSinhQuaCungMotLop() {
        kho.save(new Enrollment(new ClassId(lopA), gv, ClassRole.TEACHER, NOW));
        kho.save(new Enrollment(new ClassId(lopA), hs, ClassRole.STUDENT, NOW));
        UUID hsKhac = UUID.randomUUID();
        kho.save(new Enrollment(new ClassId(lopB), hsKhac, ClassRole.STUDENT, NOW));
        assertThat(membership.lopHoc(hs)).contains(lopA);
        assertThat(membership.giaoVienDayHocSinh(gv, hs)).isTrue();
        assertThat(membership.giaoVienDayHocSinh(gv, hsKhac)).isFalse();
        assertThat(membership.giaoVienDayHocSinh(hs, hs)).isFalse();
        assertThat(membership.hocSinhCuaLop(gv, lopA)).containsExactly(hs);
        assertThatThrownBy(() -> membership.hocSinhCuaLop(hs, lopA)).isInstanceOf(KhongThuocLopException.class);
        assertThatThrownBy(() -> membership.hocSinhCuaLop(gv, lopB)).isInstanceOf(KhongThuocLopException.class);
    }

    @Test
    void caiDatChoHocSinhChiLaCoCuaLopEmDangHocMacDinhKhongMoLoiGiai() {
        kho.save(new Enrollment(new ClassId(lopA), hs, ClassRole.STUDENT, NOW));
        assertThat(membership.caiDatChoHocSinh(hs)).contains(new CaiDatChoHocSinh(lopA, false));

        khoCaiDat.save(ClassSettings.macDinh(new ClassId(lopA), NOW).capNhat(true, "offline", false, gv, NOW));
        assertThat(membership.caiDatChoHocSinh(hs)).contains(new CaiDatChoHocSinh(lopA, true));

        // Cài của lớp khác không lẫn sang; chưa ghi danh, hay chỉ là giáo viên, thì rỗng.
        khoCaiDat.save(ClassSettings.macDinh(new ClassId(lopB), NOW));
        kho.save(new Enrollment(new ClassId(lopB), gv, ClassRole.TEACHER, NOW));
        assertThat(membership.caiDatChoHocSinh(UUID.randomUUID())).isEmpty();
        assertThat(membership.caiDatChoHocSinh(gv)).isEmpty();
    }

    /** Kho ghi danh trong bộ nhớ, thứ tự như adapter thật (ghi danh cũ trước). */
    private static final class KhoGhiDanh implements EnrollmentRepository {

        private final List<Enrollment> rows = new ArrayList<>();

        @Override
        public Enrollment save(Enrollment enrollment) {
            rows.add(enrollment);
            return enrollment;
        }

        @Override
        public Optional<Enrollment> find(ClassId classId, UUID userId) {
            return rows.stream().filter(e -> e.classId().equals(classId) && e.userId().equals(userId)).findFirst();
        }

        @Override
        public List<Enrollment> findByUser(UUID userId) {
            return rows.stream().filter(e -> e.userId().equals(userId)).sorted(Comparator.comparing(Enrollment::enrolledAt)).toList();
        }

        @Override
        public List<Enrollment> findByClass(ClassId classId, ClassRole role) {
            return rows.stream().filter(e -> e.classId().equals(classId) && e.role() == role)
                .sorted(Comparator.comparing(Enrollment::enrolledAt)).toList();
        }
    }
}
