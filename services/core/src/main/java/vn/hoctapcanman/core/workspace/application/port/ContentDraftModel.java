package vn.hoctapcanman.core.workspace.application.port;
import java.util.Optional;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.GeneratedDraft;
public interface ContentDraftModel {
    Optional<GeneratedDraft> generate(java.util.UUID classId,String material);
    default Optional<GeneratedDraft> generate(java.util.UUID classId,String material,String skillCode) {
        return generate(classId,material);
    }
}
