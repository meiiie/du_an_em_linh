package vn.hoctapcanman.core.practice.infrastructure.seed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

    public GiaoBaiNguyenTraiSeeder(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String sql;
        try {
            sql = new ClassPathResource(TEP).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được " + TEP, e);
        }
        int moi = 0;
        for (String ten : TruongNguyenTrai.LOP) {
            if (LopSeedAnToan.kiem(jdbc, ten)) moi += jdbc.sql(sql).param("lop", TruongNguyenTrai.id("lop:" + ten)).update();
        }
        LOG.info("Giao bài trường hư cấu: {} lượt giao mới", moi);
    }
}
