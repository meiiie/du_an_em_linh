package vn.hoctapcanman.core.classroom.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClassSettingsTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Test
    void macDinhKhongMoLoiGiaiNhaOfflineKhongMayCucBo() {
        ClassSettings s = ClassSettings.macDinh(ClassId.newId(), NOW);
        assertThat(s.revealSolutionAfterSubmit()).isFalse();
        assertThat(s.aiProvider()).isEqualTo("offline");
        assertThat(s.aiAllowLocal()).isFalse();
        assertThat(s.updatedBy()).isNull();
    }

    @Test
    void capNhatGhiNguoiDoiVaThoiDiem() {
        UUID gv = UUID.randomUUID();
        ClassSettings s = ClassSettings.macDinh(ClassId.newId(), NOW).capNhat(true, "openrouter", false, gv, NOW.plusSeconds(60));
        assertThat(s.revealSolutionAfterSubmit()).isTrue();
        assertThat(s.aiProvider()).isEqualTo("openrouter");
        assertThat(s.updatedBy()).isEqualTo(gv);
        assertThat(s.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void maNhaChiLaMaKhongPhaiDiaChi() {
        ClassSettings s = ClassSettings.macDinh(ClassId.newId(), NOW);
        for (String sai : new String[] {"", "OpenAI", "http://127.0.0.1:11434", "a".repeat(33), "nha ai"}) {
            assertThatThrownBy(() -> s.capNhat(false, sai, false, UUID.randomUUID(), NOW))
                .as(sai)
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
