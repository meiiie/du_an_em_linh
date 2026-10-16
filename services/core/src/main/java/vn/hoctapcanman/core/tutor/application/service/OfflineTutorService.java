package vn.hoctapcanman.core.tutor.application.service;

import java.util.*;
import java.text.Normalizer;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.tutor.application.port.HintModel;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.WorkspaceStore;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

/** Gia sư trực tiếp: prompt bảo vệ đáp án, token nội dung được phát nguyên văn. */
@Service
public class OfflineTutorService {
    private final WorkspaceStore store;
    private final ClassMembership membership;
    private final HintModel model;
    private final TutorRateLimiter limiter;
    private final vn.hoctapcanman.core.tutor.application.port.TermFeedback terms;
    public OfflineTutorService(WorkspaceStore store, ClassMembership membership,
        HintModel model, TutorRateLimiter limiter) {
        this(store,membership,model,limiter,(function,expression) -> Optional.empty());
    }
    @org.springframework.beans.factory.annotation.Autowired
    public OfflineTutorService(WorkspaceStore store, ClassMembership membership, HintModel model, TutorRateLimiter limiter,
        vn.hoctapcanman.core.tutor.application.port.TermFeedback terms) {
        this.terms=terms;
        this.store=store; this.membership=membership; this.model=model; this.limiter=limiter;
    }
    public UUID authorize(UUID student, TutorRequest request) {
        UUID cl=membership.lopHoc(student).orElseThrow(() -> new WorkspaceException(404,"Em chưa có lớp."));
        context(student,cl,request);
        limiter.acquire(student);
        return cl;
    }
    /** Ngữ cảnh/quyền đã kiểm ở ask; không đọc lại DB trước khi phát start. */
    public void authorizePrepared(UUID student) { limiter.acquire(student); }
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
        String provider=ready(cl).provider();
        var text=new StringBuilder();
        phase.accept("goi");
        generate(student,cl,request,c,termPrompt(TutorPrompt.compose(c,request),c,request),text::append,ignored -> {},() -> {
            var now=context(student,cl,request);
            if(!now.statement().equals(c.statement()) || !now.formulas().equals(c.formulas()))
                throw new WorkspaceException(409,"Đề hoặc bảng công thức vừa đổi. Em hỏi lại nhé.");
            if(!ready(cl).provider().equals(provider))throw new WorkspaceException(403,"Cài đặt gia sư vừa đổi. Em hỏi lại nhé.");
        });
        return new TutorEvent("xong",text.toString().strip(),"MO_HINH");
    }
    public HintPrompt termPrompt(HintPrompt prompt, TutorContext context, TutorRequest request) {
        return TutorTerms.add(prompt,context,request,terms);
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
    public void generate(UUID student, UUID cl, TutorRequest request, TutorContext context, HintPrompt prompt,
        Consumer<String> delta, Consumer<ModelUsage> usage, Runnable checkpoint) {
        generate(student,cl,request,context,prompt,delta,usage,checkpoint,6000,new HintModel.Cancellation());
    }
    public void generate(UUID student, UUID cl, TutorRequest request, TutorContext context, HintPrompt prompt,
        Consumer<String> delta, Consumer<ModelUsage> usage, Runnable checkpoint,int budget,HintModel.Cancellation cancellation) {
        HintModel chosen=ready(cl);
        if (!TutorPrompt.safeContext(context,request.cauHoi()))
            throw new WorkspaceException(400,"Em bỏ thông tin cá nhân khỏi câu hỏi rồi thử lại nhé.");
        if(HintModel.estimate(prompt.system()+prompt.user())+16+350>budget)
            throw new WorkspaceException(413,"Ngữ cảnh đã vượt giới hạn. Em thử câu hỏi ngắn hơn.");
        checkpoint.run();cancellation.check();
        StringBuilder raw=new StringBuilder();
        ModelUsage measured=null;
        try {
            measured=chosen.stream(prompt,text -> {
                checkpoint.run();cancellation.check();raw.append(text);
                // Chunk nhỏ độc lập câu; không sửa dấu cách, markdown hay delimiter toán.
                for(int from=0;from<text.length();) {
                    int to=Math.min(from+64,text.length());
                    if(to<text.length() && Character.isHighSurrogate(text.charAt(to-1)) && Character.isLowSurrogate(text.charAt(to)))to--;
                    checkpoint.run();cancellation.check();delta.accept(text.substring(from,to));from=to;
                }
            },cancellation);
            checkpoint.run();cancellation.check();
            if(raw.toString().isBlank())throw new WorkspaceException(503,"Mô hình chưa trả lời. Em thử lại sau.");
        } finally {
            usage.accept(measured!=null?measured:new ModelUsage(HintModel.estimate(prompt.system()+prompt.user()),HintModel.estimate(raw.toString()),true));
        }
    }
    public static boolean asksCheck(String text) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replace('đ', 'd');
        return s.matches("(?s).*(kiem\\s*tra|xac\\s*nhan|dung\\s*khong|dung\\s*chua|duoc\\s*chua|check|confirm).*" );
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
