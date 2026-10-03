package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Kỹ năng của danh mục lab Sư phạm ({@code data/supham/danh-muc-ky-nang-DH.json}), gồm cả kỹ năng tiên quyết ngoài chủ
 * đề. {@code description} là yêu cầu cần đạt trích nguyên văn CT GDPT 2018 kèm trang (FR-003), như cột của v0.
 */
public record Skill(String code, String topicCode, String name, @Nullable String description, @Nullable Integer grade, boolean core) {

    public Skill {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(topicCode, "topicCode");
        Objects.requireNonNull(name, "name");
        Kiem.ma(code, 32, "kỹ năng");
        Kiem.ma(topicCode, 32, "chủ đề");
        Kiem.toiDa(Kiem.khongTrong(name, "Tên kỹ năng"), 300, "Tên kỹ năng");
        if (grade != null && (grade < 1 || grade > 12)) {
            throw new IllegalArgumentException("Khối lớp ngoài 1–12");
        }
    }
}
