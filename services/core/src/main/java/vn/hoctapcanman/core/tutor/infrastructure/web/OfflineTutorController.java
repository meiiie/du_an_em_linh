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
    public SseEmitter ask(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TutorRequest request) {
        UUID student = UUID.fromString(jwt.getSubject());
        var accepted = tutor.startLegacy(student, request);
        // Cùng lượt bền vững và pipeline với API v2.
        SseEmitter emitter = new SseEmitter(120000L);
        Thread worker = Thread.ofVirtual().unstarted(() -> {
            try {
                emitter.send(SseEmitter.event().name("goi").data(new TutorEvent("goi", "")));
                TutorEvent answer = tutor.await(accepted);
                emitter.send(SseEmitter.event().name(answer.status()).data(answer));
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
        return emitter;
    }

    private static void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("loi").data(new TutorEvent("loi", message)));
            emitter.complete();
        } catch (IOException ex) { emitter.completeWithError(ex); }
    }
}
