package vn.hoctapcanman.core.workspace.application.port;
import java.time.Instant;
import java.util.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
public interface ContentDraftStore {
    List<String> authoringSkills(UUID classId);
    /** Danh mục kỹ năng soạn bài của lớp (mã, tên), không phụ thuộc ngân hàng hay học sinh đã có. */
    default List<vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item> authoringSkillCatalog(UUID classId) {
        return authoringSkills(classId).stream().map(c->new vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item(c,c,"","KY_NANG",java.util.Map.of())).toList();
    }
    Optional<String> material(UUID classId, UUID document);
    void recordUsage(UUID classId,String provider,int input,int output,Instant now);
    void save(UUID actor, DraftRequest request, GeneratedDraft draft, Instant now);
    List<Item> pending(UUID classId);
    Optional<GeneratedDraft> lock(UUID classId, UUID id);
    void decide(UUID actor, UUID id, Review review, Instant now);
}
