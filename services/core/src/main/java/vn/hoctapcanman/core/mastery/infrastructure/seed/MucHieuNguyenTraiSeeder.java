package vn.hoctapcanman.core.mastery.infrastructure.seed;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.hoctapcanman.core.mastery.domain.model.Level4;
import vn.hoctapcanman.core.mastery.domain.model.MasteryEvent;
import vn.hoctapcanman.core.mastery.domain.model.MasteryState;
import vn.hoctapcanman.core.mastery.domain.repository.MasteryRepository;
import vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;

/** Tính BKT thật từ bài nộp lịch sử hư cấu, một lần lúc bootstrap; không ghi đè tiến độ đang học. */
@Component
@Profile("dev")
@Order(6)
public class MucHieuNguyenTraiSeeder implements ApplicationRunner {
    private final JdbcClient db;
    private final MasteryRepository mastery;
    private final TransactionTemplate tx;
    public MucHieuNguyenTraiSeeder(JdbcClient db, MasteryRepository mastery, PlatformTransactionManager manager) {
        this.db=db; this.mastery=mastery; this.tx=new TransactionTemplate(manager);
    }
    @Override public void run(ApplicationArguments args) {
        var config=mastery.config();
        for (String name:TruongNguyenTrai.LOP) {
            if (!LopSeedAnToan.kiem(db,name)) continue;
            UUID cl=TruongNguyenTrai.id("lop:"+name);
            List<UUID> students=db.sql("SELECT user_id FROM enrollments WHERE class_id=:cl AND role_in_class='STUDENT'")
                .param("cl",cl).query(UUID.class).list();
            for (UUID student:students) {
                tx.executeWithoutResult(t -> {
                    if (!mastery.statesOf(List.of(student)).isEmpty()) return;
                    var works=db.sql("""
                        SELECT s.id,s.skill_code,s.level4,s.result,s.submitted_at,g.error_code
                        FROM submissions s JOIN problems p ON p.id=s.problem_id JOIN grading_results g ON g.id=s.result_grading_id
                        WHERE s.student_id=:student AND s.class_id=:cl AND p.origin='GIAO_VIEN_NGUYEN_TRAI'
                        AND s.status='DA_NOP' AND s.result IN ('DAT','SAI') ORDER BY s.submitted_at,s.id
                        """).param("student",student).param("cl",cl).query((r,n) -> new Work(r.getObject(1,UUID.class),r.getString(2),
                            Level4.valueOf(r.getString(3)),r.getString(4).equals("DAT"),r.getTimestamp(5).toInstant(),r.getString(6))).list();
                    for (Work work:works) {
                        if (mastery.eventOf(work.id()).isPresent()) continue;
                        var before=mastery.lock(MasteryState.initial(student,work.skill(),config,work.at()));
                        var update=before.apply(new MasteryState.Evidence(work.correct(),work.level(),work.error(),false),config,work.at());
                        mastery.save(update.after(),new MasteryEvent(TruongNguyenTrai.id("muc-hieu:"+work.id()),student,work.skill(),work.id(),
                            update.delta(),MasteryEvent.Rule.THEO_KY_NANG_BAI,work.correct()?null:"B.DH.DAOHAM",work.error(),null,
                            false,before.level(),update.after().level(),config.version(),work.at()));
                        if (update.stuckAlert()) db.sql("""
                            INSERT INTO escalations(id,class_id,student_id,skill_code,kind,reason,created_at)
                            VALUES(:id,:cl,:hs,:skill,'KET','Cần củng cố quy tắc đạo hàm sau các lượt sai liên tiếp.',:at)
                            ON CONFLICT DO NOTHING
                            """).param("id",TruongNguyenTrai.id("ket:"+student+":"+work.skill())).param("cl",cl).param("hs",student)
                            .param("skill",work.skill()).param("at",Timestamp.from(work.at())).update();
                    }
                });
            }
        }
    }
    private record Work(UUID id,String skill,Level4 level,boolean correct,Instant at,@org.jspecify.annotations.Nullable String error) {}
}
