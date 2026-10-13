package vn.hoctapcanman.core.tutor.infrastructure.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.hoctapcanman.core.tutor.application.service.OfflineTutorService;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@RestController
public class OfflineTutorController {
    private final OfflineTutorService tutor;
    public OfflineTutorController(OfflineTutorService tutor) { this.tutor = tutor; }

    @PostMapping(value="/api/hs/gia-su", produces="text/event-stream")
    public SseEmitter ask(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TutorRequest request) {
        UUID student = UUID.fromString(jwt.getSubject());
        UUID cl = tutor.authorize(student, request);
        // Đủ thời gian cho GLM và hai lần thử cổng toán (bản nháp rồi thang), mỗi job có hạn riêng.
        SseEmitter emitter = new SseEmitter(120000L);
        Thread worker = Thread.ofVirtual().unstarted(() -> {
            try {
                TutorEvent answer = tutor.answerAuthorized(student, cl, request, state -> {
                    try { emitter.send(SseEmitter.event().name(state).data(new TutorEvent(state, ""))); }
                    catch (IOException ex) { throw new UncheckedIOException(ex); }
                });
                emitter.send(SseEmitter.event().name(answer.status()).data(answer));
                emitter.complete();
            } catch (UncheckedIOException ex) {
                emitter.completeWithError(ex.getCause());
            } catch (WorkspaceException ex) {
                sendError(emitter, ex.getMessage());
            } catch (Exception ex) {
                sendError(emitter, "Chưa kiểm tra được gợi ý an toàn. Em thử lại hoặc nhờ giáo viên nhé.");
            }
        });
        emitter.onCompletion(worker::interrupt);
        emitter.onTimeout(worker::interrupt);
        emitter.onError(ignored -> worker.interrupt());
        worker.start();
        return emitter;
    }

    private static void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("loi").data(new TutorEvent("loi", message)));
            emitter.complete();
        } catch (IOException ex) { emitter.completeWithError(ex); }
    }
}
