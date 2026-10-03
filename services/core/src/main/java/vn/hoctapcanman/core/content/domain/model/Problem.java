package vn.hoctapcanman.core.content.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bài của ngân hàng: nội dung chung của chủ đề (FR-003), đủ thuộc tính của v0: 4 mức, 3 mức CV 7991, Bloom, kỹ năng,
 * khung bước. Lời giải và dữ kiện bảo vệ ở {@link Solution} riêng, nên đề cho học sinh không mang lời giải. Trạng thái
 * phát hành tính theo lớp ở {@link ProblemRelease}. {@code contentHash} là dấu vân tay của đề, lời giải, thang gợi ý:
 * đổi nội dung thì lượt kiểm cũ không còn dùng được.
 */
public record Problem(
        UUID id,
        String code,
        String skillCode,
        List<String> extraSkillCodes,
        Level4 level4,
        @Nullable Level3 level3,
        @Nullable BloomLevel bloomLevel,
        @Nullable Double difficulty,
        String statementText,
        String statementLatex,
        @Nullable String functionSympy,
        String answerForm,
        @Nullable String startStep,
        String origin,
        String contentHash,
        @Nullable UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    /** Dạng trả lời có khung 5 bước trên app (SP-06 của v0). */
    public static final String TU_LUAN_5_BUOC = "TU_LUAN_5_BUOC";

    public Problem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(skillCode, "skillCode");
        Objects.requireNonNull(extraSkillCodes, "extraSkillCodes");
        Objects.requireNonNull(level4, "level4");
        Objects.requireNonNull(statementText, "statementText");
        Objects.requireNonNull(statementLatex, "statementLatex");
        Objects.requireNonNull(answerForm, "answerForm");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(contentHash, "contentHash");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        Kiem.ma(code, 64, "bài");
        Kiem.ma(skillCode, 32, "kỹ năng");
        extraSkillCodes = List.copyOf(extraSkillCodes);
        extraSkillCodes.forEach(ma -> Kiem.ma(ma, 32, "kỹ năng phụ"));
        if (difficulty != null && !(difficulty >= 0 && difficulty <= 1)) {
            throw new IllegalArgumentException("Độ khó ngoài [0, 1]");
        }
        Kiem.khongTrong(statementText, "Đề bài");
        Kiem.maHoa(answerForm, "dạng trả lời");
        Kiem.maNeuCo(startStep, 32, "bước bắt đầu");
        Kiem.maHoa(origin, "nguồn bài");
        Kiem.sha256(contentHash, "Dấu vân tay nội dung");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Thời điểm sửa trước thời điểm tạo");
        }
    }

    /** Làm theo khung 5 bước trên app. */
    public boolean isFiveStep() {
        return TU_LUAN_5_BUOC.equals(answerForm);
    }
}
