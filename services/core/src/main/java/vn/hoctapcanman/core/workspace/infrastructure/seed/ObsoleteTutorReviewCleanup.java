package vn.hoctapcanman.core.workspace.infrastructure.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Removes only obsolete formula-gate items; safe to repeat, never active outside dev. */
@Component
@Profile("dev")
@Order(100)
public class ObsoleteTutorReviewCleanup implements ApplicationRunner {
    private final JdbcClient db;
    public ObsoleteTutorReviewCleanup(JdbcClient db) { this.db=db; }
    @Override @Transactional public void run(ApplicationArguments arguments) {
        db.sql("DELETE FROM tutor_review_queue WHERE reason LIKE 'Cổng công thức chưa xác minh được lời gợi ý%'").update();
    }
}
