package vn.hoctapcanman.core.practice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Nghi đoán mò như {@code nghiDoanMo} của v0: một ô bị đổi từ ngưỡng lần trở lên. */
class NghiDoanMoTest {

    private static final Instant LUC = Instant.parse("2026-10-05T08:00:00Z");

    @Test
    void duoiNguongKhongNghi() {
        List<InputEvent> suKien = doi("DAU_YPHAY", 1, 3);
        suKien.addAll(doi("DAU_YPHAY", 3, 3));
        assertThat(NghiDoanMo.lyDo(suKien, 4)).isEmpty();
    }

    @Test
    void oDauTienChamNguongTheoThuTuXuatHien() {
        List<InputEvent> suKien = doi("DAU_YPHAY", 3, 5);
        suKien.addAll(0, doi("DAU_YPHAY", 1, 4));
        assertThat(NghiDoanMo.lyDo(suKien, 4)).contains("Ô DAU_YPHAY:1 bị đổi 4 lần trước khi nộp (ngưỡng 4).");
    }

    @Test
    void suKienKhongGanOKhongTinh() {
        List<InputEvent> suKien = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            suKien.add(new InputEvent("B.DH.DAOHAM", null, null, null, "3x^2" + i, LUC));
        }
        assertThat(NghiDoanMo.lyDo(suKien, 4)).isEmpty();
    }

    @Test
    void nguongPhaiDuong() {
        assertThatThrownBy(() -> NghiDoanMo.lyDo(List.of(), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static List<InputEvent> doi(String hang, int k, int soLan) {
        List<InputEvent> suKien = new ArrayList<>();
        for (int i = 0; i < soLan; i++) {
            suKien.add(new InputEvent("B.DH.XETDAU", hang, k, i % 2 == 0 ? "+" : "-", i % 2 == 0 ? "-" : "+", LUC.plusSeconds(i)));
        }
        return suKien;
    }
}
