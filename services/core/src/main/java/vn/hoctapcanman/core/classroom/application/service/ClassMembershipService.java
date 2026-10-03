package vn.hoctapcanman.core.classroom.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatChoHocSinh;
import vn.hoctapcanman.core.classroom.application.exception.KhongThuocLopException;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;
import vn.hoctapcanman.core.classroom.domain.repository.EnrollmentRepository;

/** Quyền theo lớp đọc thẳng từ ghi danh, mỗi lần gọi (không bộ nhớ đệm: rút khỏi lớp có hiệu lực ngay). */
@Service
@Transactional(readOnly = true)
public class ClassMembershipService implements ClassMembership {

    private final EnrollmentRepository enrollments;
    private final ClassSettingsRepository settings;

    public ClassMembershipService(EnrollmentRepository enrollments, ClassSettingsRepository settings) {
        this.enrollments = enrollments;
        this.settings = settings;
    }

    @Override
    public List<UUID> lopDay(UUID giaoVienId) {
        return lopTheoVaiTro(giaoVienId, ClassRole.TEACHER);
    }

    @Override
    public Optional<UUID> lopDangDay(UUID giaoVienId, @Nullable UUID lopChon) {
        List<UUID> lop = lopDay(giaoVienId);
        if (lopChon == null) {
            return lop.stream().findFirst();
        }
        return lop.contains(lopChon) ? Optional.of(lopChon) : Optional.empty();
    }

    @Override
    public Optional<UUID> lopHoc(UUID hocSinhId) {
        return lopTheoVaiTro(hocSinhId, ClassRole.STUDENT).stream().findFirst();
    }

    @Override
    public Optional<CaiDatChoHocSinh> caiDatChoHocSinh(UUID hocSinhId) {
        return lopHoc(hocSinhId).map(lop -> new CaiDatChoHocSinh(lop, settings.findByClassId(new ClassId(lop))
                .map(ClassSettings::revealSolutionAfterSubmit)
                .orElse(false)));
    }

    @Override
    public boolean laGiaoVien(UUID userId, UUID lopId) {
        return coVaiTro(userId, lopId, ClassRole.TEACHER);
    }

    @Override
    public boolean laHocSinh(UUID userId, UUID lopId) {
        return coVaiTro(userId, lopId, ClassRole.STUDENT);
    }

    @Override
    public void kiemGiaoVien(UUID userId, UUID lopId) {
        if (!laGiaoVien(userId, lopId)) {
            throw new KhongThuocLopException();
        }
    }

    @Override
    public boolean giaoVienDayHocSinh(UUID giaoVienId, UUID hocSinhId) {
        return lopHoc(hocSinhId).map(lop -> laGiaoVien(giaoVienId, lop)).orElse(false);
    }

    @Override
    public List<UUID> hocSinhCuaLop(UUID giaoVienId, UUID lopId) {
        kiemGiaoVien(giaoVienId, lopId);
        return enrollments.findByClass(new ClassId(lopId), ClassRole.STUDENT).stream().map(Enrollment::userId).toList();
    }

    private List<UUID> lopTheoVaiTro(UUID userId, ClassRole role) {
        return enrollments.findByUser(userId).stream()
                .filter(e -> e.role() == role)
                .map(e -> e.classId().value())
                .toList();
    }

    private boolean coVaiTro(UUID userId, UUID lopId, ClassRole role) {
        return enrollments.find(new ClassId(lopId), userId).filter(e -> e.role() == role).isPresent();
    }
}
