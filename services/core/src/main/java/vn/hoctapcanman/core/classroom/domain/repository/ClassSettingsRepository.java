package vn.hoctapcanman.core.classroom.domain.repository;

import java.util.Optional;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;

public interface ClassSettingsRepository {

    Optional<ClassSettings> findByClassId(ClassId classId);

    ClassSettings save(ClassSettings settings);
}
