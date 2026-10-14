package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.*;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.port.*;
import vn.hoctapcanman.core.tutor.application.service.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

class TutorSessionServiceTest {
    final UUID student=UUID.randomUUID(),cl=UUID.randomUUID(),assignment=UUID.randomUUID(),problem=UUID.randomUUID();
    final ClassMembership members=mock(ClassMembership.class);
    final WorkspaceStore workspace=mock(WorkspaceStore.class);
    final ContentWorkflow gate=mock(ContentWorkflow.class);
    final HintModel model=mock(HintModel.class,CALLS_REAL_METHODS);
    final MemoryStore store=new MemoryStore(new Assignment(assignment,student,cl,"DH1",1,"THONG_HIEU"));
    final OfflineTutorService pipeline=new OfflineTutorService(workspace,members,gate,model,new TutorRateLimiter(Clock.systemUTC(),100,Duration.ofMinutes(10)));
    final TutorSessionService service=new TutorSessionService(store,pipeline,members,workspace,6000);
    @BeforeEach void setup() {
        when(members.lopHoc(student)).thenReturn(Optional.of(cl));when(model.provider()).thenReturn("GLM");
        when(workspace.settings(cl)).thenReturn(new Settings(cl,false,"glm"));
        when(workspace.tutorContext(eq(student),eq(cl),any())).thenReturn(Optional.of(new TutorContext(problem,"DH1","Đề","x**2","B.DH.TXD","Thang không được dùng")));
        when(gate.sanitize(any(),any(),anyString(),anyString(),anyList())).thenAnswer(i -> Optional.of(i.<String>getArgument(2)));
        when(model.draft(any())).thenReturn(Optional.of("Em cần tìm điều gì ở bước này?"));
    }
    Ask request(UUID id){return new Ask(id,"B.DH.TXD",1,"Em đã thử cách này");}
    Accepted ask(){Session s=service.open(student,assignment);return service.ask(student,s.id(),request(UUID.randomUUID()));}
    Turn terminal(Accepted a) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end){var t=store.turns(a.session().id(),0,1).getFirst();if(!t.status().equals("generating"))return t;Thread.sleep(10);}
        fail("Worker không kết thúc");return null;
    }
    @Test void remembersEarlierCheckedTurnsAndReplayNeverCallsModel() throws Exception {
        var prompts=new CopyOnWriteArrayList<HintPrompt>();when(model.draft(any())).thenAnswer(i -> {prompts.add(i.getArgument(0));return Optional.of("Em cần tìm điều gì?");});
        Accepted first=ask();assertEquals("completed",terminal(first).status());
        var initial=service.events(student,first.session().id(),0);
        assertEquals(List.of("start","delta","done"),initial.stream().map(Event::type).toList());
        assertEquals(List.of(1L,2L,3L),initial.stream().map(Event::seq).toList());
        assertEquals(initial.subList(1,3),service.events(student,first.session().id(),1));
        verify(model,times(1)).draft(any());
        Accepted next=ask();assertEquals(first.session().id(),next.session().id());terminal(next);
        assertTrue(prompts.getLast().user().contains("Gia sư đã kiểm: Em cần tìm điều gì?"));
        assertTrue(prompts.getLast().system().contains("giải thích lựa chọn"));
    }
    @Test void duplicateRequestIsIdempotentAndDifferentPayloadConflicts() throws Exception {
        Accepted first=ask();terminal(first);
        var dup=service.ask(student,first.session().id(),request(first.turn().requestId()));
        assertFalse(dup.created());assertEquals(first.turn().id(),dup.turn().id());verify(model,times(1)).draft(any());
        assertEquals(409,assertThrows(WorkspaceException.class,() -> service.ask(student,first.session().id(),new Ask(first.turn().requestId(),"B.DH.TXD",1,"Nội dung khác"))).status());
    }
    @Test void foreignStudentCannotReadResumeOrCancel() throws Exception {
        Accepted a=ask();terminal(a);UUID other=UUID.randomUUID();
        assertThrows(WorkspaceException.class,() -> service.history(other,a.session().id(),0,50));
        assertThrows(WorkspaceException.class,() -> service.events(other,a.session().id(),0));
        assertThrows(WorkspaceException.class,() -> service.cancel(other,a.session().id(),a.turn().id()));
    }
    @Test void cancellationWinsAgainstUnreleasedSentenceAndIsIdempotent() throws Exception {
        CountDownLatch inGate=new CountDownLatch(1),release=new CountDownLatch(1);
        when(gate.sanitize(any(),any(),anyString(),anyString(),anyList())).thenAnswer(i -> {
            inGate.countDown();release.await(3,TimeUnit.SECONDS);return Optional.of(i.<String>getArgument(2));
        });
        Accepted a=ask();assertTrue(inGate.await(3,TimeUnit.SECONDS));
        CancelAck cancelled=service.cancel(student,a.session().id(),a.turn().id());release.countDown();
        assertEquals("cancelled",terminal(a).status());assertEquals(cancelled.lastSeq(),service.cancel(student,a.session().id(),a.turn().id()).lastSeq());
        assertEquals(List.of("start","done"),store.events(a.session().id(),0).stream().map(Event::type).toList());
    }
    @Test void cancellationAndStreamErrorNeverInterruptPersistenceWrite() throws Exception {
        for(boolean streamError:List.of(false,true)) {
            var writing=new CountDownLatch(1);var release=new CountDownLatch(1);var finished=new CountDownLatch(1);
            var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
            var db=new MemoryStore(store.a) {
                @Override public void usage(UUID student,UUID cl,UUID turn,String provider,String purpose,ModelUsage usage) {
                    writing.countDown();
                    try { release.await(3,TimeUnit.SECONDS); }
                    catch(InterruptedException ex) { interrupted.set(true); }
                    finally { finished.countDown(); }
                    super.usage(student,cl,turn,provider,purpose,usage);
                }
            };
            var svc=new TutorSessionService(db,pipeline,members,workspace,6000);
            var s=svc.open(student,assignment);var accepted=svc.ask(student,s.id(),request(UUID.randomUUID()));
            try {
                assertTrue(writing.await(3,TimeUnit.SECONDS));
                if(streamError)svc.streamError(student,s.id(),accepted.turn().id(),new WorkspaceException(403,"Quyền vừa đổi"));
                else svc.cancel(student,s.id(),accepted.turn().id());
            } finally { release.countDown(); }
            assertTrue(finished.await(3,TimeUnit.SECONDS));
            assertFalse(interrupted.get(),"Không được ngắt thread đang ghi JDBC");
            assertEquals(streamError?"error":"cancelled",svc.history(student,s.id(),0,1).turns().getFirst().status());
        }
    }
    @Test void cancelAckKeepsOldTerminalSeqWhenNewTurnHasStarted() throws Exception {
        Accepted first=ask();terminal(first);long terminalSeq=store.events(first.session().id(),0).getLast().seq();
        Accepted next=ask();terminal(next);
        var ack=service.cancel(student,first.session().id(),first.turn().id());
        assertEquals(terminalSeq,ack.lastSeq());assertEquals("completed",ack.status());assertTrue(store.s.lastSeq()>ack.lastSeq());
    }
    @Test void unavailableModelIsAnErrorEventWithNoTutorText() throws Exception {
        when(model.draft(any())).thenReturn(Optional.empty());Accepted a=ask();Turn t=terminal(a);
        assertEquals("error",t.status());assertEquals("",t.answer());assertEquals(List.of("start","error"),store.events(a.session().id(),0).stream().map(Event::type).toList());assertEquals(1,store.calls.size());
    }
    @Test void unavailableBindingProducesDurableErrorAfterStart() throws Exception {
        when(model.provider()).thenReturn("unavailable");Accepted a=ask();
        assertEquals("error",terminal(a).status());assertEquals(List.of("start","error"),store.events(a.session().id(),0).stream().map(Event::type).toList());
        verify(model,never()).draft(any());assertTrue(store.calls.isEmpty());
    }
    @Test void compactionUsesModelGatesSummaryAndKeepsThreeRecentPairs() throws Exception {
        service.open(student,assignment);
        for(int i=0;i<10;i++)store.seed("Em thử "+"cách ".repeat(250),"Em làm "+"tiếp ".repeat(250));
        var prompts=new CopyOnWriteArrayList<HintPrompt>();when(model.draft(any())).thenAnswer(i -> {
            HintPrompt p=i.getArgument(0);prompts.add(p);
            return Optional.of(p.system().startsWith("Tóm tắt")?summary("Em đang thử cách đã chọn."):"Em cần tìm điều gì?");
        });
        Accepted a=ask();assertEquals("completed",terminal(a).status());
        assertEquals(2,prompts.size());assertTrue(prompts.getFirst().system().startsWith("Tóm tắt"));
        assertFalse(store.s.tomTat().isEmpty());assertEquals(7,store.s.summaryThrough());
        assertFalse(prompts.getLast().user().contains("Lượt 1 —"));assertTrue(prompts.getLast().user().contains("Lượt 8 —"));
        assertEquals(11,store.turns(store.s.id(),0,100).size());assertEquals(2,store.calls.size());
    }
    @Test void compactsAtSeventyPercentIncludingFailedAndCancelledQuestions() throws Exception {
        service.open(student,assignment);
        // ~4,300 token including the system prompt: above 70%, below the old 75%.
        for(int i=0;i<7;i++)store.seed("Em thử "+"cách ".repeat(280),"Em kiểm tra nhé?");
        for(int i=0;i<3;i++)store.seed("Em đã thử "+"cách ".repeat(300),"");
        for(int i=7;i<10;i++) {
            Turn t=store.turns.get(i);
            store.turns.set(i,new Turn(t.id(),t.sessionId(),t.requestId(),t.ordinal(),t.step(),t.level(),t.question(),t.answer(),i==8?"cancelled":"error",t.createdAt()));
        }
        var prompts=new CopyOnWriteArrayList<HintPrompt>();when(model.draft(any())).thenAnswer(i -> {
            HintPrompt p=i.getArgument(0);prompts.add(p);
            return Optional.of(p.system().startsWith("Tóm tắt")?summary("Em đang thử cách đã chọn."):"Em cần tìm điều gì?");
        });
        Accepted a=ask();assertEquals("completed",terminal(a).status());
        History h=service.history(student,a.session().id(),0,50);
        assertEquals(7,h.session().summaryThrough());assertFalse(h.session().tomTat().isBlank());
        assertTrue(prompts.getLast().user().contains("Lượt 8 —"));assertTrue(prompts.getLast().user().contains("Lượt 10 —"));
    }
    @Test void rejectedSummaryIsNeverSavedOrIncludedAsMemory() throws Exception {
        service.open(student,assignment);for(int i=0;i<10;i++)store.seed("Em thử "+"cách ".repeat(250),"Em làm "+"tiếp ".repeat(250));
        when(model.draft(any())).thenAnswer(i -> Optional.of(((HintPrompt)i.getArgument(0)).system().startsWith("Tóm tắt")?summary("Tóm tắt bị chặn."):"Em cần tìm gì?"));
        when(gate.sanitize(any(),any(),eq("Tóm tắt bị chặn."),anyString(),anyList())).thenReturn(Optional.empty());
        Accepted a=ask();terminal(a);assertEquals("",store.s.tomTat());assertEquals(0,store.s.summaryThrough());
    }
    @Test void usageRequiresTeacherClassMembershipOrAdmin() {
        when(workspace.accountRole(student)).thenReturn("TEACHER");service.usage(student,cl);verify(members).kiemGiaoVien(student,cl);
        UUID admin=UUID.randomUUID();when(workspace.accountRole(admin)).thenReturn("ADMIN");service.usage(admin,cl);verify(members,never()).kiemGiaoVien(admin,cl);
    }
    static String summary(String goal) {
        return "{\"mucTieuBuoc\":\""+goal+"\",\"cachDaThu\":\"\",\"loiDaDuocCham\":\"\",\"cauHoiConMo\":\"\",\"goiYDaDung\":\"\",\"dieuDaSua\":\"\",\"luotNguon\":[1,2,3,4,5,6,7]}";
    }
    /** Test double for orchestration only; persistence is tested separately on PostgreSQL. */
    static class MemoryStore implements TutorSessionStore {
        final Assignment a;Session s;final List<Turn> turns=new ArrayList<>();final List<Event> log=new ArrayList<>();final List<ModelUsage> calls=new ArrayList<>();
        MemoryStore(Assignment a){this.a=a;}
        @Override public Optional<Assignment> assignment(UUID student,UUID id){return a.studentId().equals(student)&&a.id().equals(id)?Optional.of(a):Optional.empty();}
        @Override public Optional<Assignment> assignmentByCode(UUID student,String code){return assignment(student,a.id()).filter(x -> x.code().equals(code));}
        @Override public synchronized Session open(Assignment a){if(s==null)s=new Session(UUID.randomUUID(),a.studentId(),a.id(),a.classId(),"",0,"",0,0,0);return s;}
        @Override public synchronized Optional<Session> find(UUID student,UUID id){return s!=null&&s.id().equals(id)&&s.studentId().equals(student)?Optional.of(s):Optional.empty();}
        @Override public synchronized Session snapshot(UUID id){return s;}
        @Override public synchronized Accepted accept(Session expected,Ask q,String question,String version){
            for(var t:turns)if(t.requestId().equals(q.requestId())){if(!t.question().equals(question)||!t.step().equals(q.maBuoc())||t.level()!=q.cap())throw new WorkspaceException(409,"Khác nội dung");return new Accepted(s,t,false);}
            if(turns.stream().anyMatch(t -> t.status().equals("generating")))throw new WorkspaceException(409,"Đang viết");
            // Seed memory establishes its version on the first real request.
            s=new Session(s.id(),s.studentId(),s.assignmentId(),s.classId(),s.tomTat(),s.summaryThrough(),version,s.lastSeq(),s.revision(),s.tokenCount());
            var t=new Turn(UUID.randomUUID(),s.id(),q.requestId(),turns.size()+1,q.maBuoc(),q.cap(),question,"","generating",Instant.now());turns.add(t);
            append(s.id(),t.id(),"start",Map.of("requestId",q.requestId()));return new Accepted(s,t,true);
        }
        synchronized void seed(String q,String reply){turns.add(new Turn(UUID.randomUUID(),s.id(),UUID.randomUUID(),turns.size()+1,"B.DH.TXD",1,q,reply,"completed",Instant.now()));}
        @Override public synchronized List<Turn> turns(UUID id,int offset,int limit){var copy=new ArrayList<>(turns);Collections.reverse(copy);return copy.stream().skip(offset).limit(limit).toList();}
        @Override public synchronized List<Event> events(UUID id,long after){return log.stream().filter(e -> e.seq()>after).toList();}
        @Override public synchronized Optional<Event> append(UUID id,UUID turn,String type,Map<String,Object> data){
            for(int i=0;i<turns.size();i++){var t=turns.get(i);if(!t.id().equals(turn))continue;if(!t.status().equals("generating"))return Optional.empty();
                String status=type.equals("error")?"error":type.equals("done")?data.get("status").toString():"generating";
                String delta=type.equals("delta")?data.get("text").toString():"";
                turns.set(i,new Turn(t.id(),t.sessionId(),t.requestId(),t.ordinal(),t.step(),t.level(),t.question(),t.answer()+delta,status,t.createdAt()));
                var e=new Event(2,id,turn,s.lastSeq()+1,type,data);log.add(e);s=new Session(s.id(),s.studentId(),s.assignmentId(),s.classId(),s.tomTat(),s.summaryThrough(),s.contextVersion(),e.seq(),s.revision()+1,s.tokenCount());return Optional.of(e);
            }return Optional.empty();
        }
        @Override public synchronized void compact(Session expected,String summary,long through,int tokens){if(s.revision()==expected.revision())s=new Session(s.id(),s.studentId(),s.assignmentId(),s.classId(),summary,through,s.contextVersion(),s.lastSeq(),s.revision()+1,tokens);}
        @Override public synchronized void usage(UUID student,UUID cl,UUID turn,String provider,String purpose,ModelUsage usage){calls.add(usage);}
        @Override public List<Usage> usage(UUID cl){return List.of();}
        @Override public void expire(){}
    }
}
