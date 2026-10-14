package vn.hoctapcanman.core.tutor.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.*;
import vn.hoctapcanman.core.tutor.application.port.*;
import vn.hoctapcanman.core.tutor.application.service.TutorSessionService;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@Repository
public class TutorSessionStoreAdapter implements TutorSessionStore {
    private final JdbcClient db;
    private final TransactionTemplate tx;
    private static final JsonMapper JSON=JsonMapper.builder().build();
    public TutorSessionStoreAdapter(JdbcClient db, PlatformTransactionManager manager) {
        this.db=db; this.tx=new TransactionTemplate(manager);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    private static final String ASSIGNMENT="""
        SELECT a.id,a.student_id,a.class_id,p.code,p.content_version,
        coalesce((SELECT o.level4 FROM mastery_overrides o WHERE o.student_id=a.student_id
            AND o.skill_code=p.skill_code AND o.removed_at IS NULL ORDER BY o.created_at DESC,o.id DESC LIMIT 1),
            (SELECT m.level4 FROM mastery_states m WHERE m.student_id=a.student_id AND m.skill_code=p.skill_code),'NHAN_BIET') learning_level
        FROM assignments a JOIN problems p ON p.id=a.problem_id
        JOIN problem_releases pr ON pr.problem_id=p.id AND pr.class_id=a.class_id
        JOIN enrollments e ON e.class_id=a.class_id AND e.user_id=a.student_id AND e.role_in_class='STUDENT'
        JOIN classes cl ON cl.id=a.class_id
        WHERE a.student_id=:student AND a.status='DA_GIAO' AND a.mode='ON_TAP'
        AND (a.due_at IS NULL OR a.due_at>clock_timestamp()) AND pr.status='DA_PHAT_HANH' AND cl.enabled
        """;
    @Override public Optional<Assignment> assignment(UUID student,UUID id) {
        return db.sql(ASSIGNMENT+" AND a.id=:id").param("student",student).param("id",id).query(TutorSessionStoreAdapter::assignment).optional();
    }
    @Override public Optional<Assignment> assignmentByCode(UUID student,String code) {
        return db.sql(ASSIGNMENT+" AND p.code=:code ORDER BY a.assigned_at DESC LIMIT 1").param("student",student).param("code",code).query(TutorSessionStoreAdapter::assignment).optional();
    }
    private static Assignment assignment(ResultSet r,int n)throws SQLException {
        return new Assignment(r.getObject("id",UUID.class),r.getObject("student_id",UUID.class),r.getObject("class_id",UUID.class),r.getString("code"),r.getInt("content_version"),r.getString("learning_level"));
    }
    @Override public Session open(Assignment a) {
        return Objects.requireNonNull(tx.execute(s -> {
            db.sql("INSERT INTO phien_gia_su(id,hoc_sinh_id,giao_bai_id,lop_id) VALUES(:id,:s,:a,:c) ON CONFLICT(hoc_sinh_id,giao_bai_id) DO NOTHING")
                .param("id",UUID.randomUUID()).param("s",a.studentId()).param("a",a.id()).param("c",a.classId()).update();
            return db.sql("SELECT * FROM phien_gia_su WHERE hoc_sinh_id=:s AND giao_bai_id=:a").param("s",a.studentId()).param("a",a.id()).query(TutorSessionStoreAdapter::session).single();
        }));
    }
    @Override public Optional<Session> find(UUID student,UUID session) {
        return db.sql("SELECT * FROM phien_gia_su WHERE id=:id AND hoc_sinh_id=:s").param("id",session).param("s",student).query(TutorSessionStoreAdapter::session).optional();
    }
    @Override public Session snapshot(UUID id) { return read(id,false); }
    private Session read(UUID id,boolean lock) {
        return db.sql("SELECT * FROM phien_gia_su WHERE id=:id"+(lock?" FOR UPDATE":"")).param("id",id).query(TutorSessionStoreAdapter::session).single();
    }
    private static Session session(ResultSet r,int n)throws SQLException {
        return new Session(r.getObject("id",UUID.class),r.getObject("hoc_sinh_id",UUID.class),r.getObject("giao_bai_id",UUID.class),r.getObject("lop_id",UUID.class),r.getString("tom_tat"),r.getLong("summary_den_luot"),r.getString("context_version"),r.getLong("last_seq"),r.getLong("revision"),r.getInt("token_count"));
    }
    private static Turn turn(ResultSet r,int n)throws SQLException {
        return new Turn(r.getObject("id",UUID.class),r.getObject("phien_id",UUID.class),r.getObject("request_id",UUID.class),r.getLong("thu_tu"),r.getString("ma_buoc"),r.getInt("cap"),r.getString("cau_hoi"),r.getString("tra_loi"),r.getString("trang_thai"),r.getTimestamp("created_at").toInstant(),references(r.getString("su_kien")),turnVersion(r.getString("su_kien")));
    }
    private static String turnVersion(String events) {
        for(var event:JSON.readTree(events))
            if("start".equals(event.path("type").asString()))return event.path("data").path("contextVersion").asString("");
        return "";
    }
    private static List<Reference> references(String events) {
        var refs=new ArrayList<Reference>();
        for(var event:JSON.readTree(events)) {
            if(!"start".equals(event.path("type").asString()))continue;
            for(var ref:event.path("data").path("references"))
                refs.add(new Reference(ref.path("index").asInt(),ref.path("text").asString(),ref.path("latex").asString()));
        }
        return refs;
    }
    @Override public Accepted accept(Session session,Ask request,String question,String version) {
        return accept(session,request,question,version,List.of());
    }
    @Override public Accepted accept(Session session,Ask request,String question,String version,List<Reference> references) {
        return Objects.requireNonNull(tx.execute(s -> {
            Session current=read(session.id(),true);
            var existing=db.sql("SELECT * FROM yeu_cau_gia_su WHERE phien_id=:s AND request_id=:r").param("s",session.id()).param("r",request.requestId()).query(TutorSessionStoreAdapter::turn).optional();
            if(existing.isPresent()) {
                Turn t=existing.get();
                if(!t.step().equals(request.maBuoc()) || t.level()!=request.cap() || !t.question().equals(question)) throw conflict();
                return new Accepted(current,t,false);
            }
            if(db.sql("SELECT count(*) FROM yeu_cau_gia_su WHERE phien_id=:s AND trang_thai='generating'").param("s",session.id()).query(Long.class).single()>0)throw conflict();
            if(!current.contextVersion().equals(version)) {
                db.sql("UPDATE phien_gia_su SET tom_tat='',summary_den_luot=(SELECT coalesce(max(thu_tu),0) FROM yeu_cau_gia_su WHERE phien_id=:s),context_version=:v,revision=revision+1,token_count=0 WHERE id=:s").param("s",session.id()).param("v",version).update();
            }
            UUID id=UUID.randomUUID();
            long ordinal=db.sql("SELECT coalesce(max(thu_tu),0)+1 FROM yeu_cau_gia_su WHERE phien_id=:s").param("s",session.id()).query(Long.class).single();
            db.sql("INSERT INTO yeu_cau_gia_su(id,phien_id,thu_tu,request_id,ma_buoc,cap,cau_hoi) VALUES(:id,:s,:n,:r,:step,:cap,:q)")
                .param("id",id).param("s",session.id()).param("n",ordinal).param("r",request.requestId()).param("step",request.maBuoc()).param("cap",request.cap()).param("q",question).update();
            db.sql("INSERT INTO luot_gia_su(id,phien_id,luot_id,seq,role,content,tokens) VALUES(:id,:s,:t,:n,'user',:q,:tokens)").param("id",UUID.randomUUID()).param("s",session.id()).param("t",id).param("n",ordinal*2-1).param("q",question).param("tokens",HintModel.estimate(question)).update();
            db.sql("UPDATE phien_gia_su SET token_count=token_count+:tokens WHERE id=:s").param("s",session.id()).param("tokens",HintModel.estimate(question)).update();
            append(session.id(),id,"start",Map.of("requestId",request.requestId().toString(),"maBuoc",request.maBuoc(),"cap",request.cap(),"contextVersion",version,"references",references));
            Turn t=db.sql("SELECT * FROM yeu_cau_gia_su WHERE id=:id").param("id",id).query(TutorSessionStoreAdapter::turn).single();
            return new Accepted(read(session.id(),false),t,true);
        }));
    }
    private static WorkspaceException conflict(){return new WorkspaceException(409,"Phiên đã có lượt đang viết hoặc yêu cầu gửi lại khác nội dung.");}
    @Override public List<Turn> turns(UUID session,int offset,int limit) {
        return db.sql("SELECT * FROM yeu_cau_gia_su WHERE phien_id=:s ORDER BY thu_tu DESC LIMIT :limit OFFSET :offset").param("s",session).param("limit",limit).param("offset",offset).query(TutorSessionStoreAdapter::turn).list();
    }
    @Override public List<Turn> memory(UUID session,long after,UUID exclude) {
        return db.sql("SELECT * FROM yeu_cau_gia_su WHERE phien_id=:s AND thu_tu>:after AND id<>:exclude AND trang_thai<>'generating' ORDER BY thu_tu")
            .param("s",session).param("after",after).param("exclude",exclude).query(TutorSessionStoreAdapter::turn).list();
    }
    @Override public Optional<Turn> turn(UUID session,UUID turn) {
        return db.sql("SELECT * FROM yeu_cau_gia_su WHERE phien_id=:s AND id=:t").param("s",session).param("t",turn).query(TutorSessionStoreAdapter::turn).optional();
    }
    @Override public History history(UUID session,int offset,int limit) {
        var read=new TransactionTemplate(Objects.requireNonNull(tx.getTransactionManager()));
        read.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        read.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        read.setReadOnly(true);
        return Objects.requireNonNull(read.execute(s -> new History(snapshot(session),turns(session,offset,limit))));
    }
    @Override public List<Event> events(UUID session,long after) {
        return db.sql("SELECT e.item::text FROM yeu_cau_gia_su t CROSS JOIN LATERAL jsonb_array_elements(t.su_kien) e(item) WHERE t.phien_id=:s AND (e.item->>'seq')::bigint>:after ORDER BY (e.item->>'seq')::bigint")
            .param("s",session).param("after",after).query((r,n)->JSON.readValue(r.getString(1),Event.class)).list();
    }
    @Override public Optional<Event> append(UUID session,UUID turn,String type,Map<String,Object> data) {
        // Called inside accept's transaction too; use the same transaction for seq + log + status.
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) return appendLocked(session,turn,type,data);
        return Objects.requireNonNull(tx.execute(s -> appendLocked(session,turn,type,data)));
    }
    private Optional<Event> appendLocked(UUID session,UUID turn,String type,Map<String,Object> data) {
        Session current=read(session,true);
        var t=db.sql("SELECT * FROM yeu_cau_gia_su WHERE id=:t AND phien_id=:s").param("t",turn).param("s",session).query(TutorSessionStoreAdapter::turn).optional();
        if(t.isEmpty() || !t.get().status().equals("generating")) return Optional.empty();
        if(type.equals("delta")) validateDelta(current,t.get());
        Event e=new Event(2,session,turn,current.lastSeq()+1,type,Map.copyOf(data));
        String status=type.equals("error")?"error":type.equals("done")?Objects.toString(data.get("status"),"completed"):"generating";
        String delta=type.equals("delta")?Objects.toString(data.get("text"),""):"";
        db.sql("UPDATE yeu_cau_gia_su SET su_kien=su_kien||CAST(:event AS jsonb),tra_loi=tra_loi||:delta,trang_thai=:status WHERE id=:t")
            .param("event",JSON.writeValueAsString(List.of(e))).param("delta",delta).param("status",status).param("t",turn).update();
        db.sql("UPDATE phien_gia_su SET last_seq=:seq,revision=revision+1,updated_at=now(),token_count=token_count+:tokens WHERE id=:s")
            .param("seq",e.seq()).param("s",session).param("tokens",HintModel.estimate(t.get().answer()+delta)-HintModel.estimate(t.get().answer())).update();
        if(type.equals("done") || type.equals("error")) {
            db.sql("INSERT INTO luot_gia_su(id,phien_id,luot_id,seq,role,content,tokens) SELECT :id,phien_id,id,thu_tu*2,'assistant',tra_loi,:tokens FROM yeu_cau_gia_su WHERE id=:t")
                .param("id",UUID.randomUUID()).param("t",turn).param("tokens",HintModel.estimate(t.get().answer()+delta)).update();
        }
        return Optional.of(e);
    }
    /** Quyền và phiên bản cùng giao dịch khóa với seq/hủy; không có model/CAS trong giao dịch. */
    private void validateDelta(Session session,Turn turn) {
        var context=db.sql("""
            SELECT p.id,p.code,p.content_version,p.statement_text FROM assignments a
            JOIN problems p ON p.id=a.problem_id JOIN classes c ON c.id=a.class_id
            JOIN enrollments e ON e.class_id=a.class_id AND e.user_id=a.student_id
            JOIN problem_releases pr ON pr.problem_id=p.id AND pr.class_id=a.class_id
            WHERE a.id=:a AND a.student_id=:student AND a.class_id=:cl AND a.status='DA_GIAO' AND a.mode='ON_TAP'
            AND (a.due_at IS NULL OR a.due_at>clock_timestamp()) AND c.enabled
            AND e.role_in_class='STUDENT' AND pr.status='DA_PHAT_HANH'
            FOR SHARE OF a,p,c,e,pr
            """).param("a",session.assignmentId()).param("student",session.studentId()).param("cl",session.classId())
            .query((r,n) -> new TutorContext(r.getObject("id",UUID.class),r.getString("code"),r.getString("statement_text"),
                Integer.toString(r.getInt("content_version")),turn.step(),"")).optional()
            .orElseThrow(() -> new WorkspaceException(409,"Bài này vừa đóng gia sư."));
        var formulas=db.sql("""
            SELECT f.ordinal,f.code,f.title,f.latex,f.statement FROM formulas f JOIN formula_sheets s ON s.id=f.formula_sheet_id
            WHERE s.class_id=:cl AND s.status='KHOA'
            AND s.version=(SELECT max(version) FROM formula_sheets WHERE class_id=:cl AND status='KHOA')
            ORDER BY f.ordinal FOR SHARE OF s,f
            """).param("cl",session.classId()).query((r,n) -> new TutorFormula(r.getInt("ordinal"),r.getString("code"),r.getString("title"),r.getString("latex"),r.getString("statement"))).list();
        var c=new TutorContext(context.problemId(),context.code(),context.statement(),"",turn.step(),"",turn.step(),formulas,List.of());
        var a=new Assignment(session.assignmentId(),session.studentId(),session.classId(),context.code(),Integer.parseInt(context.function()),"NHAN_BIET");
        if(!TutorSessionService.version(a,c).equals(session.contextVersion()))throw new WorkspaceException(409,"Đề hoặc bảng công thức vừa đổi. Em hỏi lại nhé.");
    }
    @Override public void compact(Session expected,String summary,long through,int tokens) {
        tx.executeWithoutResult(s -> db.sql("UPDATE phien_gia_su SET tom_tat=:text,summary_den_luot=:n,revision=revision+1,token_count=:tokens,updated_at=now() WHERE id=:id AND revision=:revision")
            .param("text",summary).param("n",through).param("tokens",tokens).param("id",expected.id()).param("revision",expected.revision()).update());
    }
    @Override public void usage(UUID student,UUID cl,UUID turn,String provider,String purpose,ModelUsage usage) {
        tx.executeWithoutResult(s -> db.sql("INSERT INTO gia_su_usage(id,hoc_sinh_id,lop_id,luot_id,provider,purpose,input_tokens,output_tokens,estimated) VALUES(:id,:s,:c,:t,:p,:purpose,:input,:output,:estimated)")
            .param("id",UUID.randomUUID()).param("s",student).param("c",cl).param("t",turn).param("p",provider).param("purpose",purpose).param("input",usage.inputTokens()).param("output",usage.outputTokens()).param("estimated",usage.estimated()).update());
    }
    @Override public List<Usage> usage(UUID cl) {
        return db.sql("SELECT hoc_sinh_id,count(*) requests,sum(input_tokens) input,sum(output_tokens) output,sum(CASE WHEN estimated THEN input_tokens+output_tokens ELSE 0 END) estimate FROM gia_su_usage WHERE lop_id=:cl GROUP BY hoc_sinh_id ORDER BY hoc_sinh_id")
            .param("cl",cl).query((r,n)->new Usage(r.getObject("hoc_sinh_id",UUID.class),r.getLong("requests"),r.getLong("input"),r.getLong("output"),r.getLong("estimate"))).list();
    }
    @Override public void expire() {
        var expired=db.sql("SELECT id,phien_id FROM yeu_cau_gia_su WHERE trang_thai='generating' AND lease_until<clock_timestamp()")
            .query((r,n)->new UUID[]{r.getObject("phien_id",UUID.class),r.getObject("id",UUID.class)}).list();
        for(var pair:expired)append(pair[0],pair[1],"error",Map.of("code","TURN_EXPIRED","message","Lượt hỏi đã hết thời gian. Em có thể hỏi lại.","retryable",true));
    }
}
