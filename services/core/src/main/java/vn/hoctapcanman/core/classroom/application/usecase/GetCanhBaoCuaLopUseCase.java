package vn.hoctapcanman.core.classroom.application.usecase;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.dto.CanhBaoDto;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.repository.EscalationRepository;

/**
 * Cảnh báo kẹt và «gửi thầy cô» của một lớp, chỉ cho giáo viên của lớp đó (FR-025, F-08). Chỉ cảnh báo của học sinh còn
 * ghi danh trong lớp, như v0 lọc theo học sinh của giáo viên (`apps/web/app/gv/page.tsx`): học sinh đã rời lớp thì
 * giáo viên cũ không còn thấy.
 */
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
        List<UUID> hocSinh = membership.hocSinhCuaLop(giaoVienId, lopId);
        return escalations.findByClass(new ClassId(lopId), chiChuaXuLy).stream()
                .filter(e -> hocSinh.contains(e.studentId()))
                .map(CanhBaoDto::from)
                .toList();
    }
}
