package vn.hoctapcanman.core.practice.infrastructure.persistence;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.shared.infrastructure.ClockConfig;
import vn.hoctapcanman.core.workspace.infrastructure.persistence.*;
import vn.hoctapcanman.core.reminder.infrastructure.persistence.PushStoreAdapter;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
@DataJpaTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class,ClockConfig.class,WorkspaceStoreAdapter.class,ContentDraftStoreAdapter.class,PushStoreAdapter.class,SolutionRevealEventsAdapter.class})
@Testcontainers(disabledWithoutDocker=true)
class ConfirmedDecisionsPersistenceTest {
 @Autowired JdbcClient db;@Autowired WorkspaceStoreAdapter workspace;@Autowired ContentDraftStoreAdapter drafts;@Autowired PushStoreAdapter push;@Autowired SolutionRevealEventsAdapter reveals;
 UUID teacher,student,cl,problem;final Instant now=Instant.now();
 @BeforeEach void setup(){DuLieuPractice.danhMuc(db);teacher=DuLieuPractice.nguoi(db,"TEACHER");student=DuLieuPractice.nguoi(db,"STUDENT");cl=DuLieuPractice.lop(db);DuLieuPractice.ghiDanh(db,cl,teacher,"TEACHER");DuLieuPractice.ghiDanh(db,cl,student,"STUDENT");problem=DuLieuPractice.bai(db,"DECISION");DuLieuPractice.phatHanh(db,cl,problem);}
 @Test void examGateClosesWithoutDeadlineAndOpensExactlyAtDeadline(){workspace.assign(teacher,new Assign(cl,List.of(student),List.of(problem),"KIEM_TRA",null,"Kiểm tra"),List.of(student),now);assertThat(reveals.blocked(student,cl,problem,now)).isTrue();workspace.assign(teacher,new Assign(cl,List.of(student),List.of(problem),"KIEM_TRA",now.plusSeconds(60),"Kiểm tra"),List.of(student),now);assertThat(reveals.blocked(student,cl,problem,now.plusSeconds(59))).isTrue();assertThat(reveals.blocked(student,cl,problem,now.plusSeconds(60))).isFalse();}
 @Test void draftsPersistMeterAndReviewDecisionWithoutStudentRelease(){workspace.document(teacher,cl,"Tài liệu","tu_soan","Quy tắc đạo hàm",now);UUID document=db.sql("SELECT id FROM documents WHERE class_id=:cl").param("cl",cl).query(UUID.class).single();var p=new NewProblem(cl,"AI.1","T12.DH.03","NHAN_BIET","Đề bài","x^2","x**2");drafts.save(teacher,new DraftRequest(cl,document),new GeneratedDraft(p,"Lời giải nháp","GLM",120,70),now);assertThat(drafts.material(UUID.randomUUID(),document)).isEmpty();var queued=drafts.pending(cl).getFirst();UUID id=UUID.fromString(queued.id());assertThat(drafts.lock(cl,id).orElseThrow().solution()).isEqualTo("Lời giải nháp");drafts.decide(teacher,id,new Review(cl,false,"Cần sửa"),now);assertThat(drafts.pending(cl)).isEmpty();assertThat(drafts.lock(cl,id)).isEmpty();}
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Test void meteringCommitsIndependentlyOfDraftTransaction(){
  var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  UUID usageClass=tx.execute(status->DuLieuPractice.lop(db));
  try{drafts.recordUsage(usageClass,"GLM",120,70,now);assertThat(db.sql("SELECT input_tokens FROM ai_usage WHERE class_id=:cl").param("cl",usageClass).query(Integer.class).single()).isEqualTo(120);}
  finally{tx.executeWithoutResult(status->db.sql("DELETE FROM classes WHERE id=:cl").param("cl",usageClass).update());}
 }
 @Test void subscriptionsAreOwnedAndDeliveryDeduplicated(){var sub=new Subscription("https://fcm.googleapis.com/send/synthetic",new Keys("synthetic","synthetic"));push.subscribe(student,sub,now);push.unsubscribe(teacher,sub.endpoint());UUID id=push.targets().getFirst().id();push.mark(id,"reminder",now);push.mark(id,"reminder",now);assertThat(push.delivered(id,"reminder")).isTrue();push.unsubscribe(student,sub.endpoint());assertThat(push.targets()).isEmpty();}
 @Test void choosingSelfStudyCannotOverwriteExamOrDeadline(){String code=db.sql("SELECT code FROM problems WHERE id=:id").param("id",problem).query(String.class).single();workspace.assign(teacher,new Assign(cl,List.of(student),List.of(problem),"KIEM_TRA",now.plusSeconds(3600),"Kiểm tra"),List.of(student),now);assertThatThrownBy(()->workspace.chooseStudy(student,cl,code,now)).isInstanceOf(RuntimeException.class);assertThat(workspace.modes(student,cl)).singleElement().satisfies(m->assertThat(m.status()).isEqualTo("KIEM_TRA"));}
}
