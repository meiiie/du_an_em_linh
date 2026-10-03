package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Một bước của khung tự luận ({@code data/v0/khung-buoc.json}): {@code B.DH.TXD} … {@code B.DH.KETLUAN}. */
public record StepTemplate(String stepCode, String topicCode, int ordinal, InputKind inputKind, @Nullable String skillCode, String description) {

    public StepTemplate {
        Objects.requireNonNull(stepCode, "stepCode");
        Objects.requireNonNull(topicCode, "topicCode");
        Objects.requireNonNull(inputKind, "inputKind");
        Objects.requireNonNull(description, "description");
        Kiem.ma(stepCode, 32, "bước");
        Kiem.ma(topicCode, 32, "chủ đề");
        Kiem.maNeuCo(skillCode, 32, "kỹ năng");
        if (ordinal < 1 || ordinal > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Thứ tự bước ngoài 1–32767");
        }
        Kiem.toiDa(Kiem.khongTrong(description, "Mô tả bước"), 300, "Mô tả bước");
    }
}
