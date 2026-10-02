package vn.hoctapcanman.core.content.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kết quả job {@code kiem-dong-cong-thuc} cho một dòng của bảng (contracts/math-v1.md): loại dòng, trạng thái tầng 1 và
 * tầng 2, căn cứ đầy đủ của từng tầng (JSON nguyên như dịch vụ toán trả), và đoạn tài liệu được trích dẫn chính.
 * {@code rowFingerprint} là {@link FormulaSheet#fingerprintOf} của đúng dòng đã gửi job: dòng đổi sau khi kiểm thì kết
 * quả này không áp được nữa ({@link FormulaSheet#withCheckResults}).
 */
public record FormulaCheck(
        String rowFingerprint,
        FormulaKind kind,
        CheckStatus tier1,
        CheckStatus tier2,
        @Nullable String tier1DetailJson,
        @Nullable String tier2DetailJson,
        @Nullable UUID citationPassageId) {

    public FormulaCheck {
        Objects.requireNonNull(rowFingerprint, "rowFingerprint");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tier1, "tier1");
        Objects.requireNonNull(tier2, "tier2");
        Kiem.sha256(rowFingerprint, "Dấu vân tay của dòng đã kiểm");
        if (!tier1.isMachineVerdict() || !tier2.isMachineVerdict()) {
            throw new IllegalArgumentException("Dòng công thức không duyệt riêng (ADR 013)");
        }
        if (tier2 == CheckStatus.DAT && citationPassageId == null) {
            throw new IllegalArgumentException("Tầng 2 DAT phải có đoạn trích dẫn");
        }
    }

    /** Kết quả cho đúng dòng {@code row} (dấu vân tay tính từ nội dung dòng lúc gửi job). */
    public static FormulaCheck of(Formula row, FormulaKind kind, CheckStatus tier1, CheckStatus tier2,
            @Nullable String tier1DetailJson, @Nullable String tier2DetailJson, @Nullable UUID citationPassageId) {
        return new FormulaCheck(FormulaSheet.fingerprintOf(List.of(row)), kind, tier1, tier2, tier1DetailJson,
            tier2DetailJson, citationPassageId);
    }
}
