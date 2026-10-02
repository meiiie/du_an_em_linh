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
        assertThatThrownBy(() -> duyet("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> duyet("x".repeat(1001))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ghiChuKhongCoKyTuDieuKhienHayDinhDang() {
        // U+202E đảo chiều (định dạng), NUL và ESC (điều khiển)
        for (char kyTu : new char[] {(char) 0x202E, (char) 0x00, (char) 0x1B}) {
            assertThatThrownBy(() -> duyet("Đúng" + kyTu + "rồi")).as("U+%04X", (int) kyTu).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void toStringKhongInGhiChu() {
        assertThat(duyet("Bí mật của thầy cô").toString()).doesNotContain("Bí mật");
    }
}
