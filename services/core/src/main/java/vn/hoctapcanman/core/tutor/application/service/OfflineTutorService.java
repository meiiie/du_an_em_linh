package vn.hoctapcanman.core.tutor.application.service;

import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.Normalizer;
import java.text.BreakIterator;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.tutor.application.port.HintModel;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

/** Gia sư Socratic với thang dự phòng; mọi bản nháp đều qua hai lớp cổng toán. */
@Service
public class OfflineTutorService {
    private static final Logger LOG = LoggerFactory.getLogger(OfflineTutorService.class);
    private static final String REFUSAL = "Mình sẽ giúp em tự làm. Em đang vướng ở đâu trong bước này? Hãy nêu cách em đã thử.";
    private static final String SAFE = "Em xem lại bước này theo bảng công thức của lớp rồi thử lại, hoặc gửi thầy cô.";
    private final WorkspaceStore store;
    private final ClassMembership membership;
    private final ContentWorkflow gate;
    private final HintModel model;
    private final TutorRateLimiter limiter;

    public OfflineTutorService(WorkspaceStore store, ClassMembership membership, ContentWorkflow gate,
        HintModel model, TutorRateLimiter limiter) {
        this.store = store;
        this.membership = membership;
        this.gate = gate;
        this.model = model;
        this.limiter = limiter;
    }

    /** Kiểm quyền và tính đúng một lượt trước khi controller tạo SSE. */
    public UUID authorize(UUID student, TutorRequest request) {
        UUID cl = membership.lopHoc(student).orElseThrow(() -> new WorkspaceException(404, "Em chưa có lớp."));
        if (store.testMode(student, cl, request.maBai())) throw new WorkspaceException(403,
            "Gia sư không mở trong bài kiểm tra hoặc bài đã hết hạn.");
        // Chỉ tính lượt khi bài, bước và cấp còn hợp lệ: yêu cầu cũ hay sai không ăn hạn mức.
        if (store.tutorContext(student, cl, request).isEmpty()) throw new WorkspaceException(404,
            "Chưa có gợi ý phù hợp cho bước này. Em có thể nhờ giáo viên.");
        limiter.acquire(student);
        return cl;
    }

    public TutorEvent answer(UUID student, TutorRequest request) {
        return answerAuthorized(student, authorize(student, request), request, ignored -> {});
    }

    /** Chỉ controller đã cấp quyền được gọi đường này; không đếm lượt lần hai. */
    public TutorEvent answerAuthorized(UUID student, UUID cl, TutorRequest request, Consumer<String> phase) {
        long start = System.nanoTime();
        String outcome = "LOI";
        String problemCode = "khong_xac_dinh";
        try {
            phase.accept("kho");
            TutorContext context = store.tutorContext(student, cl, request).orElseThrow(() -> new WorkspaceException(404,
                "Chưa có gợi ý phù hợp cho bước này. Em có thể nhờ giáo viên."));
            problemCode = context.code();
            phase.accept("goi");
            boolean refusal = asksAnswer(request.cauHoi());
            Optional<String> draft = Optional.empty();
            if (!refusal && TutorPrompt.safeToSend(request.cauHoi(), context.submittedLines())) {
                try { draft = model.draft(TutorPrompt.compose(context, request)); }
                catch (RuntimeException ignored) { /* Nhà lỗi: chỉ dùng thang sau khi qua cổng. */ }
            }
            phase.accept("loc");
            if (refusal) {
                // Câu từ chối cố định, không chứa toán: không cần qua mô hình.
                outcome = "TU_CHOI";
                return release(student, cl, request, context, "xong", REFUSAL, "TU_CHOI");
            }
            Optional<String> fromModel = draft.filter(d -> hasSafeShape(context, d)).flatMap(d -> allowed(cl, context, d, request));
            if (fromModel.isEmpty() && draft.isPresent()) {
                // Cổng bỏ hết bản nháp: thử lại đúng một lần với yêu cầu chặt hơn (không nới cổng, ADR 013).
                try {
                    var first = TutorPrompt.compose(context, request);
                    var retry = model.draft(new HintPrompt(first.system(), first.user() + TutorPrompt.RETRY));
                    fromModel = retry.filter(d -> hasSafeShape(context, d)).flatMap(d -> allowed(cl, context, d, request));
                } catch (RuntimeException ignored) { /* Nhà lỗi: dùng thang. */ }
            }
            if (fromModel.isPresent()) {
                outcome = "DAT_MO_HINH";
                return release(student, cl, request, context, "xong", fromModel.get(), "MO_HINH");
            }
            Optional<String> ladder = hasSafeShape(context, context.hint()) ? allowed(cl, context, context.hint(), request) : Optional.empty();
            if (ladder.isPresent()) {
                outcome = "THANG";
                return release(student, cl, request, context, "xong", ladder.get(), "THANG");
            }
            // ADR 013 §6: không còn câu nào qua cổng thì dùng câu cố định không chứa toán.
            outcome = "CAU_CO_DINH";
            return release(student, cl, request, context, "xong", SAFE, "CO_DINH");
        } catch (RuntimeException ex) {
            outcome = "LOI";
            throw ex;
        } finally {
            LOG.info("gia_su hoc_sinh={} bai={} cap={} nha={} tre_ms={} ket_qua={}",
                hashed(student), problemCode, Math.clamp(request.cap(), 1, 3), model.provider(),
                (System.nanoTime() - start) / 1_000_000, outcome);
        }
    }

    private TutorEvent release(UUID student, UUID cl, TutorRequest request, TutorContext asked, String status, String text, String nguon) {
        if (!membership.lopHoc(student).filter(cl::equals).isPresent()) throw new WorkspaceException(404, "Em chưa có lớp.");
        if (store.testMode(student, cl, request.maBai())) throw new WorkspaceException(403,
            "Gia sư không mở trong bài kiểm tra hoặc bài đã hết hạn.");
        // Gọi ngoài có thể lâu: kiểm lại toàn bộ điều kiện giao bài (hủy, gỡ phát hành, hết hạn) trước khi phát.
        TutorContext now = store.tutorContext(student, cl, request).orElseThrow(() -> new WorkspaceException(404,
            "Chưa có gợi ý phù hợp cho bước này. Em có thể nhờ giáo viên."));
        // Bảng công thức đổi giữa lúc dựng prompt và lúc phát thì mã [n] có thể trỏ sai dòng: không phát.
        if (!now.formulas().equals(asked.formulas())) throw new WorkspaceException(409,
            "Bảng công thức của lớp vừa cập nhật. Em hỏi lại nhé.");
        return new TutorEvent(status, text, nguon);
    }

    private static boolean hasSafeShape(TutorContext context, String draft) {
        // Mô hình đôi khi chèn chữ Hán/Nhật/Hàn giữa câu tiếng Việt: bỏ cả bản nháp.
        if (draft.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN || Character.UnicodeScript.of(c) == Character.UnicodeScript.HIRAGANA || Character.UnicodeScript.of(c) == Character.UnicodeScript.KATAKANA || Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL)) return false;
        Set<String> references = new HashSet<>();
        context.formulas().forEach(row -> references.add(Integer.toString(row.number())));
        var matcher = Pattern.compile("\\[(\\d+)\\]").matcher(draft);
        while (matcher.find()) if (!references.contains(matcher.group(1))) return false;
        // Dấu câu trong toán không phải ranh giới câu văn; không cắt bản nháp vì có thể làm hỏng KaTeX.
        String prose = draft.replaceAll("(?s)\\$\\$.*?\\$\\$|\\$[^$]*\\$|\\\\\\(.*?\\\\\\)|\\\\\\[.*?\\\\\\]", "toán");
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.forLanguageTag("vi"));
        sentences.setText(prose);
        int count = 0;
        for (int from = sentences.first(), to = sentences.next(); to != BreakIterator.DONE; from = to, to = sentences.next()) {
            if (!prose.substring(from, to).isBlank() && ++count > 3) return false;
        }
        return count > 0;
    }

    private Optional<String> allowed(UUID cl, TutorContext context, String draft, TutorRequest request) {
        if (draft.isBlank()) return Optional.empty();
        try { return gate.sanitize(cl, context.problemId(), draft, request.cauHoi(), context.submittedLines()); }
        catch (RuntimeException ignored) { return Optional.empty(); }
    }

    public static boolean asksAnswer(String text) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replace('đ', 'd');
        return s.matches("(?s).*(dap\\s*(an|so)|loi\\s*giai|lam\\s*(ho|giup)|giai\\s*(ho|giup)|dung\\s*khong|"
            + "ket\\s*qua\\s*cuoi|ra\\s*bao\\s*nhieu|bo\\s*qua\\s*huong\\s*dan|answer|solve|solution|"
            + "ignore\\s+(previous|all|prior)|system\\s*prompt|ban\\s*la\\s*ai\\s*that|role[ -]?play|"
            + "dong\\s*vai|gia\\s*vo|pretend|act\\s+as|gia\\s*su.*giao\\s*vien|em\\s*la\\s*giao\\s*vien).*" );
    }

    private static String hashed(UUID student) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(student.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("Không có thuật toán băm định danh.", ex); }
    }
}
