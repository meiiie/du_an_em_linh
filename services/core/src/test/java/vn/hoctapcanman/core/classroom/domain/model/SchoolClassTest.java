package vn.hoctapcanman.core.classroom.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SchoolClassTest {

    private static final Instant NOW = Instant.parse("2026-10-02T08:00:00Z");

    @Test
    void taoLopHopLe() {
        SchoolClass lop = SchoolClass.create("  12A1 thử ", 12, "2026-2027", NOW);
        assertThat(lop.name()).isEqualTo("12A1 thử");
        assertThat(lop.grade()).isEqualTo(12);
    }

    @Test
    void tuChoiTenKhoiNamHocSai() {
        assertThatThrownBy(() -> SchoolClass.create(" ", 12, "2026-2027", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SchoolClass.create("x".repeat(61), 12, "2026-2027", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SchoolClass.create("12A1", 13, "2026-2027", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SchoolClass.create("12A1", 0, "2026-2027", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SchoolClass.create("12A1", 12, "2026-2028", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SchoolClass.create("12A1", 12, "2026", NOW)).isInstanceOf(IllegalArgumentException.class);
    }
}
