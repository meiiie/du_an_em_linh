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

/** Pipeline dùng chung: chỉ lời thuần không toán/đáp án được bỏ qua cổng. */
@Service
public class OfflineTutorService {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(OfflineTutorService.class);
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
        checkShape(c,text);
        // Lỗi hạ tầng phải được adapter báo riêng, không kích hoạt regen.
        return gate.sanitize(cl,c.problemId(),text.strip(),request.cauHoi(),c.submittedLines())
            .filter(text.strip()::equals).orElseThrow(() -> new Rejected(gateViolation(text)));
    }
    public void generate(UUID student, UUID cl, TutorRequest request, TutorContext context, HintPrompt original,
        Consumer<String> sentence, Consumer<Integer> regen, Consumer<ModelUsage> usage, Runnable checkpoint) {
        generate(student,cl,request,context,original,sentence,regen,usage,checkpoint,6000);
    }
    public void generate(UUID student, UUID cl, TutorRequest request, TutorContext context, HintPrompt original,
        Consumer<String> sentence, Consumer<Integer> regen, Consumer<ModelUsage> usage, Runnable checkpoint,int budget) {
        generate(student,cl,request,context,original,sentence,regen,usage,checkpoint,budget,new HintModel.Cancellation());
    }
    public void generate(UUID student, UUID cl, TutorRequest request, TutorContext context, HintPrompt original,
        Consumer<String> sentence, Consumer<Integer> regen, Consumer<ModelUsage> usage, Runnable checkpoint,int budget,HintModel.Cancellation cancellation) {
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
            HintPrompt prompt=strict?TutorPrompt.strictQuestion(context):new HintPrompt(original.system()+instruction,original.user());
            if(HintModel.estimate(prompt.system()+prompt.user())+16+350>budget)
                throw new WorkspaceException(413,"Ngữ cảnh đã vượt giới hạn. Em thử câu hỏi ngắn hơn.");
            StringBuilder raw=new StringBuilder();
            ModelUsage measured=null;
            try {
                var buffer=new SentenceBuffer(s -> {
                    checkpoint.run();
                    if (count[0]>=3) throw new Completed();
                    if (strict && (count[0]>before || s.strip().length()>160 || !s.strip().endsWith("?")
                        || s.contains("$") || s.contains("\\(") || s.contains("\\[") || s.codePoints().anyMatch(Character::isDigit)))
                        throw new Rejected("STRICT_QUESTION_SHAPE");
                    checkShape(context,s);
                    String clean=!strict && plainSentence(s)?s.strip():checked(cl,context,request,s);
                    var now=context(student,cl,request);
                    if (!now.statement().equals(context.statement()) || !now.formulas().equals(context.formulas()))
                        throw new WorkspaceException(409,"Đề hoặc bảng công thức vừa đổi. Em hỏi lại nhé.");
                    if(!ready(cl).provider().equals(chosen.provider()))throw new WorkspaceException(403,"Cài đặt gia sư vừa đổi. Em hỏi lại nhé.");
                    checkpoint.run();
                    // Lần viết lại hay lặp câu mở đầu đã phát; bỏ câu trùng nguyên văn thay vì phát hai lần.
                    if(!clean.isBlank() && accepted.toString().contains(clean.strip()))return;
                    String delta=(accepted.isEmpty()?"":" ")+clean;
                    sentence.accept(delta);
                    accepted.append(delta);
                    count[0]++;
                    if(strict || count[0]==3)throw new Completed();
                });
                measured=chosen.stream(prompt,t -> { checkpoint.run(); raw.append(t); buffer.add(t); },cancellation);
                buffer.finish();
                if (count[0]==before) throw new WorkspaceException(503,"Mô hình chưa trả lời. Em thử lại sau.");
                return;
            } catch (Completed ex) {
                return;
            } catch (Rejected ex) {
                feedback=ex.code+" — "+repair(ex.code);
                // Chỉ mã phân loại/chỉ số thử, không ghi câu bị bỏ hay dữ kiện bảo vệ.
                LOG.info("Gia sư chưa đạt: ma={}, buoc={}, lan={}",ex.code,context.step(),attempt+1);
                if (attempt==3) throw new WorkspaceException(503,"Chưa có lời gợi ý đạt kiểm tra. Em thử lại hoặc nhờ giáo viên.");
            } finally {
                usage.accept(measured!=null?measured:new ModelUsage(HintModel.estimate(prompt.system()+prompt.user()),HintModel.estimate(raw.toString()),true));
            }
        }
    }
    private static void checkShape(TutorContext c,String text) {
        if (text.isBlank()) throw new Rejected("EMPTY_SENTENCE");
        if (!TutorPrompt.safeToSend(text,List.of())) throw new Rejected("OUTPUT_IDENTITY");
        if (text.codePoints().anyMatch(x -> Set.of(Character.UnicodeScript.HAN,Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,Character.UnicodeScript.HANGUL).contains(Character.UnicodeScript.of(x)))) throw new Rejected("OUTPUT_LANGUAGE");
        Set<String> refs=new HashSet<>(); c.formulas().forEach(f -> refs.add(Integer.toString(f.number())));
        var matcher=Pattern.compile("\\[(\\d+)\\]").matcher(text);
        while (matcher.find()) if (!refs.contains(matcher.group(1))) throw new Rejected("UNKNOWN_CITATION");
    }
    /** Chỉ prose Latin với dấu câu thường; nghi toán/đáp án bất kỳ thì chuyển cổng hiện có. */
    private static boolean plainSentence(String text) {
        String normalized=Normalizer.normalize(text,Normalizer.Form.NFKC);
        if(normalized.isBlank())return false;
        // Kiểm cả bản gốc: NFKC không được biến số La Mã/ký hiệu thành chữ rồi cho đi tắt.
        for(String candidate:List.of(Normalizer.normalize(text,Normalizer.Form.NFC),normalized))
            if(!candidate.codePoints().allMatch(cp ->
                Character.isLetter(cp) && Character.UnicodeScript.of(cp)==Character.UnicodeScript.LATIN
                || Character.isWhitespace(cp) || ",.!?".indexOf(cp)>=0))return false;
        if(LATIN_VARIABLE.matcher(normalized).find())return false;
        String words=Normalizer.normalize(normalized,Normalizer.Form.NFD).replaceAll("\\p{M}","")
            .toLowerCase(Locale.ROOT).replace('đ','d').replaceAll("[^a-z]+"," ").strip();
        if(ANSWER_WORDS.matcher(words).find())return false;
        // Blacklist không bao phủ được mọi cách xác nhận/diễn đạt đáp án bằng lời.
        // Đây chỉ là tập nhận dạng để bỏ cổng, không phải nguồn sinh câu trả lời.
        return NEUTRAL_SENTENCES.contains(Normalizer.normalize(text,Normalizer.Form.NFC)
            .strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT));
    }
    private static final Set<String> NEUTRAL_SENTENCES=Set.of(
        "mình cùng em làm rõ bước đang vướng nhé.", "em cứ bình tĩnh rồi thử lại nhé.",
        "em đã thử gì?", "em đã thử cách nào?", "em cần tìm điều gì?", "em cần tìm gì?",
        "em cần tìm gì tiếp theo?", "em muốn kiểm tra điều gì tiếp?", "em định kiểm tra điều gì?",
        "em sẽ chọn cách làm nào?", "em sẽ kiểm tra điều kiện nào?", "em đang tìm điều gì?",
        "mình có thể cùng em chọn bước cần làm, em đã thử gì?");
    private static final Pattern LATIN_VARIABLE=Pattern.compile("(?<![\\p{L}\\p{M}\\p{N}_])[\\p{IsLatin}&&\\p{L}](?![\\p{L}\\p{M}\\p{N}_])");
    private static final Pattern ANSWER_WORDS=Pattern.compile("\\b(?:dong bien|nghich bien|cuc dai|cuc tieu|cuc tri|nghiem|bang|duong|am|tang|giam|"
        + "lon hon|nho hon|khoang|gia tri|ket qua|dap an|dap so|tap xac dinh|dao ham la|"
        + "la|neu|thi|suy ra|doi dau|nhan|chia|cong|tru|binh phuong|hang so|vo cuc|"
        + "mot|hai|ba|bon|nam|sau|bay|tam|chin|muoi|tram|nghin|trieu|"
        + "increasing|decreasing|positive|negative|equals|roots?|solutions?|answers?)\\b");

    /** Phân loại hình thức câu đã bị cổng từ chối; không thay phán quyết của CAS. */
    private static String gateViolation(String text) {
        if(Pattern.compile("[$=≠≤≥^]|\\\\[a-zA-Z(\\[]|[a-zA-Z][′']").matcher(text).find())return "GATE_REJECTED_MATH";
        if(text.replaceAll("\\[\\d+\\]", "").codePoints().anyMatch(Character::isDigit))return "GATE_REJECTED_NUMBER";
        return "GATE_REJECTED_STATEMENT";
    }
    private static String repair(String code) {
        return switch(code) {
            case "GATE_REJECTED_MATH" -> "Bỏ biểu thức bị từ chối; không tính hay biến đổi đề. Chỉ hỏi em chọn thao tác nào, hoặc dẫn [n] có trong bảng, không viết lại công thức.";
            case "GATE_REJECTED_NUMBER" -> "Bỏ số và giá trị cụ thể, kể cả số viết bằng chữ; không nêu nghiệm, dấu hay khoảng. Hỏi em tự kiểm tra thao tác nào ở bước hiện tại.";
            case "UNKNOWN_CITATION" -> "Mã dòng dẫn không có trong bảng đã khóa. Chỉ dẫn mã [n] thật trong bảng hoặc bỏ dẫn số dòng.";
            case "OUTPUT_IDENTITY" -> "Bỏ tên riêng và thông tin cá nhân. Chỉ dùng em và mình.";
            case "OUTPUT_LANGUAGE" -> "Chỉ viết tiếng Việt có dấu, không chèn chữ ngôn ngữ khác.";
            case "STRICT_QUESTION_SHAPE" -> "Chỉ viết đúng một câu hỏi tiếng Việt tối đa 160 ký tự, kết thúc bằng dấu hỏi. Bỏ số, ký hiệu, dẫn [n], biểu thức và câu dẫn; hỏi thao tác em tự làm.";
            case "EMPTY_SENTENCE" -> "Bản trả lời chưa có câu có nội dung. Viết một câu hỏi tiếng Việt ngắn về thao tác ở bước hiện tại, kết thúc bằng dấu hỏi.";
            case "GATE_REJECTED_STATEMENT" -> "Bỏ khẳng định, quy tắc, dấu và kết luận, kể cả xác nhận đúng sai bằng câu hỏi. Thao tác bằng lời vẫn có thể hàm ý quy tắc: chỉ nêu hành động áp dụng quy tắc kèm [n] phù hợp trong cùng câu, dùng đúng số dòng của bảng đã khóa, không chép hay diễn đạt lại công thức. Ví dụ nếu lũy thừa ở [1]: Em hãy áp dụng quy tắc đạo hàm của lũy thừa [1] cho từng hạng tử. Nếu thiếu căn cứ, chuyển thành câu hỏi mở về lựa chọn hoặc tiến độ của em, như: Em định chọn quy tắc nào cho bước hiện tại? Không chỉ thêm dấu hỏi vào chỉ dẫn tính toán; không nói thao tác cho ra điều gì.";
            default -> "Viết lại bằng một câu hỏi ngắn về thao tác em cần tự làm; bỏ mọi biểu thức, số, nghiệm, dấu, kết luận và xác nhận đúng sai.";
        };
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
