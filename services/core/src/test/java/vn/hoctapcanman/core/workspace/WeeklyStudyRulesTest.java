package vn.hoctapcanman.core.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.workspace.domain.model.PersonalLearningRules.Level4;
import vn.hoctapcanman.core.workspace.domain.model.PersonalLearningRules.Skill;
import vn.hoctapcanman.core.workspace.application.service.WeeklyStudyRules;

class WeeklyStudyRulesTest {
    final Instant sundayUtc = Instant.parse("2026-10-11T18:00:00Z");
    @Test void emptyHistoryHasStarterAdviceAndThreeShortSessionsInVietnameseWeek() {
        var plan = WeeklyStudyRules.build(List.of(), sundayUtc, 0);
        assertThat(plan.loiKhuyen()).contains("Bắt đầu", "Nhận biết");
        assertThat(plan.sessions()).hasSize(3);
        assertThat(plan.sessions().getFirst().details().get("startsAt")).isEqualTo("2026-10-12T12:30:00Z");
        assertThat(plan.sessions().getFirst().details().get("endsAt")).isEqualTo("2026-10-12T12:50:00Z");
    }
    @Test void focusesWeakestEffectiveLevelThenMasteryAndNamesItInAdvice() {
        var stronger = new Skill("K1", "Đạo hàm", Level4.VAN_DUNG, .02, 4, false, "", true, sundayUtc);
        var weak = new Skill("K2", "Xét dấu đạo hàm", Level4.THONG_HIEU, .5, 2, false, "", true, sundayUtc);
        var plan = WeeklyStudyRules.build(List.of(stronger, weak), sundayUtc, 0);
        assertThat(plan.loiKhuyen()).contains("Xét dấu đạo hàm", "Thông hiểu");
        assertThat(plan.sessions()).allSatisfy(b -> assertThat(b.title()).contains("Xét dấu đạo hàm"));
    }
    @Test void stuckAdviceReviewsThatTypeWithoutIncreasingDifficulty() {
        var weak = new Skill("K1", "Xét dấu đạo hàm", Level4.NHAN_BIET, .05, 3, true, "Nhầm dấu", false, sundayUtc);
        var plan = WeeklyStudyRules.build(List.of(weak), sundayUtc, 0);
        assertThat(plan.loiKhuyen()).contains("Xét dấu đạo hàm", "Nhầm dấu", "chưa tăng độ khó");
        assertThat(plan.sessions()).allSatisfy(b -> assertThat(b.title()).doesNotContain("Nâng"));
    }
    @Test void nextWeekCrossesYearBoundaryAndKeepsLocalTime() {
        var plan = WeeklyStudyRules.build(List.of(), Instant.parse("2026-12-31T18:00:00Z"), 1);
        assertThat(plan.sessions().getFirst().details().get("startsAt")).isEqualTo("2027-01-04T12:30:00Z");
    }
}
