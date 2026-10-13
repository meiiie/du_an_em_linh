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
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
import vn.hoctapcanman.core.workspace.application.port.ContentWorkflow;
import vn.hoctapcanman.core.shared.infrastructure.math.*;
/** Content gate owns protected facts. Tutor receives only the allow/deny decision, never a solution. */
@Component
public class ContentWorkflowAdapter implements ContentWorkflow {
 private static final JsonMapper JSON=JsonMapper.builder().build();
 private final JdbcClient db;private final VerificationRunRepository runs;private final ProblemRepository problems;private final ProblemReleaseRepository releases;private final FormulaSheetRepository sheets;private final DocumentRepository docs;private final SolutionRepository solutions;private final MathServiceClient math;private final Clock clock;
 public ContentWorkflowAdapter(JdbcClient db,VerificationRunRepository runs,ProblemRepository problems,ProblemReleaseRepository releases,FormulaSheetRepository sheets,DocumentRepository docs,SolutionRepository solutions,MathServiceClient math,Clock clock){this.db=db;this.runs=runs;this.problems=problems;this.releases=releases;this.sheets=sheets;this.docs=docs;this.solutions=solutions;this.math=math;this.clock=clock;}
 public void review(UUID actor,UUID id,Review req){
  db.sql("SELECT id FROM classes WHERE id=:id FOR UPDATE").param("id",req.lopId()).query(UUID.class).single();
  VerificationRun run=runs.findById(id).filter(r->r.classId().equals(req.lopId())).orElseThrow(()->new WorkspaceException(404,"Không tìm thấy lượt kiểm trong lớp."));
  db.sql("SELECT id FROM problems WHERE id=:id FOR UPDATE").param("id",run.subjectId()).query(UUID.class).single();
  var p=problems.findById(run.subjectId()).orElseThrow(()->new WorkspaceException(404,"Không tìm thấy bài."));
  boolean latest=runs.findLatest(req.lopId(),SubjectKind.PROBLEM,p.id()).map(r->r.id().equals(id)).orElse(false);
  UUID current=sheets.findCurrent(req.lopId()).map(FormulaSheet::id).orElse(null);
  boolean blocked=db.sql("SELECT count(*) FROM review_blocks WHERE run_id=:id").param("id",id).query(Integer.class).single()>0;
  if(blocked||run.approvalRefusal(latest,p.contentHash(),current).isPresent())throw new WorkspaceException(409,"Lượt kiểm không còn được duyệt; hãy kiểm lại nội dung.");
  if(req.approve())runs.saveApproval(run.approve(actor,req.reason(),latest,p.contentHash(),current,clock.instant()));
  else {db.sql("INSERT INTO review_blocks(run_id,teacher_id,reason,created_at) VALUES(:id,:actor,:reason,:now)").param("id",id).param("actor",actor).param("reason",req.reason()).param("now",java.sql.Timestamp.from(clock.instant())).update();releases.find(req.lopId(),p.id()).ifPresent(r->releases.save(r.backToDraft(clock.instant())));}
 }
 public int checkFormulas(UUID actor,UUID cl){
  db.sql("SELECT id FROM classes WHERE id=:cl FOR UPDATE").param("cl",cl).query(UUID.class).single();
  FormulaSheet draft=sheets.findDraft(cl).orElseThrow(()->new WorkspaceException(400,"Chưa có bảng nháp để kiểm."));
  Map<UUID,DocumentPassage> passages=new LinkedHashMap<>();List<Map<String,Object>> library=new ArrayList<>();
  for(Document doc:docs.findByClass(cl)){if(!doc.usableAsBasis())continue;List<DocumentPassage> ps=docs.findPassages(doc.id());ps.forEach(p->passages.put(p.id(),p));library.add(Map.of("id",doc.id().toString(),"ten",doc.title(),"license_status",doc.licenseStatus(),"doan",ps.stream().map(p->Map.of("id",p.id().toString(),"text",p.text())).toList()));}
  var response=math.call(MathJob.KIEM_DONG_CONG_THUC,Map.of("dong",draft.rows().stream().map(f->Map.of("id",f.code(),"tieu_de",f.title(),"latex",f.latex(),"phat_bieu",f.statement())).toList(),"tai_lieu",library));
  if(!(response instanceof MathResult.Ok ok))throw new WorkspaceException(503,"Chưa kết nối được bộ kiểm. Hãy thử lại.");
  Map<String,FormulaCheck> checks=new HashMap<>();
  for(Object value:list(ok.body().get("dong"))){Map<String,Object> row=map(value);Formula f=draft.rows().stream().filter(x->x.code().equals(row.get("id"))).findFirst().orElse(null);if(f==null)continue;Map<String,Object> t1=map(row.get("tang1")),t2=map(row.get("tang2"));UUID cite=citation(map(t2.get("trich_dan")),passages);List<UUID> extras=new ArrayList<>();boolean invalid=false;for(Object extra:list(t2.get("trich_dan_them"))){UUID e=citation(map(extra),passages);if(e==null)invalid=true;else if(!e.equals(cite)&&!extras.contains(e))extras.add(e);}CheckStatus c2=status(t2.get("trang_thai"));if(c2==CheckStatus.DAT&&(cite==null||invalid)){c2=CheckStatus.KHONG_KIEM_DUOC;cite=null;extras.clear();}FormulaKind kind;try{kind=FormulaKind.valueOf(String.valueOf(row.get("loai")));}catch(IllegalArgumentException ex){kind=FormulaKind.KHONG_BIET;}checks.put(f.code(),FormulaCheck.of(f,kind,status(t1.get("trang_thai")),c2,JSON.writeValueAsString(t1),JSON.writeValueAsString(t2),cite,extras));}
  FormulaSheet checked=draft.withCheckResults(checks);sheets.save(checked);if(!checked.rows().isEmpty()&&checked.rowsNotPassing().isEmpty())sheets.save(checked.lock(actor,clock.instant()));return checks.size();
 }
 public boolean filter(UUID classId,UUID problemId,String draft,String question){
  try {var solution=solutions.findByProblemId(problemId);if(solution.isEmpty())return false;Object facts=JSON.readValue(solution.get().protectedFactsJson(),Object.class);if(!(facts instanceof List<?> f)||f.isEmpty())return false;
   var result=math.call(MathJob.FILTER,Map.of("ban_nhap",draft,"su_kien",facts,"cau_hs",question));
   if(!(result instanceof MathResult.Ok ok)||!Boolean.TRUE.equals(ok.body().get("cho_phep"))||!Boolean.FALSE.equals(ok.body().get("loi")))return false;
   List<Map<String,Object>> formulas=sheets.findCurrent(classId).map(s->s.rows().stream().map(f->Map.<String,Object>of("id",f.code(),"latex",f.latex(),"phat_bieu",f.statement(),"trich_dan",Map.of("tai_lieu",Objects.toString(f.citationPassageId(),"")))).toList()).orElse(List.of());
   String function=problems.findById(problemId).map(p->Objects.requireNonNullElse(p.functionSympy(),"")).orElse("");
   var gate=math.call(MathJob.KIEM_LOI_GIANG,Map.of("cau",draft,"bang_cong_thuc",formulas,"ham",function,"bai_lam_hoc_sinh",List.of()));
   return gate instanceof MathResult.Ok clean&&draft.equals(clean.body().get("cau_sach"))&&Boolean.FALSE.equals(clean.body().get("thay_bang_goi_y"));
  }catch(RuntimeException ex){return false;}
 }
 public String extractPdf(byte[] bytes){var result=math.call(MathJob.EXTRACT,Map.of("pdf_base64",Base64.getEncoder().encodeToString(bytes)));if(result instanceof MathResult.Ok ok&&"OK".equals(ok.body().get("trang_thai"))&&ok.body().get("text") instanceof String text&&!text.isBlank())return text;throw new WorkspaceException(400,"PDF chưa có văn bản đọc được; thử tệp TXT hoặc PDF có chữ.");}
 @SuppressWarnings("unchecked") private static Map<String,Object> map(@Nullable Object o){return o instanceof Map<?,?>?(Map<String,Object>)o:Map.of();}
 private static List<?> list(@Nullable Object o){return o instanceof List<?> l?l:List.of();}
 private static CheckStatus status(@Nullable Object o){if("DAT".equals(o))return CheckStatus.DAT;if("SAI".equals(o))return CheckStatus.SAI;return CheckStatus.KHONG_KIEM_DUOC;}
 private static @Nullable UUID citation(Map<String,Object> c,Map<UUID,DocumentPassage> ps){try{UUID id=UUID.fromString(String.valueOf(c.get("doan")));DocumentPassage p=ps.get(id);String quote=Objects.toString(c.get("trich"),"");return p!=null&&p.documentId().toString().equals(c.get("tai_lieu"))&&!quote.isBlank()&&p.text().startsWith(quote)?id:null;}catch(IllegalArgumentException e){return null;}}
}
