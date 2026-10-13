package vn.hoctapcanman.core.tutor.application.service;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
/** Safe offline ladder. No solution-bearing type can enter the composition stage. */
@Service
public class OfflineTutorService {
 private final WorkspaceStore store;private final ClassMembership membership;private final ContentWorkflow gate;
 public OfflineTutorService(WorkspaceStore store,ClassMembership membership,ContentWorkflow gate){this.store=store;this.membership=membership;this.gate=gate;}
 public UUID authorize(UUID student,TutorRequest request){UUID cl=membership.lopHoc(student).orElseThrow(()->new WorkspaceException(404,"Em chưa có lớp."));if(store.testMode(student,cl,request.maBai()))throw new WorkspaceException(403,"Gia sư không mở trong bài kiểm tra hoặc bài đã hết hạn.");return cl;}
 @Transactional(readOnly=true)
 public TutorEvent answer(UUID student,TutorRequest request){UUID cl=authorize(student,request);TutorContext context=store.tutorContext(student,cl,request).orElseThrow(()->new WorkspaceException(404,"Chưa có gợi ý phù hợp cho bước này. Em có thể nhờ giáo viên."));String draft=asksAnswer(request.cauHoi())?"Mình sẽ giúp em tự làm. Em đang vướng ở đâu trong bước này? Hãy nêu cách em đã thử.":context.hint();if(draft.isBlank()||!gate.filter(cl,context.problemId(),draft,request.cauHoi()))return new TutorEvent("loi","Chưa kiểm tra được gợi ý an toàn. Em thử lại hoặc nhờ giáo viên nhé.");return new TutorEvent("xong",draft);}
 public static boolean asksAnswer(String text){String s=Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('đ','d');return s.matches("(?s).*(dap\\s*an|loi\\s*giai|lam\\s*ho|giai\\s*ho|dung\\s*khong|answer|solve|solution|gia\\s*su.*giao\\s*vien).* ".stripTrailing());}
}
