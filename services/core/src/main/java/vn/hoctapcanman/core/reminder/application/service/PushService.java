package vn.hoctapcanman.core.reminder.application.service;
import java.util.*;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
import vn.hoctapcanman.core.reminder.application.port.*;
import vn.hoctapcanman.core.workspace.application.port.WorkspaceStore;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
@Service
public class PushService {
    private final PushStore store;
    private final PushSender sender;
    private final WorkspaceStore workspace;
    private final ClassMembership membership;
    private final Clock clock;
    public PushService(PushStore store,PushSender sender,WorkspaceStore workspace,ClassMembership membership,Clock clock) {
        this.store=store;this.sender=sender;this.workspace=workspace;this.membership=membership;this.clock=clock;
    }
    public Config config(){return sender.config();}
    @Transactional public Result subscribe(UUID student,Subscription request) {
        if(!sender.config().available())throw new WorkspaceException(503,"Nhắc trong ứng dụng vẫn hoạt động; thông báo trình duyệt chưa sẵn sàng.");
        validateEndpoint(request.endpoint());
        store.subscribe(student,request,clock.instant());
        return new Result("Đã bật nhắc học trên thiết bị này.");
    }
    @Transactional public Result unsubscribe(UUID student,Subscription request) {
        store.unsubscribe(student,request.endpoint());
        return new Result("Đã tắt nhắc học trên thiết bị này.");
    }
    public static void validateEndpoint(String endpoint) {
        try {
            var uri=java.net.URI.create(endpoint);
            String host=uri.getHost();
            boolean approved=Set.of("fcm.googleapis.com","updates.push.services.mozilla.com","web.push.apple.com").contains(host)
                || host!=null&&(host.endsWith(".push.services.mozilla.com")||host.endsWith(".notify.windows.com"));
            if(!"https".equals(uri.getScheme())||!approved||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getPort()!=-1)
                throw new IllegalArgumentException();
        } catch(RuntimeException ex) {throw new WorkspaceException(400,"Địa chỉ thông báo của trình duyệt chưa được hỗ trợ.");}
    }
    /** Opaque reminders: no student name or exercise content leaves the application. */
    public void dispatch() {
        if(!sender.config().available())return;
        for(Target target:store.targets()) {
            var cl=membership.lopHoc(target.student());
            if(cl.isEmpty())continue;
            for(var reminder:workspace.reminders(target.student(),clock.instant())) {
                String key=reminder.id()+":"+Objects.toString(reminder.details().getOrDefault("startsAt",reminder.details().get("dueAt")),"");
                if(key.length()>250||store.delivered(target.id(),key))continue;
                int status=sender.send(target.subscription());
                if(status==404||status==410){store.remove(target.id());break;}
                if(status>=200&&status<300)store.mark(target.id(),key,clock.instant());
            }
        }
    }
}
