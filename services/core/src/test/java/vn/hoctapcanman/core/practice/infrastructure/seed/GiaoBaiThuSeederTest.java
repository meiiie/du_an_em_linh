package vn.hoctapcanman.core.practice.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;

/**
 * Profile dev: lớp «12A1 thử» do {@code TaiKhoanThuSeeder} và {@code LopThuSeeder} thật tạo lúc khởi động; {@code giao-bai.sql}
 * giao mọi bài đang phát hành của lớp cho An, Bình, Chi (tài khoản tổng hợp), người giao là giáo viên thử, chạy lại không đổi
 * dòng nào; học sinh và giáo viên thật trong lớp không bị đụng tới. Cùng ngữ cảnh Spring với {@code LopThuSeederTest}, nên
 * dọn mọi thứ đã thêm.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class GiaoBaiThuSeederTest {

    @Autowired
    private GiaoBaiThuSeeder seeder;

    @Autowired
    private JdbcClient jdbc;

    private final List<UUID> bai = new ArrayList<>();
    private final List<UUID> nguoiThat = new ArrayList<>();

    @AfterEach
    void don() {
        bai.forEach(id -> jdbc.sql("delete from problems where id = ?").params(id).update());
        nguoiThat.forEach(id -> jdbc.sql("delete from users where id = ?").params(id).update());
    }

    @Test
    void giaoBaiPhatHanhChoHocSinhTongHopChayLaiKhongDoiDong() {
        UUID lop = jdbc.sql("select id from classes where name = '12A1 thử' and school_year = '2026-2027'").query(UUID.class).single();
        UUID hocSinhThat = nguoiThat("STUDENT");
        UUID giaoVienThat = nguoiThat("TEACHER");
        DuLieuPractice.ghiDanh(jdbc, lop, hocSinhThat, "STUDENT");
        // Giáo viên thật ghi danh trước giáo viên thử: nếu câu lệnh chọn giáo viên đầu tiên bất kể tài khoản thì sẽ chọn người này.
        jdbc.sql("insert into enrollments (class_id, user_id, role_in_class, enrolled_at) values (?, ?, 'TEACHER', '2000-01-01T00:00:00Z')")
            .params(lop, giaoVienThat).update();
        DuLieuPractice.danhMuc(jdbc);
        UUID nb = baiMoi("NB");
        UUID th = baiMoi("TH");
        UUID chua = baiMoi("CHUA");
        DuLieuPractice.phatHanh(jdbc, lop, nb);
        DuLieuPractice.phatHanh(jdbc, lop, th);

        seeder.run(new DefaultApplicationArguments());
        List<String> dau = giao(nb, th, chua);
        assertThat(dau).containsExactlyInAnyOrder(
            "hs.an@demo.local NB DA_GIAO Đơn điệu và cực trị gv@demo.local 7 days",
            "hs.an@demo.local TH DA_GIAO Đơn điệu và cực trị gv@demo.local 7 days",
            "hs.binh@demo.local NB DA_GIAO Đơn điệu và cực trị gv@demo.local 7 days",
            "hs.binh@demo.local TH DA_GIAO Đơn điệu và cực trị gv@demo.local 7 days",
            "hs.chi@demo.local NB DA_GIAO Đơn điệu và cực trị gv@demo.local 7 days",
            "hs.chi@demo.local TH DA_GIAO Đơn điệu và cực trị gv@demo.local 7 days");
        List<String> dongDau = dong(nb, th, chua);

        seeder.run(new DefaultApplicationArguments());
        assertThat(dong(nb, th, chua)).as("chạy lại: cùng id, hạn, lúc giao").isEqualTo(dongDau);
        assertThat(jdbc.sql("select count(*) from assignments where student_id = ?").params(hocSinhThat).query(Integer.class).single())
            .isZero();
    }

    /** Mỗi lượt giao của các bài: email học sinh, mã bài (bỏ hậu tố), trạng thái, bộ, email người giao, hạn trừ lúc giao. */
    private List<String> giao(UUID... cacBai) {
        return jdbc.sql("""
                select hs.email || ' ' || split_part(p.code, '-', 1) || ' ' || a.status || ' ' || a.set_name || ' ' || gv.email || ' '
                    || (a.due_at - a.assigned_at)::text
                from assignments a join users hs on hs.id = a.student_id join users gv on gv.id = a.assigned_by
                join problems p on p.id = a.problem_id
                where a.problem_id in (:bai)""")
            .param("bai", List.of(cacBai)).query(String.class).list();
    }

    private List<String> dong(UUID... cacBai) {
        return jdbc.sql("select id || ' ' || due_at || ' ' || assigned_at from assignments where problem_id in (:bai) order by id")
            .param("bai", List.of(cacBai)).query(String.class).list();
    }

    private UUID baiMoi(String ma) {
        UUID id = DuLieuPractice.bai(jdbc, ma);
        bai.add(id);
        return id;
    }

    private UUID nguoiThat(String vaiTro) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into users (id, email, password_hash, display_name, role, synthetic, created_at, updated_at)
                values (?, ?, 'x', 'Người thật', ?, false, now(), now())""")
            .params(id, id + "@that.example", vaiTro).update();
        nguoiThat.add(id);
        return id;
    }
}
