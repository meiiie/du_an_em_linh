package vn.hoctapcanman.core.tutor.application.port;

import java.util.List;
import java.util.Optional;

/** Không mang đáp án; chỉ chữ gốc của học sinh và vị trí cần xem lại từ CAS. */
public interface TermFeedback {
    Optional<Feedback> derivative(String function, String studentExpression);
    record Term(int index, String text, String status) {}
    record Feedback(List<Term> terms, int missingCount) {
        public Feedback { terms = List.copyOf(terms); }
        @Override public String toString() { return "Feedback[đã ẩn nội dung]"; }
    }
}
