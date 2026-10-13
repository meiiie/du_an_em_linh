package vn.hoctapcanman.core.tutor.infrastructure.web;

import java.io.IOException;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.hoctapcanman.core.tutor.application.service.OfflineTutorService;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@RestController public class OfflineTutorController {
    private final OfflineTutorService tutor;
    public OfflineTutorController(OfflineTutorService tutor) {
        this.tutor=tutor;
    }

    @PostMapping(value="/api/hs/gia-su",produces="text/event-stream") public SseEmitter ask(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody TutorRequest request) {
        UUID student=UUID.fromString(jwt.getSubject());
        tutor.authorize(student,request);
        SseEmitter emitter=new SseEmitter(45000L);
        Thread.startVirtualThread(()-> {
            try {
                for(String state:new String[] {
                    "kho","goi","loc"
                })emitter.send(SseEmitter.event().name(state).data(new TutorEvent(state,"")));TutorEvent answer=tutor.answer(student,request);emitter.send(SseEmitter.event().name(answer.status()).data(answer));emitter.complete();
            }
            catch(WorkspaceException ex) {
                try {
                    emitter.send(SseEmitter.event().name("loi").data(new TutorEvent("loi",ex.getMessage())));emitter.complete();
                }
                catch(IOException ignored) {
                    emitter.completeWithError(ignored);
                }
            }
            catch(Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }
}
