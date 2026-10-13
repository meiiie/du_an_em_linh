package vn.hoctapcanman.core.workspace.application.service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.domain.model.PersonalLearningRules;
import vn.hoctapcanman.core.workspace.domain.model.PersonalLearningRules.Skill;

/** Kế hoạch gợi ý tính lại từ mức hiểu; buổi tự thêm vẫn được lưu riêng. */
public final class WeeklyStudyRules {
    public static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private WeeklyStudyRules() {}
    public static WeeklyPlan build(List<Skill> skills, Instant now, int weekOffset) {
        var weak = PersonalLearningRules.weakestFirst(skills).stream().findFirst();
        boolean starter = skills.stream().noneMatch(s -> s.attempts() > 0);
        String name = starter ? "Tập xác định và đạo hàm" : weak.orElseThrow().name();
        String advice = "Bắt đầu bằng một bài Nhận biết về tập xác định và đạo hàm; học từng buổi ngắn.";
        if (!starter) {
            Skill s = weak.orElseThrow();
            advice = s.stuck() ? "Ôn lại " + name + (s.errorType().isBlank()?"":": "+s.errorType())
                + "; em đang kẹt, chưa tăng độ khó. Gửi thầy cô nếu vẫn chưa làm được."
                : "Ưu tiên " + name + ", kỹ năng cần củng cố nhất, ở mức " + PersonalLearningRules.levelName(s.level())
                    + ". Xem lại bước đã sai trước khi làm bài mới.";
        }
        var monday = now.atZone(SCHOOL_ZONE).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset);
        var sessions = new ArrayList<Item>();
        String[] tasks = {"Ôn công thức · ", "Luyện đúng dạng · ", "Xem lại bước đã sai · "};
        int[] days = {0,2,5};
        for (int i=0;i<days.length;i++) {
            var start = monday.plusDays(days[i]).atTime(19,30).atZone(SCHOOL_ZONE).toInstant();
            sessions.add(new Item("goi-y-"+monday+"-"+i,tasks[i]+name,"Buổi học gợi ý","GOI_Y",
                Map.of("startsAt",start.toString(),"endsAt",start.plusSeconds(20*60).toString())));
        }
        return new WeeklyPlan(List.copyOf(sessions),advice);
    }
}
