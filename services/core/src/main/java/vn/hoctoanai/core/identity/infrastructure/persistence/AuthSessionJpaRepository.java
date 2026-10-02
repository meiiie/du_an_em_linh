package vn.hoctoanai.core.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoctoanai.core.identity.infrastructure.persistence.entity.AuthSessionJpaEntity;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, UUID> {

    /** SELECT … FOR UPDATE (hoặc FOR NO KEY UPDATE): xung đột với UPDATE của đăng xuất trên cùng dòng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSessionJpaEntity s where s.id = :id")
    Optional<AuthSessionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AuthSessionJpaEntity s set s.revokedAt = :now where s.id = :id and s.revokedAt is null")
    int revoke(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AuthSessionJpaEntity s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AuthSessionJpaEntity s where not exists (select t.id from RefreshTokenJpaEntity t where t.sessionId = s.id)")
    int deleteWithoutTokens();
}
