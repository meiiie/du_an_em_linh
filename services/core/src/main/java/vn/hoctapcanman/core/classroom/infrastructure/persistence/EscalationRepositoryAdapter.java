package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;
import vn.hoctapcanman.core.classroom.domain.repository.EscalationRepository;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.EscalationJpaEntity;

/**
 * Ghi cảnh báo bằng JDBC {@code insert … on conflict do nothing} trên chỉ mục duy nhất một phần
 * {@code escalations_one_open_idx}: trùng thì không ghi và không ném lỗi, nên giao dịch của bên gọi (tutor, mastery)
 * không bị đánh dấu rollback. JdbcClient dùng chung kết nối với JPA trong cùng giao dịch (JpaTransactionManager).
 */
@Repository
public class EscalationRepositoryAdapter implements EscalationRepository {

    private final EscalationJpaRepository jpa;
    private final JdbcClient jdbc;

    public EscalationRepositoryAdapter(EscalationJpaRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public boolean saveIfNoOpenDuplicate(Escalation e) {
        if (!e.isOpen()) {
            throw new IllegalArgumentException("Chỉ ghi mới cảnh báo chưa xử lý");
        }
        jpa.flush(); // JDBC không tự xả ngữ cảnh JPA: lớp / ghi danh vừa lưu trong cùng giao dịch phải có trước khóa ngoại
        int ghi = jdbc.sql("""
                insert into escalations (id, class_id, student_id, skill_code, problem_code, step_code, kind, reason, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict do nothing""")
                .params(e.id(), e.classId().value(), e.studentId(), e.skillCode(), e.problemCode(), e.stepCode(),
                        e.kind().name(), e.reason(), Timestamp.from(e.createdAt()))
                .update();
        return ghi == 1;
    }

    @Override
    public Optional<Escalation> findById(UUID id) {
        return jpa.findById(id).map(EscalationJpaEntity::toDomain);
    }

    @Override
    public List<Escalation> findByClass(ClassId classId, boolean onlyOpen) {
        List<EscalationJpaEntity> rows = onlyOpen
                ? jpa.findByClassIdAndHandledAtIsNullOrderByCreatedAtDescIdAsc(classId.value())
                : jpa.findByClassIdOrderByCreatedAtDescIdAsc(classId.value());
        return rows.stream().map(EscalationJpaEntity::toDomain).toList();
    }

    @Override
    public boolean markHandled(UUID id, UUID handledBy, Instant at) {
        return jpa.markHandled(id, handledBy, at) == 1;
    }
}
