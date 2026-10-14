package vn.hoctapcanman.core.reminder.application.port;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
public interface PushSender {
    Config config();
    int send(Subscription subscription);
}
