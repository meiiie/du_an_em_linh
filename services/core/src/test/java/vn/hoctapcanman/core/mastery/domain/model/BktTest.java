package vn.hoctapcanman.core.mastery.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BKT và mức sau bài của v0 trên vài điểm tính tay (bảng đầy đủ: {@code DoiChieuBktV0Test} với tệp vàng của v0). */
class BktTest {

    private static final BktConfig V0 = new BktConfig(1, 0.12, 0.2, 0.1, 0.65, 3, new BktConfig.Thresholds(0.4, 0.62, 0.82));
    private static final Instant LUC = Instant.parse("2026-10-06T08:00:00Z");

    @Test
    void bktNhuV0() {
        assertThat(Bkt.next(0.3, true, V0)).isEqualTo(0.6995121951219513);
        assertThat(Bkt.next(0.3, false, V0)).isEqualTo(0.05084745762711865);
        assertThat(Bkt.next(0.5, false, V0)).isEqualTo(0.11111111111111112);
        assertThat(Bkt.next(0.9233158878885195, true, V0)).as("kẹp trên").isEqualTo(0.98);
        assertThat(Bkt.next(0.02, false, V0)).as("kẹp dưới").isEqualTo(0.02);
    }

    @Test
    void mucChiDoiMotNacVaBaiDeKhongDayLen() {
        BktConfig.Thresholds t = V0.thresholds();
        assertThat(Bkt.levelAfter(Level4.NHAN_BIET, 0.95, true, Level4.VAN_DUNG_CAO, t)).isEqualTo(Level4.THONG_HIEU);
        assertThat(Bkt.levelAfter(Level4.THONG_HIEU, 0.95, true, Level4.NHAN_BIET, t)).isEqualTo(Level4.THONG_HIEU);
        assertThat(Bkt.levelAfter(Level4.VAN_DUNG_CAO, 0.98, true, Level4.VAN_DUNG_CAO, t)).isEqualTo(Level4.VAN_DUNG_CAO);
        assertThat(Bkt.levelAfter(Level4.VAN_DUNG, 0.05, false, Level4.VAN_DUNG, t)).isEqualTo(Level4.THONG_HIEU);
        assertThat(Bkt.levelAfter(Level4.VAN_DUNG, 0.62, false, Level4.VAN_DUNG, t)).as("đúng ngưỡng thì giữ").isEqualTo(Level4.VAN_DUNG);
    }

    @Test
    void nghiDoanMoGiuMucHieuNhungGhiMaLoi() {
        MasteryState s = new MasteryState(UUID.randomUUID(), "T12.DH.03", 0.05, Level4.NHAN_BIET, 2, 2,
            List.of("ERR.DH.01", "ERR.DH.02", "ERR.DH.03", "ERR.DH.04", "ERR.DH.05", "ERR.DH.06", "ERR.DH.07", "ERR.DH.08"), null);

        MasteryState.Update nghi = s.apply(new MasteryState.Evidence(false, Level4.THONG_HIEU, "ERR.DH.09", true), V0, LUC);
        MasteryState.Update sai = s.apply(new MasteryState.Evidence(false, Level4.THONG_HIEU, "ERR.DH.02", false), V0, LUC);

        assertThat(nghi.after()).isEqualTo(new MasteryState(s.studentId(), "T12.DH.03", 0.05, Level4.NHAN_BIET, 2, 2,
            List.of("ERR.DH.02", "ERR.DH.03", "ERR.DH.04", "ERR.DH.05", "ERR.DH.06", "ERR.DH.07", "ERR.DH.08", "ERR.DH.09"), null));
        assertThat(nghi.delta()).isZero();
        assertThat(nghi.stuckAlert()).isFalse();
        assertThat(sai.after().stuckCounter()).isEqualTo(3);
        assertThat(sai.after().lastErrorCodes()).as("mã đã có giữ chỗ cũ").isEqualTo(s.lastErrorCodes());
        assertThat(sai.stuckAlert()).isTrue();
    }
}
