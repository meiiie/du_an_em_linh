package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.ClassSettingsJpaEntity;

@Repository
public class ClassSettingsRepositoryAdapter implements ClassSettingsRepository {

    private final ClassSettingsJpaRepository jpa;

    public ClassSettingsRepositoryAdapter(ClassSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ClassSettings> findByClassId(ClassId classId) {
        return jpa.findById(classId.value()).map(ClassSettingsJpaEntity::toDomain);
    }

    @Override
    public ClassSettings save(ClassSettings settings) {
        return jpa.save(ClassSettingsJpaEntity.from(settings)).toDomain();
    }
}
