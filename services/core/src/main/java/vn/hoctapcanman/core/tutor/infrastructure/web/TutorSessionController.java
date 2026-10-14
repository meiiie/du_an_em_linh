package vn.hoctapcanman.core.tutor.infrastructure.web;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.*;
import vn.hoctapcanman.core.tutor.application.service.TutorSessionService;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@RestController
public class TutorSessionController {
    private final TutorSessionService tutor;
    public TutorSessionController(TutorSessionService tutor){this.tutor=tutor;}
    @PostMapping("/api/hs/gia-su/v2/phien")
    public Opened open(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Open request) {
        var session=tutor.open(student(jwt),request.giaoBaiId());
        var history=tutor.history(student(jwt),session.id(),0,1);
        var last=history.turns().stream().findFirst();
        return new Opened(session.id(),history.session().lastSeq(),last.map(Turn::id).orElse(null),last.map(Turn::status).orElse("idle"));
    }
    @GetMapping("/api/hs/gia-su/v2/phien/{id}/luot")
    public History history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {
        return tutor.history(student(jwt),id,offset,limit);
    }
    @PostMapping(value="/api/hs/gia-su/v2/phien/{id}/luot",produces="text/event-stream")
    public ResponseEntity<SseEmitter> ask(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Ask request) {
        var accepted=tutor.ask(student(jwt),id,request);
        long cursor=tutor.cursorForTurn(student(jwt),id,accepted.turn().id());
        return stream(student(jwt),id,cursor,accepted.turn().id());
    }
    @GetMapping(value="/api/hs/gia-su/v2/phien/{id}/su-kien",produces="text/event-stream")
    public ResponseEntity<SseEmitter> resume(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
        @RequestParam(required=false) @Nullable Long lastSeq,
        @RequestHeader(value="Last-Event-ID",required=false) @Nullable String lastEvent) {
        long cursor=cursor(id,lastSeq,lastEvent);
        tutor.events(student(jwt),id,cursor); // kiểm trước khi mở SSE
        return stream(student(jwt),id,cursor,null);
    }
    @PostMapping({"/api/hs/gia-su/v2/phien/{id}/cancel","/api/hs/gia-su/v2/phien/{id}/huy"})
    public CancelAck cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Cancel request) {
        return tutor.cancel(student(jwt),id,request.turnId());
    }
    @GetMapping({"/api/gv/lop/{cl}/gia-su/su-dung","/api/qt/lop/{cl}/gia-su/su-dung"})
    public UsageReport usage(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID cl) {
        return tutor.usageReport(student(jwt),cl);
    }
    @PostMapping(value="/api/hs/gia-su/tra-loi",produces="application/json")
    public TutorEvent answer(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody TutorRequest request) {
        return tutor.answer(student(jwt),request);
    }
    private ResponseEntity<SseEmitter> stream(UUID student,UUID session,long after,@Nullable UUID target) {
        SseEmitter emitter=new SseEmitter(135000L);
        AtomicBoolean connected=new AtomicBoolean(true);
        emitter.onCompletion(() -> connected.set(false));
        emitter.onError(ignored -> connected.set(false));
        emitter.onTimeout(() -> connected.set(false));
        Thread.startVirtualThread(() -> {
            long cursor=after,heartbeat=System.nanoTime();
            try {
                while(connected.get()) {
                    List<Event> events=tutor.events(student,session,cursor);
                    for(Event e:events) {
                        send(emitter,e);
                        cursor=e.seq();
                        if(target!=null && e.turnId().equals(target) && (e.type().equals("done") || e.type().equals("error"))) {emitter.complete();return;}
                    }
                    var h=tutor.history(student,session,0,1);
                    if(h.session().lastSeq()<=cursor && (h.turns().isEmpty() || !h.turns().getFirst().status().equals("generating"))) {emitter.complete();return;}
                    if(System.nanoTime()-heartbeat>=15_000_000_000L) {
                        Map<String,Object> data=new LinkedHashMap<>();data.put("sessionId",session);data.put("lastSeq",cursor);
                        if(!h.turns().isEmpty())data.put("turnId",h.turns().getFirst().id());
                        emitter.send(SseEmitter.event().name("heartbeat").data(data));heartbeat=System.nanoTime();
                    }
                    Thread.sleep(50);
                }
            } catch(IOException ex){emitter.completeWithError(ex);}
            catch(InterruptedException ex){Thread.currentThread().interrupt();emitter.complete();}
            catch(RuntimeException ex) {
                try {
                    var error=tutor.streamError(student,session,target,ex);
                    if(error.isPresent()){send(emitter,error.get());emitter.complete();}
                    else emitter.completeWithError(ex);
                } catch(IOException | RuntimeException closed){emitter.completeWithError(closed);}
            }
        });
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-cache, no-transform").header("X-Accel-Buffering","no").body(emitter);
    }
    private static void send(SseEmitter emitter,Event event)throws IOException {
        Map<String,Object> data=new LinkedHashMap<>(event.data());
        data.put("v",event.v());data.put("sessionId",event.sessionId());data.put("turnId",event.turnId());data.put("seq",event.seq());
        emitter.send(SseEmitter.event().id(event.sessionId()+":"+event.seq()).name(event.type()).data(data));
    }
    static long cursor(UUID session,@Nullable Long query,@Nullable String header) {
        Long parsed=null;
        if(header!=null) {
            try {
                int colon=header.lastIndexOf(':');
                if(colon<0 || !UUID.fromString(header.substring(0,colon)).equals(session))throw new IllegalArgumentException();
                parsed=Long.parseLong(header.substring(colon+1));
            } catch(IllegalArgumentException ex){throw new WorkspaceException(409,"Mốc nối lại chưa hợp lệ.");}
        }
        if(query!=null && parsed!=null && !query.equals(parsed))throw new WorkspaceException(409,"Hai mốc nối lại không khớp.");
        return query!=null?query:parsed!=null?parsed:0;
    }
    private static UUID student(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
