package vn.hoctapcanman.core.practice.application.port;
import java.time.Instant;
import java.util.UUID;
public interface SolutionRevealEvents {
    boolean blocked(UUID student, UUID classId, UUID problem, Instant now);
    void record(UUID submission, Instant now);
    void modelCall(UUID classId,String provider,Instant now);
}
