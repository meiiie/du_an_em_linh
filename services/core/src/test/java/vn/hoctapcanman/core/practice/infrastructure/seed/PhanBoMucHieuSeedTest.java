package vn.hoctapcanman.core.practice.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
            var topicCodes = topics.stream().filter(c -> ((Number)c.get("lop")).intValue()==grade)
                .map(c -> c.get("ma").toString()).toList();
            var tasks = exercises.stream().filter(e -> topicCodes.contains(e.get("chu_de")))
                .filter(e -> !Boolean.TRUE.equals(e.get("kiem_tra_seed")))
                .filter(e -> !Boolean.TRUE.equals(e.get("luyen_hien_tai_seed")))
                // SQL giao bài loại ngân hàng tự học; các bài này không sinh lịch sử.
                .filter(e -> !Boolean.TRUE.equals(e.get("tu_hoc_seed")))
                .filter(e -> "DA_DUYET".equals(((Map<?,?>)e.get("duyet_seed")).get("trang_thai")))
                .sorted(Comparator.<Map<String,Object>,String>comparing(e -> e.get("ky_nang_chinh").toString())
                    .thenComparing(e -> level(e.get("muc_do_4").toString()))
                    .thenComparing(e -> e.get("id").toString())).toList();
            var chapters = tasks.stream().map(e -> e.get("chu_de").toString()).distinct().toList();
            int[] spread = new int[4];
            for (int student=0; student<10; student++) {
                Map<String,MasteryState> states = new java.util.HashMap<>();
                int attempts=0;
                for (var exercise : tasks) {
                    String chapterCode=exercise.get("chu_de").toString();
                    var chapterTasks=tasks.stream().filter(e -> chapterCode.equals(e.get("chu_de"))).toList();
                    String kind=LichSuNguyenTraiSeeder.outcome(student,chapters.indexOf(chapterCode),
                        chapterTasks.indexOf(exercise),chapterTasks.size());
                    String id=exercise.get("id").toString();
                    assertThat(fixtures.stream().filter(f -> id.equals(f.get("ma")) && kind.equals(f.get("loai"))).count())
                        .as("Bài %s, học sinh %s, kịch bản %s", id, student, kind).isEqualTo(1);
                    if (kind.equals("CAN_DUYET")) { pending++; continue; }
                    boolean correct=kind.equals("DUNG");
                    if(correct) { passed++; studentPasses[student]++; } else failed++;
                    attempts++;
                    String skill=exercise.get("ky_nang_chinh").toString();
                    var state=states.computeIfAbsent(skill, key -> MasteryState.initial(UUID.randomUUID(),key,config,Instant.EPOCH));
                    var after=state.apply(new MasteryState.Evidence(correct,
                        level(exercise.get("muc_do_4").toString()),null,false),config,Instant.EPOCH).after();
                    states.put(skill,new MasteryState(after.studentId(),after.skillCode(),(double)(float)after.mastery(),
                        after.level(),after.attempts(),after.stuckCounter(),after.lastErrorCodes(),after.completedAt()));
                }
                assertThat(states.values().stream().mapToInt(MasteryState::attempts).sum()).isEqualTo(attempts);
                for(var state:states.values()) spread[state.level().ordinal()]++;
            }
            System.out.println("Seed khối "+grade+": "+java.util.Arrays.toString(spread));
        }
        int total=passed+failed+pending;
        assertThat(total).isEqualTo(390);
        assertThat(passed).isPositive();
        assertThat(failed).isPositive();
        assertThat(pending).isPositive();
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
