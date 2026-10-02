package vn.hoctapcanman.core.classroom.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;

@Entity
@Table(name = "class_settings")
public class ClassSettingsJpaEntity {

    @Id
    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "reveal_solution_after_submit", nullable = false)
    private boolean revealSolutionAfterSubmit;

    @Column(name = "ai_provider", nullable = false, length = 32)
    private String aiProvider;

    @Column(name = "ai_allow_local", nullable = false)
    private boolean aiAllowLocal;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private @Nullable UUID updatedBy;

    @SuppressWarnings("NullAway.Init")
    protected ClassSettingsJpaEntity() {}

    public static ClassSettingsJpaEntity from(ClassSettings settings) {
        ClassSettingsJpaEntity entity = new ClassSettingsJpaEntity();
        entity.classId = settings.classId().value();
        entity.revealSolutionAfterSubmit = settings.revealSolutionAfterSubmit();
        entity.aiProvider = settings.aiProvider();
        entity.aiAllowLocal = settings.aiAllowLocal();
        entity.updatedAt = settings.updatedAt();
        entity.updatedBy = settings.updatedBy();
        return entity;
    }

    public ClassSettings toDomain() {
        return new ClassSettings(new ClassId(classId), revealSolutionAfterSubmit, aiProvider, aiAllowLocal, updatedAt, updatedBy);
    }
}
