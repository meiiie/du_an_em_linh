package vn.hoctapcanman.core.classroom.application.usecase;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.dto.CanhBaoDto;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.repository.EscalationRepository;

/** Cảnh báo kẹt và «gửi thầy cô» của một lớp, chỉ cho giáo viên của lớp đó (FR-025, F-08). */
@Service
public class GetCanhBaoCuaLopUseCase {

    private final ClassMembership membership;
    private final EscalationRepository escalations;

    public GetCanhBaoCuaLopUseCase(ClassMembership membership, EscalationRepository escalations) {
        this.membership = membership;
        this.escalations = escalations;
    }

    @Transactional(readOnly = true)
    public List<CanhBaoDto> execute(UUID giaoVienId, UUID lopId, boolean chiChuaXuLy) {
        membership.kiemGiaoVien(giaoVienId, lopId);
        return escalations.findByClass(new ClassId(lopId), chiChuaXuLy).stream().map(CanhBaoDto::from).toList();
    }
}
