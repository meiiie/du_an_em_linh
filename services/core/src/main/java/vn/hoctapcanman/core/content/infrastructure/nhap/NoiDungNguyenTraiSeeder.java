package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;

/** Nhập ngân hàng tự soạn và kho của đúng lớp hư cấu; lỗi CAS không tự phát hành. */
@Component
@Profile("dev")
@Order(3)
@ConditionalOnBooleanProperty("app.content.import-on-startup")
public class NoiDungNguyenTraiSeeder implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(NoiDungNguyenTraiSeeder.class);
    private final NhapNoiDungChung chung;
    private final NhapTheoLop theoLop;
    private final JdbcClient jdbc;
    public NoiDungNguyenTraiSeeder(NhapNoiDungChung chung, NhapTheoLop theoLop, JdbcClient jdbc) {
        this.chung = chung;
        this.theoLop = theoLop;
        this.jdbc = jdbc;
    }
    @Override
    public void run(ApplicationArguments args) {
        var lop = TruongNguyenTrai.LOP.stream().filter(ten -> LopSeedAnToan.kiem(jdbc, ten)).toList();
        if (lop.isEmpty()) return;
        NhapNoiDungChung.DaNhap noiDung = chung.nhapTruong();
        for (String ten : lop) {
            UUID id = TruongNguyenTrai.id("lop:" + ten);
            try {
                var skills = jdbc.sql("SELECT code FROM skills WHERE grade=(SELECT grade FROM classes WHERE id=:cl)")
                    .param("cl", id).query(String.class).list();
                var bank = noiDung.bai().stream().filter(b -> skills.contains(b.kyNang())).toList();
                theoLop.nhapTruong(id, bank);
                theoLop.napBangChuong(id);
                UUID teacher = jdbc.sql("""
                    SELECT e.user_id FROM enrollments e JOIN users u ON u.id=e.user_id
                    WHERE e.class_id=:cl AND e.role_in_class='TEACHER' AND u.synthetic AND u.enabled
                    AND u.role='TEACHER' ORDER BY e.enrolled_at,e.user_id LIMIT 1
                    """).param("cl", id).query(UUID.class).single();
                theoLop.duyetTruong(id, teacher, bank);
            } catch (RuntimeException e) {
                // Không in payload/lời giải/định danh trong lỗi của dịch vụ ngoài.
                LOG.warn("Chưa nhập xong kho của một lớp hư cấu; kiểm kết nối CAS và nguồn nội dung.");
            }
        }
    }
}
