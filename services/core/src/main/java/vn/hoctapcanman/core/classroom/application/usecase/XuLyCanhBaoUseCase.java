package vn.hoctapcanman.core.classroom.application.usecase;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.dto.CanhBaoDto;
import vn.hoctapcanman.core.classroom.application.exception.CanhBaoKhongTimThayException;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;
import vn.hoctapcanman.core.classroom.domain.repository.EscalationRepository;

/**
 * Giáo viên đánh dấu đã xử lý một cảnh báo của lớp mình. Cảnh báo của lớp khác trả cùng lỗi «không tìm thấy» như cảnh
 * báo không có (contracts/api-core.md: 404 không lộ tồn tại). Đã xử lý rồi thì giữ người và thời điểm xử lý đầu tiên.
 */
@Service
public class XuLyCanhBaoUseCase {

    private final ClassMembership membership;
    private final EscalationRepository escalations;
    private final Clock clock;

    public XuLyCanhBaoUseCase(ClassMembership membership, EscalationRepository escalations, Clock clock) {
        this.membership = membership;
        this.escalations = escalations;
        this.clock = clock;
    }

    @Transactional
    public CanhBaoDto execute(UUID giaoVienId, UUID canhBaoId) {
        Escalation canhBao = escalations.findById(canhBaoId)
                .filter(e -> membership.laGiaoVien(giaoVienId, e.classId().value()))
                .orElseThrow(CanhBaoKhongTimThayException::new);
        escalations.markHandled(canhBaoId, giaoVienId, clock.instant());
        return escalations.findById(canhBao.id()).map(CanhBaoDto::from).orElseThrow(CanhBaoKhongTimThayException::new);
    }
}
