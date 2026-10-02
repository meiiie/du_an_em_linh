package vn.hoctoanai.core.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void chuanHoaChuThuongVaKhoangTrang() {
        assertThat(new Email("  HS.An@Demo.Local ").value()).isEqualTo("hs.an@demo.local");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "khong-co-a-cong", "a@b", "a b@c.d", "@c.d"})
    void tuChoiDangSai(String raw) {
        assertThatThrownBy(() -> new Email(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tuChoiQuaDai() {
        assertThatThrownBy(() -> new Email("a".repeat(250) + "@b.vn")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void khongInEmailRaLog() {
        assertThat(new Email("hs.an@demo.local").toString()).doesNotContain("hs.an");
    }
}
