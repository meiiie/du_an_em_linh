package vn.hoctapcanman.core.classroom.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;
import vn.hoctapcanman.core.classroom.domain.repository.SchoolClassRepository;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.entity.SchoolClassJpaEntity;

@Repository
public class SchoolClassRepositoryAdapter implements SchoolClassRepository {

    private final SchoolClassJpaRepository jpa;

    public SchoolClassRepositoryAdapter(SchoolClassJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        return jpa.save(SchoolClassJpaEntity.from(schoolClass)).toDomain();
    }

    @Override
    public Optional<SchoolClass> findById(ClassId id) {
        return jpa.findById(id.value()).map(SchoolClassJpaEntity::toDomain);
    }

    @Override
    public Optional<SchoolClass> findByNameAndSchoolYear(String name, String schoolYear) {
        return jpa.findByNameAndSchoolYear(name.strip(), schoolYear).map(SchoolClassJpaEntity::toDomain);
    }

    @Override
    public List<ClassId> findAllIds() {
        return jpa.findAllIdsOrdered().stream().map(ClassId::new).toList();
    }
}
