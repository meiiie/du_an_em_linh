package vn.hoctapcanman.core.practice.infrastructure.seed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;
import vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan;

/** Giao bài cho lớp hư cấu, chỉ các bài đã qua cổng; giữ hạn và mode khi chạy lại. */
@Component
@Profile("dev")
@Order(4)
public class GiaoBaiNguyenTraiSeeder implements ApplicationRunner {

    static final String TEP = "seed-nguyen-trai/giao-bai.sql";
    private static final Logger LOG = LoggerFactory.getLogger(GiaoBaiNguyenTraiSeeder.class);

    private final JdbcClient jdbc;
    private final Path source;

    public GiaoBaiNguyenTraiSeeder(JdbcClient jdbc, @Value("${app.content.source}") String source) {
        this.jdbc = jdbc;
        this.source = Path.of(source);
    }

    @SuppressWarnings("unchecked")
    static List<String> testCodes(String json) {
        List<Map<String,Object>> exercises = JsonMapper.builder().build().readValue(json, List.class);
        return exercises.stream().filter(e -> Boolean.TRUE.equals(e.get("kiem_tra_seed")))
            .map(e -> (String)e.get("id")).toList();
    }

    @SuppressWarnings("unchecked")
    static List<String> studyCodes(String json) {
        List<Map<String,Object>> exercises = JsonMapper.builder().build().readValue(json,List.class);
        var codes=exercises.stream().filter(e->Boolean.TRUE.equals(e.get("tu_hoc_seed"))).map(e->(String)e.get("id")).toList();
        return codes.isEmpty()?List.of("KHONG_CO_BAI_TU_HOC_SEED"):codes;
    }

    @Override
    public void run(ApplicationArguments args) {
        String sql;
        List<String> testCodes;
        List<String> studyCodes;
        try {
            sql = new ClassPathResource(TEP).getContentAsString(StandardCharsets.UTF_8);
            String json=Files.readString(source.resolve("dev/nguyen-trai/bai-tap.json"));
            testCodes = testCodes(json);
            studyCodes = studyCodes(json);
        } catch (IOException e) {
            LOG.warn("Bỏ qua giao bài trường hư cấu: thiếu hoặc không đọc được data/dev hoặc SQL seed.");
            return;
        }
        int moi = 0;
        for (String ten : TruongNguyenTrai.LOP) {
            if (LopSeedAnToan.kiem(jdbc, ten)) moi += jdbc.sql(sql).param("lop", TruongNguyenTrai.id("lop:" + ten)).param("kiemTra", testCodes).param("tuHoc",studyCodes).update();
        }
        LOG.info("Giao bài trường hư cấu: {} lượt giao mới", moi);
    }
}
