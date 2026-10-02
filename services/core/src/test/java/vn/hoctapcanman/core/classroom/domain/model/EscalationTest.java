package vn.hoctapcanman.core.classroom.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EscalationTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Test
    void canhBaoMoiChuaXuLy() {
        Escalation e = Escalation.open(ClassId.newId(), UUID.randomUUID(), EscalationKind.NHO_GV, "T12.DH.03", "B12-01",
            "B.DH.XETDAU", "  Em nhờ thầy cô ở bước xét dấu. ", NOW);
        assertThat(e.isOpen()).isTrue();
        assertThat(e.reason()).isEqualTo("Em nhờ thầy cô ở bước xét dấu.");
    }

    @Test
    void tuChoiMaVaLyDoKhongHopLe() {
        ClassId lop = ClassId.newId();
        UUID hs = UUID.randomUUID();
        assertThatThrownBy(() -> Escalation.open(lop, hs, EscalationKind.KET, "", null, null, "Kẹt", NOW))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Escalation.open(lop, hs, EscalationKind.KET, "T12 DH", null, null, "Kẹt", NOW))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Escalation.open(lop, hs, EscalationKind.KET, "T12.DH.03", "x".repeat(65), null, "Kẹt", NOW))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Escalation.open(lop, hs, EscalationKind.KET, "T12.DH.03", null, null, " ", NOW))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Escalation.open(lop, hs, EscalationKind.KET, "T12.DH.03", null, null, "x".repeat(501), NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coNguoiXuLyThiPhaiCoThoiDiem() {
        assertThatThrownBy(() -> new Escalation(UUID.randomUUID(), ClassId.newId(), UUID.randomUUID(), EscalationKind.KET,
            "T12.DH.03", null, null, "Kẹt", NOW, null, UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
