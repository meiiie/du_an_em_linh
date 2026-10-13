package vn.hoctapcanman.core.workspace.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.hoctapcanman.core.workspace.domain.model.PersonalLearningRules.Level4;
import vn.hoctapcanman.core.workspace.domain.model.PersonalLearningRules.*;

class PersonalLearningRulesTest {
    final Instant now = Instant.parse("2026-10-14T08:00:00Z");
    Skill skill(Level4 level, boolean passed, boolean stuck, String error) {
        return new Skill("K1", "Xét dấu đạo hàm", level, .3, 3, stuck, error, passed, now);
    }
    Candidate problem(String code, Level4 level) { return new Candidate(code, "K1", level, ""); }

    @ParameterizedTest @EnumSource(Level4.class)
    void neverJumpsMoreThanOneLevelEvenForTeacherChoice(Level4 level) {
        var candidates = java.util.Arrays.stream(Level4.values())
            .map(l -> new Candidate(l.name(), "K1", l, "Thầy cô chọn")).toList();
        var result = PersonalLearningRules.choose(List.of(skill(level, true, false, "")), candidates).orElseThrow();
        assertThat(result.level().ordinal()).isLessThanOrEqualTo(level.ordinal() + 1);
    }
    @Test void teacherChoiceOnAnotherSkillComesFirstWithinTheCeiling() {
        var stronger = new Skill("K2", "Đạo hàm", Level4.THONG_HIEU, .6, 2, false, "", true, now);
        var result = PersonalLearningRules.choose(List.of(skill(Level4.NHAN_BIET,false,false,""), stronger),
            List.of(problem("weak",Level4.NHAN_BIET),new Candidate("teacher","K2",Level4.VAN_DUNG,"Ôn trên lớp"))).orElseThrow();
        assertThat(result.code()).isEqualTo("teacher");
        assertThat(result.kind()).isEqualTo("THAY_CO_GIAO");
        assertThat(result.reason()).contains("Ôn trên lớp");
    }
    @Test void keepsCurrentLevelWhenTheOnlyOtherAssignmentIsEasier() {
        assertThat(PersonalLearningRules.choose(List.of(skill(Level4.VAN_DUNG,false,false,"")),
            List.of(problem("easy",Level4.NHAN_BIET)))).isEmpty();
    }
    @Test void successThatAlreadyRaisedMasteryDoesNotRaiseTwiceOrSwitchToAnUntouchedSkill() {
        var passed = new Skill("K1","Đơn điệu",Level4.THONG_HIEU,.7,1,false,"",true,now,Level4.NHAN_BIET);
        var untouched = new Skill("K2","Cực trị",Level4.NHAN_BIET,.3,0,false,"",false,Instant.EPOCH);
        var result = PersonalLearningRules.choose(List.of(untouched,passed),List.of(
            new Candidate("unrelated","K2",Level4.NHAN_BIET,""),problem("next",Level4.THONG_HIEU),problem("too-soon",Level4.VAN_DUNG))).orElseThrow();
        assertThat(result.code()).isEqualTo("next");
        assertThat(result.reason()).contains("Nhận biết", "Thông hiểu");
        assertThat(result.kind()).isEqualTo("NANG_1_NAC");
    }
    @Test void prerequisiteStuckSkillWithNoAssignedProblemStillAsksTeacher() {
        var prerequisite = new Skill("T11.DH.01","Tính đạo hàm",Level4.NHAN_BIET,.05,3,true,"Sai quy tắc đạo hàm",false,now);
        var result=PersonalLearningRules.choose(List.of(prerequisite),List.of(problem("other",Level4.NHAN_BIET))).orElseThrow();
        assertThat(result.kind()).isEqualTo("GUI_THAY_CO");
        assertThat(result.reason()).contains("Tính đạo hàm", "gửi thầy cô");
    }
    @Test void rejectsOnlyTooHardCandidates() {
        assertThat(PersonalLearningRules.choose(List.of(skill(Level4.NHAN_BIET, true, false, "")),
            List.of(problem("cao", Level4.VAN_DUNG), problem("rat-cao", Level4.VAN_DUNG_CAO)))).isEmpty();
    }
    @Test void raisesExactlyOneLevelAfterSuccess() {
        var result = PersonalLearningRules.choose(List.of(skill(Level4.THONG_HIEU, true, false, "")),
            List.of(problem("cu", Level4.THONG_HIEU), problem("ke", Level4.VAN_DUNG))).orElseThrow();
        assertThat(result.code()).isEqualTo("ke");
        assertThat(result.kind()).isEqualTo("NANG_1_NAC");
        assertThat(result.reason()).contains("Thông hiểu", "Vận dụng");
    }
    @Test void consolidatesWithoutSuccessfulEvidence() {
        var result = PersonalLearningRules.choose(List.of(skill(Level4.THONG_HIEU, false, false, "")),
            List.of(problem("cu", Level4.THONG_HIEU), problem("ke", Level4.VAN_DUNG))).orElseThrow();
        assertThat(result.code()).isEqualTo("cu");
        assertThat(result.kind()).isEqualTo("CUNG_CO");
    }
    @Test void fixesMostRecentErrorInsteadOfUnrelatedWeakestSkill() {
        var weak = new Skill("K2", "Tập xác định", Level4.NHAN_BIET, .05, 1, false, "", false, now.minusSeconds(1));
        var result = PersonalLearningRules.choose(List.of(weak, skill(Level4.THONG_HIEU, false, false, "Nhầm dấu đạo hàm")),
            List.of(new Candidate("khac", "K2", Level4.NHAN_BIET, ""), problem("sua", Level4.THONG_HIEU), problem("cao", Level4.VAN_DUNG))).orElseThrow();
        assertThat(result.code()).isEqualTo("sua");
        assertThat(result.reason()).contains("Nhầm dấu đạo hàm");
        assertThat(result.kind()).isEqualTo("CHUA_LOI");
    }
    @Test void stuckUsesEasierProblemAndNeverRaises() {
        var result = PersonalLearningRules.choose(List.of(skill(Level4.VAN_DUNG, false, true, "Nhầm dấu")),
            List.of(problem("de", Level4.THONG_HIEU), problem("cu", Level4.VAN_DUNG), problem("cao", Level4.VAN_DUNG_CAO))).orElseThrow();
        assertThat(result.code()).isEqualTo("de");
        assertThat(result.kind()).isEqualTo("DE_HON");
    }
    @Test void stuckAtLowestLevelOrNoEasierAssignmentAsksTeacher() {
        for (Level4 level : List.of(Level4.NHAN_BIET, Level4.VAN_DUNG)) {
            var result = PersonalLearningRules.choose(List.of(skill(level, false, true, "Nhầm dấu")),
                List.of(problem("cu", level))).orElseThrow();
            assertThat(result.code()).isEmpty();
            assertThat(result.kind()).isEqualTo("GUI_THAY_CO");
            assertThat(result.reason()).contains("gửi thầy cô");
        }
    }
    @Test void successfulLatestAttemptDoesNotReuseOldError() {
        var old = new Skill("K2", "Tập xác định", Level4.THONG_HIEU, .6, 2, false, "Quên điều kiện", false, now.minusSeconds(10));
        var result = PersonalLearningRules.choose(List.of(old, skill(Level4.NHAN_BIET, true, false, "")),
            List.of(problem("ke", Level4.THONG_HIEU), new Candidate("cu", "K2", Level4.THONG_HIEU, ""))).orElseThrow();
        assertThat(result.code()).isEqualTo("ke");
        assertThat(result.kind()).isEqualTo("NANG_1_NAC");
    }
}
