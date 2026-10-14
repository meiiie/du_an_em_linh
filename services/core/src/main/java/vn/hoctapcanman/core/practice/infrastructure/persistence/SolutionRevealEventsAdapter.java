package vn.hoctapcanman.core.practice.infrastructure.persistence;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.practice.application.port.SolutionRevealEvents;
@Component
public class SolutionRevealEventsAdapter implements SolutionRevealEvents {
    private final JdbcClient db;
    public SolutionRevealEventsAdapter(JdbcClient db) { this.db=db; }
    public boolean blocked(UUID student, UUID classId, UUID problem, Instant now) {
        return db.sql("SELECT EXISTS(SELECT 1 FROM assignments WHERE student_id=:student AND problem_id=:problem AND status='DA_GIAO' AND mode='KIEM_TRA' AND (due_at IS NULL OR due_at>:now))")
            .param("student",student).param("problem",problem).param("now",Timestamp.from(now)).query(Boolean.class).single();
    }
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void modelCall(UUID classId,String provider,Instant now) {
        // HintModel exposes no token counts. NULL means unknown, never an invented zero.
        db.sql("INSERT INTO ai_usage(id,class_id,purpose,provider,input_tokens,output_tokens,created_at) VALUES(:id,:cl,'GIAI_THICH_LOI_GIAI',:provider,NULL,NULL,:now)")
            .param("id",UUID.randomUUID()).param("cl",classId).param("provider",provider).param("now",Timestamp.from(now)).update();
    }
    public void record(UUID submission, Instant now) {
        db.sql("INSERT INTO solution_reveals(submission_id,revealed_at) VALUES(:id,:now) ON CONFLICT(submission_id) DO NOTHING")
            .param("id",submission).param("now",Timestamp.from(now)).update();
    }
}
