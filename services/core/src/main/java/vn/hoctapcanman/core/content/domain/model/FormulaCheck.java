package vn.hoctapcanman.core.content.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kết quả job {@code kiem-dong-cong-thuc} cho một dòng của bảng (contracts/math-v1.md): loại dòng, trạng thái tầng 1 và
 * tầng 2, căn cứ đầy đủ của từng tầng (JSON nguyên như dịch vụ toán trả), đoạn tài liệu được trích dẫn chính và các đoạn
 * trích thêm ({@code trich_dan_them}: dòng định lí cần một đoạn cho mỗi mệnh đề; adapter ghi vào
 * {@code formula_citations}). {@code rowFingerprint} là {@link Formula#contentFingerprint} của đúng dòng đã gửi job:
 * dòng đổi sau khi kiểm thì kết quả này không áp được nữa ({@link FormulaSheet#withCheckResults}).
 */
public record FormulaCheck(
        String rowFingerprint,
        FormulaKind kind,
        CheckStatus tier1,
        CheckStatus tier2,
        @Nullable String tier1DetailJson,
        @Nullable String tier2DetailJson,
        @Nullable UUID citationPassageId,
        List<UUID> extraCitationPassageIds) {

    public FormulaCheck {
        Objects.requireNonNull(rowFingerprint, "rowFingerprint");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tier1, "tier1");
        Objects.requireNonNull(tier2, "tier2");
        Objects.requireNonNull(extraCitationPassageIds, "extraCitationPassageIds");
        Kiem.sha256(rowFingerprint, "Dấu vân tay của dòng đã kiểm");
        if (!tier1.isMachineVerdict() || !tier2.isMachineVerdict()) {
            throw new IllegalArgumentException("Dòng công thức không duyệt riêng (ADR 013)");
        }
        if (tier2 == CheckStatus.DAT && citationPassageId == null) {
            throw new IllegalArgumentException("Tầng 2 DAT phải có đoạn trích dẫn");
        }
        extraCitationPassageIds = List.copyOf(extraCitationPassageIds);
        if (new HashSet<>(extraCitationPassageIds).size() != extraCitationPassageIds.size()) {
            throw new IllegalArgumentException("Đoạn trích thêm ghi hai lần");
        }
    }

    /** Kết quả cho đúng dòng {@code row} (dấu vân tay tính từ nội dung dòng lúc gửi job), không có đoạn trích thêm. */
    public static FormulaCheck of(Formula row, FormulaKind kind, CheckStatus tier1, CheckStatus tier2,
            @Nullable String tier1DetailJson, @Nullable String tier2DetailJson, @Nullable UUID citationPassageId) {
        return of(row, kind, tier1, tier2, tier1DetailJson, tier2DetailJson, citationPassageId, List.of());
    }

    /** Kết quả cho đúng dòng {@code row}, kèm các đoạn trích thêm của tầng 2. */
    public static FormulaCheck of(Formula row, FormulaKind kind, CheckStatus tier1, CheckStatus tier2,
            @Nullable String tier1DetailJson, @Nullable String tier2DetailJson, @Nullable UUID citationPassageId,
            List<UUID> extraCitationPassageIds) {
        return new FormulaCheck(row.contentFingerprint(), kind, tier1, tier2, tier1DetailJson, tier2DetailJson,
            citationPassageId, extraCitationPassageIds);
    }
}
