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
        soBangDaKhoaVoiPhanHoiThat(lop);
        soDanhMucVaKhoLop(lop, v0);
        // Phán quyết độc lập #135 (N4): tập bảng của CACH_KIEM không suy ra từ chính nó. Mọi bảng có dòng sau lần nhập, trừ
        // bảng lớp do test tự ghi, lịch sử Flyway và tham số BKT do chính V9 ghi, phải đúng là các bảng đã khai: importer ghi
        // thêm bảng thì đỏ.
        List<String> coDong = jdbc.sql("""
                select table_name from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE'
                and table_name not in ('classes', 'flyway_schema_history', 'mastery_config') order by 1""").query(String.class).list().stream()
            .filter(b -> jdbc.sql("select exists (select 1 from " + b + ")").query(Boolean.class).single()).toList();
        assertThat(coDong).as("bảng có dòng sau lần nhập so với bảng đã khai cách kiểm").containsExactlyInAnyOrderElementsOf(
            CACH_KIEM.keySet().stream().map(k -> k.substring(0, k.indexOf('.'))).distinct().toList());
        Map<String, List<String>> sauLanDau = chupBang(lop);

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
        // «Nhập hai lần cùng kết quả» trên chính CSDL: mọi dòng của mọi bảng importer ghi giống hệt sau lần thứ hai.
        Map<String, List<String>> sauLanHai = chupBang(lop);
        sauLanDau.forEach((bang, dong) -> assertThat(sauLanHai.get(bang))
            .as("dòng của %s sau lần nhập thứ hai so với sau lần đầu", bang).containsExactlyElementsOf(dong));
        jdbc.sql("set constraints all immediate").update();
    }

    /**
     * Codex #135, tấn công tiền đề: 19 vòng góp ý đều là «test chưa so cột X», vì test liệt kê tay cột cần so. Bảng này khai
     * cách kiểm của từng cột mà importer ghi: tên phương thức so của test này, hay «v2: …» khi giá trị do v2 sinh và không có
     * bản tương ứng ở v0 hay tệp nguồn. {@link #moiCotImporterGhiDeuCoCachKiem} đọc cột thật từ CSDL và đỏ khi có cột chưa
     * khai, nên thêm cột mà quên kiểm thì test chặn, không chờ người rà phát hiện.
     */
    private static final Map<String, String> CACH_KIEM = new LinkedHashMap<>();

    static {
        String v2Id = "v2: khóa chính hay khóa ngoại UUID do v2 sinh; các phép so nối bảng qua nó";
        String v2Luc = "v2: thời điểm ghi theo đồng hồ của lần nhập";
        khai("topics", "soDanhMucVaKhoLop", "code", "name", "grade");
        khai("skills", "soDanhMucVaKhoLop", "code", "topic_code", "name", "description", "grade", "is_core");
        khai("skill_prerequisites", "soDanhMucVaKhoLop", "skill_code", "prerequisite_code", "min_level");
        khai("step_templates", "soDanhMucVaKhoLop", "step_code", "topic_code", "ordinal", "input_kind", "skill_code", "description");
        khai("error_types", "soDanhMucVaKhoLop", "code", "skill_code", "step_code", "name", "fix_hint", "result_types");
        khai("problems", "soVoiDaGhi", "code", "skill_code", "extra_skill_codes", "level4", "level3", "bloom_level", "difficulty",
            "statement_text", "statement_latex", "function_sympy", "start_step");
        khai("problems", "nhapNhuV0TrenCungDauVaoVaNhapHaiLanCungKetQua", "origin", "answer_form");
        khai("problems", "vanTayDocLap", "content_hash");
        // content_version: bằng phiên bản của lượt kiểm và không đổi khi nhập lại (chupBang). Không so với 1: V5 tăng theo
        // từng dòng lời giải / gợi ý trong cùng lần ghi (#141).
        khai("problems", "soDanhMucVaKhoLop", "content_version", "created_by");
        khai("problems", v2Id, "id");
        khai("problems", v2Luc, "created_at", "updated_at");
        khai("solutions", "soVoiDaGhi", "problem_id", "worked_solution", "protected_facts", "final_answer");
        khai("hint_levels", "soVoiDaGhi", "problem_id", "step_code", "level", "text");
        khai("documents", "soDanhMucVaKhoLop", "class_id", "code", "title", "kind", "source", "license_status", "file_ref", "text_content",
            "version", "uploaded_by");
        khai("documents", v2Id, "id");
        khai("documents", v2Luc, "created_at");
        khai("document_passages", "soDanhMucVaKhoLop", "document_id", "page", "char_start", "char_end", "text");
        khai("document_passages", v2Id, "id");
        khai("document_passages", "v2: chữ đã gấp (thường, bỏ dấu) dẫn xuất từ text để tìm kiếm; text đã được so", "text_folded");
        khai("formula_sheets", "soDanhMucVaKhoLop", "class_id", "version", "status", "locked_by");
        khai("formula_sheets", v2Id, "id");
        khai("formula_sheets", v2Luc, "created_at", "locked_at");
        khai("formula_sheets", "v2: ghi chú đánh dấu bảng của importer và dấu vân tay các dòng lúc khóa; NhapTheoLopTest kiểm chúng",
            "note", "fingerprint");
        khai("formulas", "soDanhMucVaKhoLop", "formula_sheet_id", "ordinal", "code", "skill_code", "title", "latex", "statement");
        khai("formulas", "soBangDaKhoaVoiPhanHoiThat", "kind", "tier1_status", "tier2_status", "tier1_detail", "tier2_detail",
            "citation_passage_id");
        khai("formulas", v2Id, "id");
        khai("formulas", "v2: dấu vân tay nội dung dòng lúc kiểm (Formula.contentFingerprint), trigger V4 giữ nó khớp nội dung",
            "checked_fingerprint");
        khai("formula_citations", "soBangDaKhoaVoiPhanHoiThat", "formula_id", "passage_id");
        khai("verification_runs", "soDanhMucVaKhoLop", "class_id", "subject_kind", "subject_id", "content_hash", "formula_sheet_id",
            "formula_sheet_status", "publish_status", "stale", "content_version");
        khai("verification_runs", "nhapNhuV0TrenCungDauVaoVaNhapHaiLanCungKetQua", "overall_status");
        khai("verification_runs", v2Id, "id");
        khai("verification_runs", v2Luc, "created_at");
        khai("verification_tier_results", "soTangVoiPhanHoi", "run_id", "tier", "status", "result_type", "wrong_steps", "error_code",
            "confidence", "reason", "citation", "raw");
        khai("verification_run_citations", "soTrichDanVoiPhanHoi", "run_id", "passage_id");
        khai("problem_releases", "soDanhMucVaKhoLop", "class_id", "problem_id", "status", "run_id");
        khai("problem_releases", v2Luc, "updated_at");
    }

    private static void khai(String bang, String cach, String... cot) {
        for (String c : cot) {
            assertThat(CACH_KIEM.put(bang + "." + c, cach)).as("khai hai lần %s.%s", bang, c).isNull();
        }
    }

    @Test
    void moiCotImporterGhiDeuCoCachKiem() {
        List<String> bang = CACH_KIEM.keySet().stream().map(k -> k.substring(0, k.indexOf('.'))).distinct().toList();
        List<String> cotThat = jdbc.sql("""
                select table_name || '.' || column_name from information_schema.columns
                where table_schema = 'public' and table_name in (:bang) order by 1""").param("bang", bang).query(String.class).list();
        assertThat(cotThat).as("cột của các bảng importer ghi, so với bảng khai cách kiểm")
            .containsExactlyInAnyOrderElementsOf(CACH_KIEM.keySet());
        Set<String> phuongThuc = Arrays.stream(NhapNoiDungTest.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName)
            .collect(java.util.stream.Collectors.toSet());
        CACH_KIEM.forEach((cot, cach) -> assertThat(cach.startsWith("v2: ") || phuongThuc.contains(cach))
            .as("%s khai cách kiểm «%s»: phải là phương thức so của test hay «v2: lý do»", cot, cach).isTrue());
    }

    /**
     * Mọi dòng của các bảng importer ghi (danh mục và nội dung chung toàn bộ, phần của lớp theo {@code lop}), mỗi dòng là
     * {@code row_to_json} sắp theo chữ, để so hai lần nhập.
     */
    private Map<String, List<String>> chupBang(UUID lop) {
        Map<String, String> phamVi = new LinkedHashMap<>();
        for (String b : List.of("topics", "skills", "skill_prerequisites", "step_templates", "error_types", "problems", "solutions",
                "hint_levels")) {
            phamVi.put(b, "select row_to_json(t)::text from " + b + " t");
        }
        phamVi.put("documents", "select row_to_json(t)::text from documents t where t.class_id = :lop");
        phamVi.put("document_passages", """
                select row_to_json(t)::text from document_passages t join documents d on d.id = t.document_id where d.class_id = :lop""");
        phamVi.put("formula_sheets", "select row_to_json(t)::text from formula_sheets t where t.class_id = :lop");
        phamVi.put("formulas", """
                select row_to_json(t)::text from formulas t join formula_sheets s on s.id = t.formula_sheet_id where s.class_id = :lop""");
        phamVi.put("formula_citations", """
                select row_to_json(t)::text from formula_citations t join formulas f on f.id = t.formula_id
                join formula_sheets s on s.id = f.formula_sheet_id where s.class_id = :lop""");
        phamVi.put("verification_runs", "select row_to_json(t)::text from verification_runs t where t.class_id = :lop");
        phamVi.put("verification_tier_results", """
                select row_to_json(t)::text from verification_tier_results t join verification_runs r on r.id = t.run_id
                where r.class_id = :lop""");
        phamVi.put("verification_run_citations", """
                select row_to_json(t)::text from verification_run_citations t join verification_runs r on r.id = t.run_id
                where r.class_id = :lop""");
        phamVi.put("problem_releases", "select row_to_json(t)::text from problem_releases t where t.class_id = :lop");
        Map<String, List<String>> anh = new LinkedHashMap<>();
        phamVi.forEach((b, sql) -> anh.put(b, (sql.contains(":lop") ? jdbc.sql(sql).param("lop", lop) : jdbc.sql(sql)).query(String.class)
            .list().stream().sorted().toList()));
        assertThat(anh.keySet()).as("bảng được chụp là đúng các bảng đã khai").containsExactlyInAnyOrderElementsOf(
            CACH_KIEM.keySet().stream().map(k -> k.substring(0, k.indexOf('.'))).distinct().toList());
        return anh;
    }

    /**
     * Phần importer ghi mà các phép so khác chưa phủ, so với tệp nguồn ({@code data/supham}, {@code data/v0}), tệp vàng
     * khóa bảng (chữ từng đoạn do script Python chia) và tệp vàng v0 (trạng thái phát hành): danh mục; tài liệu, đoạn, bảng
     * công thức của lớp; thuộc tính lượt kiểm và phát hành đã ghi.
     */
    @SuppressWarnings("unchecked")
    private void soDanhMucVaKhoLop(UUID lop, Map<String, Map<String, Object>> v0) {
        Path data = NhapNoiDungChungTest.thuMucData();
        Map<String, Object> danhMuc = (Map<String, Object>) PhatLai.docTep(data.resolve("supham/danh-muc-ky-nang-DH.json"));
        List<Map<String, Object>> kyNang = (List<Map<String, Object>>) danhMuc.get("ky_nang");
        assertThat(dong("select code || '|' || name || '|' || grade from topics")).as("chủ đề")
            .containsExactly("DH12|" + danhMuc.get("chu_de") + "|12");
        assertThat(dong("select concat_ws('|', code, topic_code, name, coalesce(description, '∅'), grade, is_core::text) from skills"))
            .as("kỹ năng").containsExactlyInAnyOrderElementsOf(kyNang.stream().map(k -> String.join("|", (String) k.get("ma"), "DH12",
                (String) k.get("ten"), k.get("yccd_gdpt2018") == null ? "∅" : (String) k.get("yccd_gdpt2018"), String.valueOf(k.get("lop")),
                String.valueOf(Boolean.TRUE.equals(k.get("la_cot_loi_chu_de"))))).toList());
        assertThat(dong("select concat_ws('|', skill_code, prerequisite_code, coalesce(min_level, '∅')) from skill_prerequisites"))
            .as("tiên quyết").containsExactlyInAnyOrderElementsOf(kyNang.stream().flatMap(k -> ((List<Map<String, Object>>) k.get("tien_quyet"))
                .stream().map(t -> String.join("|", (String) k.get("ma"), (String) t.get("ma"),
                    t.get("muc_toi_thieu") == null ? "∅" : (String) t.get("muc_toi_thieu")))).toList());
        List<Map<String, Object>> khung = (List<Map<String, Object>>) PhatLai.docTep(data.resolve("v0/khung-buoc.json"));
        assertThat(dong("select concat_ws('|', step_code, topic_code, ordinal, input_kind, coalesce(skill_code, '∅'), description) from step_templates"))
            .as("khung bước").containsExactlyInAnyOrderElementsOf(khung.stream().map(b -> String.join("|", (String) b.get("maBuoc"),
                (String) b.get("topicCode"), String.valueOf(b.get("thuTu")), (String) b.get("dangNhap"),
                b.get("skillCode") == null ? "∅" : (String) b.get("skillCode"), (String) b.get("moTa"))).toList());
        List<List<String>> csv = docCsv(data.resolve("supham/ma-loi-DH.csv"));
        List<String> cot = csv.getFirst();
        List<String> maLoi = new ArrayList<>();
        for (List<String> h : csv.subList(1, csv.size())) {
            String ma = h.get(cot.indexOf("ma_loi")).strip();
            if (ma.isEmpty()) {
                continue;
            }
            java.util.function.Function<String, String> o = ten -> h.get(cot.indexOf(ten)).strip().isEmpty() ? "∅" : h.get(cot.indexOf(ten)).strip();
            maLoi.add(String.join("|", ma, o.apply("ky_nang_chinh"), o.apply("ma_buoc"), h.get(cot.indexOf("mo_ta")).strip(),
                o.apply("goi_y_sua"), Arrays.stream(h.get(cot.indexOf("loai_ket_qua_lien_quan")).split("\\|")).map(String::strip)
                    .filter(x -> !x.isEmpty()).collect(java.util.stream.Collectors.joining(","))));
        }
        assertThat(dong("""
                select concat_ws('|', code, coalesce(skill_code, '∅'), coalesce(step_code, '∅'), name, coalesce(fix_hint, '∅'),
                    array_to_string(result_types, ',')) from error_types"""))
            .as("mã lỗi").containsExactlyInAnyOrderElementsOf(maLoi);

        List<Map<String, Object>> taiLieu = new ArrayList<>((List<Map<String, Object>>) PhatLai.docTep(data.resolve("v0/tai-lieu.json")));
        for (String lab : List.of("sp-tai-lieu-0001", "sp-tai-lieu-0002")) {
            taiLieu.add((Map<String, Object>) PhatLai.docTep(data.resolve("supham/tai-lieu/" + lab + ".json")));
        }
        Map<String, Object> chuDoan = (Map<String, Object>) ((Map<String, Object>) docJson("khoa-bang-v0.json")).get("doan");
        for (Map<String, Object> t : taiLieu) {
            String ma = (String) t.get("ma");
            String vanBan = java.text.Normalizer.normalize((String) t.get("textContent"), java.text.Normalizer.Form.NFC);
            assertThat(jdbc.sql("""
                    select concat_ws('|', title, kind, coalesce(source, '∅'), license_status, coalesce(file_ref, '∅'), version,
                        coalesce(uploaded_by::text, '∅')), text_content from documents where class_id = ? and code = ?""")
                    .params(lop, ma).query((rs, i) -> List.of(rs.getString(1), rs.getString(2))).single())
                .as("tài liệu %s", ma).containsExactly(String.join("|", (String) t.get("title"), (String) t.get("kind"),
                    t.get("source") == null ? "∅" : (String) t.get("source"), (String) t.get("licenseStatus"), "∅",
                    String.valueOf(t.get("version")), "∅"), vanBan);
            List<String> mongDoi = new ArrayList<>();
            int tu = 0;
            for (int k = 0; chuDoan.containsKey(ma + "#" + k); k++) {
                String chu = (String) chuDoan.get(ma + "#" + k);
                int dau = vanBan.indexOf(chu, tu);
                assertThat(dau).as("đoạn %s#%d nằm trong văn bản nguồn", ma, k).isNotNegative();
                mongDoi.add(dau + "|" + (dau + chu.length()) + "|∅|" + chu);
                tu = dau + chu.length();
            }
            assertThat(jdbc.sql("""
                    select concat_ws('|', p.char_start, p.char_end, coalesce(p.page::text, '∅'), p.text) from document_passages p
                    join documents d on d.id = p.document_id where d.class_id = ? and d.code = ? order by p.char_start""")
                    .params(lop, ma).query(String.class).list())
                .as("đoạn của tài liệu %s so với chữ Python chia trong tệp vàng khóa bảng", ma).isEqualTo(mongDoi);
        }
        assertThat(jdbc.sql("select count(*) from documents where class_id = ?").params(lop).query(Integer.class).single())
            .as("số tài liệu của lớp").isEqualTo(taiLieu.size());

        assertThat(jdbc.sql("""
                select concat_ws('|', version, status, coalesce(locked_by::text, '∅')) from formula_sheets where class_id = ?""")
                .params(lop).query(String.class).list())
            .as("bảng công thức của lớp").containsExactly("1|KHOA|∅");
        List<Map<String, Object>> dongBang = (List<Map<String, Object>>) ((Map<String, Object>) PhatLai.docTep(
            data.resolve("v0/bang-cong-thuc.json"))).get("formulas");
        List<String> mongDoiDong = new ArrayList<>();
        for (int i = 0; i < dongBang.size(); i++) {
            Map<String, Object> f = dongBang.get(i);
            mongDoiDong.add(String.join("|", String.valueOf(i + 1), (String) f.get("ma"), (String) f.get("skillCode"),
                (String) f.get("title"), (String) f.get("latex"), (String) f.get("noiDung")));
        }
        assertThat(jdbc.sql("""
                select concat_ws('|', f.ordinal, f.code, coalesce(f.skill_code, '∅'), f.title, f.latex, f.statement) from formulas f
                join formula_sheets s on s.id = f.formula_sheet_id where s.class_id = ? order by f.ordinal""")
                .params(lop).query(String.class).list())
            .as("các dòng của bảng công thức so với data/v0/bang-cong-thuc.json").isEqualTo(mongDoiDong);

        List<String> luot = new ArrayList<>();
        List<String> phatHanh = new ArrayList<>();
        v0.values().stream().sorted(java.util.Comparator.comparing(b -> (String) b.get("ma"))).forEach(b -> {
            luot.add(b.get("ma") + "|PROBLEM|true|true|true|true|KHOA|" + b.get("trang_thai_phat_hanh") + "|false|∅");
            phatHanh.add(b.get("ma") + "|" + b.get("trang_thai_phat_hanh") + "|true");
        });
        assertThat(jdbc.sql("""
                select concat_ws('|', p.code, v.subject_kind, (v.subject_id = p.id)::text, (v.content_hash = p.content_hash)::text,
                    (v.content_version = p.content_version)::text, coalesce((v.formula_sheet_id = s.id)::text, '∅'), v.formula_sheet_status,
                    coalesce(v.publish_status, '∅'), v.stale::text, coalesce(p.created_by::text, '∅'))
                from verification_runs v join problems p on p.id = v.subject_id
                join formula_sheets s on s.class_id = v.class_id and s.status = 'KHOA'
                where v.class_id = ? order by p.code""").params(lop).query(String.class).list())
            .as("lượt kiểm của lớp (một mỗi bài), phiên bản lượt bằng phiên bản bài, người tạo của bài").isEqualTo(luot);
        assertThat(jdbc.sql("""
                select concat_ws('|', p.code, r.status, (r.run_id = v.id)::text) from problem_releases r join problems p on p.id = r.problem_id
                join verification_runs v on v.class_id = r.class_id and v.subject_id = r.problem_id
                where r.class_id = ? order by p.code""").params(lop).query(String.class).list())
            .as("phát hành đã ghi của lớp so với v0").isEqualTo(phatHanh);
    }

    private List<String> dong(String sql) {
        return jdbc.sql(sql).query(String.class).list();
    }

    /** CSV theo RFC 4180 (ngoặc kép, «""» là một dấu ngoặc kép), độc lập với bộ đọc của importer. */
    private static List<List<String>> docCsv(Path tep) {
        String chu;
        try {
            chu = Files.readString(tep);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<List<String>> hang = new ArrayList<>();
        List<String> o = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean trongNgoac = false;
        for (int i = 0; i < chu.length(); i++) {
            char c = chu.charAt(i);
            if (trongNgoac) {
                if (c == '"' && i + 1 < chu.length() && chu.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    trongNgoac = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                trongNgoac = true;
            } else if (c == ',') {
                o.add(cur.toString());
                cur.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < chu.length() && chu.charAt(i + 1) == '\n') {
                    i++;
                }
                o.add(cur.toString());
                cur.setLength(0);
                if (!(o.size() == 1 && o.getFirst().isEmpty())) {
                    hang.add(o);
                }
                o = new ArrayList<>();
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0 || !o.isEmpty()) {
            o.add(cur.toString());
            hang.add(o);
        }
        return hang;
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
     * Codex #135 (P2): các dòng của bảng đã khóa mang đúng loại, hai tầng, trích dẫn chính ({@code citation_passage_id}) và
     * trích dẫn thêm ({@code formula_citations}) mà job khóa bảng thật trả cho từng dòng ({@code khoa-bang-v0.json}). Đoạn đã
     * ghi đọc lại thành «mã tài liệu#vị trí» bằng SQL (vị trí theo {@code char_start} trong tài liệu), không qua importer.
     * Chi tiết hai tầng ({@code tier1_detail}, {@code tier2_detail}) phải đúng nguyên {@code tang1}, {@code tang2} của phản
     * hồi, sau khi đổi id thật về id ổn định của tệp vàng (so cây JSON, vì {@code jsonb} đổi thứ tự khóa).
     */
    @SuppressWarnings("unchecked")
    private void soBangDaKhoaVoiPhanHoiThat(UUID lop) {
        Map<UUID, String> maDoan = new HashMap<>();
        jdbc.sql("""
                select p.id, d.code || '#' || (row_number() over (partition by p.document_id order by p.char_start) - 1)
                from document_passages p join documents d on d.id = p.document_id where d.class_id = ?""").params(lop)
            .query((rs, i) -> maDoan.put(rs.getObject(1, UUID.class), rs.getString(2))).list();
        Map<String, String> idOnDinh = new HashMap<>();
        maDoan.forEach((id, ma) -> idOnDinh.put(id.toString(), ma));
        jdbc.sql("select id, code from documents where class_id = ?").params(lop)
            .query((rs, i) -> idOnDinh.put(rs.getObject(1, UUID.class).toString(), rs.getString(2))).list();
        Map<String, List<@Nullable JsonNode>> chiTietDaGhi = new LinkedHashMap<>();
        jdbc.sql("""
                select f.code, f.tier1_detail::text, f.tier2_detail::text from formulas f
                join formula_sheets s on s.id = f.formula_sheet_id where s.class_id = ? and s.status = 'KHOA' order by f.ordinal""")
            .params(lop)
            .query((rs, i) -> chiTietDaGhi.put(rs.getString(1), Arrays.asList(veIdOnDinh(rs.getString(2), idOnDinh),
                veIdOnDinh(rs.getString(3), idOnDinh))))
            .list();
        Map<String, List<@Nullable JsonNode>> chiTietVang = new LinkedHashMap<>();
        List<String> daGhi = jdbc.sql("""
                select f.id, f.code, f.kind, f.tier1_status, f.tier2_status, f.citation_passage_id from formulas f
                join formula_sheets s on s.id = f.formula_sheet_id where s.class_id = ? and s.status = 'KHOA' order by f.ordinal""")
            .params(lop)
            .query((rs, i) -> List.of(rs.getObject(1, UUID.class).toString(), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), String.valueOf(maDoan.get(rs.getObject(6, UUID.class)))))
            .list()
            .stream()
            .map(f -> {
                List<String> them = jdbc.sql("select passage_id from formula_citations where formula_id = ?").params(UUID.fromString(f.get(0)))
                    .query(UUID.class).list().stream().map(maDoan::get).sorted().toList();
                return String.join(" ", f.get(1), f.get(2), f.get(3), f.get(4), f.get(5), them.toString());
            })
            .toList();
        List<String> mongDoi = new ArrayList<>();
        for (Map<String, Object> d : (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) docJson("khoa-bang-v0.json"))
                .get("phan_hoi")).get("dong")) {
            Map<String, Object> t2 = (Map<String, Object>) d.get("tang2");
            String chinh = (String) ((Map<String, Object>) t2.get("trich_dan")).get("doan");
            List<String> them = ((List<Map<String, Object>>) Objects.requireNonNullElse(t2.get("trich_dan_them"), List.of())).stream()
                .map(x -> (String) x.get("doan")).filter(x -> !x.equals(chinh)).distinct().sorted().toList();
            mongDoi.add(String.join(" ", (String) d.get("id"), (String) d.get("loai"),
                (String) ((Map<String, Object>) d.get("tang1")).get("trang_thai"), (String) t2.get("trang_thai"), chinh, them.toString()));
            chiTietVang.put((String) d.get("id"), Arrays.asList(cayV0(d.get("tang1")), cayV0(t2)));
        }
        assertThat(chiTietDaGhi).as("chi tiết hai tầng của các dòng bảng đã khóa so với phản hồi thật").isEqualTo(chiTietVang);
        assertThat(mongDoi).as("tệp vàng khóa bảng có trích dẫn thêm").anyMatch(m -> !m.endsWith("[]"));
        assertThat(daGhi).as("các dòng của bảng đã khóa so với phản hồi thật của job khóa bảng").isEqualTo(mongDoi);
    }

    /** Cây JSON của chi tiết tầng đã ghi, id thật (tài liệu, đoạn) đổi về id ổn định của tệp vàng; id lạ thì ném. */
    private static @Nullable JsonNode veIdOnDinh(@Nullable String json, Map<String, String> idOnDinh) {
        return json == null ? null : JSON.valueToTree(PhatLai.doiId(JSON.readValue(json, Object.class), idOnDinh));
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

        /**
         * Codex #135 (P2): phản hồi thật của job khóa bảng cho đúng yêu cầu này ({@code khoa-bang-v0.json}, sinh bằng
         * {@code khoa-bang-v0.py} từ {@code kiem_dong_cong_thuc} của dịch vụ toán): loại, hai tầng, trích dẫn chính và trích
         * dẫn thêm của từng dòng. Id ổn định của tệp vàng (mã tài liệu, «mã#vị trí» của đoạn) đổi sang id thật của yêu cầu;
         * đoạn ở vị trí đó phải đúng chữ của tệp vàng (importer và script chia đoạn như nhau). Yêu cầu đã được
         * {@link #soYeuCauKhoaBang} kiểm: tài liệu thứ i là tài liệu nguồn thứ i.
         */
        @SuppressWarnings("unchecked")
        static Map<String, @Nullable Object> phatLaiKhoaBang(Map<String, ?> yeuCau) {
            Map<String, Object> vang = (Map<String, Object>) docJson("khoa-bang-v0.json");
            Map<String, Object> chuDoan = (Map<String, Object>) vang.get("doan");
            List<String> ma = new ArrayList<>(((List<Map<String, Object>>) docTep(NhapNoiDungChungTest.thuMucData().resolve("v0/tai-lieu.json")))
                .stream().map(t -> (String) t.get("ma")).toList());
            ma.addAll(List.of("sp-tai-lieu-0001", "sp-tai-lieu-0002"));
            List<Map<String, Object>> gui = (List<Map<String, Object>>) yeuCau.get("tai_lieu");
            Map<String, String> id = new HashMap<>();
            for (int i = 0; i < ma.size(); i++) {
                id.put(ma.get(i), (String) gui.get(i).get("id"));
                List<Map<String, Object>> doan = (List<Map<String, Object>>) gui.get(i).get("doan");
                for (int k = 0; k < doan.size(); k++) {
                    String maDoan = ma.get(i) + "#" + k;
                    assertThat(doan.get(k).get("text")).as("đoạn %s so với tệp vàng khóa bảng", maDoan).isEqualTo(chuDoan.get(maDoan));
                    id.put(maDoan, (String) doan.get(k).get("id"));
                }
            }
            assertThat(id).as("số đoạn đã gửi so với tệp vàng khóa bảng").hasSize(ma.size() + chuDoan.size());
            return (Map<String, @Nullable Object>) Objects.requireNonNull(doiId(vang.get("phan_hoi"), id));
        }

        /** Chép sâu, đổi giá trị chuỗi của khóa {@code tai_lieu}, {@code doan} theo {@code id} (thiếu thì ném: test đỏ). */
        @SuppressWarnings("unchecked")
        private static @Nullable Object doiId(@Nullable Object giaTri, Map<String, String> id) {
            if (giaTri instanceof Map<?, ?> m) {
                Map<String, @Nullable Object> ra = new LinkedHashMap<>();
                ((Map<String, @Nullable Object>) m).forEach((k, v) -> ra.put(k, (k.equals("tai_lieu") || k.equals("doan")) && v instanceof String s
                    ? Objects.requireNonNull(id.get(s), "id của " + s) : doiId(v, id)));
                return ra;
            }
            if (giaTri instanceof List<?> l) {
                return l.stream().map(x -> doiId(x, id)).toList();
            }
            return giaTri;
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
                    return phatLaiKhoaBang(yeuCau);
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
