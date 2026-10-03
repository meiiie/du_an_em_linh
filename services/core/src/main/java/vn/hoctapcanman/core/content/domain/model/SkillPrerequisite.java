package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Kỹ năng tiên quyết của một kỹ năng. {@code minLevel} là mức tối thiểu của kỹ năng tiên quyết trước khi mở kỹ năng này
 * ({@code muc_toi_thieu} của danh mục, mã ngắn 4 mức {@code NB}, {@code TH}, {@code VD}, {@code VDC}).
 */
public record SkillPrerequisite(String skillCode, String prerequisiteCode, @Nullable String minLevel) {

    private static final Set<String> MUC = Set.of("NB", "TH", "VD", "VDC");

    public SkillPrerequisite {
        Objects.requireNonNull(skillCode, "skillCode");
        Objects.requireNonNull(prerequisiteCode, "prerequisiteCode");
        Kiem.ma(skillCode, 32, "kỹ năng");
        Kiem.ma(prerequisiteCode, 32, "kỹ năng tiên quyết");
        if (skillCode.equals(prerequisiteCode)) {
            throw new IllegalArgumentException("Kỹ năng không làm tiên quyết của chính nó");
        }
        if (minLevel != null && !MUC.contains(minLevel)) {
            throw new IllegalArgumentException("Mức tối thiểu không hợp lệ");
        }
    }
}
