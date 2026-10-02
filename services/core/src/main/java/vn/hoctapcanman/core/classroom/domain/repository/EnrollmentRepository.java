package vn.hoctapcanman.core.classroom.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassRole;
import vn.hoctapcanman.core.classroom.domain.model.Enrollment;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> find(ClassId classId, UUID userId);

    /** Mọi ghi danh của một người, cũ trước mới sau. */
    List<Enrollment> findByUser(UUID userId);

    /** Ghi danh của lớp theo vai trò, cũ trước mới sau. */
    List<Enrollment> findByClass(ClassId classId, ClassRole role);
}
