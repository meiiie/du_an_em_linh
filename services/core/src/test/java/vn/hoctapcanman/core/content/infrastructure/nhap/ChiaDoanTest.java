package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;

class ChiaDoanTest {

    private static final UUID TL = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    @Test
    void chiaTheoCauSauChamHayChamPhayVaGiuViTri() {
        String vanBan = "  Câu một là $(x^n)' = n x^{n-1}$. Câu hai; câu ba.\nCâu bốn 1.5 không cắt  ";
        List<DocumentPassage> doan = ChiaDoan.theoCau(TL, vanBan);
        assertThat(doan).extracting(DocumentPassage::text)
            .containsExactly("Câu một là $(x^n)' = n x^{n-1}$.", "Câu hai;", "câu ba.", "Câu bốn 1.5 không cắt");
        for (DocumentPassage d : doan) {
            assertThat(vanBan.substring(d.charStart(), d.charEnd())).isEqualTo(d.text());
            assertThat(d.documentId()).isEqualTo(TL);
        }
    }

    @Test
    void vanBanRongThiKhongCoDoan() {
        assertThat(ChiaDoan.theoCau(TL, "   ")).isEmpty();
    }

    @Test
    void trichDanAnhXaVeDoanChuaNoKeCaKhiVatQuaHaiCau() {
        String vanBan = "Đạo hàm dương thì đồng biến. Đổi dấu từ dương sang âm thì cực đại.";
        List<DocumentPassage> doan = ChiaDoan.theoCau(TL, vanBan);
        int viTri = vanBan.indexOf("đồng biến");
        assertThat(ChiaDoan.doanCua(doan, vanBan, viTri, "đồng biến")).contains(List.of(doan.get(0)));
        String vat = "đồng biến. Đổi dấu";
        assertThat(ChiaDoan.doanCua(doan, vanBan, vanBan.indexOf(vat), vat)).contains(doan);
    }

    @Test
    void viTriTheoCodePointCuaPythonDoiSangChiSoJava() {
        // Codex #134 (P2): ký tự ngoài BMP (𝑥 = U+1D465, hai đơn vị UTF-16) đứng trước chữ trích; Python đếm nó là một.
        String vanBan = "Cho \uD835\uDC65 là biến. Đạo hàm dương thì đồng biến.";
        List<DocumentPassage> doan = ChiaDoan.theoCau(TL, vanBan);
        int viTriPython = vanBan.codePointCount(0, vanBan.indexOf("Đạo hàm"));
        assertThat(viTriPython).isEqualTo(vanBan.indexOf("Đạo hàm") - 1);
        assertThat(ChiaDoan.doanCua(doan, vanBan, viTriPython, "Đạo hàm dương")).contains(List.of(doan.get(1)));
        assertThat(ChiaDoan.doanCua(doan, vanBan, vanBan.codePointCount(0, vanBan.length()) + 1, "x")).isEmpty();
    }

    @Test
    void trichKhongDungNguyenVanHayRaNgoaiThiKhongAnhXa() {
        String vanBan = "Đạo hàm dương thì đồng biến.";
        List<DocumentPassage> doan = ChiaDoan.theoCau(TL, vanBan);
        assertThat(ChiaDoan.doanCua(doan, vanBan, 0, "Đạo hàm âm")).isEmpty();
        assertThat(ChiaDoan.doanCua(doan, vanBan, -1, "Đạo")).isEmpty();
        assertThat(ChiaDoan.doanCua(doan, vanBan, vanBan.length() - 2, "biến. Thêm")).isEmpty();
        assertThat(ChiaDoan.doanCua(doan, vanBan, 3, "")).isEmpty();
    }
}
