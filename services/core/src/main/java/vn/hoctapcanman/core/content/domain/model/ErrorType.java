package vn.hoctapcanman.core.content.domain.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Mã lỗi của lab Sư phạm ({@code data/supham/ma-loi-DH.csv}). {@code stepCode} có thể là bước ngoài khung 5 bước
 * ({@code B.DH.DOCBANG}, {@code B.DH.THAMSO}…): bước của dạng bài khác, như v0. {@code resultTypes} là loại kết quả liên
 * quan ({@code loai_ket_qua_lien_quan}, tách theo «|»).
 */
public record ErrorType(String code, @Nullable String skillCode, @Nullable String stepCode, String name, @Nullable String fixHint, List<String> resultTypes) {

    public ErrorType {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(resultTypes, "resultTypes");
        Kiem.ma(code, 32, "lỗi");
        Kiem.maNeuCo(skillCode, 32, "kỹ năng");
        Kiem.maNeuCo(stepCode, 32, "bước");
        Kiem.khongTrong(name, "Mô tả mã lỗi");
        resultTypes = List.copyOf(resultTypes);
        resultTypes.forEach(t -> Kiem.maHoa(t, "loại kết quả"));
    }
}
