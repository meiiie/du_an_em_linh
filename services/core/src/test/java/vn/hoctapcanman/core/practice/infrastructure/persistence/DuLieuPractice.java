package vn.hoctapcanman.core.practice.infrastructure.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Dữ liệu nền cho test của practice, ghi bằng SQL (không dùng adapter của module khác): danh mục chủ đề, người dùng, lớp,
 * ghi danh, bài, phát hành bài cho lớp theo một lượt kiểm DAT.
 */
final class DuLieuPractice {

    static final String BAM = "a".repeat(64);

    private DuLieuPractice() {}

    /** Chủ đề, kỹ năng và khung 5 bước của v0; ghi rồi thì giữ (test khác dùng chung). */
    static void danhMuc(JdbcClient jdbc) {
        jdbc.sql("insert into topics (code, name, grade) values ('DH12', 'Đơn điệu và cực trị', 12) on conflict do nothing").update();
        jdbc.sql("insert into skills (code, topic_code, name, is_core) values ('T12.DH.02', 'DH12', 'Tính đạo hàm', true)"
            + " on conflict do nothing").update();
        String[][] buoc = {
            {"B.DH.TXD", "1", "DONG", "Tập xác định"}, {"B.DH.DAOHAM", "2", "DONG", "Tính đạo hàm"},
            {"B.DH.NGHIEM", "3", "DONG", "Tìm nghiệm của đạo hàm"}, {"B.DH.XETDAU", "4", "BANG", "Xét dấu đạo hàm"},
            {"B.DH.KETLUAN", "5", "DONG", "Kết luận"}};
        for (String[] b : buoc) {
            jdbc.sql("""
                    insert into step_templates (step_code, topic_code, ordinal, input_kind, description) values (?, 'DH12', ?, ?, ?)
                    on conflict do nothing""")
                .params(b[0], Short.parseShort(b[1]), b[2], b[3]).update();
        }
    }

    static UUID nguoi(JdbcClient jdbc, String vaiTro) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into users (id, email, password_hash, display_name, role, synthetic, created_at, updated_at)
                values (?, ?, 'x', 'Người thử', ?, true, now(), now())""")
            .params(id, id + "@demo.local", vaiTro).update();
        return id;
    }

    static UUID lop(JdbcClient jdbc) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, ?, 12, '2026-2027', now())")
            .params(id, "Lớp " + id.toString().substring(0, 8)).update();
        return id;
    }

    static void ghiDanh(JdbcClient jdbc, UUID lop, UUID nguoi, String vaiTro) {
        jdbc.sql("insert into enrollments (class_id, user_id, role_in_class, enrolled_at) values (?, ?, ?, now())")
            .params(lop, nguoi, vaiTro).update();
    }

    static UUID bai(JdbcClient jdbc, String ma) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into problems (id, code, skill_code, level4, statement_text, statement_latex, function_sympy, origin,
                    content_hash, created_at, updated_at)
                values (?, ?, 'T12.DH.02', 'THONG_HIEU', 'Xét tính đơn điệu của y = x^3 - 3x^2 + 2.', 'y = x^3 - 3x^2 + 2',
                    'x**3 - 3*x**2 + 2', 'SUPHAM', ?, now(), now())""")
            .params(id, ma + "-" + id.toString().substring(0, 8), BAM).update();
        return id;
    }

    /** Lượt kiểm DAT mới cho bài ở lớp, rồi phát hành theo lượt đó. */
    static void phatHanh(JdbcClient jdbc, UUID lop, UUID bai) {
        UUID luot = UUID.randomUUID();
        jdbc.sql("""
                insert into verification_runs (id, class_id, subject_kind, subject_id, content_hash, overall_status,
                    publish_status, content_version, created_at)
                values (?, ?, 'PROBLEM', ?, ?, 'DAT', 'DA_PHAT_HANH', (select content_version from problems where id = ?), clock_timestamp())""")
            .params(luot, lop, bai, BAM, bai).update();
        jdbc.sql("""
                insert into problem_releases (class_id, problem_id, status, run_id, updated_at) values (?, ?, 'DA_PHAT_HANH', ?, now())
                on conflict (class_id, problem_id) do update set status = excluded.status, run_id = excluded.run_id""")
            .params(lop, bai, luot).update();
    }
}
