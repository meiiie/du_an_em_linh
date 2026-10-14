package vn.hoctapcanman.core.workspace.application.port;
import java.time.Instant;
import java.util.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
public interface ContentDraftStore {
    Optional<String> material(UUID classId, UUID document);
    void recordUsage(UUID classId,String provider,int input,int output,Instant now);
    void save(UUID actor, DraftRequest request, GeneratedDraft draft, Instant now);
    List<Item> pending(UUID classId);
    Optional<GeneratedDraft> lock(UUID classId, UUID id);
    void decide(UUID actor, UUID id, Review review, Instant now);
}
