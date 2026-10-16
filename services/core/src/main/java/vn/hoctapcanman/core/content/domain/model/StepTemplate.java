package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Một bước của khung tự luận. Bước dùng chung như {@code B.TD.TRALOI} không có chủ đề;
 * chủ đề của bài luôn lấy qua kỹ năng chính.
 */
public record StepTemplate(String stepCode, @Nullable String topicCode, int ordinal, InputKind inputKind, @Nullable String skillCode, String description) {

    public StepTemplate {
        Objects.requireNonNull(stepCode, "stepCode");
        Objects.requireNonNull(inputKind, "inputKind");
        Objects.requireNonNull(description, "description");
        Kiem.ma(stepCode, 32, "bước");
        Kiem.maNeuCo(topicCode, 32, "chủ đề");
        Kiem.maNeuCo(skillCode, 32, "kỹ năng");
        if (ordinal < 1 || ordinal > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Thứ tự bước ngoài 1–32767");
        }
        Kiem.toiDa(Kiem.khongTrong(description, "Mô tả bước"), 300, "Mô tả bước");
    }
}
