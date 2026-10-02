package vn.hoctapcanman.core.classroom.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;
import vn.hoctapcanman.core.classroom.domain.model.EscalationKind;

/** Chỉ đọc qua JPA; ghi mới bằng JDBC «on conflict do nothing» ở adapter để không ghi trùng cảnh báo mở. */
@Entity
@Table(name = "escalations")
public class EscalationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "skill_code", nullable = false, length = 64)
    private String skillCode;

    @Column(name = "problem_code", length = 64)
    private @Nullable String problemCode;

    @Column(name = "step_code", length = 64)
    private @Nullable String stepCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EscalationKind kind;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "handled_at")
    private @Nullable Instant handledAt;

    @Column(name = "handled_by")
    private @Nullable UUID handledBy;

    @SuppressWarnings("NullAway.Init")
    protected EscalationJpaEntity() {}

    public Escalation toDomain() {
        return new Escalation(id, new ClassId(classId), studentId, kind, skillCode, problemCode, stepCode, reason, createdAt,
                handledAt, handledBy);
    }
}
