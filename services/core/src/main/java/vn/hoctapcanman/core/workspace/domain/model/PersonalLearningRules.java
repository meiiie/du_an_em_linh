package vn.hoctapcanman.core.workspace.domain.model;

import java.time.Instant;
import java.util.*;

/** Luật bài kế, chỉ nhận các bài còn được giao và phát hành cho chính em. Không đọc lời giải. */
public final class PersonalLearningRules {
    private PersonalLearningRules() {}
    public enum Level4 { NHAN_BIET, THONG_HIEU, VAN_DUNG, VAN_DUNG_CAO }
    public record Skill(String code, String name, Level4 level, double mastery, int attempts,
                        boolean stuck, String errorType, boolean lastPassed, Instant gradedAt, Level4 practicedLevel) {
        public Skill(String code, String name, Level4 level, double mastery, int attempts, boolean stuck,
                     String errorType, boolean lastPassed, Instant gradedAt) {
            this(code,name,level,mastery,attempts,stuck,errorType,lastPassed,gradedAt,level);
        }
    }
    public record Candidate(String code, String skillCode, Level4 level, String teacherReason) {}
    public record Choice(String code, String skillCode, Level4 level, String kind, String reason) {}

    public static String levelName(Level4 level) {
        return switch (level) {
            case NHAN_BIET -> "Nhận biết";
            case THONG_HIEU -> "Thông hiểu";
            case VAN_DUNG -> "Vận dụng";
            case VAN_DUNG_CAO -> "Vận dụng cao";
        };
    }
    public static List<Skill> weakestFirst(List<Skill> skills) {
        return skills.stream().sorted(Comparator.comparingInt((Skill s) -> s.level().ordinal())
            .thenComparingDouble(Skill::mastery).thenComparing(s -> !s.stuck()).thenComparing(Skill::code)).toList();
    }
    public static Optional<Choice> choose(List<Skill> skills, List<Candidate> candidates) {
        var order = new ArrayList<>(weakestFirst(skills));
        var latest = skills.stream().filter(s -> s.attempts() > 0)
            .max(Comparator.comparing(Skill::gradedAt).thenComparing(Skill::code));
        // Chữa lỗi vừa gặp trước kỹ năng yếu không liên quan; thành công mới nhất bỏ ưu tiên lỗi cũ.
        latest.filter(s -> s.lastPassed() || !s.errorType().isBlank()).ifPresent(s -> {
            var justGraded = order.stream().filter(k -> k.gradedAt().equals(s.gradedAt())).toList();
            order.removeAll(justGraded); order.addAll(0,justGraded);
        });
        if (order.isEmpty()) return Optional.empty();
        if (!order.getFirst().stuck()) {
            var teacherChoice = candidates.stream().filter(p -> !p.teacherReason().isBlank())
                .filter(p -> skills.stream().anyMatch(s -> s.code().equals(p.skillCode())
                    && p.level().ordinal() <= s.level().ordinal() + 1
                    && (!s.stuck() || p.level().ordinal() < s.level().ordinal())))
                .sorted(Comparator.comparing(Candidate::code)).findFirst();
            if (teacherChoice.isPresent()) {
                var p=teacherChoice.get();
                return Optional.of(new Choice(p.code(),p.skillCode(),p.level(),"THAY_CO_GIAO","Thầy cô giao: "+p.teacherReason()));
            }
        }
        for (Skill s : order) {
            var available = candidates.stream().filter(p -> p.skillCode().equals(s.code())
                && p.level().ordinal() <= s.level().ordinal() + 1).toList();
            int target = s.stuck() ? s.level().ordinal() - 1
                : s.lastPassed() ? Math.max(s.level().ordinal(), Math.min(s.level().ordinal() + 1, Math.min(3,s.practicedLevel().ordinal() + 1))) : s.level().ordinal();
            if (s.stuck()) {
                available = available.stream().filter(p -> p.level().ordinal() <= target).toList();
                if (available.isEmpty()) return Optional.of(new Choice("", s.code(), s.level(), "GUI_THAY_CO",
                    "Em đang kẹt ở " + s.name() + ". Hãy ôn lại dạng này và gửi thầy cô để được hỗ trợ; chưa tăng độ khó."));
            } else {
                available = available.stream().filter(p -> p.level().ordinal() >= s.level().ordinal()
                    && p.level().ordinal() <= target).toList();
            }
            var selected = available.stream().min(Comparator.comparing((Candidate p) -> p.teacherReason().isBlank())
                .thenComparingInt(p -> Math.abs(p.level().ordinal() - target)).thenComparing(Candidate::code));
            if (selected.isEmpty()) continue;
            var p = selected.get();
            if (!p.teacherReason().isBlank()) return Optional.of(new Choice(p.code(), s.code(), p.level(), "THAY_CO_GIAO",
                "Thầy cô giao: " + p.teacherReason()));
            String kind = s.stuck() ? "DE_HON" : !s.lastPassed() && !s.errorType().isBlank() ? "CHUA_LOI"
                : s.lastPassed() && p.level().ordinal() == s.practicedLevel().ordinal() + 1 ? "NANG_1_NAC" : "CUNG_CO";
            String reason = switch (kind) {
                case "DE_HON" -> "Em đang kẹt ở " + s.name() + ". Luyện bài dễ hơn ở mức " + levelName(p.level()) + "; chưa tăng độ khó.";
                case "CHUA_LOI" -> "Chữa dạng lỗi vừa gặp: " + s.errorType() + ". Ôn " + s.name() + " ở mức " + levelName(p.level()) + ".";
                case "NANG_1_NAC" -> "Em đã làm đạt. Nâng đúng một nấc từ " + levelName(s.practicedLevel()) + " lên " + levelName(p.level()) + " ở kỹ năng " + s.name() + ".";
                default -> "Củng cố " + s.name() + " ở mức " + levelName(p.level()) + ".";
            };
            return Optional.of(new Choice(p.code(), s.code(), p.level(), kind, reason));
        }
        return Optional.empty();
    }
}
