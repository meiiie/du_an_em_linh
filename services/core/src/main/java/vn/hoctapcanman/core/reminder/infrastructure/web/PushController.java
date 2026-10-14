package vn.hoctapcanman.core.reminder.infrastructure.web;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
import vn.hoctapcanman.core.reminder.application.service.PushService;
@RestController
@RequestMapping("/api/hs/nhac/push")
public class PushController {
    private final PushService service;
    public PushController(PushService service){this.service=service;}
    @GetMapping public Config config(){return service.config();}
    @PostMapping public Result subscribe(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Subscription request){return service.subscribe(UUID.fromString(jwt.getSubject()),request);}
    @DeleteMapping public Result unsubscribe(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Subscription request){return service.unsubscribe(UUID.fromString(jwt.getSubject()),request);}
}
