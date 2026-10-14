package vn.hoctapcanman.core.tutor.application.service;

import java.util.*;
import java.util.regex.Pattern;
import vn.hoctapcanman.core.tutor.application.port.TermFeedback;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;

/** Heuristic nhỏ: một dòng đạo hàm/biểu thức thuần, không lấy biểu thức từ lời kể hay nhiều phương trình. */
public final class TutorTerms {
    private TutorTerms() {}
    private static final Pattern LABEL = Pattern.compile("(?i)(?:y|f\\s*\\(\\s*x\\s*\\))\\s*(?:'|′|\\\\prime)\\s*=|[fy]\\s*(?:'|′|\\\\prime)\\s*\\(\\s*x\\s*\\)\\s*=");
    private static final Pattern EXPRESSION = Pattern.compile("[0-9x+\\-−–—*/^().,{}\\s\\\\a-zA-Z]+");
    public static Optional<String> expression(String question) {
        String text = question.strip();
        var label = LABEL.matcher(text);
        if (label.find()) text = text.substring(label.end()).strip();
        else if (text.startsWith("$") && text.endsWith("$")) text = text.substring(1, text.length()-1).strip();
        // Sau biểu thức chỉ nhận một hậu tố câu hỏi, không nhặt một mảnh toán từ câu tùy ý.
        text = text.replaceFirst("(?iu)\\s*[,;?!]?\\s*(?:đúng không(?:\\s+ạ)?|kiểm tra(?:\\s+giúp em)?|nhờ kiểm tra|em sửa vậy được chưa|được chưa)(?:[.?!]|\\s)*$", "").strip();
        text = text.replaceFirst("^\\$+", "").replaceFirst("\\$+$", "").strip();
        if (text.isEmpty() || text.length()>300 || !EXPRESSION.matcher(text).matches()) return Optional.empty();
        // Whitelist tên, không đẩy văn xuôi/chỉ dẫn vào một ghi chú system.
        var names = Pattern.compile("[A-Za-z]+").matcher(text);
        while (names.find()) if (!Set.of("x","sqrt","frac","dfrac","cdot","times","left","right","Abs","sin","cos","tan","cot","log","ln","exp","pi","e").contains(names.group())) return Optional.empty();
        if (!label.reset(question).find() && !text.contains("x") && !text.matches(".*[+*/^−–—-].*")) return Optional.empty();
        return Optional.of(text);
    }
    public static HintPrompt add(HintPrompt prompt, TutorContext context, TutorRequest request, TermFeedback math) {
        if (!"B.DH.DAOHAM".equals(context.step()) || context.function().isBlank()) return prompt;
        var expression = expression(request.cauHoi());
        if (expression.isEmpty()) return prompt;
        try {
            var feedback = math.derivative(context.function(), expression.get());
            if (feedback.isEmpty()) return prompt;
            var notes = new ArrayList<String>();
            for (var term : feedback.get().terms()) {
                if (term.status().equals("sai")) notes.add("hạng tử thứ " + term.index() + " «" + term.text() + "» cần xem lại");
                if (term.status().equals("thua")) notes.add("hạng tử «" + term.text() + "» thừa");
            }
            notes.add("còn thiếu " + feedback.get().missingCount() + " hạng tử");
            return new HintPrompt(prompt.system() + "\nGhi chú riêng (không nhắc nguyên văn, không nói đúng/sai toàn bài): trong dòng em gõ, "
                + String.join("; ", notes) + ". Chỉ nhắc hạng tử cần xem lại do em gõ, không suy ra giá trị thay thế."
                + " Nếu không có hạng tử cần xem lại, chỉ hướng em nhập vào ô trả lời của bước và bấm «Kiểm tra»; không xác nhận cả dòng.", prompt.user());
        } catch (RuntimeException ex) { return prompt; }
    }
}
