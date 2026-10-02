package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Một dòng của bảng công thức: tiêu đề, LaTeX, lời phát biểu, và kết quả kiểm lúc khóa (ADR 013). Dòng chưa kiểm thì
 * các trường kiểm để trống. {@code code} là mã dòng ổn định trong bảng ({@code d-1}… của v0): job trả kết quả theo mã.
 */
public record Formula(
        UUID id,
        int ordinal,
        String code,
        @Nullable String skillCode,
        String title,
        String latex,
        String statement,
        @Nullable FormulaKind kind,
        @Nullable CheckStatus tier1Status,
        @Nullable CheckStatus tier2Status,
        @Nullable String tier1DetailJson,
        @Nullable String tier2DetailJson,
        @Nullable UUID citationPassageId) {

    public Formula {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(latex, "latex");
        Objects.requireNonNull(statement, "statement");
        if (ordinal < 1 || ordinal > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Thứ tự dòng ngoài 1–32767");
        }
        Kiem.ma(code, 32, "dòng công thức");
        Kiem.maNeuCo(skillCode, 32, "kỹ năng");
        Kiem.toiDa(Kiem.hopLeUtf16(Kiem.khongTrong(title, "Tiêu đề dòng"), "Tiêu đề dòng"), 200, "Tiêu đề dòng");
        Kiem.hopLeUtf16(Kiem.khongTrong(latex, "LaTeX của dòng"), "LaTeX của dòng");
        Kiem.hopLeUtf16(Kiem.khongTrong(statement, "Lời phát biểu của dòng"), "Lời phát biểu của dòng");
        if ((tier1Status != null && !tier1Status.isMachineVerdict()) || (tier2Status != null && !tier2Status.isMachineVerdict())) {
            throw new IllegalArgumentException("Dòng công thức không duyệt riêng (ADR 013)");
        }
        if ((tier1Status != null || tier2Status != null) && kind == null) {
            throw new IllegalArgumentException("Dòng đã kiểm phải có loại dòng");
        }
        if (tier2Status == CheckStatus.DAT && citationPassageId == null) {
            throw new IllegalArgumentException("Tầng 2 DAT phải có đoạn trích dẫn");
        }
    }

    /** Dòng mới, chưa kiểm. */
    public static Formula unchecked(int ordinal, String code, @Nullable String skillCode, String title, String latex, String statement) {
        return new Formula(UUID.randomUUID(), ordinal, code, skillCode, title, latex, statement, null, null, null, null, null, null);
    }

    /** Đạt cả tầng 1 và tầng 2 có trích dẫn: điều kiện để khóa bảng (ADR 013). */
    public boolean passes() {
        return tier1Status == CheckStatus.DAT && tier2Status == CheckStatus.DAT && citationPassageId != null;
    }

    Formula withCheck(@Nullable FormulaCheck check) {
        if (check == null) {
            return new Formula(id, ordinal, code, skillCode, title, latex, statement, null, null, null, null, null, null);
        }
        return new Formula(id, ordinal, code, skillCode, title, latex, statement, check.kind(), check.tier1(), check.tier2(),
            check.tier1DetailJson(), check.tier2DetailJson(), check.citationPassageId());
    }

    /** Bản chép cho bảng nháp mới: mã và nội dung giữ, kết quả kiểm bỏ (phải kiểm lại khi khóa). */
    Formula copyUnchecked() {
        return unchecked(ordinal, code, skillCode, title, latex, statement);
    }
}
