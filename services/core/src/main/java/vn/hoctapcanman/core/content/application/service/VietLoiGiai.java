package vn.hoctapcanman.core.content.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.content.domain.model.Solution;

/**
 * Lời giải viết cho học sinh từ lời giải mẫu, chép {@code loiGiaiHocSinh} của {@code apps/web/lib/loi-giai.ts} (v0): đáp án
 * cuối nếu có chữ; không thì các câu tập xác định, đạo hàm, y′ = 0, y′ không xác định, đồng biến, nghịch biến, cực đại, cực
 * tiểu dựng từ {@code workedSolutionJson}; không câu nào thì rỗng. Tệp vàng do chính hàm của v0 sinh
 * ({@code specs/001-lat-cat-doc/doi-chieu/loi-giai-v0.ts}). Không đọc dữ kiện bảo vệ.
 *
 * <p>Lời giải mẫu lệch kiểu mà v0 khai báo ({@code BaiLam}: chuỗi, mảng chuỗi, đối tượng {@code ket_luan}, hay null) thì rỗng.
 * v0 khi đó in chữ ép kiểu kiểu JavaScript, ném lỗi, hay lặng lẽ bỏ cả phần kết luận ({@code ket_luan} là mảng hay giá trị
 * rỗng như {@code ""}, {@code false}). Core không đưa ra cho học sinh chữ đoán hay lời giải mất kết luận.
 */
public final class VietLoiGiai {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private VietLoiGiai() {}

    public static Optional<String> viet(Solution loiGiai) {
        return viet(loiGiai.workedSolutionJson(), loiGiai.finalAnswer());
    }

    static Optional<String> viet(@Nullable String baiLamJson, @Nullable String dapAnCuoi) {
        if (dapAnCuoi != null && !dapAnCuoi.isEmpty()) {
            return Optional.of(dapAnCuoi);
        }
        if (baiLamJson == null) {
            return Optional.empty();
        }
        JsonNode bl = JSON.readTree(baiLamJson);
        if (!bl.isObject()) {
            return Optional.empty();
        }
        try {
            List<String> cau = cau(bl);
            return cau.isEmpty() ? Optional.empty() : Optional.of(String.join(" ", cau));
        } catch (LechKieu e) {
            return Optional.empty();
        }
    }

    private static List<String> cau(JsonNode bl) {
        List<String> cau = new ArrayList<>();
        String txd = chu(bl.get("TXD"));
        if (txd != null && !txd.isEmpty()) {
            cau.add("Tập xác định: " + txd + ".");
        }
        String daoHam = chu(bl.get("dao_ham"));
        if (daoHam != null && !daoHam.isEmpty()) {
            cau.add("Đạo hàm: " + daoHam + ".");
        }
        List<String> bang0 = mang(bl.get("y_phay_bang_0"));
        if (!bang0.isEmpty()) {
            cau.add("y′ = 0 tại " + String.join(", ", bang0) + ".");
        }
        List<String> khongXd = mang(bl.get("y_phay_khong_xd"));
        if (!khongXd.isEmpty()) {
            cau.add("y′ không xác định tại " + String.join(", ", khongXd) + ".");
        }
        JsonNode kl = bl.get("ket_luan");
        if (kl == null || kl.isNull()) {
            return cau;
        }
        if (!kl.isObject()) {
            throw new LechKieu();
        }
        khoang(cau, kl, "dong_bien", "Đồng biến trên ", "Không đồng biến trên khoảng nào.");
        khoang(cau, kl, "nghich_bien", "Nghịch biến trên ", "Không nghịch biến trên khoảng nào.");
        cucTri(cau, kl, "cuc_dai_x", "gia_tri_cuc_dai", "Cực đại tại ", "Không có cực đại.");
        cucTri(cau, kl, "cuc_tieu_x", "gia_tri_cuc_tieu", "Cực tiểu tại ", "Không có cực tiểu.");
        return cau;
    }

    /** Khoảng có thì nối bằng «và»; khóa có mà rỗng hay null thì câu «không» (v0: {@code "khóa" in kl}). */
    private static void khoang(List<String> cau, JsonNode kl, String khoa, String dau, String khong) {
        List<String> k = mang(kl.get(khoa));
        if (!k.isEmpty()) {
            cau.add(dau + String.join(" và ", k) + ".");
        } else if (kl.has(khoa)) {
            cau.add(khong);
        }
    }

    /** Điểm cực trị kèm giá trị cùng vị trí khi giá trị có chữ, như v0. */
    private static void cucTri(List<String> cau, JsonNode kl, String khoaX, String khoaY, String dau, String khong) {
        List<String> xs = mang(kl.get(khoaX));
        if (xs.isEmpty()) {
            if (kl.has(khoaX)) {
                cau.add(khong);
            }
            return;
        }
        List<String> ys = mang(kl.get(khoaY));
        List<String> diem = new ArrayList<>();
        for (int i = 0; i < xs.size(); i++) {
            String y = i < ys.size() ? ys.get(i) : "";
            diem.add(y.isEmpty() ? "x = " + xs.get(i) : "x = " + xs.get(i) + ", y = " + y);
        }
        cau.add(dau + String.join("; ", diem) + ".");
    }

    private static @Nullable String chu(@Nullable JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (!n.isString()) {
            throw new LechKieu();
        }
        return n.stringValue();
    }

    private static List<String> mang(@Nullable JsonNode n) {
        if (n == null || n.isNull()) {
            return List.of();
        }
        if (!n.isArray()) {
            throw new LechKieu();
        }
        List<String> ra = new ArrayList<>();
        for (JsonNode p : n) {
            if (!p.isString()) {
                throw new LechKieu();
            }
            ra.add(p.stringValue());
        }
        return ra;
    }

    /** Lời giải mẫu lệch kiểu khai báo của v0. */
    private static final class LechKieu extends RuntimeException {
        LechKieu() {
            super(null, null, false, false);
        }
    }
}
