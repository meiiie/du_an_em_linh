package vn.hoctapcanman.core.classroom.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;

public interface EscalationRepository {

    /**
     * Ghi cảnh báo mở nếu chưa có cảnh báo mở cùng (lớp, học sinh, loại, kỹ năng, bài). Trả {@code true} khi đã ghi.
     * Không ném lỗi khi trùng, kể cả hai lượt ghi đồng thời: giao dịch của bên gọi (tutor, mastery) không bị hỏng.
     */
    boolean saveIfNoOpenDuplicate(Escalation escalation);

    Optional<Escalation> findById(UUID id);

    /** Cảnh báo của lớp, mới trước cũ sau; {@code onlyOpen} thì chỉ cảnh báo chưa xử lý. */
    List<Escalation> findByClass(ClassId classId, boolean onlyOpen);

    /** Đánh dấu đã xử lý nếu còn mở; trả {@code true} khi đổi. Hai giáo viên bấm cùng lúc: một bên thắng. */
    boolean markHandled(UUID id, UUID handledBy, Instant at);
}
