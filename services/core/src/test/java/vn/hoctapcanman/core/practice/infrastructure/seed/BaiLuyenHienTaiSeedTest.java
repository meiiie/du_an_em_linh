package vn.hoctapcanman.core.practice.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class BaiLuyenHienTaiSeedTest {
    @SuppressWarnings("unchecked")
    @Test void moiKhoiCoHaiBaiOnCoBuocKhongNamTrongLichSuHayKiemTra() throws Exception {
        var root = Path.of("../../data/dev/nguyen-trai");
        var json = JsonMapper.builder().build();
        List<Map<String,Object>> exercises = json.readValue(Files.readString(root.resolve("bai-tap.json")), List.class);
        List<Map<String,Object>> skills = json.readValue(Files.readString(root.resolve("ky-nang.json")), List.class);
        List<Map<String,Object>> steps = json.readValue(Files.readString(root.resolve("khung-buoc.json")), List.class);
        List<Map<String,Object>> history = json.readValue(Files.readString(root.resolve("bai-lam-lich-su.json")), List.class);
        for (int grade : List.of(10,11,12)) {
            var codes = skills.stream().filter(s -> ((Number)s.get("lop")).intValue() == grade).map(s -> s.get("ma")).toList();
            var practice = exercises.stream().filter(e -> Boolean.TRUE.equals(e.get("luyen_hien_tai_seed")) && codes.contains(e.get("ky_nang_chinh"))).toList();
            assertThat(practice).hasSize(2);
            for (var exercise : practice) {
                assertThat(exercise.get("kiem_tra_seed")).isNotEqualTo(true);
                assertThat(history.stream().anyMatch(h -> h.get("ma").equals(exercise.get("id")))).isFalse();
                assertThat(steps.stream().filter(s -> exercise.get("ky_nang_chinh").equals(s.get("skillCode"))).count()).isPositive();
                assertThat((List<?>)exercise.get("thang_goi_y")).isNotEmpty();
            }
        }
    }
}
