package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Level4Test {

    @Test
    void docMaDayDuVaMaNganNhuBangMuc4CuaV0() {
        assertThat(Level4.parse("NB")).isEqualTo(Level4.NHAN_BIET);
        assertThat(Level4.parse("TH")).isEqualTo(Level4.THONG_HIEU);
        assertThat(Level4.parse("VD")).isEqualTo(Level4.VAN_DUNG);
        assertThat(Level4.parse("VDC")).isEqualTo(Level4.VAN_DUNG_CAO);
        for (Level4 muc : Level4.values()) {
            assertThat(Level4.parse(muc.name())).isEqualTo(muc);
        }
        assertThatThrownBy(() -> Level4.parse("nb")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Level4.parse("BIET")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void quyDoiBaMucNhuBangMuc3CuaV0VanDungCaoVanLaVanDung() {
        assertThat(Level4.NHAN_BIET.toLevel3()).isEqualTo(Level3.BIET);
        assertThat(Level4.THONG_HIEU.toLevel3()).isEqualTo(Level3.HIEU);
        assertThat(Level4.VAN_DUNG.toLevel3()).isEqualTo(Level3.VAN_DUNG);
        assertThat(Level4.VAN_DUNG_CAO.toLevel3()).isEqualTo(Level3.VAN_DUNG);
    }
}
