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

/**
 * Giao bài thử cho dev / demo (T021): chạy {@code du-lieu-thu/giao-bai.sql} sau khi nhập nội dung theo lớp (Order 3), chỉ với
 * profile {@code dev}. Dữ liệu thử bằng SQL theo yêu cầu chủ repo (2026-10-06), không qua mã giả. Câu lệnh idempotent: chạy
 * lại không thêm, không đổi dòng nào. Nhập nội dung lỗi thì lớp không có bài phát hành và câu lệnh không giao gì.
 */
@Component
@Profile("dev")
@Order(4)
public class GiaoBaiThuSeeder implements ApplicationRunner {

    static final String TEP = "du-lieu-thu/giao-bai.sql";
    private static final Logger LOG = LoggerFactory.getLogger(GiaoBaiThuSeeder.class);

    private final JdbcClient jdbc;

    public GiaoBaiThuSeeder(JdbcClient jdbc) {
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
        int moi = jdbc.sql(sql).update();
        LOG.info("Giao bài thử: {} lượt giao mới", moi);
    }
}
