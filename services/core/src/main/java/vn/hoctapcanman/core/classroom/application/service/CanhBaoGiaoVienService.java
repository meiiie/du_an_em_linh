package vn.hoctapcanman.core.classroom.application.service;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.CanhBaoGiaoVien;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;
import vn.hoctapcanman.core.classroom.domain.model.EscalationKind;
import vn.hoctapcanman.core.classroom.domain.repository.EscalationRepository;

/** Cảnh báo ghi vào lớp của học sinh (lấy từ ghi danh), trong giao dịch của bên gọi; trùng thì bỏ qua, không ném lỗi. */
@Service
public class CanhBaoGiaoVienService implements CanhBaoGiaoVien {

    private final ClassMembership membership;
    private final EscalationRepository escalations;
    private final Clock clock;

    public CanhBaoGiaoVienService(ClassMembership membership, EscalationRepository escalations, Clock clock) {
        this.membership = membership;
        this.escalations = escalations;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean ghiKet(UUID hocSinhId, String kyNang, @Nullable String maBai, @Nullable String maBuoc, String lyDo) {
        return ghi(hocSinhId, EscalationKind.KET, kyNang, maBai, maBuoc, lyDo);
    }

    @Override
    @Transactional
    public boolean ghiNhoGiaoVien(UUID hocSinhId, String kyNang, String maBai, @Nullable String maBuoc, String lyDo) {
        return ghi(hocSinhId, EscalationKind.NHO_GV, kyNang, Objects.requireNonNull(maBai, "maBai"), maBuoc, lyDo);
    }

    private boolean ghi(UUID hocSinhId, EscalationKind loai, String kyNang, @Nullable String maBai, @Nullable String maBuoc, String lyDo) {
        return membership.lopHoc(hocSinhId)
                .map(lop -> escalations.saveIfNoOpenDuplicate(
                        Escalation.open(new ClassId(lop), hocSinhId, loai, kyNang, maBai, maBuoc, lyDo, clock.instant())))
                .orElse(false);
    }
}
