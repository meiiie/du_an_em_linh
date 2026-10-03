package vn.hoctapcanman.core.content.domain.model;

/**
 * Kết quả kiểm của cổng 3 tầng, giữ mã của v0 ({@code services/math/app/verify.py}). {@link #GV_DUYET} chỉ có ở trạng
 * thái tổng của lượt kiểm bài sau khi giáo viên duyệt; một tầng không bao giờ mang {@link #GV_DUYET}.
 */
public enum CheckStatus {
    DAT,
    SAI,
    KHONG_KIEM_DUOC,
    GV_DUYET;

    /** Là kết quả máy của một tầng (không phải {@link #GV_DUYET}). */
    public boolean isMachineVerdict() {
        return this != GV_DUYET;
    }
}
