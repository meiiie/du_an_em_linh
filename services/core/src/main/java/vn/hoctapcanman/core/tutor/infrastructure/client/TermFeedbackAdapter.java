package vn.hoctapcanman.core.tutor.infrastructure.client;

import java.util.*;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.shared.infrastructure.math.*;
import vn.hoctapcanman.core.tutor.application.port.TermFeedback;

@Component
public final class TermFeedbackAdapter implements TermFeedback {
    private final MathServiceClient math;
    public TermFeedbackAdapter(MathServiceClient math) { this.math = math; }
    @Override public Optional<Feedback> derivative(String function, String studentExpression) {
        try {
            if (!(math.call(MathJob.SO_HANG, Map.of("function", function, "student", studentExpression, "variable", "x")) instanceof MathResult.Ok ok))
                return Optional.empty();
            var body = ok.body();
            if (!(body.get("terms") instanceof List<?> rows) || rows.isEmpty() || rows.size() > 32
                || !(body.get("missing_count") instanceof Number missing) || missing.intValue() != missing.doubleValue()
                || missing.intValue() < 0 || missing.intValue() > 32) return Optional.empty();
            var terms = new ArrayList<Term>();
            for (var row : rows) {
                if (!(row instanceof Map<?,?> term) || !(term.get("index") instanceof Number index)
                    || index.doubleValue() != terms.size() + 1 || !(term.get("text") instanceof String text) || text.isBlank()
                    || !(term.get("status") instanceof String status) || !Set.of("ok", "sai", "thua").contains(status))
                    return Optional.empty();
                terms.add(new Term(index.intValue(), text, status));
            }
            // Không đưa chữ từ dịch vụ vào system nếu không đúng toàn bộ các mảnh em đã gõ.
            String joined = terms.stream().map(Term::text).collect(java.util.stream.Collectors.joining());
            if (!joined.replaceAll("(?U)\\s+", "").equals(studentExpression.replaceAll("(?U)\\s+", ""))) return Optional.empty();
            return Optional.of(new Feedback(terms, missing.intValue()));
        } catch (RuntimeException ex) { return Optional.empty(); }
    }
}
