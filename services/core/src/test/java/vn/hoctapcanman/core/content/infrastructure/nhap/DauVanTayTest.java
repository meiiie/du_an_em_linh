package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.infrastructure.nhap.NhapNoiDungChung.BaiNhap;

/**
 * Codex #135 (P2): dấu vân tay nội dung đổi khi bất kỳ trường nào nó phủ đổi (data-model §problems: đề, LaTeX, hàm, dạng trả
 * lời, bước bắt đầu, lời giải, dữ kiện bảo vệ, thang gợi ý). Bỏ sót một trường thì sửa riêng trường đó không làm V5 coi lượt
 * kiểm cũ là cũ, và học sinh thấy nội dung chưa kiểm.
 */
class DauVanTayTest {

    private static final BaiNhap GOC = goc();

    @Test
    void cungNoiDungCungDauVanTay() {
        // Hai bài dựng riêng (đối tượng khác nhau), cùng nội dung; mã bài không thuộc nội dung.
        BaiNhap khac = goc();
        BaiNhap doiMa = new BaiNhap("DH12-02", khac.kyNang(), khac.kyNangPhu(), khac.muc4(), khac.muc3(), khac.bloom(), khac.deBai(),
            khac.latex(), khac.ham(), khac.dangTraLoi(), khac.buocBatDau(), khac.nguonBai(), khac.baiLam(), khac.suKien(), khac.thangGoiY());
        assertThat(NhapNoiDungChung.dauVanTay(doiMa)).isEqualTo(NhapNoiDungChung.dauVanTay(GOC)).matches("[0-9a-f]{64}");
    }

    @Test
    void moiTruongDuocPhuDeuLamDoiDauVanTay() {
        Map<String, UnaryOperator<BaiNhap>> sua = new LinkedHashMap<>();
        sua.put("đề", b -> ban(b, "Xét tính đơn điệu của y = x^3 - 3x^2 + 3.", b.latex(), b.ham(), b.dangTraLoi(), b.buocBatDau(), b.baiLam(),
            b.suKien(), b.thangGoiY()));
        sua.put("LaTeX", b -> ban(b, b.deBai(), "y = x^3 - 3x^2 + 3", b.ham(), b.dangTraLoi(), b.buocBatDau(), b.baiLam(), b.suKien(),
            b.thangGoiY()));
        sua.put("hàm", b -> ban(b, b.deBai(), b.latex(), "x**3 - 3*x**2 + 3", b.dangTraLoi(), b.buocBatDau(), b.baiLam(), b.suKien(),
            b.thangGoiY()));
        sua.put("dạng trả lời", b -> ban(b, b.deBai(), b.latex(), b.ham(), "TRAC_NGHIEM", b.buocBatDau(), b.baiLam(), b.suKien(),
            b.thangGoiY()));
        sua.put("bước bắt đầu", b -> ban(b, b.deBai(), b.latex(), b.ham(), b.dangTraLoi(), "B.DH.NGHIEM", b.baiLam(), b.suKien(),
            b.thangGoiY()));
        sua.put("lời giải", b -> ban(b, b.deBai(), b.latex(), b.ham(), b.dangTraLoi(), b.buocBatDau(), Map.of("buoc", List.of("D = R\\{0}")),
            b.suKien(), b.thangGoiY()));
        sua.put("dữ kiện bảo vệ", b -> ban(b, b.deBai(), b.latex(), b.ham(), b.dangTraLoi(), b.buocBatDau(), b.baiLam(),
            List.of(Map.of("loai", "NGHIEM", "gia_tri", "2")), b.thangGoiY()));
        sua.put("thang gợi ý", b -> ban(b, b.deBai(), b.latex(), b.ham(), b.dangTraLoi(), b.buocBatDau(), b.baiLam(), b.suKien(),
            List.of(khoi("B.DH.DAOHAM", "Đạo hàm của x^2?"))));
        String goc = NhapNoiDungChung.dauVanTay(GOC);
        sua.forEach((truong, doi) -> assertThat(NhapNoiDungChung.dauVanTay(doi.apply(GOC))).as("đổi %s", truong).isNotEqualTo(goc));
    }

    private static BaiNhap goc() {
        return new BaiNhap("DH12-01", "T12.DH.02", List.of(), Level4.THONG_HIEU, null, null, "Xét tính đơn điệu của y = x^3 - 3x^2 + 2.",
            "y = x^3 - 3x^2 + 2", "x**3 - 3*x**2 + 2", "TU_LUAN_5_BUOC", null, "SUPHAM", Map.of("buoc", List.of("D = R")),
            List.of(Map.of("loai", "NGHIEM", "gia_tri", "0")), List.of(khoi("B.DH.DAOHAM", "Đạo hàm của x^3?")));
    }

    private static BaiNhap ban(BaiNhap b, String deBai, String latex, @Nullable String ham, String dang, @Nullable String buocBatDau,
            @Nullable Object baiLam, Object suKien, List<Map<String, @Nullable Object>> thangGoiY) {
        return new BaiNhap(b.ma(), b.kyNang(), b.kyNangPhu(), b.muc4(), b.muc3(), b.bloom(), deBai, latex, ham, dang, buocBatDau, b.nguonBai(),
            baiLam, suKien, thangGoiY);
    }

    private static Map<String, @Nullable Object> khoi(String buoc, String noiDung) {
        Map<String, @Nullable Object> k = new LinkedHashMap<>();
        k.put("ma_buoc", buoc);
        k.put("cac_cap", List.of(Map.of("cap", 1, "noi_dung", noiDung)));
        return k;
    }
}
