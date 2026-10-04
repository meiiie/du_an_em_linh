package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;
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
 * <p>Job khóa bảng ({@code /v1/kiem-dong-cong-thuc}) không có ở v0 nên không có trong tệp vàng: job giả kiểm yêu cầu đúng 6
 * dòng của v0 và 5 tài liệu nguồn của lớp ({@link PhatLai#soYeuCauKhoaBang}), rồi trả mọi dòng đạt hai tầng, trích đoạn đầu
 * của tài liệu đầu của kho; bảng khóa là điều kiện để kiểm bài, không phải đối tượng so ở đây (test của {@code services/math}
 * kiểm job đó với bảng 6 dòng của v0 và 5 tài liệu).
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
    /**
     * Hai mã Bloom v0 ghi mà thang Bloom 6 mức của v2 không có (CHECK của V4): {@code APPLY} của {@code /v1/generate} và
     * {@code NHAN_BIET} (mã mức 4) của bài demo bị chặn. Phép đổi đã chọn ở #122 (javadoc {@code NhapNoiDungChung.bloom}),
     * viết lại ở đây để test không so importer với chính nó.
     */
    private static final Map<Object, Object> BLOOM_V2 = Map.of("APPLY", "VAN_DUNG", "NHAN_BIET", "NHO");
    /**
     * Codex #135 (P2): loại mà dịch vụ toán thật trả cho 6 dòng của bảng v0 ({@code services/math/tests/test_dong_cong_thuc.py},
     * {@code test_bang_v0_dat_ca_6_o_hai_tang_voi_5_tai_lieu}): job khóa bảng giả trả đúng loại từng dòng, để importer gán
     * sai hay làm rơi loại thì {@code formulas.kind} lệch.
     */
    private static final Map<Object, String> LOAI_DONG_V0 = Map.of("d-1", "DANG_THUC", "d-2", "DANG_THUC", "d-3", "DANG_THUC",
        "d-4", "DINH_LI", "d-5", "DINH_LI", "d-6", "DINH_LI");

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

        PhatLai.napLai();
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
            soVoiDaGhi(b, v0.get(b.ma()));
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
            soTangVoiPhanHoi(lop, ma);
        }
        assertThat(PhatLai.chuaDung()).as("mọi bản ghi của tệp vàng được dùng đúng một lần").isEmpty();
        assertThat(PhatLai.soLanKhoaBang()).as("lần nhập đầu khóa bảng một lần").isOne();
        assertThat(jdbc.sql("""
                select f.code || ' ' || f.kind || ' ' || f.tier1_status || ' ' || f.tier2_status from formulas f
                join formula_sheets s on s.id = f.formula_sheet_id where s.class_id = ? and s.status = 'KHOA' order by f.ordinal""")
                .params(lop).query(String.class).list())
            .as("các dòng của bảng đã khóa mang đúng loại job trả")
            .containsExactly("d-1 DANG_THUC DAT DAT", "d-2 DANG_THUC DAT DAT", "d-3 DANG_THUC DAT DAT", "d-4 DINH_LI DAT DAT",
                "d-5 DINH_LI DAT DAT", "d-6 DINH_LI DAT DAT");

        // Lần nhập thứ hai dựng lại bài (solve, generate như lần đầu) nhưng không kiểm lại bài nào: còn nguyên 17 bản ghi verify.
        PhatLai.napLai();
        NhapTheoLop.KetQua lai = theoLop.nhap(lop, chung.nhapGiuBai().bai());
        assertThat(lai.daKiem()).isZero();
        Map<String, Integer> conLai = PhatLai.chuaDung();
        assertThat(conLai.keySet()).as("lần nhập thứ hai chỉ gọi lại solve, generate").allMatch(k -> k.startsWith("verify "));
        assertThat(conLai.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(v0.size());
        // Codex #135 (P2): bảng đang dùng vẫn đúng kho, nên lần nhập thứ hai không gọi lại job khóa bảng.
        assertThat(PhatLai.soLanKhoaBang()).as("lần nhập thứ hai không khóa lại bảng").isZero();
        assertThat(lai.phatHanh()).isEqualTo(kq.phatHanh());
        jdbc.sql("set constraints all immediate").update();
    }

    /**
     * Codex #135 (P2, nhiều lần): so nội dung core đã ghi với nội dung v0 đã ghi, từng cột, không với chính bài core dựng
     * (một cột core dựng sai thì so với chính nó vẫn khớp). Đọc lại bằng SQL, không qua adapter, để lỗi ánh xạ đối xứng ghi /
     * đọc không che nhau. Nguồn so là {@code cot_v0} và {@code su_kien_bao_ve} của tệp vàng: mọi cột nội dung mà mã nạp bài
     * của seed.ts ghi (lời giải và dữ kiện bảo vệ so cây JSON, vì {@code jsonb} đổi thứ tự khóa). Riêng {@code content_hash}
     * là dấu vân tay của v2 (data-model §problems), so với bài đã dựng.
     */
    @SuppressWarnings("unchecked")
    private void soVoiDaGhi(NhapNoiDungChung.BaiNhap b, Map<String, Object> vangCuaBai) {
        Map<String, @Nullable Object> v0 = (Map<String, @Nullable Object>) vangCuaBai.get("cot_v0");
        List<@Nullable Object> cotV2 = jdbc.sql("""
                select skill_code, array_to_string(extra_skill_codes, ','), level4, level3, bloom_level, difficulty::text,
                    statement_text, statement_latex, function_sympy, start_step from problems where code = ?""").params(b.ma())
            .query((rs, i) -> Arrays.<@Nullable Object>asList(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10)))
            .single();
        assertThat(cotV2).as("các cột của bài %s so với v0", b.ma()).containsExactly(v0.get("skillCode"),
            String.join(",", (List<String>) Objects.requireNonNull(v0.get("skillCodesPhu"))), v0.get("mucDo4"), v0.get("mucDoBo3"),
            BLOOM_V2.getOrDefault(v0.get("bloomLevel"), v0.get("bloomLevel")), String.valueOf(v0.get("difficulty")), v0.get("statementText"), v0.get("statementLatex"),
            v0.get("hamSympy"), v0.get("buocBatDau"));
        assertThat(jdbc.sql("select content_hash from problems where code = ?").params(b.ma()).query(String.class).single())
            .as("content_hash của %s", b.ma()).isEqualTo(vanTayDocLap(b));
        List<@Nullable String> loiGiai = jdbc.sql("""
                select s.worked_solution::text, s.protected_facts::text, s.final_answer from solutions s
                join problems p on p.id = s.problem_id where p.code = ?""").params(b.ma())
            .query((rs, i) -> Arrays.asList(rs.getString(1), rs.getString(2), rs.getString(3))).single();
        // Codex #135 (P2): đáp án cuối là nội dung lời giải (lộ theo cài lớp), v0 ghi rõ null.
        assertThat(v0).as("tệp vàng xuất đáp án cuối của %s", b.ma()).containsKey("finalAnswer");
        assertThat(loiGiai.get(2)).as("đáp án cuối đã ghi của %s so với v0", b.ma()).isEqualTo(v0.get("finalAnswer"));
        assertThat(cay(loiGiai.get(0))).as("lời giải đã ghi của %s so với v0", b.ma()).isEqualTo(cayV0(v0.get("baiLam")));
        assertThat(cay(loiGiai.get(1))).as("dữ kiện bảo vệ đã ghi của %s so với v0", b.ma())
            .isEqualTo(cayV0(vangCuaBai.get("su_kien_bao_ve")));
        List<String> capV0 = ((List<Map<String, Object>>) Objects.requireNonNull(v0.get("goiY"))).stream()
            .map(h -> h.get("maBuoc") + " " + h.get("cap") + " " + h.get("noiDung")).toList();
        List<String> capDaGhi = jdbc.sql("""
                select h.step_code || ' ' || h.level || ' ' || h.text from hint_levels h join problems p on p.id = h.problem_id
                where p.code = ?""").params(b.ma()).query(String.class).list();
        assertThat(capDaGhi).as("thang gợi ý đã ghi của %s so với v0", b.ma()).containsExactlyInAnyOrderElementsOf(capV0);
    }

    /**
     * Codex #135 (P2): mỗi dòng tầng đã ghi phải mang đúng bằng chứng của mục {@code tang} tương ứng trong phản hồi
     * {@code /v1/verify} đã phát lại (tìm theo đề bài): trạng thái, loại kết quả, bước sai, mã lỗi, độ tin cậy, lý do, căn
     * cứ ({@code trich_dan}, không có thì {@code cong_thuc}), JSON gốc. Importer không đổi bằng chứng nào khi không có lý do
     * đóng mặc định (ở bộ v0, trạng thái các tầng đã khớp v0 ở trên, nên không có).
     */
    @SuppressWarnings("unchecked")
    private void soTangVoiPhanHoi(UUID lop, String ma) {
        String deBai = jdbc.sql("select statement_text from problems where code = ?").params(ma).query(String.class).single();
        Map<String, @Nullable Object> phanHoi = Objects.requireNonNull(PhatLai.verifyTheoDe(deBai), "phản hồi verify của " + ma);
        Map<Integer, Map<String, Object>> theoTang = new LinkedHashMap<>();
        for (Map<String, Object> t : (List<Map<String, Object>>) Objects.requireNonNull(phanHoi.get("tang"))) {
            theoTang.put(((Number) t.get("tang")).intValue(), t);
        }
        List<List<@Nullable Object>> dong = jdbc.sql("""
                select t.tier, t.status, t.result_type, t.wrong_steps::text, t.error_code, t.confidence, t.reason,
                    t.citation::text, t.raw::text from problem_releases r join problems p on p.id = r.problem_id
                join verification_tier_results t on t.run_id = r.run_id where r.class_id = ? and p.code = ? order by t.tier""")
            .params(lop, ma)
            .query((rs, i) -> {
                // wasNull() nói về cột đọc ngay trước nó: đọc độ tin cậy rồi hỏi liền, trước mọi cột khác (Codex #135).
                float soTinCay = rs.getFloat(6);
                @Nullable Float tinCay = rs.wasNull() ? null : soTinCay;
                return Arrays.<@Nullable Object>asList(rs.getInt(1), rs.getString(2), rs.getString(3), cay(rs.getString(4)),
                    rs.getString(5), tinCay, rs.getString(7), cay(rs.getString(8)), cay(rs.getString(9)));
            })
            .list();
        assertThat(dong).as("số tầng của %s", ma).hasSize(theoTang.size());
        for (List<@Nullable Object> d : dong) {
            Map<String, Object> t = Objects.requireNonNull(theoTang.get((Integer) d.get(0)));
            Object canCu = t.get("trich_dan") != null ? t.get("trich_dan") : t.get("cong_thuc");
            Number tinCay = (Number) t.get("do_tin_cay");
            assertThat(d).as("tầng %s của %s so với phản hồi phát lại", d.get(0), ma).containsExactly(d.get(0), t.get("trang_thai"),
                t.get("loai_ket_qua"), cayV0(t.get("buoc_sai")), t.get("ma_loi"), tinCay == null ? null : tinCay.floatValue(),
                t.get("ly_do"), cayV0(canCu), cayV0(t));
        }
        soTrichDanVoiPhanHoi(lop, ma, theoTang.get(2));
    }

    /**
     * Codex #135 (P2): quan hệ {@code verification_run_citations} của mọi lượt kiểm (lớp, bài) phải đúng các đoạn mà
     * {@code trich_dan} tầng 2 của phản hồi phát lại chỉ tới. Test tự giải vị trí, không qua {@code ChiaDoan}: đọc văn bản
     * tài liệu (mã {@code document_id}) và các đoạn của nó từ CSDL, đổi {@code vi_tri} (điểm mã, như {@code verify.py}) sang
     * chỉ số UTF-16, kiểm chữ trích nằm đúng chỗ, rồi lấy mọi đoạn giao với khoảng trích. Không có trích dẫn thì không có
     * quan hệ nào.
     */
    @SuppressWarnings("unchecked")
    private void soTrichDanVoiPhanHoi(UUID lop, String ma, @Nullable Map<String, Object> tang2) {
        Set<UUID> canCo = new LinkedHashSet<>();
        Object trichDan = tang2 == null ? null : tang2.get("trich_dan");
        for (Map<String, Object> tr : trichDan == null ? List.<Map<String, Object>>of() : (List<Map<String, Object>>) trichDan) {
            String maTaiLieu = (String) tr.get("document_id");
            int viTriDiemMa = ((Number) tr.get("vi_tri")).intValue();
            String trich = (String) tr.get("trich");
            String vanBan = jdbc.sql("select text_content from documents where class_id = ? and code = ?").params(lop, maTaiLieu)
                .query(String.class).single();
            int dau = vanBan.offsetByCodePoints(0, viTriDiemMa);
            int het = dau + trich.length();
            assertThat(vanBan.substring(dau, het)).as("chữ trích của %s ở %s:%s", ma, maTaiLieu, viTriDiemMa).isEqualTo(trich);
            List<UUID> giao = jdbc.sql("""
                    select p.id from document_passages p join documents d on d.id = p.document_id
                    where d.class_id = ? and d.code = ? and p.char_start < ? and ? < p.char_end order by p.char_start""")
                .params(lop, maTaiLieu, het, dau).query(UUID.class).list();
            assertThat(giao).as("đoạn chứa chữ trích của %s ở %s:%s", ma, maTaiLieu, viTriDiemMa).isNotEmpty();
            canCo.addAll(giao);
        }
        List<UUID> luot = jdbc.sql("""
                select r.id from verification_runs r join problems p on p.id = r.subject_id
                where r.subject_kind = 'PROBLEM' and r.class_id = ? and p.code = ?""").params(lop, ma).query(UUID.class).list();
        assertThat(luot).as("lượt kiểm của %s", ma).isNotEmpty();
        for (UUID run : luot) {
            assertThat(jdbc.sql("select passage_id from verification_run_citations where run_id = ?").params(run).query(UUID.class).list())
                .as("đoạn căn cứ của lượt %s (%s) so với trich_dan phát lại", run, ma).containsExactlyInAnyOrderElementsOf(canCo);
        }
    }

    /**
     * Codex #135 (P2): dấu vân tay v2 tính độc lập theo định nghĩa (data-model §problems, đề, LaTeX, hàm, dạng trả lời, bước
     * bắt đầu, lời giải, dữ kiện bảo vệ, thang gợi ý), bằng Jackson, không qua {@code dauVanTay} hay {@code JsonKieuJs}: hàm
     * sản phẩm bỏ sót một trường thì lệch. Các trường của bài đã dựng đều được so với v0 ở trên (cột, cây lời giải, cây dữ
     * kiện, các cấp gợi ý). Độ nhạy từng trường: {@code DauVanTayTest}.
     */
    private static String vanTayDocLap(NhapNoiDungChung.BaiNhap b) {
        Map<String, @Nullable Object> noiDung = new LinkedHashMap<>();
        noiDung.put("de", b.deBai());
        noiDung.put("latex", b.latex());
        noiDung.put("ham", b.ham());
        noiDung.put("dang", b.dangTraLoi());
        noiDung.put("buoc", b.buocBatDau());
        noiDung.put("bl", b.baiLam());
        noiDung.put("su_kien", b.suKien());
        noiDung.put("hints", b.thangGoiY());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(noiDung)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static @Nullable JsonNode cay(@Nullable String json) {
        return json == null ? null : JSON.readTree(json);
    }

    /** Giá trị của tệp vàng thành cây JSON; trống (v0 không ghi lời giải) thì {@code null} như cột CSDL trống. */
    private static @Nullable JsonNode cayV0(@Nullable Object giaTri) {
        return giaTri == null ? null : JSON.valueToTree(giaTri);
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
     * phản hồi. Mỗi bản ghi dùng được đúng một lần: yêu cầu trùng khóa (v0 gọi {@code solve} cho {@code x**2} hai lần) có đủ
     * số bản ghi, phát lại theo thứ tự ghi, gọi nhiều hơn thì ném lỗi. Đồng hồ tất định tăng 1 ms mỗi lần đọc.
     */
    @TestConfiguration
    static class PhatLai {

        private static final JsonMapper CHUAN = JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
        /** Bản ghi của tệp vàng theo khóa yêu cầu, theo thứ tự ghi. */
        static final Map<String, List<Map<String, @Nullable Object>>> BANG = new LinkedHashMap<>();
        /** Bản ghi chưa dùng của lượt nhập hiện tại. */
        static final Map<String, Deque<Map<String, @Nullable Object>>> CON = new HashMap<>();
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
                BANG.computeIfAbsent(khoa((String) g.get("job"), yeuCau), k -> new ArrayList<>()).add(phanHoi);
            }
        }

        /** Số lần gọi job khóa bảng ở lượt nhập hiện tại. */
        private static int khoaBang;

        /** Bắt đầu một lượt nhập: mọi bản ghi của tệp vàng lại dùng được, mỗi bản một lần; đếm lại số lần khóa bảng. */
        static synchronized void napLai() {
            khoaBang = 0;
            CON.clear();
            BANG.forEach((k, v) -> CON.put(k, new ArrayDeque<>(v)));
        }

        static synchronized int soLanKhoaBang() {
            return khoaBang;
        }

        static synchronized void demKhoaBang() {
            khoaBang++;
        }

        /** Khóa còn bản ghi chưa dùng ở lượt hiện tại, kèm số bản còn lại. */
        static synchronized Map<String, Integer> chuaDung() {
            Map<String, Integer> con = new LinkedHashMap<>();
            CON.forEach((k, v) -> {
                if (!v.isEmpty()) {
                    con.put(k, v.size());
                }
            });
            return con;
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

        /** Phản hồi verify đã phát lại ở lượt hiện tại, theo đề bài của yêu cầu. */
        private static final Map<Object, Map<String, @Nullable Object>> VERIFY_THEO_DE = new HashMap<>();

        static synchronized @Nullable Map<String, @Nullable Object> verifyTheoDe(String deBai) {
            return VERIFY_THEO_DE.get(deBai);
        }

        static synchronized Map<String, @Nullable Object> tra(String job, Map<String, ?> yeuCau) {
            // Codex #135 (P2): kho gửi đi phải đúng kho của tệp vàng (đủ tài liệu, đúng thứ tự, đúng chữ, đúng các dòng bảng),
            // nếu không phản hồi phát lại là phán quyết cho một đầu vào khác.
            if (job.equals("verify")) {
                assertThat(bamKho(yeuCau)).as("kho lớp gửi tới /v1/verify khác kho của tệp vàng (v0-bai.json nguon.kho_lop)")
                    .isEqualTo(BAM_KHO);
            }
            String k = khoa(job, yeuCau);
            Deque<Map<String, @Nullable Object>> con = CON.get(k);
            if (con == null) {
                throw new AssertionError("Yêu cầu không có trong tệp vàng v0 (core gửi khác seed.ts?): " + k);
            }
            Map<String, @Nullable Object> phanHoi = con.poll();
            if (phanHoi == null) {
                throw new AssertionError("Yêu cầu được gọi nhiều lần hơn tệp vàng v0 ghi: " + k);
            }
            if (job.equals("verify")) {
                VERIFY_THEO_DE.put(Objects.requireNonNull(yeuCau.get("de_bai")), phanHoi);
            }
            return phanHoi;
        }

        /**
         * Codex #135 (P2): job khóa bảng giả chỉ trả {@code DAT} cho đúng đầu vào của v0, vì dịch vụ toán thật có thể bác
         * hay phân loại khác khi core gửi sai. Các dòng phải đúng 6 dòng của {@code data/v0/bang-cong-thuc.json} (mã, tiêu
         * đề, LaTeX, phát biểu); tài liệu phải đúng 5 tài liệu nguồn của lớp theo thứ tự (tên, quyền dùng) và đoạn của mỗi
         * tài liệu là văn bản nguồn (NFC) tách câu, có id.
         */
        @SuppressWarnings("unchecked")
        static void soYeuCauKhoaBang(Map<String, ?> yeuCau) {
            Path data = NhapNoiDungChungTest.thuMucData();
            Map<String, Object> tepBang = (Map<String, Object>) docTep(data.resolve("v0/bang-cong-thuc.json"));
            List<Map<String, Object>> bang = (List<Map<String, Object>>) tepBang.get("formulas");
            assertThat((List<Object>) yeuCau.get("dong")).as("các dòng gửi tới /v1/kiem-dong-cong-thuc").isEqualTo(bang.stream()
                .map(f -> Map.of("id", f.get("ma"), "tieu_de", f.get("title"), "latex", f.get("latex"), "phat_bieu", f.get("noiDung")))
                .toList());
            List<Map<String, Object>> nguon = new ArrayList<>((List<Map<String, Object>>) docTep(data.resolve("v0/tai-lieu.json")));
            for (String lab : List.of("sp-tai-lieu-0001", "sp-tai-lieu-0002")) {
                nguon.add((Map<String, Object>) docTep(data.resolve("supham/tai-lieu/" + lab + ".json")));
            }
            List<Map<String, Object>> gui = (List<Map<String, Object>>) yeuCau.get("tai_lieu");
            assertThat(gui).as("tài liệu gửi tới /v1/kiem-dong-cong-thuc").hasSize(nguon.size());
            for (int i = 0; i < nguon.size(); i++) {
                Map<String, Object> g = gui.get(i);
                Map<String, Object> n = nguon.get(i);
                assertThat(g.keySet()).as("tài liệu %s", n.get("ma")).containsExactlyInAnyOrder("id", "ten", "doan", "license_status");
                assertThat(UUID.fromString((String) g.get("id"))).isNotNull();
                assertThat(List.of(g.get("ten"), g.get("license_status"))).as("tài liệu %s", n.get("ma"))
                    .containsExactly(n.get("title"), n.get("licenseStatus"));
                String vanBan = java.text.Normalizer.normalize((String) n.get("textContent"), java.text.Normalizer.Form.NFC);
                List<Map<String, Object>> doan = (List<Map<String, Object>>) g.get("doan");
                assertThat(doan.stream().map(d -> d.get("text")).toList()).as("đoạn của tài liệu %s", n.get("ma"))
                    .isEqualTo(ChiaDoan.theoCau(UUID.randomUUID(), vanBan).stream().map(DocumentPassage::text).toList());
                assertThat(doan).allSatisfy(d -> assertThat(UUID.fromString((String) d.get("id"))).isNotNull());
            }
        }

        private static Object docTep(Path tep) {
            try {
                return JSON.readValue(Files.readString(tep), Object.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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
                    demKhoaBang();
                    soYeuCauKhoaBang(yeuCau);
                    Map<String, Object> taiLieu = ((List<Map<String, Object>>) yeuCau.get("tai_lieu")).getFirst();
                    Map<String, Object> doan = ((List<Map<String, Object>>) taiLieu.get("doan")).getFirst();
                    List<Map<String, Object>> dong = new ArrayList<>();
                    for (Map<String, Object> d : (List<Map<String, Object>>) yeuCau.get("dong")) {
                        dong.add(Map.of("id", d.get("id"), "loai", Objects.requireNonNull(LOAI_DONG_V0.get(d.get("id")), "loại của dòng"),
                            "tang1", Map.of("trang_thai", "DAT"),
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
