package vn.hoctapcanman.core.practice.domain.model;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Kết quả chấm. Ba mã đầu là {@code ket_qua} của {@code /v1/grade} như v0; {@link #KHONG_CHAM_DUOC} là của core: dịch vụ
 * toán lỗi, hết giờ hay trả phong bì lỗi (FR-009), không mang phán quyết nào.
 */
public enum GradeStatus {
    DAT,
    SAI,
    KHONG_KIEM_DUOC,
    KHONG_CHAM_DUOC;

    /** Mã {@code ket_qua} của dịch vụ toán; mã lạ là phản hồi hỏng: rỗng, nơi gọi coi như không chấm được. */
    public static Optional<GradeStatus> tuDichVuToan(@Nullable Object maKetQua) {
        for (GradeStatus s : values()) {
            if (s != KHONG_CHAM_DUOC && s.name().equals(maKetQua)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
