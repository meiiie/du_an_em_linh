package vn.hoctapcanman.core.tutor.application.dto;

import java.time.Instant;
import java.util.*;
import jakarta.validation.constraints.*;

public final class TutorSessionDto {
    private TutorSessionDto() {}
    public record Open(@NotNull UUID giaoBaiId) {}
    public record Ask(@NotNull UUID requestId, @NotBlank @Size(max=32) String maBuoc,
        @Min(1) @Max(3) int cap, @NotNull @Size(max=2000) String cauHoi) {
        @Override public String toString(){return "Ask[đã ẩn nội dung]";}
    }
    public record Cancel(@NotNull UUID turnId) {}
    public record Opened(UUID sessionId,long lastSeq,@org.jspecify.annotations.Nullable UUID turnId,String status) {}
    public record CancelAck(UUID sessionId,UUID turnId,String status,long lastSeq) {}
    public record Assignment(UUID id, UUID studentId, UUID classId, String code, int contentVersion, String level) {}
    public record Session(UUID id, UUID studentId, UUID assignmentId, UUID classId, String tomTat,
        long summaryThrough, String contextVersion, long lastSeq, long revision, int tokenCount) {
        @Override public String toString(){return "Session[id="+id+"]";}
    }
    public record Reference(int index, String text, String latex) {}
    public record Turn(UUID id, UUID sessionId, UUID requestId, long ordinal, String step, int level,
        String question, String answer, String status, Instant createdAt, List<Reference> references, String contextVersion) {
        public Turn { references=List.copyOf(references); }
        public Turn(UUID id,UUID sessionId,UUID requestId,long ordinal,String step,int level,
            String question,String answer,String status,Instant createdAt) {
            this(id,sessionId,requestId,ordinal,step,level,question,answer,status,createdAt,List.of(),"");
        }
        @Override public String toString(){return "Turn[id="+id+",status="+status+"]";}
    }
    public record Event(int v, UUID sessionId, UUID turnId, long seq, String type, Map<String,Object> data) {}
    public record History(Session session, List<Turn> turns) {}
    public record Accepted(Session session, Turn turn, boolean created) {}
    public record Usage(UUID studentId, long requests, long inputTokens, long outputTokens, long estimatedTokens) {}
    public record UsageReport(UUID classId,long requests,long inputTokens,long outputTokens,long estimatedTokens,List<Usage> students) {}
    public record ModelUsage(int inputTokens, int outputTokens, boolean estimated) {}
}
