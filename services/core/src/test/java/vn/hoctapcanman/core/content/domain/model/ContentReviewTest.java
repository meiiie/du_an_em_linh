package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentReviewTest {

    private static ContentReview duyet(String ghiChu) {
        return new ContentReview(UUID.randomUUID(), UUID.randomUUID(), Mau.BAM, Mau.GV, ghiChu, Mau.LUC);
    }

    @Test
    void ghiChuBatBuocCatKhoangTrangChoNhieuDong() {
        assertThat(duyet("  Đã xem.\nĐúng với bảng.  ").note()).isEqualTo("Đã xem.\nĐúng với bảng.");
        // Xuống dòng kiểu Windows (CRLF) thành \n, không bị coi là ký tự điều khiển.
        assertThat(duyet("Đã xem.\r\nĐúng.").note()).isEqualTo("Đã xem.\nĐúng.");
        assertThatThrownBy(() -> duyet("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> duyet("x".repeat(1001))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ghiChuChiCoKhoangTrangKhongNgatVanLaTrong() {
        // U+00A0, U+2007, U+202F: isBlank() của Java bỏ sót.
        for (char kyTu : new char[] {(char) 0x00A0, (char) 0x2007, (char) 0x202F}) {
            String ghiChu = String.valueOf(kyTu).repeat(3);
            assertThatThrownBy(() -> duyet(ghiChu)).as("U+%04X", (int) kyTu).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void ghiChuKhongCoKyTuDieuKhienDinhDangHaySurrogateLe() {
        // U+202E đảo chiều (định dạng), NUL và ESC (điều khiển), CR đứng riêng, surrogate lẻ
        for (char kyTu : new char[] {(char) 0x202E, (char) 0x00, (char) 0x1B, (char) 0x0D, (char) 0xD800}) {
            assertThatThrownBy(() -> duyet("Đúng" + kyTu + "rồi")).as("U+%04X", (int) kyTu).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void toStringKhongInGhiChu() {
        assertThat(duyet("Bí mật của thầy cô").toString()).doesNotContain("Bí mật");
    }
}
