package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.content.infrastructure.persistence.DocumentRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.FormulaSheetRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.HintLevelRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemReleaseRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.SolutionRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.TopicCatalogRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.VerificationRunRepositoryAdapter;

/**
 * Đối chiếu với v0 (T014, #85): nhập nội dung chung và nhập theo lớp trên PostgreSQL 18, với dịch vụ toán giả phát lại đúng
 * các phản hồi mà dịch vụ toán thật đã trả cho mã của v0 ({@code specs/001-lat-cat-doc/doi-chieu/phan-hoi-toan.json}, T013),
 * rồi so từng bài với tệp vàng {@code v0-bai.json} (sau khi đọc lại từ CSDL thấy nội dung đã ghi đúng bài đã dựng): cùng tập
 * mã bài, cùng dấu vân tay kiểu v0, cùng nguồn bài, dạng trả lời, trạng thái tổng, trạng thái phát hành và trạng thái từng
 * tầng. Phản hồi được tra theo yêu cầu đã chuẩn hóa (khóa xếp theo thứ tự, bỏ kho lớp); kho lớp của mỗi yêu cầu kiểm bài được
 * so riêng với mã băm kho của tệp vàng. Core gửi yêu cầu khác v0 dù một chút thì không có phản hồi và test đỏ, nên test này
 * cũng giữ importer dựng bài và kho đúng như {@code seed.ts}.
 *
 * <p>Job khóa bảng ({@code /v1/kiem-dong-cong-thuc}) không có ở v0 nên không có trong tệp vàng: job giả trả mọi dòng đạt hai
 * tầng, trích đoạn đầu của tài liệu đầu của kho; bảng khóa là điều kiện để kiểm bài, không phải đối tượng so ở đây (test của
 * {@code services/math} kiểm job đó với bảng 6 dòng của v0 và 5 tài liệu).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    TopicCatalogRepositoryAdapter.class,
    ProblemRepositoryAdapter.class,
    SolutionRepositoryAdapter.class,
    HintLevelRepositoryAdapter.class,
    DocumentRepositoryAdapter.class,
    FormulaSheetRepositoryAdapter.class,
    VerificationRunRepositoryAdapter.class,
    ProblemReleaseRepositoryAdapter.class,
    NguonNoiDung.class,
    NhapNoiDungChung.class,
    NhapTheoLop.class,
    NhapNoiDungTest.PhatLai.class
})
@Testcontainers(disabledWithoutDocker = true)
class NhapNoiDungTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @DynamicPropertySource
    static void nguon(DynamicPropertyRegistry r) {
        r.add("app.content.source", () -> NhapNoiDungChungTest.thuMucData().toString());
    }

    @Autowired
    private NhapNoiDungChung chung;

    @Autowired
    private NhapTheoLop theoLop;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @SuppressWarnings("unchecked")
    void nhapNhuV0TrenCungDauVaoVaNhapHaiLanCungKetQua() {
        Map<String, Object> vang = (Map<String, Object>) docJson("v0-bai.json");
        Map<String, Map<String, Object>> v0 = new LinkedHashMap<>();
        for (Map<String, Object> b : (List<Map<String, Object>>) vang.get("bai")) {
            v0.put((String) b.get("ma"), b);
        }

        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        assertThat(da.bai()).extracting(NhapNoiDungChung.BaiNhap::ma).containsExactlyInAnyOrderElementsOf(v0.keySet());
        // Dấu vân tay của v0: sha256(JSON.stringify({de, bl, hints})) của napBai trong seed.ts.
        for (NhapNoiDungChung.BaiNhap b : da.bai()) {
            Map<String, @Nullable Object> noiDung = new LinkedHashMap<>();
            noiDung.put("de", b.deBai());
            noiDung.put("bl", b.baiLam());
            noiDung.put("hints", b.thangGoiY());
            assertThat(NhapNoiDungChung.sha256(JsonKieuJs.stringify(noiDung))).as("dấu vân tay v0 của %s", b.ma())
                .isEqualTo(v0.get(b.ma()).get("dau_van_tay_v0"));
            soVoiDaGhi(b);
        }

        UUID lop = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A1 thử', 12, '2026-2027', now())")
            .params(lop).update();
        NhapTheoLop.KetQua kq = theoLop.nhap(lop, da.bai());
        assertThat(kq.daKiem()).isEqualTo(v0.size());
        for (Map<String, Object> b : v0.values()) {
            String ma = (String) b.get("ma");
            assertThat(kq.phatHanh().get(ma).name()).as("phát hành của %s", ma).isEqualTo(b.get("trang_thai_phat_hanh"));
            // Codex #135 (P2): nguồn bài (importer rút theo nguồn), dạng trả lời (cách học sinh nộp) và trạng thái tổng của lượt.
            List<String> truongV2 = jdbc.sql("""
                    select p.origin, p.answer_form, v.overall_status from problem_releases r join problems p on p.id = r.problem_id
                    join verification_runs v on v.id = r.run_id where r.class_id = ? and p.code = ?""")
                .params(lop, ma).query((rs, i) -> List.of(rs.getString(1), rs.getString(2), rs.getString(3))).single();
            assertThat(truongV2).as("nguồn, dạng trả lời, trạng thái tổng của %s", ma)
                .containsExactly((String) b.get("nguon_bai"), (String) b.get("dang_tra_loi"), (String) b.get("trang_thai_tong"));
            List<String> tangV2 = jdbc.sql("""
                    select t.status from problem_releases r join problems p on p.id = r.problem_id
                    join verification_tier_results t on t.run_id = r.run_id
                    where r.class_id = ? and p.code = ? order by t.tier""").params(lop, ma).query(String.class).list();
            List<String> tangV0 = ((List<Map<String, Object>>) b.get("tang")).stream().map(t -> (String) t.get("trang_thai")).toList();
            assertThat(tangV2).as("các tầng của %s", ma).isEqualTo(tangV0);
        }
        assertThat(PhatLai.CHUA_DUNG).as("mọi phản hồi của tệp vàng đều được dùng").isEmpty();

        NhapTheoLop.KetQua lai = theoLop.nhap(lop, chung.nhapGiuBai().bai());
        assertThat(lai.daKiem()).isZero();
        assertThat(lai.phatHanh()).isEqualTo(kq.phatHanh());
        jdbc.sql("set constraints all immediate").update();
    }

    /**
     * Codex #135 (P2): dấu vân tay v0 tính trên bài đã dựng trong bộ nhớ, nên phải chắc CSDL giữ đúng bài đó. Đọc lại bằng SQL
     * (không qua adapter, để lỗi ánh xạ đối xứng ghi / đọc không che nhau): hàng {@code problems}, lời giải và dữ kiện bảo vệ
     * (so cây JSON, vì {@code jsonb} đổi thứ tự khóa), các cấp gợi ý (cấp có nội dung, đã cắt khoảng trắng hai đầu: SP-08).
     */
    @SuppressWarnings("unchecked")
    private void soVoiDaGhi(NhapNoiDungChung.BaiNhap b) {
        List<@Nullable String> de = jdbc.sql("""
                select statement_text, statement_latex, function_sympy, answer_form, start_step, origin, content_hash
                from problems where code = ?""").params(b.ma())
            .query((rs, i) -> Arrays.asList(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7))).single();
        assertThat(de).as("bài đã ghi %s", b.ma()).containsExactly(b.deBai(), b.latex(), b.ham(), b.dangTraLoi(), b.buocBatDau(),
            b.nguonBai(), NhapNoiDungChung.dauVanTay(b));
        // Codex #135 (P2): phân loại dùng để chọn bài; độ khó 0.5 cho mọi bài như seed.ts của v0.
        List<@Nullable String> phanLoai = jdbc.sql("""
                select skill_code, array_to_string(extra_skill_codes, ','), level4, level3, bloom_level, difficulty::text
                from problems where code = ?""").params(b.ma())
            .query((rs, i) -> Arrays.asList(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6))).single();
        var muc3 = b.muc3();
        var bloom = b.bloom();
        assertThat(phanLoai).as("phân loại đã ghi của %s", b.ma()).containsExactly(b.kyNang(), String.join(",", b.kyNangPhu()),
            b.muc4().name(), muc3 == null ? null : muc3.name(), bloom == null ? null : bloom.name(), "0.5");
        List<@Nullable String> loiGiai = jdbc.sql("""
                select s.worked_solution::text, s.protected_facts::text from solutions s join problems p on p.id = s.problem_id
                where p.code = ?""").params(b.ma()).query((rs, i) -> Arrays.asList(rs.getString(1), rs.getString(2))).single();
        assertThat(cay(loiGiai.get(0))).as("lời giải đã ghi của %s", b.ma())
            .isEqualTo(cay(b.baiLam() == null ? null : JsonKieuJs.stringify(b.baiLam())));
        assertThat(cay(loiGiai.get(1))).as("dữ kiện bảo vệ đã ghi của %s", b.ma()).isEqualTo(cay(JsonKieuJs.stringify(b.suKien())));
        List<String> capMongDoi = new ArrayList<>();
        for (Map<String, @Nullable Object> khoi : b.thangGoiY()) {
            for (Map<String, @Nullable Object> c : (List<Map<String, @Nullable Object>>) Objects.requireNonNull(khoi.get("cac_cap"))) {
                String noiDung = c.get("noi_dung") == null ? "" : ((String) c.get("noi_dung")).strip();
                if (!noiDung.isEmpty()) {
                    capMongDoi.add(khoi.get("ma_buoc") + " " + c.get("cap") + " " + noiDung);
                }
            }
        }
        List<String> capDaGhi = jdbc.sql("""
                select h.step_code || ' ' || h.level || ' ' || h.text from hint_levels h join problems p on p.id = h.problem_id
                where p.code = ?""").params(b.ma()).query(String.class).list();
        assertThat(capDaGhi).as("thang gợi ý đã ghi của %s", b.ma()).containsExactlyInAnyOrderElementsOf(capMongDoi);
    }

    private static @Nullable JsonNode cay(@Nullable String json) {
        return json == null ? null : JSON.readTree(json);
    }

    private static Object docJson(String ten) {
        Path tep = NhapNoiDungChungTest.thuMucData().getParent().resolve("specs/001-lat-cat-doc/doi-chieu").resolve(ten);
        try {
            return JSON.readValue(Files.readString(tep), Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Dịch vụ toán phát lại {@code phan-hoi-toan.json}. Yêu cầu không có trong tệp vàng thì ném lỗi (test đỏ), không đoán
     * phản hồi. Đồng hồ tất định tăng 1 ms mỗi lần đọc.
     */
    @TestConfiguration
    static class PhatLai {

        private static final JsonMapper CHUAN = JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
        static final Map<String, Map<String, @Nullable Object>> BANG = new HashMap<>();
        static final Set<String> CHUA_DUNG = new HashSet<>();
        /** Mã băm kho lớp của tệp vàng: sha256(JSON.stringify(khoLop())) trong xuat-v0.ts. */
        static final String BAM_KHO;

        static {
            @SuppressWarnings("unchecked")
            Map<String, Object> nguon = (Map<String, Object>) ((Map<String, Object>) docJson("v0-bai.json")).get("nguon");
            @SuppressWarnings("unchecked")
            Map<String, Object> khoLop = (Map<String, Object>) nguon.get("kho_lop");
            BAM_KHO = (String) khoLop.get("bam");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> goi = (List<Map<String, Object>>) docJson("phan-hoi-toan.json");
            for (Map<String, Object> g : goi) {
                @SuppressWarnings("unchecked")
                Map<String, Object> yeuCau = (Map<String, Object>) g.get("yeu_cau");
                @SuppressWarnings("unchecked")
                Map<String, @Nullable Object> phanHoi = (Map<String, @Nullable Object>) g.get("phan_hoi");
                String khoa = khoa((String) g.get("job"), yeuCau);
                BANG.putIfAbsent(khoa, phanHoi);
                CHUA_DUNG.add(khoa);
            }
        }

        /**
         * Yêu cầu chuẩn hóa: bỏ kho lớp và hết giờ, khóa xếp theo thứ tự. Tệp vàng ghi kho lớp một lần ở v0-bai.json, nên
         * {@link #tra} so riêng kho của mỗi yêu cầu {@code verify} với mã băm đó trước khi phát lại.
         */
        static String khoa(String job, Map<String, ?> yeuCau) {
            Map<String, @Nullable Object> gon = new LinkedHashMap<>(yeuCau);
            gon.remove("tai_lieu");
            gon.remove("cong_thuc");
            gon.remove("timeout_s");
            return job + " " + CHUAN.writeValueAsString(gon);
        }

        static Map<String, @Nullable Object> tra(String job, Map<String, ?> yeuCau) {
            // Codex #135 (P2): kho gửi đi phải đúng kho của tệp vàng (đủ tài liệu, đúng thứ tự, đúng chữ, đúng các dòng bảng),
            // nếu không phản hồi phát lại là phán quyết cho một đầu vào khác.
            if (job.equals("verify")) {
                assertThat(bamKho(yeuCau)).as("kho lớp gửi tới /v1/verify khác kho của tệp vàng (v0-bai.json nguon.kho_lop)")
                    .isEqualTo(BAM_KHO);
            }
            String k = khoa(job, yeuCau);
            Map<String, @Nullable Object> phanHoi = BANG.get(k);
            if (phanHoi == null) {
                throw new AssertionError("Yêu cầu không có trong tệp vàng v0 (core gửi khác seed.ts?): " + k);
            }
            CHUA_DUNG.remove(k);
            return phanHoi;
        }

        /**
         * Kho của một yêu cầu {@code verify}, dựng lại theo thứ tự khóa của {@code khoLop()} trong xuat-v0.ts (Map.of của core
         * không giữ thứ tự khóa) rồi băm như tệp vàng. Mỗi mục phải có đúng các trường đó: thiếu hay thừa trường đều đỏ.
         */
        @SuppressWarnings("unchecked")
        static String bamKho(Map<String, ?> yeuCau) {
            Map<String, @Nullable Object> kho = new LinkedHashMap<>();
            kho.put("tai_lieu", theoThuTu((List<Map<String, ?>>) yeuCau.get("tai_lieu"), "id", "ten", "text", "license_status", "phien_ban"));
            kho.put("cong_thuc", theoThuTu((List<Map<String, ?>>) yeuCau.get("cong_thuc"), "id", "latex", "noi_dung", "ten"));
            return NhapNoiDungChung.sha256(JsonKieuJs.stringify(kho));
        }

        private static List<Map<String, @Nullable Object>> theoThuTu(@Nullable List<Map<String, ?>> muc, String... truong) {
            assertThat(muc).as("kho lớp của yêu cầu verify").isNotNull();
            List<Map<String, @Nullable Object>> ra = new ArrayList<>();
            for (Map<String, ?> m : muc) {
                assertThat(m.keySet()).containsExactlyInAnyOrder(truong);
                Map<String, @Nullable Object> sapXep = new LinkedHashMap<>();
                for (String t : truong) {
                    sapXep.put(t, m.get(t));
                }
                ra.add(sapXep);
            }
            return ra;
        }

        @Bean
        GiaiToan giaiToan() {
            return new GiaiToan() {
                @Override
                public Map<String, @Nullable Object> giai(Map<String, ?> yeuCau) {
                    return tra("solve", yeuCau);
                }

                @Override
                public Map<String, @Nullable Object> sinh(Map<String, ?> yeuCau) {
                    return tra("generate", yeuCau);
                }
            };
        }

        @Bean
        KiemToan kiemToan() {
            return new KiemToan() {
                @Override
                @SuppressWarnings("unchecked")
                public Map<String, @Nullable Object> kiemDongCongThuc(Map<String, ?> yeuCau) {
                    Map<String, Object> taiLieu = ((List<Map<String, Object>>) yeuCau.get("tai_lieu")).getFirst();
                    Map<String, Object> doan = ((List<Map<String, Object>>) taiLieu.get("doan")).getFirst();
                    List<Map<String, Object>> dong = new ArrayList<>();
                    for (Map<String, Object> d : (List<Map<String, Object>>) yeuCau.get("dong")) {
                        dong.add(Map.of("id", d.get("id"), "loai", "DANG_THUC", "tang1", Map.of("trang_thai", "DAT"),
                            "tang2", Map.of("trang_thai", "DAT",
                                "trich_dan", Map.of("tai_lieu", taiLieu.get("id"), "doan", doan.get("id"), "trich", doan.get("text")))));
                    }
                    return Map.of("dong", dong, "bo_qua", List.of());
                }

                @Override
                public Map<String, @Nullable Object> kiemBai(Map<String, ?> yeuCau) {
                    return tra("verify", yeuCau);
                }
            };
        }

        @Bean
        Clock clock() {
            Instant goc = Instant.parse("2026-10-04T08:00:00Z");
            AtomicLong dem = new AtomicLong();
            return new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return goc.plusMillis(dem.incrementAndGet());
                }
            };
        }
    }
}
