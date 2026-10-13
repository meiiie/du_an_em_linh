package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.EnrollmentJpaEntity;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentJpaEntity, EnrollmentJpaEntity.Key> {

    @Query(value="SELECT e.* FROM enrollments e JOIN classes c ON c.id=e.class_id WHERE e.user_id=:userId AND c.enabled ORDER BY e.enrolled_at,e.class_id",nativeQuery=true)
    List<EnrollmentJpaEntity> findByUserIdOrderByEnrolledAtAscClassIdAsc(UUID userId);

    @Query(value="SELECT e.* FROM enrollments e JOIN classes c ON c.id=e.class_id WHERE e.class_id=:classId AND e.role_in_class=:#{#role.name()} AND c.enabled ORDER BY e.enrolled_at,e.user_id",nativeQuery=true)
    List<EnrollmentJpaEntity> findByClassIdAndRoleOrderByEnrolledAtAscUserIdAsc(UUID classId, ClassRole role);

    @Query(value="SELECT e.* FROM enrollments e JOIN classes c ON c.id=e.class_id WHERE e.class_id=:classId AND e.user_id=:userId AND c.enabled",nativeQuery=true)
    Optional<EnrollmentJpaEntity> findActive(UUID classId,UUID userId);
}
