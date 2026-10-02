package vn.hoctapcanman.core.classroom.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;

@Entity
@Table(name = "enrollments")
@IdClass(EnrollmentJpaEntity.Key.class)
public class EnrollmentJpaEntity {

    @Id
    @Column(name = "class_id")
    private UUID classId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_class", nullable = false, length = 10)
    private ClassRole role;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @SuppressWarnings("NullAway.Init")
    protected EnrollmentJpaEntity() {}

    public static EnrollmentJpaEntity from(Enrollment enrollment) {
        EnrollmentJpaEntity entity = new EnrollmentJpaEntity();
        entity.classId = enrollment.classId().value();
        entity.userId = enrollment.userId();
        entity.role = enrollment.role();
        entity.enrolledAt = enrollment.enrolledAt();
        return entity;
    }

    public Enrollment toDomain() {
        return new Enrollment(new ClassId(classId), userId, role, enrolledAt);
    }

    /** Khóa chính (class_id, user_id). */
    public static class Key implements Serializable {

        private @Nullable UUID classId;
        private @Nullable UUID userId;

        public Key() {}

        public Key(UUID classId, UUID userId) {
            this.classId = classId;
            this.userId = userId;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Key k && Objects.equals(classId, k.classId) && Objects.equals(userId, k.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classId, userId);
        }
    }
}
