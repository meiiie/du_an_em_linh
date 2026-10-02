package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.EnrollmentJpaEntity;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentJpaEntity, EnrollmentJpaEntity.Key> {

    List<EnrollmentJpaEntity> findByUserIdOrderByEnrolledAtAscClassIdAsc(UUID userId);

    List<EnrollmentJpaEntity> findByClassIdAndRoleOrderByEnrolledAtAscUserIdAsc(UUID classId, ClassRole role);
}
