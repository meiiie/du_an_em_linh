package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.SchoolClassJpaEntity;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassJpaEntity, UUID> {

    Optional<SchoolClassJpaEntity> findByNameAndSchoolYear(String name, String schoolYear);
}
