package vn.hoctapcanman.core.mastery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.exception.KhongThuocLopException;
import vn.hoctapcanman.core.identity.application.port.AccessTokenIssuer;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.model.UserId;
import vn.hoctapcanman.core.mastery.application.dto.MucHieuHocSinh;
import vn.hoctapcanman.core.mastery.application.port.MucHieuCuaLop;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;

/**
 * Phía đọc của mức hiểu trên PostgreSQL 18: trang «Học» qua HTTP thật (access token thật) và cổng {@link MucHieuCuaLop} cho
 * giáo viên. Trạng thái ghi thẳng bằng SQL (phía ghi có {@code DoiChieuBktV0Test}). Mã chủ đề, kỹ năng có hậu tố ngẫu nhiên
 * vì CSDL dùng chung với các test cùng ngữ cảnh.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class MucHieuDocTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AccessTokenIssuer tokens;

    @Autowired
    private MucHieuCuaLop mucHieuCuaLop;

    @Autowired
    private JdbcClient jdbc;

    private final String hau = UUID.randomUUID().toString().substring(0, 6);
    private final List<UUID> nguoi = new ArrayList<>();
    private final List<UUID> lop = new ArrayList<>();

    @BeforeEach
    void danhMuc() {
        for (String chuDe : List.of("KA" + hau, "KB" + hau)) {
            jdbc.sql("insert into topics (code, name, grade) values (?, ?, 12)").params(chuDe, "Chủ đề " + chuDe).update();
        }
        kyNang("KA" + hau + ".01", "KA" + hau, "Đọc bảng biến thiên", true);
        kyNang("KA" + hau + ".02", "KA" + hau, "Tìm cực trị", true);
        kyNang("KA" + hau + ".03", "KA" + hau, "Bài có tham số", false);
        kyNang("KB" + hau + ".01", "KB" + hau, "Tính đạo hàm", true);
        kyNang("KB" + hau + ".02", "KB" + hau, "Đạo hàm hàm hợp", true);
    }

    @AfterEach
    void don() {
        lop.forEach(id -> jdbc.sql("delete from classes where id = ?").params(id).update());
        nguoi.forEach(id -> jdbc.sql("delete from users where id = ?").params(id).update());
        jdbc.sql("delete from skills where code like ?").params("K_" + hau + ".%").update();
        jdbc.sql("delete from topics where code like ?").params("K_" + hau).update();
    }

    @Test
    void trangHocXepKyNangYeuTruocBaoKetVaHoanThanh() {
        UUID an = nguoiMoi(Role.STUDENT);
        trangThai(an, "KA" + hau + ".01", 0.98, "VAN_DUNG_CAO", 0);
        trangThai(an, "KA" + hau + ".02", 0.95, "VAN_DUNG_CAO", 0);
        trangThai(an, "KA" + hau + ".03", 0.3, "NHAN_BIET", 3);
        trangThai(an, "KB" + hau + ".01", 0.98, "VAN_DUNG_CAO", 0);
        trangThai(an, "KC" + hau + ".01", 0.3, "NHAN_BIET", 2);

        MvcTestResult r = mvc.get().uri("/api/hs/trang-hoc").header("Authorization", bearer(an, Role.STUDENT)).exchange();

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        String a = "KA" + hau;
        assertThat(JSON.readTree(new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))).isEqualTo(JSON.readTree("""
            {"ten":"Người thử","soKyNang":[
              {"kyNang":"%1$s.03","tenKyNang":"Bài có tham số","muc4":"NHAN_BIET","ket":true},
              {"kyNang":"KC%2$s.01","tenKyNang":"KC%2$s.01","muc4":"NHAN_BIET","ket":false},
              {"kyNang":"%1$s.02","tenKyNang":"Tìm cực trị","muc4":"VAN_DUNG_CAO","ket":false},
              {"kyNang":"%1$s.01","tenKyNang":"Đọc bảng biến thiên","muc4":"VAN_DUNG_CAO","ket":false},
              {"kyNang":"KB%2$s.01","tenKyNang":"Tính đạo hàm","muc4":"VAN_DUNG_CAO","ket":false}],
             "hoanThanh":{"kyNang":["%1$s.01","%1$s.02","KB%2$s.01"],"chuDe":["%1$s"]}}""".formatted(a, hau)));
    }

    @Test
    void mucHieuCuaLopTheoThuTuGhiDanhChiChoGiaoVienCuaLop() {
        UUID l = lopMoi();
        UUID gv = nguoiMoi(Role.TEACHER);
        UUID gvKhac = nguoiMoi(Role.TEACHER);
        UUID binh = nguoiMoi(Role.STUDENT);
        UUID an = nguoiMoi(Role.STUDENT);
        UUID ngoaiLop = nguoiMoi(Role.STUDENT);
        ghiDanh(l, gv, "TEACHER", "2026-09-01T00:00:00Z");
        ghiDanh(l, binh, "STUDENT", "2026-09-02T00:00:00Z");
        ghiDanh(l, an, "STUDENT", "2026-09-03T00:00:00Z");
        String k1 = "KA" + hau + ".01";
        String k2 = "KA" + hau + ".02";
        trangThai(an, k1, 0.7, "VAN_DUNG", 0);
        trangThai(binh, k2, 0.02, "NHAN_BIET", 4);
        trangThai(binh, k1, 0.5, "THONG_HIEU", 0);
        trangThai(ngoaiLop, k1, 0.98, "VAN_DUNG_CAO", 0);

        assertThat(mucHieuCuaLop.cuaLop(gv, l)).containsExactly(
            new MucHieuHocSinh(binh, k1, "THONG_HIEU", false),
            new MucHieuHocSinh(binh, k2, "NHAN_BIET", true),
            new MucHieuHocSinh(an, k1, "VAN_DUNG", false));
        assertThatThrownBy(() -> mucHieuCuaLop.cuaLop(gvKhac, l)).isInstanceOf(KhongThuocLopException.class);
        assertThatThrownBy(() -> mucHieuCuaLop.cuaLop(an, l)).isInstanceOf(KhongThuocLopException.class);
    }

    private void kyNang(String ma, String chuDe, String ten, boolean cotLoi) {
        jdbc.sql("insert into skills (code, topic_code, name, is_core) values (?, ?, ?, ?)").params(ma, chuDe, ten, cotLoi).update();
    }

    private void trangThai(UUID hs, String kyNang, double mastery, String muc, int ket) {
        jdbc.sql("""
                insert into mastery_states (student_id, skill_code, mastery, level4, attempts, stuck_counter, completed_at)
                values (?, ?, ?, ?, 5, ?, ?)""")
            .params(hs, kyNang, mastery, muc, ket, muc.equals("VAN_DUNG_CAO") ? Timestamp.from(Instant.now()) : null).update();
    }

    private void ghiDanh(UUID l, UUID u, String vaiTro, String luc) {
        jdbc.sql("insert into enrollments (class_id, user_id, role_in_class, enrolled_at) values (?, ?, ?, cast(? as timestamptz))")
            .params(l, u, vaiTro, luc).update();
    }

    private UUID nguoiMoi(Role role) {
        UUID id = DuLieuPractice.nguoi(jdbc, role.name());
        nguoi.add(id);
        return id;
    }

    private UUID lopMoi() {
        UUID id = DuLieuPractice.lop(jdbc);
        lop.add(id);
        return id;
    }

    private String bearer(UUID id, Role role) {
        Instant now = Instant.now();
        User u = new User(new UserId(id), new Email(id + "@demo.local"), "x", "Người thử", role, true, true, now, now);
        return "Bearer " + tokens.issue(u, now).value();
    }
}
