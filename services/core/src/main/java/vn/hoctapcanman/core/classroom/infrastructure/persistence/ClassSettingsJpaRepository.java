package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.ClassSettingsJpaEntity;

public interface ClassSettingsJpaRepository extends JpaRepository<ClassSettingsJpaEntity, UUID> {}
