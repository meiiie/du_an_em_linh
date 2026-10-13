package vn.hoctapcanman.core.workspace.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item;

class WorkspaceDeadlinesTest {
    private final ReadDeadlines store = new ReadDeadlines();

    private static class ReadDeadlines extends WorkspaceStoreAdapter {
        ReadDeadlines() {
            super(mock(JdbcClient.class),Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"),ZoneOffset.UTC));
        }
        List<Item> read(List<Item> rows) { return upcomingDeadlines(rows); }
    }

    @Test void loaiHanCuVaKhongHanNhungGiuHanDungLucHienTai() {
        assertThat(store.read(List.of(row("cu","2026-10-13T09:59:59.999Z"),row("trong",""),
            row("ngay","2026-10-13T10:00:00Z"),row("sau","2026-10-14T10:00:00Z"))))
            .extracting(Item::id).containsExactly("ngay","sau");
        assertThat(store.read(List.of())).isEmpty();
    }

    @Test void sapXepTheoThoiDiemKeCaKhiDoChinhXacChuoiKhacNhau() {
        assertThat(store.read(List.of(row("sau","2026-10-13T10:00:01Z"),
            row("le","2026-10-13T10:00:00.500Z"),row("ngay","2026-10-13T10:00:00Z"))))
            .extracting(Item::id).containsExactly("ngay","le","sau");
    }

    private Item row(String id,String due) {
        return new Item(id,"Ôn tập","","ON_TAP",Map.of("dueAt",due));
    }
}
