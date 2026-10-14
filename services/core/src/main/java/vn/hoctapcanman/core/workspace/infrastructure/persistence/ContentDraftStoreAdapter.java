package vn.hoctapcanman.core.workspace.infrastructure.persistence;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.workspace.application.port.ContentDraftStore;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
@Component
public class ContentDraftStoreAdapter implements ContentDraftStore {
    private final JdbcClient db;
    private static final JsonMapper JSON=JsonMapper.builder().build();
    public ContentDraftStoreAdapter(JdbcClient db) {this.db=db;}
    @Override public List<Item> authoringSkillCatalog(UUID cl) {
        return db.sql("SELECT sk.code,sk.name FROM skills sk JOIN topics t ON t.code=sk.topic_code JOIN classes c ON c.id=:cl WHERE t.code='DH12' AND t.grade=c.grade AND coalesce(sk.grade,t.grade)=c.grade ORDER BY sk.code")
            .param("cl",cl).query((rs,i)->new Item(rs.getString("code"),rs.getString("name"),"","KY_NANG",Map.of())).list();
    }
    public List<String> authoringSkills(UUID cl) {
        return db.sql("SELECT sk.code FROM skills sk JOIN topics t ON t.code=sk.topic_code JOIN classes c ON c.id=:cl WHERE t.code='DH12' AND t.grade=c.grade AND coalesce(sk.grade,t.grade)=c.grade ORDER BY sk.code")
            .param("cl",cl).query(String.class).list();
    }
    public Optional<String> material(UUID cl,UUID doc) {
        return db.sql("SELECT text_content FROM documents WHERE id=:id AND class_id=:cl AND license_status IN ('tu_soan','duoc_phep','cong_khai')")
            .param("id",doc).param("cl",cl).query(String.class).optional();
    }
    public void save(UUID actor,DraftRequest req,GeneratedDraft draft,Instant now) {
        db.sql("INSERT INTO content_drafts(id,class_id,document_id,author_id,problem,solution,created_at) VALUES(:id,:cl,:doc,:actor,CAST(:problem AS jsonb),:solution,:now)")
            .param("id",UUID.randomUUID()).param("cl",req.lopId()).param("doc",req.documentId()).param("actor",actor)
            .param("problem",JSON.writeValueAsString(draft.problem())).param("solution",draft.solution()).param("now",Timestamp.from(now)).update();
    }
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordUsage(UUID classId,String provider,int input,int output,Instant now) {
        db.sql("INSERT INTO ai_usage(id,class_id,purpose,provider,input_tokens,output_tokens,created_at) VALUES(:id,:cl,'SOAN_NHAP',:provider,:input,:output,:now)")
            .param("id",UUID.randomUUID()).param("cl",classId).param("provider",provider).param("input",input).param("output",output).param("now",Timestamp.from(now)).update();
    }
    public List<Item> pending(UUID cl) {
        return db.sql("SELECT * FROM content_drafts WHERE class_id=:cl AND decision IS NULL ORDER BY created_at DESC")
            .param("cl",cl).query((r,n)->new Item(r.getString("id"),"AI soạn nháp", "Đọc đề và lời giải trước khi duyệt.","KHONG_KIEM_DUOC",
                Map.of("kind","CONTENT_DRAFT","statement",JSON.readValue(r.getString("problem"),NewProblem.class).statement(),"solution",r.getString("solution")))).list();
    }
    public Optional<GeneratedDraft> lock(UUID cl,UUID id) {
        return db.sql("SELECT problem,solution FROM content_drafts WHERE id=:id AND class_id=:cl AND decision IS NULL FOR UPDATE")
            .param("id",id).param("cl",cl).query((r,n)->new GeneratedDraft(JSON.readValue(r.getString("problem"),NewProblem.class),r.getString("solution"),"GLM",0,0)).optional();
    }
    public void decide(UUID actor,UUID id,Review req,Instant now) {
        db.sql("UPDATE content_drafts SET decision=:decision,reviewer_id=:actor,review_reason=:reason,reviewed_at=:now WHERE id=:id")
            .param("decision",req.approve()?"DA_DUYET":"BI_CHAN").param("actor",actor).param("reason",req.reason()).param("now",Timestamp.from(now)).param("id",id).update();
    }
}
