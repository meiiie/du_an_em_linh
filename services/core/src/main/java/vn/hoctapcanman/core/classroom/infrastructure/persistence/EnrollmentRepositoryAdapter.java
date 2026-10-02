package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;
import vn.hoctapcanman.core.classroom.domain.repository.EnrollmentRepository;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.EnrollmentJpaEntity;

@Repository
public class EnrollmentRepositoryAdapter implements EnrollmentRepository {

    private final EnrollmentJpaRepository jpa;

    public EnrollmentRepositoryAdapter(EnrollmentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Enrollment save(Enrollment enrollment) {
        return jpa.save(EnrollmentJpaEntity.from(enrollment)).toDomain();
    }

    @Override
    public Optional<Enrollment> find(ClassId classId, UUID userId) {
        return jpa.findById(new EnrollmentJpaEntity.Key(classId.value(), userId)).map(EnrollmentJpaEntity::toDomain);
    }

    @Override
    public List<Enrollment> findByUser(UUID userId) {
        return jpa.findByUserIdOrderByEnrolledAtAscClassIdAsc(userId).stream().map(EnrollmentJpaEntity::toDomain).toList();
    }

    @Override
    public List<Enrollment> findByClass(ClassId classId, ClassRole role) {
        return jpa.findByClassIdAndRoleOrderByEnrolledAtAscUserIdAsc(classId.value(), role).stream()
                .map(EnrollmentJpaEntity::toDomain)
                .toList();
    }
}
