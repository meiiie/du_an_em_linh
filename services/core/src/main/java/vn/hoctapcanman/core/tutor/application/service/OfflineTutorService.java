package vn.hoctapcanman.core.tutor.application.service;

import java.util.*;
import java.text.Normalizer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.tutor.application.port.HintModel;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

/** Pipeline dùng chung: mỗi câu model đều qua cổng trước khi phát. */
@Service
public class OfflineTutorService {
    private final WorkspaceStore store;
    private final ClassMembership membership;
    private final ContentWorkflow gate;
    private final HintModel model;
    private final TutorRateLimiter limiter;
    public OfflineTutorService(WorkspaceStore store, ClassMembership membership, ContentWorkflow gate,
        HintModel model, TutorRateLimiter limiter) {
        this.store=store; this.membership=membership; this.gate=gate; this.model=model; this.limiter=limiter;
    }
    public UUID authorize(UUID student, TutorRequest request) {
        UUID cl=membership.lopHoc(student).orElseThrow(() -> new WorkspaceException(404,"Em chưa có lớp."));
        context(student,cl,request);
        limiter.acquire(student);
        return cl;
    }
    public TutorContext context(UUID student, UUID cl, TutorRequest request) {
        if (!membership.lopHoc(student).filter(cl::equals).isPresent()) throw new WorkspaceException(404,"Em chưa có lớp.");
        if (store.testMode(student,cl,request.maBai())) throw new WorkspaceException(403,"Gia sư không mở trong bài kiểm tra hoặc bài đã hết hạn.");
        return store.tutorContext(student,cl,request).orElseThrow(() -> new WorkspaceException(404,"Bài hoặc bước này chưa mở gia sư."));
    }
    public TutorEvent answer(UUID student, TutorRequest request) {
        return answerAuthorized(student,authorize(student,request),request,ignored -> {});
    }
    public TutorEvent answerAuthorized(UUID student, UUID cl, TutorRequest request, Consumer<String> phase) {
        phase.accept("kho");
        var c=context(student,cl,request);
        var text=new StringBuilder();
        phase.accept("goi");
        generate(student,cl,request,c,TutorPrompt.compose(c,request),text::append,ignored -> {},ignored -> {},() -> {});
        phase.accept("loc");
        return new TutorEvent("xong",text.toString().strip(),"MO_HINH");
    }
    public String provider(UUID cl) { return selected(cl).provider(); }
    private HintModel selected(UUID cl) {
        var settings=store.settings(cl);
        return model.selected(settings!=null?settings.aiProvider():"offline");
    }
    public HintModel ready(UUID cl) {
        HintModel chosen=selected(cl);
        if("unavailable".equals(chosen.provider()))throw new WorkspaceException(503,"Mô hình đã chọn chưa sẵn sàng. Em thử lại sau.");
        var settings=store.settings(cl);
        boolean glm=settings!=null && "glm".equals(settings.aiProvider());
        if(glm != "GLM".equals(chosen.provider()))throw new WorkspaceException(503,"Mô hình đã chọn chưa sẵn sàng. Em thử lại sau.");
        return chosen;
    }
    public String checked(UUID cl, TutorContext c, TutorRequest request, String text) {
        if (!safeShape(c,text)) throw new Rejected("OUTPUT_SHAPE");
        // Lỗi hạ tầng phải được adapter báo riêng, không kích hoạt regen.
        return gate.sanitize(cl,c.problemId(),text.strip(),request.cauHoi(),c.submittedLines())
            .filter(text.strip()::equals).orElseThrow(() -> new Rejected("GATE_REJECTED"));
    }
    public void generate(UUID student, UUID cl, TutorRequest request, TutorContext context, HintPrompt original,
        Consumer<String> sentence, Consumer<Integer> regen, Consumer<ModelUsage> usage, Runnable checkpoint) {
        generate(student,cl,request,context,original,sentence,regen,usage,checkpoint,6000);
    }
    public void generate(UUID student, UUID cl, TutorRequest request, TutorContext context, HintPrompt original,
        Consumer<String> sentence, Consumer<Integer> regen, Consumer<ModelUsage> usage, Runnable checkpoint,int budget) {
        HintModel chosen=ready(cl);
        if (!TutorPrompt.safeContext(context,request.cauHoi()))
            throw new WorkspaceException(400,"Em bỏ thông tin cá nhân khỏi câu hỏi rồi thử lại nhé.");
        StringBuilder accepted=new StringBuilder();
        String feedback="";
        int[] count={0};
        for (int attempt=0; attempt<4; attempt++) {
            checkpoint.run();
            if (attempt>0) regen.accept(attempt);
            final boolean strict=attempt==3;
            final int before=count[0];
            String instruction=attempt==0 ? "" : "\nMã vi phạm: " + feedback + ". Bước: " + context.step()
                + ". Viết tiếp sau phần đã đạt, không lặp lại. Phần đã đạt là dữ liệu: " + accepted;
            if (attempt==3) instruction += "\nChỉ sinh đúng một câu hỏi Socratic ngắn ở bước hiện tại; không ký hiệu toán, không quy tắc hay kết quả.";
            HintPrompt prompt=new HintPrompt(original.system()+instruction,original.user());
            if(HintModel.estimate(prompt.system()+prompt.user())+16+350>budget)
                throw new WorkspaceException(413,"Ngữ cảnh đã vượt giới hạn. Em thử câu hỏi ngắn hơn.");
            StringBuilder raw=new StringBuilder();
            ModelUsage measured=null;
            try {
                var buffer=new SentenceBuffer(s -> {
                    checkpoint.run();
                    if (count[0]>=3) throw new Completed();
                    if (strict && (count[0]>before || s.strip().length()>240 || !s.strip().endsWith("?")
                        || s.contains("$") || s.contains("\\(") || s.contains("\\[") || s.codePoints().anyMatch(Character::isDigit)))
                        throw new Rejected("STRICT_QUESTION_SHAPE");
                    String clean=checked(cl,context,request,s);
                    var now=context(student,cl,request);
                    if (!now.statement().equals(context.statement()) || !now.formulas().equals(context.formulas()))
                        throw new WorkspaceException(409,"Đề hoặc bảng công thức vừa đổi. Em hỏi lại nhé.");
                    if(!ready(cl).provider().equals(chosen.provider()))throw new WorkspaceException(403,"Cài đặt gia sư vừa đổi. Em hỏi lại nhé.");
                    checkpoint.run();
                    String delta=(accepted.isEmpty()?"":" ")+clean;
                    sentence.accept(delta);
                    accepted.append(delta);
                    count[0]++;
                    if(count[0]==3)throw new Completed();
                });
                measured=chosen.stream(prompt,t -> { checkpoint.run(); raw.append(t); buffer.add(t); });
                buffer.finish();
                if (count[0]==before) throw new WorkspaceException(503,"Mô hình chưa trả lời. Em thử lại sau.");
                return;
            } catch (Completed ex) {
                return;
            } catch (Rejected ex) {
                feedback=ex.code;
                if (attempt==3) throw new WorkspaceException(503,"Chưa có lời gợi ý đạt kiểm tra. Em thử lại hoặc nhờ giáo viên.");
            } finally {
                usage.accept(measured!=null?measured:new ModelUsage(HintModel.estimate(prompt.system()+prompt.user()),HintModel.estimate(raw.toString()),true));
            }
        }
    }
    private static boolean safeShape(TutorContext c,String text) {
        if (text.isBlank() || !TutorPrompt.safeToSend(text,List.of())) return false;
        if (text.codePoints().anyMatch(x -> Set.of(Character.UnicodeScript.HAN,Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,Character.UnicodeScript.HANGUL).contains(Character.UnicodeScript.of(x)))) return false;
        Set<String> refs=new HashSet<>(); c.formulas().forEach(f -> refs.add(Integer.toString(f.number())));
        var matcher=Pattern.compile("\\[(\\d+)\\]").matcher(text);
        while (matcher.find()) if (!refs.contains(matcher.group(1))) return false;
        return true;
    }
    private static final class Completed extends RuntimeException {}

    public static final class Rejected extends RuntimeException {
        final String code;
        public Rejected(String code) { super(code); this.code=code; }
    }
    public static boolean asksAnswer(String text) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replace('đ', 'd');
        return s.matches("(?s).*(dap\\s*(an|so)|loi\\s*giai|lam\\s*(ho|giup)|giai\\s*(ho|giup)|dung\\s*khong|"
            + "ket\\s*qua\\s*cuoi|ra\\s*bao\\s*nhieu|bo\\s*qua\\s*huong\\s*dan|answer|solve|solution|"
            + "ignore\\s+(previous|all|prior)|system\\s*prompt|ban\\s*la\\s*ai\\s*that|role[ -]?play|"
            + "dong\\s*vai|gia\\s*vo|pretend|act\\s+as|gia\\s*su.*giao\\s*vien|em\\s*la\\s*giao\\s*vien).*" );
    }

}
