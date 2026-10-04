package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.SchoolClassJpaEntity;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassJpaEntity, UUID> {

    Optional<SchoolClassJpaEntity> findByNameAndSchoolYear(String name, String schoolYear);

    @Query("select c.id from SchoolClassJpaEntity c order by c.createdAt, c.id")
    List<UUID> findAllIdsOrdered();
}
