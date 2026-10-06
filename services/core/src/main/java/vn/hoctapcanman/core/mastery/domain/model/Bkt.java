package vn.hoctapcanman.core.mastery.domain.model;

/**
 * Hai hàm thuần của {@code apps/web/lib/learning.ts}, chép từng phép tính theo đúng thứ tự (số thực IEEE như JavaScript):
 * {@code bktNext} và {@code mucSauBai}. {@code DoiChieuBktV0Test} so với tệp vàng do chính mã v0 sinh.
 */
public final class Bkt {

    /** Mastery của kỹ năng chưa có lượt nào ({@code ?? 0.3} của v0). */
    public static final double INITIAL = 0.3;

    private Bkt() {}

    /** {@code bktNext}: hậu nghiệm sau một lượt đúng hay sai, kẹp vào [0,02; 0,98]. */
    public static double next(double p, boolean correct, BktConfig c) {
        if (correct) {
            double known = (p * (1 - c.pS())) / (p * (1 - c.pS()) + (1 - p) * c.pG());
            return clamp(known + (1 - known) * c.pT());
        }
        double mau = p * c.pS() + (1 - p) * (1 - c.pG());
        // `mau || 1` của v0: mẫu 0 (hay NaN) thì chia cho 1.
        return clamp((p * c.pS()) / (mau == 0 || Double.isNaN(mau) ? 1 : mau));
    }

    private static double clamp(double n) {
        if (Double.isNaN(n)) {
            return INITIAL;
        }
        return Math.min(0.98, Math.max(0.02, n));
    }

    /**
     * {@code mucSauBai}: đúng thì lên tối đa một nấc, chỉ khi mastery đã vượt mức hiện tại và bài không dễ hơn mức hiện tại;
     * sai thì xuống tối đa một nấc khi mastery đã dưới mức hiện tại.
     */
    public static Level4 levelAfter(Level4 current, double mastery, boolean correct, Level4 problemLevel, BktConfig.Thresholds t) {
        int theoMastery = t.levelOf(mastery).ordinal();
        if (correct) {
            return theoMastery > current.ordinal() && problemLevel.ordinal() >= current.ordinal() ? current.up() : current;
        }
        return theoMastery < current.ordinal() ? current.down() : current;
    }
}
