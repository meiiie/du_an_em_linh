package vn.hoctapcanman.core.mastery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.content.infrastructure.nhap.NhapNoiDungChungTest;
import vn.hoctapcanman.core.practice.application.dto.BaiDaNop;
import vn.hoctapcanman.core.practice.application.dto.KetQuaBai;
import vn.hoctapcanman.core.practice.application.dto.ThayDoiMucHieu;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ViTriSai;
import vn.hoctapcanman.core.practice.application.port.CapNhatMucHieu;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;

/**
 * T052 (research R7): mức hiểu tính đúng như v0. Tệp vàng {@code bkt-v0.json} do {@code specs/001-lat-cat-doc/doi-chieu/bkt-v0.ts}
 * sinh bằng chính mã v0 ({@code applyMastery}, {@code bktNext}, {@code mucSauBai}, điều kiện gọi trong {@code nopBuoc}). Mỗi
 * kịch bản là một học sinh tổng hợp mới trong một lớp có giáo viên; mỗi lần nộp gọi cổng {@code CapNhatMucHieu} thật trên
 * PostgreSQL 18 với đúng đầu vào v0 nhận, rồi so: thay đổi mức trả về; mọi dòng {@code mastery_states} của em (mastery, mức,
 * số lượt, kẹt, mã lỗi gần đây); dòng {@code mastery_events} (kỹ năng, luật, thay đổi, bước sai, mã lỗi, độ tin cậy, nghi đoán
 * mò); cảnh báo kẹt vừa ghi. Gửi lại cùng bài làm phải trả y hệt và không ghi gì thêm.
 *
 * <p>Danh mục mã lỗi và bước ghi bằng SQL đúng như tệp vàng (v0 đọc từ {@code ma-loi-DH.csv}, {@code khung-buoc.json}); tham
 * số BKT là dòng của V9, so với dòng v0 seed ({@code data/v0/bkt.json}). Cùng ngữ cảnh Spring với {@code CoreApplicationTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class DoiChieuBktV0Test {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Path TEP_VANG = NhapNoiDungChungTest.thuMucData().getParent()
        .resolve("specs/001-lat-cat-doc/doi-chieu/bkt-v0.json");
    private static final Instant LUC = Instant.parse("2026-10-06T08:00:00Z");

    @Autowired
    private CapNhatMucHieu mucHieu;

    @Autowired
    private JdbcClient jdbc;

    @TestFactory
    Stream<DynamicTest> moiLanNopTinhNhuV0() {
        JsonNode vang = doc();
        danhMuc(vang.path("danh_muc"));
        assertThat(JSON.readTree(jdbc.sql("select value::text from mastery_config where key = 'bkt'").query(String.class).single()))
            .as("tham số của V9 là dòng v0 seed (data/v0/bkt.json)").isEqualTo(vang.path("cau_hinh").path("value"));
        assertThat(jdbc.sql("select version from mastery_config where key = 'bkt'").query(Integer.class).single())
            .isEqualTo(vang.path("cau_hinh").path("version").asInt());
        Dem dem = new Dem();
        List<DynamicTest> ca = new ArrayList<>();
        for (JsonNode kb : vang.path("kich_ban")) {
            ca.add(dynamicTest(kb.path("ma").asString() + ": " + kb.path("mo_ta").asString(), () -> chay(kb, dem)));
        }
        JsonNode d = vang.path("dem");
        ca.add(dynamicTest("đã chạy hết tệp vàng", () -> {
            assertThat(dem.nop).as("lần nộp").isEqualTo(d.path("nop").asInt()).isGreaterThan(250);
            assertThat(dem.tinh).as("lần được tính").isEqualTo(d.path("tinh").asInt());
            assertThat(dem.canhBao).as("cảnh báo kẹt").isEqualTo(d.path("canh_bao").asInt()).isPositive();
            assertThat(dem.mucDoi).as("lần đổi mức").isEqualTo(d.path("muc_doi").asInt()).isPositive();
        }));
        return ca.stream();
    }

    private void chay(JsonNode kb, Dem dem) {
        UUID lop = DuLieuPractice.lop(jdbc);
        UUID giaoVien = DuLieuPractice.nguoi(jdbc, "TEACHER");
        UUID hs = DuLieuPractice.nguoi(jdbc, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, giaoVien, "TEACHER");
        DuLieuPractice.ghiDanh(jdbc, lop, hs, "STUDENT");
        try {
            int i = 0;
            for (JsonNode b : kb.path("buoc")) {
                String o = kb.path("ma").asString() + "#" + i;
                if (b.path("loai").asString().equals("xu_ly_canh_bao")) {
                    jdbc.sql("update escalations set handled_at = now(), handled_by = ? where student_id = ? and handled_at is null")
                        .params(giaoVien, hs).update();
                } else {
                    nop(o, b, hs, lop, LUC.plusSeconds(i), dem);
                }
                i++;
            }
        } finally {
            jdbc.sql("delete from classes where id = ?").params(lop).update();
            jdbc.sql("delete from users where id in (?, ?)").params(giaoVien, hs).update();
        }
    }

    private void nop(String o, JsonNode b, UUID hs, UUID lop, Instant luc, Dem dem) {
        JsonNode cham = b.path("cham");
        BaiDaNop bai = new BaiDaNop(UUID.randomUUID(), hs, lop, UUID.randomUUID(), b.path("bai").path("ky_nang").asString(),
            b.path("bai").path("muc").asString(), KetQuaBai.valueOf(cham.path("ket_qua").asString()),
            cham.path("buoc_sai").isNull() ? null : new ViTriSai(cham.path("buoc_sai").path("ma_buoc").asString(), null, null, null),
            chu(cham.path("ma_loi")), cham.path("do_tin_cay").isNull() ? null : cham.path("do_tin_cay").asDouble(),
            cham.has("toan_dung") ? cham.path("toan_dung").asBoolean() : null, b.path("nghi").asBoolean(), luc);
        int canhBaoTruoc = soCanhBao(hs);
        List<ThayDoiMucHieu> ra = mucHieu.sauKhiNop(bai);

        JsonNode sk = b.path("su_kien");
        List<ThayDoiMucHieu> kyVong = new ArrayList<>();
        if (!sk.isNull()) {
            String kyNang = sk.path("skillCode").asString();
            String sau = trangThaiVang(b, kyNang).path("currentMucDo4").asString();
            // Kỹ năng chưa có dòng: v0 tính từ mức của mastery 0,3, tức Nhận biết.
            kyVong.add(new ThayDoiMucHieu(kyNang, b.path("muc_truoc").isNull() ? "NHAN_BIET" : b.path("muc_truoc").asString(), sau));
        }
        assertThat(ra).as(o + ": thay đổi mức trả về").isEqualTo(kyVong);
        List<Map<String, Object>> trangThai = trangThai(hs);
        assertThat(trangThai).as(o + ": mastery_states").isEqualTo(trangThaiVang(b));
        assertThat(suKien(bai.baiLamId())).as(o + ": mastery_events").isEqualTo(sk.isNull() ? null : suKienVang(sk, kyVong.getFirst()));
        assertThat(canhBaoMoi(hs, canhBaoTruoc)).as(o + ": cảnh báo vừa ghi").isEqualTo(canhBaoVang(b));

        assertThat(mucHieu.sauKhiNop(bai)).as(o + ": gửi lại phát lại").isEqualTo(ra);
        assertThat(trangThai(hs)).as(o + ": gửi lại không tính lần hai").isEqualTo(trangThai);
        assertThat(soCanhBao(hs)).as(o + ": gửi lại không ghi cảnh báo").isEqualTo(canhBaoTruoc + b.path("canh_bao").size());

        dem.nop++;
        dem.tinh += sk.isNull() ? 0 : 1;
        dem.canhBao += b.path("canh_bao").size();
        dem.mucDoi += !kyVong.isEmpty() && !b.path("muc_truoc").isNull() && !kyVong.getFirst().muc4Truoc().equals(kyVong.getFirst().muc4Sau())
            ? 1 : 0;
    }

    /** Dòng trạng thái theo khóa của v0 ({@code masteryStates}), theo mã kỹ năng. */
    private List<Map<String, Object>> trangThai(UUID hs) {
        return jdbc.sql("""
                select skill_code, mastery, level4, attempts, stuck_counter, last_error_codes, completed_at from mastery_states
                where student_id = ? order by skill_code collate "C"
                """)
            .params(hs).query((rs, n) -> {
                assertThat(rs.getTimestamp("completed_at") != null).as("completed_at khi và chỉ khi Vận dụng cao")
                    .isEqualTo(rs.getString("level4").equals("VAN_DUNG_CAO"));
                Map<String, Object> h = new LinkedHashMap<>();
                h.put("skillCode", rs.getString("skill_code"));
                h.put("mastery", real(rs, "mastery"));
                h.put("currentMucDo4", rs.getString("level4"));
                h.put("attempts", rs.getInt("attempts"));
                h.put("stuckCounter", rs.getInt("stuck_counter"));
                h.put("lastErrorCodes", List.of((String[]) rs.getArray("last_error_codes").getArray()));
                return h;
            }).list();
    }

    private static List<Map<String, Object>> trangThaiVang(JsonNode b) {
        List<Map<String, Object>> ra = new ArrayList<>();
        for (JsonNode t : b.path("trang_thai")) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("skillCode", t.path("skillCode").asString());
            h.put("mastery", t.path("mastery").asDouble());
            h.put("currentMucDo4", t.path("currentMucDo4").asString());
            h.put("attempts", t.path("attempts").asInt());
            h.put("stuckCounter", t.path("stuckCounter").asInt());
            List<String> ma = new ArrayList<>();
            t.path("lastErrorCodes").forEach(m -> ma.add(m.asString()));
            h.put("lastErrorCodes", ma);
            ra.add(h);
        }
        return ra;
    }

    private static JsonNode trangThaiVang(JsonNode b, String kyNang) {
        for (JsonNode t : b.path("trang_thai")) {
            if (t.path("skillCode").asString().equals(kyNang)) {
                return t;
            }
        }
        throw new AssertionError("tệp vàng thiếu trạng thái của " + kyNang);
    }

    private @Nullable Map<String, @Nullable Object> suKien(UUID baiLam) {
        return jdbc.sql("""
                select skill_code, delta, rule_applied, wrong_step, error_code, confidence, guess_suspected, level4_before, level4_after
                from mastery_events where submission_id = ?""")
            .params(baiLam).query((rs, n) -> {
                Map<String, @Nullable Object> h = new LinkedHashMap<>();
                h.put("skillCode", rs.getString("skill_code"));
                h.put("delta", real(rs, "delta"));
                h.put("ruleApplied", rs.getString("rule_applied"));
                h.put("buocSai", rs.getString("wrong_step"));
                h.put("maLoi", rs.getString("error_code"));
                float tinCay = rs.getFloat("confidence");
                h.put("doTinCay", rs.wasNull() ? null : Double.valueOf(Float.toString(tinCay)));
                h.put("nghiDoanMo", rs.getBoolean("guess_suspected"));
                h.put("muc", rs.getString("level4_before") + " → " + rs.getString("level4_after"));
                return h;
            }).optional().orElse(null);
    }

    private static Map<String, @Nullable Object> suKienVang(JsonNode sk, ThayDoiMucHieu doi) {
        Map<String, @Nullable Object> h = new LinkedHashMap<>();
        h.put("skillCode", sk.path("skillCode").asString());
        h.put("delta", sk.path("delta").asDouble());
        h.put("ruleApplied", sk.path("ruleApplied").asString());
        h.put("buocSai", sk.path("buocSai").isNull() ? null : sk.path("buocSai").path("ma_buoc").asString());
        h.put("maLoi", chu(sk.path("maLoi")));
        h.put("doTinCay", sk.path("doTinCay").isNull() ? null : sk.path("doTinCay").asDouble());
        h.put("nghiDoanMo", sk.path("nghiDoanMo").asBoolean());
        h.put("muc", doi.muc4Truoc() + " → " + doi.muc4Sau());
        return h;
    }

    private List<String> canhBaoMoi(UUID hs, int truoc) {
        List<String> tatCa = jdbc.sql("""
                select kind || ' ' || skill_code || ' ' || coalesce(problem_code, '-') || ' ' || coalesce(step_code, '-') || ' ' || reason
                from escalations where student_id = ? order by created_at, id""")
            .params(hs).query(String.class).list();
        return tatCa.subList(truoc, tatCa.size());
    }

    private static List<String> canhBaoVang(JsonNode b) {
        List<String> ra = new ArrayList<>();
        b.path("canh_bao").forEach(c -> ra.add(c.path("loai").asString() + " " + c.path("skillCode").asString() + " - - "
            + c.path("reason").asString()));
        return ra;
    }

    private int soCanhBao(UUID hs) {
        return jdbc.sql("select count(*) from escalations where student_id = ?").params(hs).query(Integer.class).single();
    }

    /** Mã lỗi, bước theo tệp vàng, với kỹ năng làm khóa ngoại; ghi đè để CSDL dùng chung khớp đúng tệp vàng. */
    private void danhMuc(JsonNode dm) {
        assertThat(dm.path("ma_loi").size()).as("mã lỗi của tệp vàng").isGreaterThan(30);
        assertThat(dm.path("buoc").size()).as("bước khung của tệp vàng").isEqualTo(5);
        jdbc.sql("insert into topics (code, name, grade) values ('DH12', 'Đơn điệu và cực trị', 12) on conflict do nothing").update();
        List<String> kyNang = new ArrayList<>();
        Stream.of(dm.path("ma_loi"), dm.path("buoc")).forEach(bang -> bang.properties().forEach(e -> {
            if (!e.getValue().isNull()) {
                kyNang.add(e.getValue().asString());
            }
        }));
        kyNang.forEach(k -> DuLieuPractice.kyNang(jdbc, k));
        dm.path("ma_loi").properties().forEach(e -> jdbc.sql("""
                insert into error_types (code, skill_code, name) values (?, ?, ?)
                on conflict (code) do update set skill_code = excluded.skill_code""")
            .params(e.getKey(), chu(e.getValue()), "Mã lỗi " + e.getKey()).update());
        int thuTu = 1;
        for (Map.Entry<String, JsonNode> e : dm.path("buoc").properties()) {
            jdbc.sql("""
                    insert into step_templates (step_code, topic_code, ordinal, input_kind, skill_code, description)
                    values (?, 'DH12', ?, ?, ?, ?) on conflict (step_code) do update set skill_code = excluded.skill_code""")
                .params(e.getKey(), thuTu, e.getKey().equals("B.DH.XETDAU") ? "BANG" : "DONG", chu(e.getValue()), e.getKey()).update();
            thuTu++;
        }
    }

    private static @Nullable String chu(JsonNode n) {
        return n.isNull() || n.isMissingNode() ? null : n.asString();
    }

    private static double real(ResultSet rs, String cot) throws SQLException {
        return Double.parseDouble(Float.toString(rs.getFloat(cot)));
    }

    private static JsonNode doc() {
        try {
            return JSON.readTree(Files.readString(TEP_VANG));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Số đếm trên phía core, so với {@code dem} của tệp vàng: test phải chạy hết mọi lần nộp. */
    private static final class Dem {
        int nop;
        int tinh;
        int canhBao;
        int mucDoi;
    }
}
