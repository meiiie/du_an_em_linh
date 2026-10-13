package vn.hoctapcanman.core.mastery.domain.model;

import java.util.List;

/** Sai liên tiếp cùng một bước; bước khác hoặc một bài đạt ngắt chuỗi. Lượt nghi đoán mò không được tính. */
public final class RepeatedStepFailures {
    private RepeatedStepFailures() {}
    public static int count(List<MasteryEvent> newestFirst) {
        String step = "";
        int count = 0;
        for (var e : newestFirst) {
            if (e.guessSuspected()) continue;
            if (e.wrongStep() == null) break;
            if (step.isEmpty()) step = e.wrongStep();
            if (!step.equals(e.wrongStep())) break;
            count++;
        }
        return count;
    }
}
