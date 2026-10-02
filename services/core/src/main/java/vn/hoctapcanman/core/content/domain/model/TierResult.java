package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Căn cứ của một tầng trong lượt kiểm (FR-004): kết quả máy, loại kết quả và bước sai, trích đoạn tài liệu có vị trí,
 * dòng công thức. JSON ({@code wrongStepsJson}, {@code citationJson}, {@code rawJson}) giữ nguyên như dịch vụ toán trả.
 */
public record TierResult(
        int tier,
        CheckStatus status,
        @Nullable String resultType,
        @Nullable String wrongStepsJson,
        @Nullable String errorCode,
        @Nullable Double confidence,
        @Nullable String reason,
        @Nullable String citationJson,
        @Nullable String rawJson) {

    public TierResult {
        Objects.requireNonNull(status, "status");
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Tầng ngoài 1–3");
        }
        if (!status.isMachineVerdict()) {
            throw new IllegalArgumentException("Một tầng không mang GV_DUYET");
        }
        Kiem.maNeuCo(resultType, 32, "loại kết quả");
        Kiem.maNeuCo(errorCode, 32, "lỗi");
        if (confidence != null && !(confidence >= 0 && confidence <= 1)) {
            throw new IllegalArgumentException("Độ tin cậy ngoài [0, 1]");
        }
    }

    public static TierResult of(int tier, CheckStatus status) {
        return new TierResult(tier, status, null, null, null, null, null, null, null);
    }

    /** Không in căn cứ (raw, lý do, trích dẫn, bước sai) vào log: tầng 1 là kết quả chấm lời giải chuẩn (FR-006). */
    @Override
    public String toString() {
        return "TierResult[tier=" + tier + ", status=" + status + "]";
    }
}
