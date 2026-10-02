package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.EscalationJpaEntity;

public interface EscalationJpaRepository extends JpaRepository<EscalationJpaEntity, UUID> {

    List<EscalationJpaEntity> findByClassIdOrderByCreatedAtDescIdAsc(UUID classId);

    List<EscalationJpaEntity> findByClassIdAndHandledAtIsNullOrderByCreatedAtDescIdAsc(UUID classId);

    /** PostgreSQL khóa dòng khi UPDATE và kiểm lại điều kiện: hai giáo viên bấm cùng lúc thì một bên đổi. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EscalationJpaEntity e set e.handledAt = :at, e.handledBy = :by where e.id = :id and e.handledAt is null")
    int markHandled(@Param("id") UUID id, @Param("by") UUID handledBy, @Param("at") Instant at);
}
