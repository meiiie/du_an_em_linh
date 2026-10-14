package vn.hoctapcanman.core.reminder.infrastructure.push;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vn.hoctapcanman.core.reminder.application.service.PushService;
@Configuration(proxyBeanMethods=false)
@EnableScheduling
public class PushScheduleAdapter {
    private final PushService service;
    public PushScheduleAdapter(PushService service){this.service=service;}
    @Scheduled(fixedDelayString="${mathl.push.interval:60000}") public void dispatch(){service.dispatch();}
}
