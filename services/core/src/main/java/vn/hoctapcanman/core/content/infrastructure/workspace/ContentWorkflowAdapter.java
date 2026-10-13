package vn.hoctapcanman.core.content.infrastructure.workspace;

import java.time.Clock;
import java.util.*;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.content.domain.model.*;
import vn.hoctapcanman.core.content.domain.repository.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Review;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.NewProblem;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
import vn.hoctapcanman.core.workspace.application.port.ContentWorkflow;
import vn.hoctapcanman.core.shared.infrastructure.math.*;

/** Content gate owns protected facts. Tutor receives only the allow/deny decision, never a solution. */
@Component public class ContentWorkflowAdapter implements ContentWorkflow {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final JdbcClient db;
    private final VerificationRunRepository runs;
    private final ProblemRepository problems;
    private final ProblemReleaseRepository releases;
    private final FormulaSheetRepository sheets;
    private final DocumentRepository docs;
    private final SolutionRepository solutions;
    private final MathServiceClient math;
    private final Clock clock;
    public ContentWorkflowAdapter(JdbcClient db,VerificationRunRepository runs,ProblemRepository problems,ProblemReleaseRepository releases,FormulaSheetRepository sheets,DocumentRepository docs,SolutionRepository solutions,MathServiceClient math,Clock clock) {
        this.db=db;
        this.runs=runs;
        this.problems=problems;
        this.releases=releases;
        this.sheets=sheets;
        this.docs=docs;
        this.solutions=solutions;
        this.math=math;
        this.clock=clock;
    }

    public void createProblem(UUID actor,NewProblem request) {
        db.sql("SELECT id FROM classes WHERE id=:id FOR UPDATE").param("id",request.lopId()).query(UUID.class).single();
        if(problems.findByCode(request.code()).isPresent())throw new WorkspaceException(409,"Mã bài đã có. Hãy chọn mã khác.");
        if(db.sql("SELECT count(*) FROM skills WHERE code=:code AND topic_code='DH12'").param("code",request.skillCode()).query(Integer.class).single()!=1)throw new WorkspaceException(400,"Chọn kỹ năng của chủ đề đang học.");
        var solved=math.call(MathJob.SOLVE,Map.of("ham",request.function(),"de_bai",request.statement()));
        if(!(solved instanceof MathResult.Ok solvedOk)||!Boolean.TRUE.equals(solvedOk.body().get("dat"))||solvedOk.body().get("bai_lam")==null)throw new WorkspaceException(422,"Bộ kiểm chưa xác minh được hàm này; hãy kiểm tra lại đề.");
        Map<String,Object> body=map(solvedOk.body());
        String hash;
        try {
            hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsString(Map.of("request",request,"solution",body)).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        catch(java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
        UUID id=UUID.randomUUID();
        var now=clock.instant();
        Problem problem=new Problem(id,request.code(),request.skillCode(),List.of(),Level4.valueOf(request.level4()),null,null,0.5,request.statement(),request.latex(),request.function(),Problem.TU_LUAN_5_BUOC,null,"GIAO_VIEN",hash,actor,now,now);
        problems.save(problem);
        solutions.save(new Solution(id,JSON.writeValueAsString(body.get("bai_lam")),JSON.writeValueAsString(body.getOrDefault("su_kien",List.of())),null));
        for(Object block:list(body.get("thang_goi_y"))) {
            Map<String,Object> b=map(block);
            for(Object level:list(b.get("cac_cap"))) {
                Map<String,Object> h=map(level);
                if(h.get("noi_dung") instanceof String text&&!text.isBlank()&&h.get("cap") instanceof Number cap)db.sql("INSERT INTO hint_levels(problem_id,step_code,level,text) VALUES(:id,:step,:level,:text)").param("id",id).param("step",b.get("ma_buoc")).param("level",cap.intValue()).param("text",text).update();
            }
        }
        FormulaSheet sheet=sheets.findCurrent(request.lopId()).orElse(null);
        List<Document> library=docs.findByClass(request.lopId()).stream().filter(Document::usableAsBasis).toList();
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("ham",request.function());
        payload.put("de_bai",request.statement());
        payload.put("bai_lam",body.get("bai_lam"));
        payload.put("thang_goi_y",body.getOrDefault("thang_goi_y",List.of()));
        payload.put("tai_lieu",library.stream().map(d->Map.of("id",d.id().toString(),"ten",d.title(),"text",d.textContent(),"license_status",d.licenseStatus(),"phien_ban",d.version())).toList());
        payload.put("cong_thuc",sheet==null?List.of():sheet.rows().stream().map(f->Map.of("id",f.code(),"latex",f.latex(),"noi_dung",f.statement(),"ten",f.title())).toList());
        var verification=math.call(MathJob.VERIFY,payload);
        List<TierResult> tiers=new ArrayList<>();
        Set<UUID> citations=new LinkedHashSet<>();
        if(verification instanceof MathResult.Ok verified)for(Object value:list(verified.body().get("tang"))) {
            Map<String,Object> tier=map(value);
            if(!(tier.get("tang") instanceof Number number))continue;
            int index=number.intValue();
            if(index<1||index>3||tiers.stream().anyMatch(t->t.tier()==index))continue;
            CheckStatus status=status(tier.get("trang_thai"));
            String reason=Objects.toString(tier.get("ly_do"),"");
            if(index==2) {
                boolean valid=!list(tier.get("trich_dan")).isEmpty();
                for(Object c:list(tier.get("trich_dan"))) {
                    Map<String,Object> citation=map(c);
                    Document doc=library.stream().filter(d->d.id().toString().equals(citation.get("document_id"))).findFirst().orElse(null);
                    String quote=Objects.toString(citation.get("trich"),"");
                    if(doc==null||quote.isBlank()||!(citation.get("vi_tri") instanceof Number offset)) {
                        valid=false;
                        continue;
                    }
                    int from;
                    try {
                        from=doc.textContent().offsetByCodePoints(0,offset.intValue());
                    }
                    catch(IndexOutOfBoundsException ex) {
                        valid=false;
                        continue;
                    }
                    if(!doc.textContent().startsWith(quote,from)) {
                        valid=false;
                        continue;
                    }
                    docs.findPassages(doc.id()).stream().filter(p->p.charStart()<from+quote.length()&&p.charEnd()>from).forEach(p->citations.add(p.id()));
                }
                if(status==CheckStatus.DAT&&(!valid||citations.isEmpty())) {
                    status=CheckStatus.KHONG_KIEM_DUOC;
                    reason="Trích dẫn chưa ánh xạ được vào đoạn tài liệu đã lưu.";
                }
            }
            if(index==3&&sheet==null)status=CheckStatus.KHONG_KIEM_DUOC;
            Object evidence = tier.get("trich_dan") != null ? tier.get("trich_dan") : tier.get("cong_thuc");
            tiers.add(new TierResult(index,status,null,null,null,null,reason,evidence == null ? null : JSON.writeValueAsString(evidence),JSON.writeValueAsString(tier)));
        }
        for(int index=1;index<=3;index++) {
            int n=index;
            if(tiers.stream().noneMatch(t->t.tier()==n))tiers.add(TierResult.of(n,CheckStatus.KHONG_KIEM_DUOC));
        }
        UUID sheetId=sheet==null?null:sheet.id();
        VerificationRun run=VerificationRun.forProblem(request.lopId(),id,hash,problems.findContentVersion(id).orElseThrow(),sheetId,tiers,List.copyOf(citations),clock.instant());
        runs.save(run);
        releases.save(ProblemRelease.draft(request.lopId(),id,now).apply(run,true,hash,sheetId,clock.instant()));
    }

    public void review(UUID actor,UUID id,Review req) {
        db.sql("SELECT id FROM classes WHERE id=:id FOR UPDATE").param("id",req.lopId()).query(UUID.class).single();
        int tutor=db.sql("UPDATE tutor_review_queue SET decision=:decision,reviewer_id=:actor,review_reason=:reason,reviewed_at=:now WHERE id=:id AND class_id=:cl AND decision IS NULL").param("decision",req.approve()?"DA_XEM":"BI_CHAN").param("actor",actor).param("reason",req.reason()).param("now",java.sql.Timestamp.from(clock.instant())).param("id",id).param("cl",req.lopId()).update();
        if(tutor==1)return;
        // Acknowledgement never bypasses either runtime tutor safety gate.
        VerificationRun run=runs.findById(id).filter(r->r.classId().equals(req.lopId())).orElseThrow(()->new WorkspaceException(404,"Không tìm thấy lượt kiểm trong lớp."));
        db.sql("SELECT id FROM problems WHERE id=:id FOR UPDATE").param("id",run.subjectId()).query(UUID.class).single();
        var p=problems.findById(run.subjectId()).orElseThrow(()->new WorkspaceException(404,"Không tìm thấy bài."));
        boolean latest=runs.findLatest(req.lopId(),SubjectKind.PROBLEM,p.id()).map(r->r.id().equals(id)).orElse(false);
        UUID current=sheets.findCurrent(req.lopId()).map(FormulaSheet::id).orElse(null);
        boolean blocked=db.sql("SELECT count(*) FROM review_blocks WHERE run_id=:id").param("id",id).query(Integer.class).single()>0;
        if(blocked||run.approvalRefusal(latest,p.contentHash(),current).isPresent())throw new WorkspaceException(409,"Lượt kiểm không còn được duyệt; hãy kiểm lại nội dung.");
        if(req.approve())runs.saveApproval(run.approve(actor,req.reason(),latest,p.contentHash(),current,clock.instant()));
        else {
            db.sql("INSERT INTO review_blocks(run_id,teacher_id,reason,created_at) VALUES(:id,:actor,:reason,:now)").param("id",id).param("actor",actor).param("reason",req.reason()).param("now",java.sql.Timestamp.from(clock.instant())).update();
            releases.find(req.lopId(),p.id()).ifPresent(r->releases.save(r.backToDraft(clock.instant())));
        }
    }

    public int checkFormulas(UUID actor,UUID cl) {
        db.sql("SELECT id FROM classes WHERE id=:cl FOR UPDATE").param("cl",cl).query(UUID.class).single();
        FormulaSheet draft=sheets.findDraft(cl).orElseThrow(()->new WorkspaceException(400,"Chưa có bảng nháp để kiểm."));
        Map<UUID,DocumentPassage> passages=new LinkedHashMap<>();
        List<Map<String,Object>> library=new ArrayList<>();
        for(Document doc:docs.findByClass(cl)) {
            if(!doc.usableAsBasis())continue;
            List<DocumentPassage> ps=docs.findPassages(doc.id());
            ps.forEach(p->passages.put(p.id(),p));
            library.add(Map.of("id",doc.id().toString(),"ten",doc.title(),"license_status",doc.licenseStatus(),"doan",ps.stream().map(p->Map.of("id",p.id().toString(),"text",p.text())).toList()));
        }
        var response=math.call(MathJob.KIEM_DONG_CONG_THUC,Map.of("dong",draft.rows().stream().map(f->Map.of("id",f.code(),"tieu_de",f.title(),"latex",f.latex(),"phat_bieu",f.statement())).toList(),"tai_lieu",library));
        if(!(response instanceof MathResult.Ok ok))throw new WorkspaceException(503,"Chưa kết nối được bộ kiểm. Hãy thử lại.");
        Map<String,FormulaCheck> checks=new HashMap<>();
        for(Object value:list(ok.body().get("dong"))) {
            Map<String,Object> row=map(value);
            Formula f=draft.rows().stream().filter(x->x.code().equals(row.get("id"))).findFirst().orElse(null);
            if(f==null)continue;
            Map<String,Object> t1=map(row.get("tang1")),t2=map(row.get("tang2"));
            UUID cite=citation(map(t2.get("trich_dan")),passages);
            List<UUID> extras=new ArrayList<>();
            boolean invalid=false;
            for(Object extra:list(t2.get("trich_dan_them"))) {
                UUID e=citation(map(extra),passages);
                if(e==null)invalid=true;
                else if(!e.equals(cite)&&!extras.contains(e))extras.add(e);
            }
            CheckStatus c2=status(t2.get("trang_thai"));
            if(c2==CheckStatus.DAT&&(cite==null||invalid)) {
                c2=CheckStatus.KHONG_KIEM_DUOC;
                cite=null;
                extras.clear();
            }
            FormulaKind kind;
            try {
                kind=FormulaKind.valueOf(String.valueOf(row.get("loai")));
            }
            catch(IllegalArgumentException ex) {
                kind=FormulaKind.KHONG_BIET;
            }
            checks.put(f.code(),FormulaCheck.of(f,kind,status(t1.get("trang_thai")),c2,JSON.writeValueAsString(t1),JSON.writeValueAsString(t2),cite,extras));
        }
        FormulaSheet checked=draft.withCheckResults(checks);
        sheets.save(checked);
        if(!checked.rows().isEmpty()&&checked.rowsNotPassing().isEmpty())sheets.save(checked.lock(actor,clock.instant()));
        return checks.size();
    }

    public boolean filter(UUID classId,UUID problemId,String draft,String question) {
        return filter(classId,problemId,draft,question,List.of());
    }

    public boolean filter(UUID classId,UUID problemId,String draft,String question,List<String> submittedLines) {
        return sanitize(classId,problemId,draft,question,submittedLines).filter(draft::equals).isPresent();
    }

    /** ADR 013 §4: câu bị cổng bỏ được rút khỏi lời gia sư (và vào hàng duyệt); phần còn lại đã kiểm mới được trả. */
    public Optional<String> sanitize(UUID classId,UUID problemId,String draft,String question,List<String> submittedLines) {
        try {
            var solution=solutions.findByProblemId(problemId);
            if(solution.isEmpty())return Optional.empty();
            Object facts=JSON.readValue(solution.get().protectedFactsJson(),Object.class);
            if(!(facts instanceof List<?> protectedFacts)||protectedFacts.isEmpty())return Optional.empty();
            var result=math.call(MathJob.FILTER,Map.of("ban_nhap",draft,"su_kien",facts,"cau_hs",question));
            if(!(result instanceof MathResult.Ok ok)||!Boolean.TRUE.equals(ok.body().get("cho_phep"))||!Boolean.FALSE.equals(ok.body().get("loi")))return Optional.empty();
            List<Map<String,Object>> formulas=sheets.findCurrent(classId).map(s->s.rows().stream().map(f->Map.<String,Object>of("id",f.code(),"latex",f.latex(),"phat_bieu",f.statement(),"trich_dan",Map.of("tai_lieu",Objects.toString(f.citationPassageId(),"")))).toList()).orElse(List.of());
            String function=problems.findById(problemId).map(p->Objects.requireNonNullElse(p.functionSympy(),"")).orElse("");
            var gate=math.call(MathJob.KIEM_LOI_GIANG,Map.of("cau",draft,"bang_cong_thuc",formulas,"ham",function,"bai_lam_hoc_sinh",submittedLines));
            String sach=gate instanceof MathResult.Ok clean&&Boolean.FALSE.equals(clean.body().get("thay_bang_goi_y"))&&clean.body().get("cau_sach") instanceof String c?c.strip():"";
            if(!draft.equals(sach))db.sql("INSERT INTO tutor_review_queue(id,class_id,problem_id,draft,reason,created_at) VALUES(:id,:cl,:problem,:draft,:reason,:now) ON CONFLICT(class_id,problem_id,draft) DO NOTHING").param("id",UUID.randomUUID()).param("cl",classId).param("problem",problemId).param("draft",draft).param("reason","Cổng công thức chưa xác minh được lời gợi ý; câu này chưa gửi cho học sinh.").param("now",java.sql.Timestamp.from(clock.instant())).update();
            return sach.isBlank()?Optional.empty():Optional.of(sach);
        }
        catch(RuntimeException ex) {
            return Optional.empty();
        }
    }

    public String extractPdf(byte[] bytes) {
        var result=math.call(MathJob.EXTRACT,Map.of("pdf_base64",Base64.getEncoder().encodeToString(bytes)));
        if(result instanceof MathResult.Ok ok&&"OK".equals(ok.body().get("trang_thai"))&&ok.body().get("text") instanceof String text&&!text.isBlank())return text;
        throw new WorkspaceException(400,"PDF chưa có văn bản đọc được; thử tệp TXT hoặc PDF có chữ.");
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> map(@Nullable Object o) {
        return o instanceof Map<?,?>?(Map<String,Object>)o:Map.of();
    }

    private static List<?> list(@Nullable Object o) {
        return o instanceof List<?> l?l:List.of();
    }

    private static CheckStatus status(@Nullable Object o) {
        if("DAT".equals(o))return CheckStatus.DAT;
        if("SAI".equals(o))return CheckStatus.SAI;
        return CheckStatus.KHONG_KIEM_DUOC;
    }

    private static @Nullable UUID citation(Map<String,Object> c,Map<UUID,DocumentPassage> ps) {
        try {
            UUID id=UUID.fromString(String.valueOf(c.get("doan")));
            DocumentPassage p=ps.get(id);
            String quote=Objects.toString(c.get("trich"),"");
            return p!=null&&p.documentId().toString().equals(c.get("tai_lieu"))&&!quote.isBlank()&&p.text().startsWith(quote)?id:null;
        }
        catch(IllegalArgumentException e) {
            return null;
        }
    }
}
