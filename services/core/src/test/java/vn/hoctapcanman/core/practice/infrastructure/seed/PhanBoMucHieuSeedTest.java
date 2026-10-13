package vn.hoctapcanman.core.practice.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.mastery.domain.model.BktConfig;
import vn.hoctapcanman.core.mastery.domain.model.Level4;
import vn.hoctapcanman.core.mastery.domain.model.MasteryState;

/** Phát lại kế hoạch seed bằng BKT thật, gồm làm tròn PostgreSQL real giữa các lượt. */
class PhanBoMucHieuSeedTest {
    @SuppressWarnings("unchecked")
    @Test void lichSuTheoKhoiTaoDuBonMuc() throws Exception {
        var json = JsonMapper.builder().build();
        var root = Path.of("../../data/dev/nguyen-trai");
        List<Map<String,Object>> topics = json.readValue(Files.readString(root.resolve("danh-muc.json")), List.class);
        List<Map<String,Object>> exercises = json.readValue(Files.readString(root.resolve("bai-tap.json")), List.class);
        var config = new BktConfig(1, .12, .2, .1, .65, 3, new BktConfig.Thresholds(.4,.62,.82));
        for (int grade : List.of(10,11,12)) {
            var chapters = topics.stream().filter(c -> ((Number)c.get("lop")).intValue()==grade)
                .sorted(Comparator.comparing(c -> c.get("ky_nang").toString())).toList();
            int[] spread = new int[4];
            for (int chapter=0; chapter<chapters.size(); chapter++) {
                String code=chapters.get(chapter).get("ma").toString();
                var tasks = new ArrayList<>(exercises.stream().filter(e -> code.equals(e.get("chu_de")))
                    .filter(e -> !"NT-DH12-NB-02".equals(e.get("id")))
                    .filter(e -> "DA_DUYET".equals(((Map<?,?>)e.get("duyet_seed")).get("trang_thai"))).toList());
                tasks.sort(Comparator.comparing(e -> level(e.get("muc_do_4").toString())));
                for (int student=0; student<10; student++) {
                    var state=MasteryState.initial(UUID.randomUUID(),code,config,Instant.EPOCH);
                    int successes=LichSuNguyenTraiSeeder.successes(student,chapter);
                    for (int task=0; task<tasks.size(); task++) {
                        if (student==0 && tasks.size()>3 && task==0) continue; // chưa kiểm được
                        boolean correct=task>=tasks.size()-successes;
                        var after=state.apply(new MasteryState.Evidence(correct,
                            level(tasks.get(task).get("muc_do_4").toString()),null,false),config,Instant.EPOCH).after();
                        state=new MasteryState(after.studentId(),after.skillCode(),(double)(float)after.mastery(),
                            after.level(),after.attempts(),after.stuckCounter(),after.lastErrorCodes(),after.completedAt());
                    }
                    spread[state.level().ordinal()]++;
                }
            }
            assertThat(spread).containsExactly(grade==11 ? 18 : 14, grade==11 ? 15 : 12, grade==11 ? 12 : 10, grade==11 ? 5 : 4);
        }
    }
    private static Level4 level(String value) {
        return switch(value) {
            case "NB" -> Level4.NHAN_BIET;
            case "TH" -> Level4.THONG_HIEU;
            case "VD" -> Level4.VAN_DUNG;
            default -> Level4.VAN_DUNG_CAO;
        };
    }
}
