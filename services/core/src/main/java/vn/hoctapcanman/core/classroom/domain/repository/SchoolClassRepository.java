package vn.hoctapcanman.core.classroom.domain.repository;

import java.util.List;
import java.util.Optional;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;

public interface SchoolClassRepository {

    SchoolClass save(SchoolClass schoolClass);

    Optional<SchoolClass> findById(ClassId id);

    Optional<SchoolClass> findByNameAndSchoolYear(String name, String schoolYear);

    /** Id mọi lớp, theo thời điểm tạo rồi id. */
    List<ClassId> findAllIds();
}
