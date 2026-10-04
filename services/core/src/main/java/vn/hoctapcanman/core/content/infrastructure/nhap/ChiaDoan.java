package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;

/**
 * Đoạn của tài liệu văn bản (T012b): mỗi câu một đoạn, cắt sau dấu «.» hay «;» và khoảng trắng, đúng cách test của
 * {@code services/math} chia tài liệu khi gửi job khóa bảng ({@code tests/test_dong_cong_thuc.py}). Đoạn giữ vị trí ký tự
 * trong văn bản gốc, để trích dẫn của {@code /v1/verify} (vị trí và chữ trích trong văn bản) ánh xạ được về đoạn.
 */
final class ChiaDoan {

    private static final Pattern RANH_GIOI = Pattern.compile("(?<=[.;])\\s+");

    private ChiaDoan() {}

    /** Các câu của {@code vanBan}, theo thứ tự, bỏ khoảng trắng hai đầu; câu rỗng bị bỏ. */
    static List<DocumentPassage> theoCau(UUID taiLieu, String vanBan) {
        List<DocumentPassage> doan = new ArrayList<>();
        Matcher m = RANH_GIOI.matcher(vanBan);
        int dau = 0;
        while (m.find()) {
            them(doan, taiLieu, vanBan, dau, m.start());
            dau = m.end();
        }
        them(doan, taiLieu, vanBan, dau, vanBan.length());
        return doan;
    }

    private static void them(List<DocumentPassage> doan, UUID taiLieu, String vanBan, int tu, int den) {
        int a = tu;
        int b = den;
        while (a < b && Character.isWhitespace(vanBan.charAt(a))) {
            a++;
        }
        while (b > a && Character.isWhitespace(vanBan.charAt(b - 1))) {
            b--;
        }
        if (a < b) {
            doan.add(DocumentPassage.of(taiLieu, null, a, vanBan.substring(a, b)));
        }
    }

    /**
     * Các đoạn chứa chữ trích {@code trich} ở vị trí {@code viTriCodePoint} của {@code vanBan}, theo thứ tự. Vị trí do dịch
     * vụ toán (Python) trả, tính theo code point trên văn bản đã chuẩn hóa NFC; nơi gọi lưu và gửi văn bản ở dạng NFC
     * ({@code NhapTheoLop}), ở đây đổi code point sang chỉ số UTF-16 của Java trước khi so, để ký tự ngoài BMP (𝑥, emoji)
     * đứng trước không làm lệch. Rỗng (không ánh xạ) khi chữ ở vị trí đó không đúng nguyên văn chữ trích hay vị trí ra ngoài
     * văn bản: không đoán căn cứ.
     */
    static Optional<List<DocumentPassage>> doanCua(List<DocumentPassage> doan, String vanBan, int viTriCodePoint, String trich) {
        if (trich.isEmpty() || viTriCodePoint < 0 || viTriCodePoint > vanBan.codePointCount(0, vanBan.length())) {
            return Optional.empty();
        }
        int viTri = vanBan.offsetByCodePoints(0, viTriCodePoint);
        int het = viTri + trich.length();
        if (het > vanBan.length() || !vanBan.startsWith(trich, viTri)) {
            return Optional.empty();
        }
        Set<DocumentPassage> trung = new LinkedHashSet<>();
        for (DocumentPassage d : doan) {
            if (d.charStart() < het && viTri < d.charEnd()) {
                trung.add(d);
            }
        }
        return trung.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(trung));
    }
}
