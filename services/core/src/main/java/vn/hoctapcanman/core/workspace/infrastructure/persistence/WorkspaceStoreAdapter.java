package vn.hoctapcanman.core.workspace.infrastructure.persistence;

import java.time.*;
import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.WorkspaceStore;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@Repository public class WorkspaceStoreAdapter implements WorkspaceStore {
    private final JdbcClient db;
    private final Clock clock;
    public WorkspaceStoreAdapter(JdbcClient db,Clock clock) {
        this.db=db;
        this.clock=clock;
    }

    private static Map<String,Object> details(Object... pairs) {
        Map<String,Object> out=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);
        return out;
    }

    private static String str(ResultSet rs,String name)throws SQLException {
        String s=rs.getString(name);
        return s==null?"":s;
    }

    private static String instant(ResultSet rs,String name)throws SQLException {
        var t=rs.getTimestamp(name);
        return t==null?"":t.toInstant().toString();
    }

    private int count(String sql,UUID cl) {
        return db.sql(sql).param("cl",cl).query(Integer.class).single();
    }

    public List<Item> classes(UUID actor,boolean admin) {
        return db.sql("SELECT c.* FROM classes c WHERE :admin OR EXISTS(SELECT 1 FROM enrollments e WHERE e.class_id=c.id AND e.user_id=:actor AND e.role_in_class='TEACHER') ORDER BY c.name") .param("admin",admin).param("actor",actor).query((r,n)->new Item(r.getString("id"),r.getString("name"),r.getString("school_year"),r.getBoolean("enabled")?"MO":"KHOA",details("grade",r.getInt("grade"),"schoolYear",r.getString("school_year"),"enabled",r.getBoolean("enabled")))).list();
    }

    public Dashboard dashboard(UUID cl) {
        Map<String,Long> levels=new LinkedHashMap<>();
        for(String level:List.of("NHAN_BIET","THONG_HIEU","VAN_DUNG","VAN_DUNG_CAO"))levels.put(level,0L);
        for(Item st:students(cl)) {
            Object skills=st.details().get("skills");
            if(skills instanceof List<?> rows)for(Object value:rows)if(value instanceof Map<?,?> row) {
                String level=String.valueOf(row.get("level4"));
                levels.computeIfPresent(level,(key,n)->n+1);
            }
        }
        List<Item> deadlines=upcomingDeadlines(assignments(cl)).stream().limit(10).toList();
        return new Dashboard(count("SELECT count(*) FROM enrollments WHERE class_id=:cl AND role_in_class='STUDENT'",cl),count("SELECT count(DISTINCT student_id) FROM escalations WHERE class_id=:cl AND handled_at IS NULL",cl),reviews(cl).size(),count("SELECT count(*) FROM assignments WHERE class_id=:cl AND status='DA_GIAO'",cl),levels,deadlines);
    }

    /** Lọc hạn đã qua trước khi gom bộ hoặc giới hạn số hàng; so sánh theo thời điểm thật. */
    protected List<Item> upcomingDeadlines(List<Item> rows) {
        Instant now=clock.instant();
        return rows.stream().filter(row -> row.details().get("dueAt") instanceof String due
            && !due.isBlank() && !Instant.parse(due).isBefore(now))
            .sorted(Comparator.comparing(row -> Instant.parse((String)row.details().get("dueAt")))).toList();
    }

    public List<Item> students(UUID cl) {
        return db.sql("SELECT u.id,u.display_name,(SELECT count(*) FROM submissions s WHERE s.student_id=u.id AND s.class_id=:cl AND s.status='DA_NOP') submitted FROM users u JOIN enrollments e ON e.user_id=u.id WHERE e.class_id=:cl AND e.role_in_class='STUDENT' ORDER BY u.display_name") .param("cl",cl).query((r,n)->new Item(r.getString("id"),r.getString("display_name"),"","",details("skills",skills(UUID.fromString(r.getString("id")),cl),"submitted",r.getInt("submitted")))).list();
    }

    private List<Map<String,Object>> skills(UUID student,UUID cl) {
        return db.sql("""
 SELECT sk.code,sk.name,coalesce(o.level4,m.level4,'NHAN_BIET') level4,coalesce(m.mastery,0) mastery,(o.level4 IS NOT NULL) overridden
 FROM skills sk JOIN topics t ON t.code=sk.topic_code JOIN classes c ON c.id=:cl AND c.grade=t.grade
 LEFT JOIN mastery_states m ON m.skill_code=sk.code AND m.student_id=:student
 LEFT JOIN LATERAL(SELECT level4 FROM mastery_overrides WHERE student_id=:student AND skill_code=sk.code AND removed_at IS NULL ORDER BY created_at DESC,id DESC LIMIT 1)o ON true
 WHERE sk.is_core AND coalesce(sk.grade,t.grade)=c.grade ORDER BY sk.code
 """).param("student",student).param("cl",cl).query((r,n)->details("code",r.getString("code"),"name",r.getString("name"),"level4",r.getString("level4"),"level3",switch(r.getString("level4")) {
            case "NHAN_BIET"->"BIET";case "THONG_HIEU"->"HIEU";default->"VAN_DUNG";
        },"mastery",r.getDouble("mastery"),"overridden",r.getBoolean("overridden"))).list();
    }

    public List<Item> history(UUID student,UUID cl) {
        return db.sql("SELECT s.*,p.code FROM submissions s JOIN problems p ON p.id=s.problem_id WHERE s.student_id=:student AND s.class_id=:cl ORDER BY s.started_at DESC LIMIT 100") .param("student",student).param("cl",cl).query((r,n)->new Item(r.getString("id"),r.getString("code"),str(r,"result"),r.getString("status"),details("problemCode",r.getString("code"),"result",str(r,"result"),"submittedAt",instant(r,"submitted_at")))).list();
    }

    public List<Item> bank(UUID cl) {
        int grade = db.sql("SELECT grade FROM classes WHERE id=:cl").param("cl", cl).query(Integer.class).single();
        var rows = db.sql("""
            SELECT p.*,sk.name skill_name,t.grade,
                CASE WHEN EXISTS(SELECT 1 FROM review_blocks b WHERE b.run_id=v.id)
                    THEN 'BI_CHAN' ELSE r.status END release_status,v.overall_status
            FROM problems p JOIN skills sk ON sk.code=p.skill_code JOIN topics t ON t.code=sk.topic_code
            JOIN problem_releases r ON r.problem_id=p.id AND r.class_id=:cl
            LEFT JOIN LATERAL(SELECT id,overall_status FROM verification_runs
                WHERE class_id=r.class_id AND subject_id=p.id AND subject_kind='PROBLEM'
                ORDER BY created_at DESC,id DESC LIMIT 1)v ON true ORDER BY p.code
            """).param("cl",cl).query((r,n)->new Item(r.getString("id"),r.getString("code"),
                r.getString("statement_text"),r.getString("release_status"),
                details("code",r.getString("code"),"skillCode",r.getString("skill_code"),
                    "skillName",r.getString("skill_name"),"grade",r.getInt("grade"),
                    "level4",r.getString("level4"),"gate",str(r,"overall_status"),
                    "latex",r.getString("statement_latex")))).list();
        return bankForGrade(rows, grade);
    }

    static List<Item> bankForGrade(List<Item> rows, int grade) {
        return rows.stream().filter(row -> Integer.valueOf(grade).equals(row.details().get("grade"))).toList();
    }

    public List<Item> assignments(UUID cl) {
        return db.sql("SELECT a.*,p.code,u.display_name FROM assignments a JOIN problems p ON p.id=a.problem_id JOIN users u ON u.id=a.student_id WHERE a.class_id=:cl AND a.status='DA_GIAO' ORDER BY a.assigned_at DESC") .param("cl",cl).query((r,n)->new Item(r.getString("id"),r.getString("code"),r.getString("display_name"),r.getString("mode"),details("studentId",r.getString("student_id"),"problemCode",r.getString("code"),"mode",r.getString("mode"),"dueAt",instant(r,"due_at")))).list();
    }

    public List<Item> modes(UUID student,UUID cl) {
        return db.sql("SELECT p.code,a.mode,a.due_at FROM assignments a JOIN problems p ON p.id=a.problem_id WHERE a.student_id=:student AND a.class_id=:cl AND a.status='DA_GIAO'").param("student",student).param("cl",cl).query((r,n)->new Item(r.getString("code"),r.getString("code"),"",r.getString("mode"),details("dueAt",instant(r,"due_at")))).list();
    }

    public List<Item> recommendations(UUID student,UUID cl) {
        return db.sql("""
 SELECT p.code,p.skill_code,p.level4,(o.id IS NOT NULL) overridden FROM assignments a
 JOIN problems p ON p.id=a.problem_id JOIN problem_releases pr ON pr.problem_id=p.id AND pr.class_id=a.class_id
 LEFT JOIN mastery_states m ON m.student_id=a.student_id AND m.skill_code=p.skill_code
 LEFT JOIN LATERAL(SELECT level4 FROM mastery_overrides WHERE student_id=a.student_id AND skill_code=p.skill_code AND removed_at IS NULL ORDER BY created_at DESC LIMIT 1) mo ON true
 LEFT JOIN next_problem_overrides o ON o.student_id=a.student_id AND o.problem_id=p.id AND o.consumed_at IS NULL
 WHERE a.student_id=:student AND a.class_id=:cl AND a.status='DA_GIAO' AND pr.status='DA_PHAT_HANH'
 AND (a.due_at IS NULL OR a.due_at>now())
 AND NOT EXISTS(SELECT 1 FROM submissions s WHERE s.student_id=a.student_id AND s.problem_id=p.id AND s.class_id=a.class_id AND s.content_version=p.content_version AND s.status='DA_NOP' AND s.result='DAT')
 ORDER BY (o.id IS NOT NULL) DESC,coalesce(m.mastery,0),
 abs(array_position(ARRAY['NHAN_BIET','THONG_HIEU','VAN_DUNG','VAN_DUNG_CAO']::text[],p.level4::text)-least(4,coalesce(array_position(ARRAY['NHAN_BIET','THONG_HIEU','VAN_DUNG','VAN_DUNG_CAO']::text[],coalesce(mo.level4,m.level4)::text),0)+1)),p.code LIMIT 1
 """).param("student",student).param("cl",cl).query((r,n)->new Item(r.getString("code"),r.getString("code"),r.getBoolean("overridden")?"Bài giáo viên chọn cho em":"Luyện kỹ năng cần củng cố, tiến thêm một mức.","",details("level4",r.getString("level4"),"skillCode",r.getString("skill_code")))).list();
    }

    public Library library(UUID cl,boolean teacher) {
        List<Item> formulas=db.sql("""
 SELECT f.*,s.version,s.status sheet_status FROM formulas f JOIN formula_sheets s ON s.id=f.formula_sheet_id
 WHERE s.class_id=:cl AND (:teacher OR (s.status='KHOA' AND s.version=(SELECT max(version) FROM formula_sheets WHERE class_id=:cl AND status='KHOA')))
 ORDER BY s.version DESC,f.ordinal
 """).param("cl",cl).param("teacher",teacher).query((r,n)->new Item(r.getString("id"),r.getString("title"),r.getString("latex"),r.getString("sheet_status"),details("statement",r.getString("statement"),"tier1",str(r,"tier1_status"),"tier2",str(r,"tier2_status"),"version",r.getInt("version")))).list();
        List<Item> docs=db.sql("SELECT * FROM documents WHERE class_id=:cl AND (:teacher OR license_status<>'chua_ro') ORDER BY created_at DESC").param("cl",cl).param("teacher",teacher).query((r,n)->new Item(r.getString("id"),r.getString("title"),r.getString("text_content"),r.getString("license_status"),Map.of())).list();
        return new Library(formulas,docs);
    }

    public List<Item> reviews(UUID cl) {
        List<Item> result=new ArrayList<>(db.sql("""
 SELECT v.id,p.code,v.overall_status FROM verification_runs v JOIN problems p ON p.id=v.subject_id
 WHERE v.class_id=:cl AND v.subject_kind='PROBLEM' AND v.overall_status='KHONG_KIEM_DUOC' AND NOT v.stale
 AND NOT EXISTS(SELECT 1 FROM review_blocks b WHERE b.run_id=v.id)
 AND v.content_hash=p.content_hash AND v.id=(SELECT z.id FROM verification_runs z WHERE z.class_id=v.class_id AND z.subject_id=v.subject_id AND z.subject_kind='PROBLEM' ORDER BY z.created_at DESC,z.id DESC LIMIT 1)
 ORDER BY p.code
 """).param("cl",cl).query((rs,n)->new Item(rs.getString("id"),rs.getString("code"),"Máy chưa xác minh được đủ ba tầng. Cần giáo viên đọc căn cứ.",rs.getString("overall_status"),details("kind","PROBLEM","tiers",reviewEvidence(UUID.fromString(rs.getString("id")))))).list());
        result.addAll(db.sql("SELECT q.*,p.code FROM tutor_review_queue q JOIN problems p ON p.id=q.problem_id WHERE q.class_id=:cl AND q.decision IS NULL ORDER BY q.created_at DESC").param("cl",cl).query((rs,n)->new Item(rs.getString("id"),"Gợi ý · "+rs.getString("code"),rs.getString("reason"),"KHONG_KIEM_DUOC",details("kind","TUTOR","draft",rs.getString("draft")))).list());
        return result;
    }

    private List<Map<String,Object>> reviewEvidence(UUID id) {
        return db.sql("SELECT tier,status,reason,citation FROM verification_tier_results WHERE run_id=:id ORDER BY tier").param("id",id).query((rs,n)->details("tier",rs.getInt("tier"),"status",rs.getString("status"),"reason",str(rs,"reason"),"citation",str(rs,"citation"))).list();
    }

    public void assign(UUID actor,Assign r,List<UUID> students,Instant now) {
        for(UUID student:students)for(UUID problem:r.problemIds().stream().distinct().toList())db.sql("INSERT INTO assignments(id,class_id,problem_id,student_id,status,set_name,due_at,assigned_by,assigned_at,mode) VALUES(:id,:cl,:p,:s,'DA_GIAO',:name,:due,:actor,:now,:mode) ON CONFLICT(class_id,problem_id,student_id) DO UPDATE SET status='DA_GIAO',set_name=excluded.set_name,due_at=excluded.due_at,assigned_by=excluded.assigned_by,assigned_at=excluded.assigned_at,mode=excluded.mode") .param("id",UUID.randomUUID()).param("cl",r.lopId()).param("p",problem).param("s",student).param("name",r.setName()).param("due",r.dueAt()==null?null:java.sql.Timestamp.from(r.dueAt())).param("actor",actor).param("now",java.sql.Timestamp.from(now)).param("mode",r.mode()).update();
        log(actor,"GIAO_BAI",r.lopId().toString(),r.mode(),now);
    }

    public void recommend(UUID actor,NextProblem r,Instant now) {
        db.sql("INSERT INTO assignments(id,class_id,problem_id,student_id,status,set_name,assigned_by,assigned_at,mode) VALUES(:id,:cl,:problem,:student,'DA_GIAO','Bài giáo viên đề xuất',:actor,:now,'ON_TAP') ON CONFLICT(class_id,problem_id,student_id) DO NOTHING").param("id",UUID.randomUUID()).param("cl",r.lopId()).param("problem",r.problemId()).param("student",r.studentId()).param("actor",actor).param("now",java.sql.Timestamp.from(now)).update();
        db.sql("SELECT id FROM users WHERE id=:id FOR UPDATE").param("id",r.studentId()).query(UUID.class).single();
        db.sql("UPDATE next_problem_overrides SET consumed_at=:now WHERE student_id=:student AND consumed_at IS NULL").param("now",java.sql.Timestamp.from(now)).param("student",r.studentId()).update();
        db.sql("INSERT INTO next_problem_overrides(id,student_id,problem_id,reason,teacher_id,created_at) VALUES(:id,:student,:problem,:reason,:actor,:now)").param("id",UUID.randomUUID()).param("student",r.studentId()).param("problem",r.problemId()).param("reason",r.reason()).param("actor",actor).param("now",java.sql.Timestamp.from(now)).update();
        log(actor,"GHI_DE_BAI_KE",r.studentId().toString(),r.reason(),now);
    }

    public void override(UUID actor,MasteryOverride r,Instant now) {
        if(countSkill(r.skillCode())==0)throw new WorkspaceException(400,"Không tìm thấy kỹ năng.");
        db.sql("SELECT id FROM users WHERE id=:id FOR UPDATE").param("id",r.studentId()).query(UUID.class).single();
        db.sql("UPDATE mastery_overrides SET removed_at=:now,removed_by=:actor WHERE student_id=:student AND skill_code=:skill AND removed_at IS NULL").param("now",java.sql.Timestamp.from(now)).param("actor",actor).param("student",r.studentId()).param("skill",r.skillCode()).update();
        db.sql("INSERT INTO mastery_overrides(id,student_id,skill_code,level4,reason,teacher_id,created_at) VALUES(:id,:student,:skill,:level,:reason,:actor,:now)").param("id",UUID.randomUUID()).param("student",r.studentId()).param("skill",r.skillCode()).param("level",r.level4()).param("reason",r.reason()).param("actor",actor).param("now",java.sql.Timestamp.from(now)).update();
        log(actor,"GHI_DE_MUC",r.studentId().toString(),r.reason(),now);
    }

    private int countSkill(String skill) {
        return db.sql("SELECT count(*) FROM skills WHERE code=:skill").param("skill",skill).query(Integer.class).single();
    }

    public void document(UUID actor,UUID cl,String title,String license,String text,Instant now) {
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO documents(id,class_id,title,kind,license_status,text_content,uploaded_by,created_at) VALUES(:id,:cl,:title,'tu_soan',:license,:text,:actor,:now)").param("id",id).param("cl",cl).param("title",title).param("license",license).param("text",text).param("actor",actor).param("now",java.sql.Timestamp.from(now)).update();
        for(int start=0;start<text.length();start+=2000) {
            String chunk=text.substring(start,Math.min(text.length(),start+2000));
            String folded=java.text.Normalizer.normalize(chunk,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('đ','d');
            db.sql("INSERT INTO document_passages(id,document_id,char_start,char_end,text,text_folded) VALUES(:id,:doc,:start,:end,:text,:fold)").param("id",UUID.randomUUID()).param("doc",id).param("start",start).param("end",start+chunk.length()).param("text",chunk).param("fold",folded).update();
        }
        log(actor,"NAP_TAI_LIEU",cl.toString(),title,now);
    }

    public void formula(UUID actor,Formula r,Instant now) {
        db.sql("SELECT id FROM classes WHERE id=:cl FOR UPDATE").param("cl",r.lopId()).query(UUID.class).single();
        UUID sheet=db.sql("SELECT id FROM formula_sheets WHERE class_id=:cl AND status='NHAP'").param("cl",r.lopId()).query(UUID.class).optional().orElseGet(()-> {
            UUID id=UUID.randomUUID();db.sql("INSERT INTO formula_sheets(id,class_id,version,status,created_at) SELECT :id,:cl,coalesce(max(version),0)+1,'NHAP',:now FROM formula_sheets WHERE class_id=:cl").param("id",id).param("cl",r.lopId()).param("now",java.sql.Timestamp.from(now)).update();db.sql("INSERT INTO formulas(id,formula_sheet_id,ordinal,code,skill_code,title,latex,statement) SELECT gen_random_uuid(),:id,f.ordinal,f.code,f.skill_code,f.title,f.latex,f.statement FROM formulas f JOIN formula_sheets s ON s.id=f.formula_sheet_id WHERE s.class_id=:cl AND s.status='KHOA' AND s.version=(SELECT max(version) FROM formula_sheets WHERE class_id=:cl AND status='KHOA')").param("id",id).param("cl",r.lopId()).update();return id;
        });
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO formulas(id,formula_sheet_id,ordinal,code,title,latex,statement) SELECT :id,:sheet,coalesce(max(ordinal),0)+1,:code,:title,:latex,:statement FROM formulas WHERE formula_sheet_id=:sheet").param("id",id).param("sheet",sheet).param("code","f"+id.toString().replace("-","").substring(0,30)).param("title",r.title()).param("latex",r.latex()).param("statement",r.statement()).update();
        log(actor,"THEM_CONG_THUC",r.lopId().toString(),r.title(),now);
    }

    public Settings settings(UUID cl) {
        return db.sql("SELECT reveal_solution_after_submit,ai_provider FROM class_settings WHERE class_id=:cl").param("cl",cl).query((r,n)->new Settings(cl,r.getBoolean(1),r.getString(2))).optional().orElse(new Settings(cl,false,"offline"));
    }

    public void settings(UUID actor,Settings r,Instant now) {
        db.sql("INSERT INTO class_settings(class_id,reveal_solution_after_submit,ai_provider,updated_at,updated_by) VALUES(:cl,:reveal,:provider,:now,:actor) ON CONFLICT(class_id) DO UPDATE SET reveal_solution_after_submit=excluded.reveal_solution_after_submit,ai_provider=excluded.ai_provider,updated_at=excluded.updated_at,updated_by=excluded.updated_by").param("cl",r.lopId()).param("reveal",r.revealSolutionAfterSubmit()).param("provider",r.aiProvider()).param("now",java.sql.Timestamp.from(now)).param("actor",actor).update();
        log(actor,"CAI_DAT_LOP",r.lopId().toString(),"Cập nhật cài lớp",now);
    }

    public List<Item> schedule(UUID student) {
        return db.sql("SELECT * FROM study_sessions WHERE student_id=:student ORDER BY starts_at DESC LIMIT 200").param("student",student).query((r,n)->new Item(r.getString("id"),r.getString("title"),"",r.getBoolean("completed")?"XONG":"CHUA_XONG",details("startsAt",instant(r,"starts_at"),"endsAt",instant(r,"ends_at")))).list();
    }

    public void schedule(UUID student,Schedule r) {
        db.sql("SELECT id FROM users WHERE id=:id FOR UPDATE").param("id", student).query(UUID.class).single();
        int overlap = db.sql("SELECT count(*) FROM study_sessions WHERE student_id=:id AND NOT completed AND starts_at<:end AND ends_at>:start")
            .param("id", student).param("start", java.sql.Timestamp.from(r.startsAt())).param("end", java.sql.Timestamp.from(r.endsAt())).query(Integer.class).single();
        if (overlap > 0) throw new WorkspaceException(409, "Khung giờ này đã có buổi học. Hãy chọn khoảng trống khác.");
        db.sql("INSERT INTO study_sessions(id,student_id,title,starts_at,ends_at) VALUES(:id,:student,:title,:start,:end)").param("id",UUID.randomUUID()).param("student",student).param("title",r.title()).param("start",java.sql.Timestamp.from(r.startsAt())).param("end",java.sql.Timestamp.from(r.endsAt())).update();
    }

    public boolean complete(UUID student,UUID id) {
        return db.sql("UPDATE study_sessions SET completed=true WHERE id=:id AND student_id=:student").param("id",id).param("student",student).update()==1;
    }

    public List<Item> reminders(UUID student,Instant now) {
        List<Item> out=new ArrayList<>(schedule(student).stream().filter(i->i.status().equals("CHUA_XONG")&&!Instant.parse((String)i.details().get("endsAt")).isBefore(now)&&Instant.parse((String)i.details().get("startsAt")).isBefore(now.plus(Duration.ofDays(1)))).toList());
        out.addAll(db.sql("SELECT a.id,p.code,a.due_at,a.mode FROM assignments a JOIN problems p ON p.id=a.problem_id WHERE a.student_id=:student AND a.status='DA_GIAO' AND a.due_at BETWEEN :now AND :until AND NOT EXISTS(SELECT 1 FROM submissions s WHERE s.student_id=a.student_id AND s.problem_id=a.problem_id AND s.class_id=a.class_id AND s.content_version=p.content_version AND s.status='DA_NOP')").param("student",student).param("now",java.sql.Timestamp.from(now)).param("until",java.sql.Timestamp.from(now.plus(Duration.ofDays(1)))).query((r,n)->new Item(r.getString("id"),"Sắp đến hạn: "+r.getString("code"),"",r.getString("mode"),details("dueAt",instant(r,"due_at")))).list());
        return out;
    }

    public String accountRole(UUID actor) {
        return db.sql("SELECT role FROM users WHERE id=:id AND enabled").param("id",actor).query(String.class).optional().orElse("");
    }

    public boolean managesUser(UUID actor,UUID target) {
        return db.sql("SELECT count(*) FROM enrollments a JOIN enrollments t ON t.class_id=a.class_id JOIN users u ON u.id=t.user_id WHERE a.user_id=:actor AND a.role_in_class='TEACHER' AND t.user_id=:target AND u.role IN ('STUDENT','TEACHER')").param("actor",actor).param("target",target).query(Integer.class).single()>0;
    }

    public List<Item> users() {
        return db.sql("SELECT id,display_name,email,role,enabled,synthetic FROM users ORDER BY display_name LIMIT 1000").query((r,n)->new Item(r.getString("id"),r.getString("display_name"),r.getString("email"),r.getString("role"),details("enabled",r.getBoolean("enabled"),"synthetic",r.getBoolean("synthetic")))).list();
    }

    public void createUser(UUID actor,NewUser r,String hash,Instant now) {
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO users(id,email,password_hash,display_name,role,enabled,synthetic,created_at,updated_at) VALUES(:id,:email,:hash,:name,:role,true,true,:now,:now)").param("id",id).param("email",r.email().strip().toLowerCase(Locale.ROOT)).param("hash",hash).param("name",r.displayName()).param("role",r.role()).param("now",java.sql.Timestamp.from(now)).update();
        log(actor,"TAO_TAI_KHOAN",id.toString(),r.role(),now);
    }

    public void bulk(UUID actor,Bulk r,Instant now) {
        for(UUID id:r.ids().stream().distinct().toList()) {
            if(db.sql("UPDATE users SET enabled=:enabled,updated_at=:now WHERE id=:id").param("enabled",r.enabled()).param("now",java.sql.Timestamp.from(now)).param("id",id).update()!=1)
                throw new WorkspaceException(404,"Không tìm thấy tài khoản.");
            if(!r.enabled()) {
                db.sql("UPDATE auth_sessions SET revoked_at=:now WHERE user_id=:id AND revoked_at IS NULL").param("now",java.sql.Timestamp.from(now)).param("id",id).update();
                db.sql("UPDATE refresh_tokens SET revoked_at=:now WHERE user_id=:id AND revoked_at IS NULL").param("now",java.sql.Timestamp.from(now)).param("id",id).update();
            }
            log(actor,r.enabled()?"MO_TAI_KHOAN":"KHOA_TAI_KHOAN",id.toString(),"Thao tác quản trị",now);
        }
    }

    public void createClass(UUID actor,NewClass r,Instant now) {
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO classes(id,name,grade,school_year,created_at) VALUES(:id,:name,:grade,:year,:now)").param("id",id).param("name",r.name()).param("grade",r.grade()).param("year",r.schoolYear()).param("now",java.sql.Timestamp.from(now)).update();
        log(actor,"TAO_LOP",id.toString(),r.name(),now);
    }

    public boolean lockClass(UUID actor,UUID cl,boolean locked,Instant now) {
        int changed=db.sql("UPDATE classes SET enabled=:enabled WHERE id=:id")
            .param("enabled",!locked).param("id",cl).update();
        if(changed==1)log(actor,locked?"KHOA_LOP":"MO_LOP",cl.toString(),"Thao tác quản trị",now);
        return changed==1;
    }

    public void enroll(UUID actor,Enrollment r,Instant now) {
        String role=db.sql("SELECT role FROM users WHERE id=:id AND enabled").param("id",r.userId()).query(String.class).optional().orElse("");
        if(!(r.role().equals(role)||("TEACHER".equals(r.role())&&"SCHOOL_ADMIN".equals(role))))throw new WorkspaceException(400,"Vai trò tài khoản không khớp với phân công.");
        db.sql("INSERT INTO enrollments(class_id,user_id,role_in_class,enrolled_at) VALUES(:cl,:user,:role,:now) ON CONFLICT(class_id,user_id) DO NOTHING").param("cl",r.lopId()).param("user",r.userId()).param("role",r.role()).param("now",java.sql.Timestamp.from(now)).update();
        log(actor,"PHAN_CONG",r.lopId().toString(),r.role()+" "+r.userId(),now);
    }

    public List<Item> audit() {
        return db.sql("SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 200").query((r,n)->new Item(r.getString("id"),r.getString("action"),r.getString("reason"),r.getString("target"),details("actorId",str(r,"actor_id"),"createdAt",instant(r,"created_at")))).list();
    }

    public void log(UUID actor,String action,String target,String reason,Instant now) {
        db.sql("INSERT INTO audit_log(id,actor_id,action,target,reason,created_at) VALUES(:id,:actor,:action,:target,:reason,:now)").param("id",UUID.randomUUID()).param("actor",actor).param("action",action).param("target",target).param("reason",reason).param("now",java.sql.Timestamp.from(now)).update();
    }

    public boolean testMode(UUID student,UUID cl,String code) {
        return db.sql("""
            SELECT count(*) FROM assignments a JOIN problems p ON p.id=a.problem_id
            WHERE a.student_id=:student AND a.class_id=:cl AND a.status='DA_GIAO'
            AND ((p.code=:code AND a.due_at<=now()) OR
                (a.mode='KIEM_TRA' AND p.code=:code AND (a.due_at IS NULL OR a.due_at>now())))
            """).param("student",student).param("cl",cl).param("code",code).query(Integer.class).single()>0;
    }

    public Optional<TutorContext> tutorContext(UUID student,UUID cl,TutorRequest request) {
        return db.sql("""
 SELECT p.id,p.code,p.statement_text,p.function_sympy,h.text FROM assignments a
 JOIN problems p ON p.id=a.problem_id JOIN problem_releases pr ON pr.problem_id=p.id AND pr.class_id=a.class_id
 JOIN hint_levels h ON h.problem_id=p.id AND h.step_code=:step AND h.level=:level
 WHERE a.student_id=:student AND a.class_id=:cl AND p.code=:code AND a.status='DA_GIAO' AND a.mode='ON_TAP' AND pr.status='DA_PHAT_HANH'
 AND (a.due_at IS NULL OR a.due_at>now())
 LIMIT 1 FOR SHARE OF a
 """).param("student",student).param("cl",cl).param("code",request.maBai()).param("step",request.maBuoc()).param("level",request.cap()).query((r,n)->new TutorContext(UUID.fromString(r.getString("id")),r.getString("code"),r.getString("statement_text"),str(r,"function_sympy"),request.maBuoc(),r.getString("text"))).optional();
    }
}
