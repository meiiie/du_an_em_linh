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

/** Phát lại lịch sử thật sự được chọn, không gán mức hiểu độc lập với bài nộp. */
class PhanBoMucHieuSeedTest {
    @SuppressWarnings("unchecked")
    @Test void lichSuCanBangVaHocSinhManhDatNhieuHon() throws Exception {
        var json = JsonMapper.builder().build();
        var root = Path.of("../../data/dev/nguyen-trai");
        List<Map<String,Object>> topics = json.readValue(Files.readString(root.resolve("danh-muc.json")), List.class);
        List<Map<String,Object>> exercises = json.readValue(Files.readString(root.resolve("bai-tap.json")), List.class);
        List<Map<String,Object>> fixtures = json.readValue(Files.readString(root.resolve("bai-lam-lich-su.json")), List.class);
        var config = new BktConfig(1, .12, .2, .1, .65, 3, new BktConfig.Thresholds(.4,.62,.82));
        int passed=0, failed=0, pending=0;
        int[] studentPasses=new int[10];
        for (int grade : List.of(10,11,12)) {
            var chapters = topics.stream().filter(c -> ((Number)c.get("lop")).intValue()==grade)
                .sorted(Comparator.comparing(c -> c.get("ky_nang").toString())).toList();
            int[] spread = new int[4];
            for (int chapter=0; chapter<chapters.size(); chapter++) {
                String code=chapters.get(chapter).get("ma").toString();
                var tasks = new ArrayList<>(exercises.stream().filter(e -> code.equals(e.get("chu_de")))
                    .filter(e -> !"NT-DH12-NB-02".equals(e.get("id")))
                    .filter(e -> "DA_DUYET".equals(((Map<?,?>)e.get("duyet_seed")).get("trang_thai"))).toList());
                tasks.sort(Comparator.<Map<String,Object>,Level4>comparing(e -> level(e.get("muc_do_4").toString()))
                    .thenComparing(e -> e.get("id").toString()));
                for (int student=0; student<10; student++) {
                    var state=MasteryState.initial(UUID.randomUUID(),code,config,Instant.EPOCH);
                    int attempts=0;
                    for (int task=0; task<tasks.size(); task++) {
                        String kind=LichSuNguyenTraiSeeder.outcome(student,chapter,task,tasks.size());
                        String id=tasks.get(task).get("id").toString();
                        assertThat(fixtures.stream().filter(f -> id.equals(f.get("ma")) && kind.equals(f.get("loai"))).count()).isEqualTo(1);
                        assertThat(LichSuNguyenTraiSeeder.outcome(student,chapter,task,tasks.size())).isEqualTo(kind);
                        if (kind.equals("CAN_DUYET")) { pending++; continue; }
                        boolean correct=kind.equals("DUNG");
                        if(correct) { passed++; studentPasses[student]++; } else failed++;
                        attempts++;
                        var after=state.apply(new MasteryState.Evidence(correct,
                            level(tasks.get(task).get("muc_do_4").toString()),null,false),config,Instant.EPOCH).after();
                        state=new MasteryState(after.studentId(),after.skillCode(),(double)(float)after.mastery(),
                            after.level(),after.attempts(),after.stuckCounter(),after.lastErrorCodes(),after.completedAt());
                    }
                    assertThat(state.attempts()).isEqualTo(attempts);
                    if(student==1) assertThat(state.mastery()).isGreaterThan(.82);
                    if(student==9) assertThat(state.level()).isEqualTo(Level4.NHAN_BIET);
                    spread[state.level().ordinal()]++;
                }
            }
            for(int count:spread) assertThat(count).isPositive();
            System.out.println("Seed khối "+grade+": "+java.util.Arrays.toString(spread));
        }
        int total=passed+failed+pending;
        assertThat(total).isEqualTo(620);
        assertThat(passed/(double)total).isBetween(.55,.65);
        assertThat(failed/(double)total).isBetween(.22,.28);
        assertThat(pending/(double)total).isBetween(.08,.20);
        assertThat(studentPasses[1]).isGreaterThan(studentPasses[7]);
        assertThat(studentPasses[7]).isGreaterThan(studentPasses[9]);
        System.out.println("Seed lịch sử: đạt="+passed+", sai="+failed+", chờ="+pending);
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
