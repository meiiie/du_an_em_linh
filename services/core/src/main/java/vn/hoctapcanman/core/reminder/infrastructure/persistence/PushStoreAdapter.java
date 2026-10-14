package vn.hoctapcanman.core.reminder.infrastructure.persistence;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
import vn.hoctapcanman.core.reminder.application.port.PushStore;
@Component
public class PushStoreAdapter implements PushStore {
    private final JdbcClient db;
    public PushStoreAdapter(JdbcClient db){this.db=db;}
    public void subscribe(UUID student,Subscription s,Instant now) {
        // A browser changing accounts transfers its subscription and clears old reminder history.
        db.sql("DELETE FROM push_subscriptions WHERE endpoint=:endpoint AND student_id<>:student").param("endpoint",s.endpoint()).param("student",student).update();
        db.sql("INSERT INTO push_subscriptions(id,student_id,endpoint,p256dh,auth,created_at) VALUES(:id,:student,:endpoint,:key,:auth,:now) ON CONFLICT(endpoint) DO UPDATE SET student_id=excluded.student_id,p256dh=excluded.p256dh,auth=excluded.auth")
            .param("id",UUID.randomUUID()).param("student",student).param("endpoint",s.endpoint()).param("key",s.keys().p256dh()).param("auth",s.keys().auth()).param("now",Timestamp.from(now)).update();
    }
    public void unsubscribe(UUID student,String endpoint){db.sql("DELETE FROM push_subscriptions WHERE student_id=:student AND endpoint=:endpoint").param("student",student).param("endpoint",endpoint).update();}
    public List<Target> targets(){return db.sql("SELECT p.* FROM push_subscriptions p JOIN users u ON u.id=p.student_id AND u.enabled").query((r,n)->new Target(r.getObject("id",UUID.class),r.getObject("student_id",UUID.class),new Subscription(r.getString("endpoint"),new Keys(r.getString("p256dh"),r.getString("auth"))))).list();}
    public boolean delivered(UUID id,String key){return db.sql("SELECT EXISTS(SELECT 1 FROM push_deliveries WHERE subscription_id=:id AND reminder_key=:key)").param("id",id).param("key",key).query(Boolean.class).single();}
    public void mark(UUID id,String key,Instant now){db.sql("INSERT INTO push_deliveries VALUES(:id,:key,:now) ON CONFLICT DO NOTHING").param("id",id).param("key",key).param("now",Timestamp.from(now)).update();}
    public void remove(UUID id){db.sql("DELETE FROM push_subscriptions WHERE id=:id").param("id",id).update();}
}
