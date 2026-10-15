package vn.hoctapcanman.core.tutor.infrastructure.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.hoctapcanman.core.tutor.application.service.TutorSessionService;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@RestController
public class OfflineTutorController {
    private final TutorSessionService tutor;
    public OfflineTutorController(TutorSessionService tutor) { this.tutor = tutor; }

    @PostMapping(value="/api/hs/gia-su", produces="text/event-stream")
    public org.springframework.http.ResponseEntity<SseEmitter> ask(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TutorRequest request) {
        UUID student = UUID.fromString(jwt.getSubject());
        var accepted = tutor.startLegacy(student, request);
        // Cùng lượt bền vững và pipeline với API v2.
        SseEmitter emitter = new SseEmitter(120000L);
        Thread worker = Thread.ofVirtual().unstarted(() -> {
            try {
                emitter.send(SseEmitter.event().name("goi").data(new TutorEvent("goi", "")));
                long cursor = tutor.cursorForTurn(student, accepted.session().id(), accepted.turn().id());
                var text = new StringBuilder();
                boolean finished = false;
                while (!finished && !Thread.currentThread().isInterrupted()) {
                    for (var event : tutor.events(student, accepted.session().id(), cursor)) {
                        cursor = event.seq();
                        if (!event.turnId().equals(accepted.turn().id())) continue;
                        if (event.type().equals("delta")) {
                            String delta = String.valueOf(event.data().get("text"));
                            text.append(delta);
                            emitter.send(SseEmitter.event().name("delta").data(java.util.Map.of("text", delta)));
                        } else if (event.type().equals("error")) {
                            sendError(emitter, String.valueOf(event.data().get("message")));
                            return;
                        } else if (event.type().equals("done")) {
                            emitter.send(SseEmitter.event().name("xong").data(new TutorEvent("xong", text.toString())));
                            finished = true;
                            break;
                        }
                    }
                    if (!finished) Thread.sleep(50);
                }
                emitter.complete();
            } catch (UncheckedIOException ex) {
                emitter.completeWithError(ex.getCause());
            } catch (WorkspaceException ex) {
                sendError(emitter, ex.getMessage());
            } catch (Exception ex) {
                sendError(emitter, "Chưa thể nhận lời gợi ý. Em thử lại hoặc nhờ giáo viên nhé.");
            }
        });
        // Ngắt kết nối chỉ dừng đọc; model tiếp tục để có thể resume qua phiên v2.
        emitter.onCompletion(worker::interrupt);
        emitter.onTimeout(worker::interrupt);
        emitter.onError(ignored -> worker.interrupt());
        worker.start();
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control", "no-cache, no-transform")
            .header("X-Accel-Buffering", "no").body(emitter);
    }

    private static void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("loi").data(new TutorEvent("loi", message)));
            emitter.complete();
        } catch (IOException ex) { emitter.completeWithError(ex); }
    }
}
