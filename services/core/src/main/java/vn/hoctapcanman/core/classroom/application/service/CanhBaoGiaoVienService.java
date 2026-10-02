package vn.hoctapcanman.core.classroom.application.service;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.CanhBaoGiaoVien;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;
import vn.hoctapcanman.core.classroom.domain.model.EscalationKind;
import vn.hoctapcanman.core.classroom.domain.repository.EnrollmentRepository;
import vn.hoctapcanman.core.classroom.domain.repository.EscalationRepository;

/** Cảnh báo ghi vào lớp của học sinh (lấy từ ghi danh), trong giao dịch của bên gọi; trùng thì bỏ qua, không ném lỗi. */
@Service
public class CanhBaoGiaoVienService implements CanhBaoGiaoVien {

    private final ClassMembership membership;
    private final EnrollmentRepository enrollments;
    private final EscalationRepository escalations;
    private final Clock clock;

    public CanhBaoGiaoVienService(ClassMembership membership, EnrollmentRepository enrollments, EscalationRepository escalations,
            Clock clock) {
        this.membership = membership;
        this.enrollments = enrollments;
        this.escalations = escalations;
        this.clock = clock;
    }

    @Override
    @Transactional
    public KetQua ghiKet(UUID hocSinhId, String kyNang, @Nullable String maBai, @Nullable String maBuoc, String lyDo) {
        return ghi(hocSinhId, EscalationKind.KET, kyNang, maBai, maBuoc, lyDo);
    }

    @Override
    @Transactional
    public KetQua ghiNhoGiaoVien(UUID hocSinhId, String kyNang, String maBai, @Nullable String maBuoc, String lyDo) {
        return ghi(hocSinhId, EscalationKind.NHO_GV, kyNang, Objects.requireNonNull(maBai, "maBai"), maBuoc, lyDo);
    }

    private KetQua ghi(UUID hocSinhId, EscalationKind loai, String kyNang, @Nullable String maBai, @Nullable String maBuoc, String lyDo) {
        Optional<UUID> lop = membership.lopHoc(hocSinhId);
        if (lop.isEmpty()) {
            return KetQua.CHUA_THUOC_LOP;
        }
        ClassId lopId = new ClassId(lop.get());
        if (enrollments.findByClass(lopId, ClassRole.TEACHER).isEmpty()) {
            return KetQua.LOP_CHUA_CO_GIAO_VIEN;
        }
        boolean daGhi = escalations.saveIfNoOpenDuplicate(
                Escalation.open(lopId, hocSinhId, loai, kyNang, maBai, maBuoc, lyDo, clock.instant()));
        return daGhi ? KetQua.DA_GHI : KetQua.DA_CO_CANH_BAO_MO;
    }
}
