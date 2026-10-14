package vn.hoctapcanman.core.workspace.application.port;

import java.util.UUID;
import java.util.List;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;

public interface ContentWorkflow {
    void createProblem(UUID actor,NewProblem request);
    void createApprovedDraft(UUID actor,NewProblem request,String solution);
    void review(UUID actor,UUID id,Review request);
    int checkFormulas(UUID actor,UUID classId);
    boolean filter(UUID classId,UUID problemId,String draft,String question);
    boolean filter(UUID classId,UUID problemId,String draft,String question,List<String> submittedLines);
    java.util.Optional<String> sanitize(UUID classId,UUID problemId,String draft,String question,List<String> submittedLines);
    String extractPdf(byte[] bytes);
}
