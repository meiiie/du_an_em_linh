package vn.hoctapcanman.core.mastery.domain.model;

/**
 * Tham số BKT của v0 ({@code BktConfig} của {@code learning.ts}, dòng {@code bkt} của {@code mastery_config}): học được
 * {@code pT}, đoán đúng {@code pG}, lỡ sai {@code pS}; mã lỗi chỉ dời kỹ năng khi độ tin cậy từ {@code errorCodeConfidence};
 * kẹt từ {@code stuckAfter} lượt sai liền; ngưỡng mastery của ba mức trên.
 */
public record BktConfig(int version, double pT, double pG, double pS, double errorCodeConfidence, int stuckAfter, Thresholds thresholds) {

    /** Ngưỡng {@code nguong_muc} của v0: mastery từ ngưỡng nào thì ở mức đó. */
    public record Thresholds(double thongHieu, double vanDung, double vanDungCao) {

        /** {@code mucFromMastery} của {@code levels.ts}. */
        Level4 levelOf(double mastery) {
            if (mastery >= vanDungCao) {
                return Level4.VAN_DUNG_CAO;
            }
            if (mastery >= vanDung) {
                return Level4.VAN_DUNG;
            }
            if (mastery >= thongHieu) {
                return Level4.THONG_HIEU;
            }
            return Level4.NHAN_BIET;
        }
    }
}
