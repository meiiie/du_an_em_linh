package vn.hoctapcanman.core.workspace.application.port;
import java.util.UUID;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
public interface ContentWorkflow {
 void review(UUID actor,UUID id,Review request);
 int checkFormulas(UUID actor,UUID classId);
 boolean filter(UUID classId,UUID problemId,String draft,String question);
 String extractPdf(byte[] bytes);
}
