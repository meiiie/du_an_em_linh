package vn.hoctapcanman.core.content.domain.model;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** Kiểm dùng chung của model nội dung, khớp các ràng buộc CHECK của {@code V4__content.sql}. */
final class Kiem {

    /** Mã bài, kỹ năng, bước, mã lỗi, dòng công thức: chữ, số, dấu chấm, gạch dưới, gạch nối. */
    private static final Pattern MA = Pattern.compile("[A-Za-z0-9._-]+");
    /** Mã viết hoa của v0 ({@code TU_LUAN_5_BUOC}, {@code SUPHAM}…). */
    private static final Pattern MA_HOA = Pattern.compile("[A-Z][A-Z0-9_]{0,31}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private Kiem() {}

    static String ma(String ma, int doDaiToiDa, String ten) {
        if (ma.length() > doDaiToiDa || !MA.matcher(ma).matches()) {
            throw new IllegalArgumentException("Mã " + ten + " không hợp lệ");
        }
        return ma;
    }

    static @Nullable String maNeuCo(@Nullable String ma, int doDaiToiDa, String ten) {
        return ma == null ? null : ma(ma, doDaiToiDa, ten);
    }

    static String maHoa(String ma, String ten) {
        if (!MA_HOA.matcher(ma).matches()) {
            throw new IllegalArgumentException("Mã " + ten + " không hợp lệ");
        }
        return ma;
    }

    /** SHA-256 viết thường dạng hex, 64 ký tự. */
    static String sha256(String hex, String ten) {
        if (!SHA256.matcher(hex).matches()) {
            throw new IllegalArgumentException(ten + " không phải SHA-256 hex");
        }
        return hex;
    }

    static String khongTrong(String chu, String ten) {
        if (chu.isBlank()) {
            throw new IllegalArgumentException(ten + " trống");
        }
        return chu;
    }

    static String toiDa(String chu, int doDaiToiDa, String ten) {
        if (chu.length() > doDaiToiDa) {
            throw new IllegalArgumentException(ten + " quá dài");
        }
        return chu;
    }
}
