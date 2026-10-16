package vn.hoctapcanman.core.practice.infrastructure.seed;

import java.time.Clock;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;

/** Duy trì kịch bản demo cho tài khoản tổng hợp; không ghi đè lượt giao do giáo viên tự đặt. */
@Component @Profile("dev") @Order(7)
public class DemoNamSeeder implements ApplicationRunner {
    private final JdbcClient db;
    private final Clock clock;
    public DemoNamSeeder(JdbcClient db,Clock clock) { this.db=db;this.clock=clock; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(!LopSeedAnToan.kiem(db,"12A3"))return;
        UUID cl=TruongNguyenTrai.id("lop:12A3");
        var student=db.sql("""
            SELECT u.id FROM users u JOIN enrollments e ON e.user_id=u.id
            WHERE u.email='nam.nguyen.hoai@thpt-nguyentrai.edu.vn' AND u.synthetic AND u.enabled
                AND u.archived_at IS NULL AND e.class_id=:cl AND e.role_in_class='STUDENT'
            FOR UPDATE OF u
            """).param("cl",cl).query(UUID.class).optional();
        if(student.isEmpty())return;
        UUID hs=student.get();
        ensureOpen(cl,hs,"KIEM_TRA",1);
        ensureOpen(cl,hs,"ON_TAP",2);
    }
    private void ensureOpen(UUID cl,UUID hs,String mode,int minimum) {
        var now=Timestamp.from(clock.instant());
        int open=db.sql("""
            SELECT count(*) FROM assignments a JOIN problems p ON p.id=a.problem_id
            JOIN problem_releases r ON r.problem_id=p.id AND r.class_id=a.class_id AND r.status='DA_PHAT_HANH'
            WHERE a.class_id=:cl AND a.student_id=:hs AND a.status='DA_GIAO' AND a.mode=:mode AND a.due_at>:now
                AND p.function_sympy IS NOT NULL AND p.answer_form='TU_LUAN_5_BUOC'
                AND NOT EXISTS(SELECT 1 FROM submissions s WHERE s.student_id=:hs AND s.class_id=:cl
                    AND s.problem_id=p.id AND s.content_version=p.content_version AND s.status='DA_NOP'
                    AND (s.started_at>=a.assigned_at OR s.assignment_at>=a.assigned_at))
            """).param("cl",cl).param("hs",hs).param("mode",mode).param("now",now).query(Integer.class).single();
        if(open>=minimum)return;
        db.sql("""
            INSERT INTO assignments(id,class_id,problem_id,student_id,status,set_name,mode,due_at,assigned_by,assigned_at)
            SELECT gen_random_uuid(),:cl,p.id,:hs,'DA_GIAO',:title,:mode,:due,a.assigned_by,:now
            FROM assignments a JOIN problems p ON p.id=a.problem_id
            JOIN problem_releases r ON r.problem_id=p.id AND r.class_id=:cl AND r.status='DA_PHAT_HANH'
            JOIN users teacher ON teacher.id=a.assigned_by AND teacher.synthetic AND teacher.enabled
            WHERE a.student_id=:hs AND a.class_id=:cl AND a.mode=:mode AND a.status='DA_GIAO'
                AND NOT EXISTS(SELECT 1 FROM audit_log change WHERE change.action='GIAO_BAI' AND change.actor_id=a.assigned_by
                    AND change.target=a.class_id::text AND change.created_at=a.assigned_at)
                AND (a.set_name LIKE 'Ôn tập % – tuần 7' OR a.set_name LIKE 'Demo · %'
                    OR a.set_name='Kiểm tra hàm số — 45 phút' OR a.set_name='Luyện tập hiện tại · có gia sư')
                AND (a.due_at<=:now OR EXISTS(SELECT 1 FROM submissions done WHERE done.student_id=:hs AND done.class_id=:cl
                    AND done.problem_id=p.id AND done.status='DA_NOP' AND done.started_at>=a.assigned_at))
                AND p.function_sympy IS NOT NULL AND p.answer_form='TU_LUAN_5_BUOC'
            ORDER BY (EXISTS(SELECT 1 FROM submissions s WHERE s.student_id=:hs AND s.problem_id=p.id)),p.code
            LIMIT :needed
            ON CONFLICT(class_id,problem_id,student_id) DO UPDATE SET
                status='DA_GIAO',set_name=excluded.set_name,due_at=excluded.due_at,assigned_at=excluded.assigned_at
            """).param("cl",cl).param("hs",hs).param("mode",mode).param("now",now)
            .param("due",Timestamp.from(clock.instant().plusSeconds(7*86400)))
            .param("title",mode.equals("KIEM_TRA")?"Demo · Kiểm tra đang mở":"Demo · Luyện tập có gia sư")
            .param("needed",minimum-open).update();
    }
}
