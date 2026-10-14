package vn.hoctapcanman.core.tutor.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@DataJpaTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class,TutorSessionStoreAdapter.class})
@Testcontainers(disabledWithoutDocker=true)
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class TutorSessionPersistenceTest {
    @Autowired JdbcClient db;
    @Autowired PlatformTransactionManager manager;
    @Autowired TutorSessionStoreAdapter store;
    UUID student,cl,problem,assignment;
    Session session;
    String version;
    @BeforeEach void fixture() {
        DuLieuPractice.danhMuc(db);student=DuLieuPractice.nguoi(db,"STUDENT");cl=DuLieuPractice.lop(db);DuLieuPractice.ghiDanh(db,cl,student,"STUDENT");
        problem=DuLieuPractice.bai(db,"TUTOR");DuLieuPractice.phatHanh(db,cl,problem);assignment=UUID.randomUUID();
        db.sql("INSERT INTO assignments(id,class_id,problem_id,student_id,status,assigned_at) VALUES(:a,:c,:p,:s,'DA_GIAO',now())")
            .param("a",assignment).param("c",cl).param("p",problem).param("s",student).update();
        var a=store.assignment(student,assignment).orElseThrow();
        session=store.open(a);
        var statement=db.sql("SELECT statement_text FROM problems WHERE id=:id").param("id",problem).query(String.class).single();
        version=vn.hoctapcanman.core.tutor.application.service.TutorSessionService.version(a,
            new vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.TutorContext(problem,a.code(),statement,"","B.DH.TXD",""));
    }
    @AfterEach void clean() {
        db.sql("DELETE FROM classes WHERE id=:id").param("id",cl).update();
        db.sql("DELETE FROM users WHERE id=:id").param("id",student).update();
        db.sql("DELETE FROM problems WHERE id=:id").param("id",problem).update();
    }
    Ask ask(UUID id){return new Ask(id,"B.DH.TXD",1,"Em đã thử gì?");}
    Accepted accept(){var q=ask(UUID.randomUUID());return store.accept(store.snapshot(session.id()),q,q.cauHoi(),version);}
    @Test void ownerAssignmentForeignKeyAndPersistentIdentity() {
        assertEquals(session.id(),store.open(store.assignment(student,assignment).orElseThrow()).id());
        assertTrue(store.find(UUID.randomUUID(),session.id()).isEmpty());
        UUID other=DuLieuPractice.nguoi(db,"STUDENT");
        try{assertThrows(DataIntegrityViolationException.class,() -> db.sql("INSERT INTO phien_gia_su(id,hoc_sinh_id,giao_bai_id,lop_id) VALUES(:id,:s,:a,:c)")
            .param("id",UUID.randomUUID()).param("s",other).param("a",assignment).param("c",cl).update());}
        finally{db.sql("DELETE FROM users WHERE id=:s").param("s",other).update();}
    }
    @Test void requestIdempotencyConflictAndSingleWriter() throws Exception {
        Accepted a=accept();
        assertFalse(store.accept(store.snapshot(session.id()),ask(a.turn().requestId()),a.turn().question(),version).created());
        assertThrows(WorkspaceException.class,() -> store.accept(store.snapshot(session.id()),new Ask(a.turn().requestId(),"B.DH.TXD",2,"Khác"),"Khác",version));
        assertThrows(WorkspaceException.class,this::accept);assertEquals(1,store.events(session.id(),0).size());
    }
    @Test void restartReplaySummaryAndUsageRemainDurable() {
        Accepted a=accept();
        store.append(session.id(),a.turn().id(),"delta",Map.of("text","Em cần tìm điều gì?"));
        store.append(session.id(),a.turn().id(),"done",Map.of("status","completed"));
        var snapshot=store.snapshot(session.id());store.compact(snapshot,"Em đang thử cách đã chọn.",1,7);
        store.usage(student,cl,a.turn().id(),"GLM","reply",new ModelUsage(80,12,false));
        store.usage(student,cl,a.turn().id(),"GLM","summary",new ModelUsage(40,8,true));
        var restarted=new TutorSessionStoreAdapter(db,manager);
        assertEquals("Em đang thử cách đã chọn.",restarted.find(student,session.id()).orElseThrow().tomTat());
        assertEquals(List.of("delta","done"),restarted.events(session.id(),1).stream().map(Event::type).toList());
        assertEquals("Em cần tìm điều gì?",restarted.turns(session.id(),0,50).getFirst().answer());
        assertEquals(2,restarted.usage(cl).getFirst().requests());assertEquals(120,restarted.usage(cl).getFirst().inputTokens());assertEquals(48,restarted.usage(cl).getFirst().estimatedTokens());
        assertEquals(2,db.sql("SELECT count(*) FROM luot_gia_su WHERE phien_id=:s").param("s",session.id()).query(Integer.class).single());
    }
    @Test void tokenCountAndStoredAnswerAreIndependentOfTokenChunkBoundaries() {
        Accepted a=accept();int before=store.snapshot(session.id()).tokenCount();
        for(String token:List.of("E","m"," ","$","x","^","2","$","\n"))
            store.append(session.id(),a.turn().id(),"delta",Map.of("text",token));
        String answer="Em $x^2$\n";
        assertEquals(before+vn.hoctapcanman.core.tutor.application.port.HintModel.estimate(answer),store.snapshot(session.id()).tokenCount());
        store.append(session.id(),a.turn().id(),"done",Map.of("status","cancelled"));
        assertEquals(answer,store.turns(session.id(),0,1).getFirst().answer());
        var replay=store.events(session.id(),1);assertEquals(10,replay.size());
        assertFalse(replay.getLast().data().containsKey("lastSentenceIndex"));
    }
    @Test void cancelAndDeltaRaceHasOnlyOneTerminalAndNoLateDelta() throws Exception {
        Accepted a=accept();CountDownLatch go=new CountDownLatch(1);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var delta=executor.submit(() -> {go.await();return store.append(session.id(),a.turn().id(),"delta",Map.of("text","Em thử gì?"));});
            var cancel=executor.submit(() -> {go.await();return store.append(session.id(),a.turn().id(),"done",Map.of("status","cancelled"));});
            go.countDown();delta.get(3,TimeUnit.SECONDS);cancel.get(3,TimeUnit.SECONDS);
        }
        assertTrue(store.append(session.id(),a.turn().id(),"delta",Map.of("text","Không được phát")).isEmpty());
        assertTrue(store.append(session.id(),a.turn().id(),"done",Map.of("status","cancelled")).isEmpty());
        var events=store.events(session.id(),0);assertEquals("done",events.getLast().type());
        assertEquals(1,events.stream().filter(e -> e.type().equals("done")).count());
        for(int i=0;i<events.size();i++)assertEquals(i+1,events.get(i).seq());
    }
    @Test void permissionOrContentRevokedBeforeCommitNeverPublishesDelta() {
        Accepted a=accept();db.sql("UPDATE assignments SET mode='KIEM_TRA' WHERE id=:id").param("id",assignment).update();
        assertThrows(WorkspaceException.class,() -> store.append(session.id(),a.turn().id(),"delta",Map.of("text","Em thử gì?")));
        assertEquals(List.of("start"),store.events(session.id(),0).stream().map(Event::type).toList());
    }
    @Test void staleSummaryRevisionCannotOverwriteNewState() {
        Accepted a=accept();Session expected=store.snapshot(session.id());
        store.append(session.id(),a.turn().id(),"done",Map.of("status","cancelled"));
        store.compact(expected,"Tóm tắt cũ",1,4);assertEquals("",store.snapshot(session.id()).tomTat());
    }
    @Test void expiredLeaseProducesReplayableErrorAfterRestart() {
        Accepted a=accept();db.sql("UPDATE yeu_cau_gia_su SET lease_until=now()-interval '1 second' WHERE id=:id").param("id",a.turn().id()).update();
        new TutorSessionStoreAdapter(db,manager).expire();
        assertEquals(List.of("start","error"),store.events(session.id(),0).stream().map(Event::type).toList());
        assertEquals("error",store.turns(session.id(),0,1).getFirst().status());
    }
    @Test void formulaSnapshotSurvivesStartReplayAndHistoryAfterContextChange() {
        var references=List.of(new Reference(1,"Lũy thừa — Quy tắc đã khóa","x^n"));
        var q=ask(UUID.randomUUID());
        var a=store.accept(session,q,q.cauHoi(),"1:snapshot",references);
        store.append(session.id(),a.turn().id(),"done",Map.of("status","completed"));
        var next=ask(UUID.randomUUID());
        store.accept(store.snapshot(session.id()),next,next.cauHoi(),"2:snapshot",List.of(new Reference(1,"Bảng mới","x")));
        assertEquals(references,store.turns(session.id(),0,50).getLast().references());
        assertEquals(references,store.history(session.id(),0,50).turns().getLast().references());
        assertEquals("start",store.events(session.id(),0).getFirst().type());
        var snapshot=store.events(session.id(),0).getFirst().data().get("references");
        assertTrue(snapshot.toString().contains("Quy tắc đã khóa"));
    }
    @Test void changedContentInvalidatesSummaryButKeepsOriginalHistory() {
        Accepted a=accept();store.append(session.id(),a.turn().id(),"done",Map.of("status","completed"));
        store.compact(store.snapshot(session.id()),"Tóm tắt đã kiểm",1,5);
        Ask next=ask(UUID.randomUUID());store.accept(store.snapshot(session.id()),next,next.cauHoi(),"2:snapshot");
        assertEquals("",store.snapshot(session.id()).tomTat());assertEquals(1,store.snapshot(session.id()).summaryThrough());assertEquals(2,store.turns(session.id(),0,50).size());
    }
    @Test void currentAssignmentPermissionUsesModeDeadlineAndEnabledClass() {
        db.sql("UPDATE assignments SET mode='KIEM_TRA' WHERE id=:id").param("id",assignment).update();assertTrue(store.assignment(student,assignment).isEmpty());
        db.sql("UPDATE assignments SET mode='ON_TAP',due_at=now()-interval '1 second' WHERE id=:id").param("id",assignment).update();assertTrue(store.assignment(student,assignment).isEmpty());
        db.sql("UPDATE assignments SET due_at=NULL WHERE id=:id").param("id",assignment).update();
        db.sql("UPDATE classes SET enabled=false WHERE id=:id").param("id",cl).update();assertTrue(store.assignment(student,assignment).isEmpty());
    }
}
