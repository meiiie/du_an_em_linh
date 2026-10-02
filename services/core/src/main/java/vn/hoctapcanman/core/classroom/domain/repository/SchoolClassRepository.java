package vn.hoctapcanman.core.classroom.domain.repository;

import java.util.Optional;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;

public interface SchoolClassRepository {

    SchoolClass save(SchoolClass schoolClass);

    Optional<SchoolClass> findById(ClassId id);

    Optional<SchoolClass> findByNameAndSchoolYear(String name, String schoolYear);
}
