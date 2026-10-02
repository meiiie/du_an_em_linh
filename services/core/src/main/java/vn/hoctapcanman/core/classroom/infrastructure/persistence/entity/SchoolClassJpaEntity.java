package vn.hoctapcanman.core.classroom.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.SchoolClass;

@Entity
@Table(name = "classes")
public class SchoolClassJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private short grade;

    @Column(name = "school_year", nullable = false, length = 9)
    private String schoolYear;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @SuppressWarnings("NullAway.Init")
    protected SchoolClassJpaEntity() {}

    public static SchoolClassJpaEntity from(SchoolClass schoolClass) {
        SchoolClassJpaEntity entity = new SchoolClassJpaEntity();
        entity.id = schoolClass.id().value();
        entity.name = schoolClass.name();
        entity.grade = (short) schoolClass.grade();
        entity.schoolYear = schoolClass.schoolYear();
        entity.createdAt = schoolClass.createdAt();
        return entity;
    }

    public SchoolClass toDomain() {
        return new SchoolClass(new ClassId(id), name, grade, schoolYear, createdAt);
    }
}
