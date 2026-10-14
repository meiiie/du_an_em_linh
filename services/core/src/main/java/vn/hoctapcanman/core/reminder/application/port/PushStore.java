package vn.hoctapcanman.core.reminder.application.port;
import java.util.*;
import java.time.Instant;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
public interface PushStore {
    void subscribe(UUID student,Subscription subscription,Instant now);
    void unsubscribe(UUID student,String endpoint);
    List<Target> targets();
    boolean delivered(UUID subscription,String reminderKey);
    void mark(UUID subscription,String reminderKey,Instant now);
    void remove(UUID id);
}
