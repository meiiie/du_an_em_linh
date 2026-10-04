package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.content.domain.event.BaiDaNhap;
import vn.hoctapcanman.core.content.domain.event.BangCongThucDaKhoa;
import vn.hoctapcanman.core.content.domain.model.Formula;
import vn.hoctapcanman.core.content.domain.model.FormulaSheet;
import vn.hoctapcanman.core.content.domain.model.ReleaseStatus;
import vn.hoctapcanman.core.content.infrastructure.persistence.DocumentRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.FormulaSheetRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.HintLevelRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemReleaseRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.SolutionRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.TopicCatalogRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.VerificationRunRepositoryAdapter;

/**
 * Nhập theo lớp (T012b) trên PostgreSQL 18 với dịch vụ toán giả: 5 tài liệu chia đoạn, bảng 6 dòng khóa khi mọi dòng đạt,
 * kiểm từng bài, trích dẫn ánh xạ về đoạn, phát hành theo lớp, nhập lại không kiểm lại; bảng có dòng chưa đạt thì không
 * khóa, không kiểm bài nào; trích dẫn không ánh xạ được thì tầng 2 không đạt (đóng mặc định).
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
    NhapNoiDungChungTest.ToanGia.class,
    NhapTheoLopTest.KiemGia.class
})
@Testcontainers(disabledWithoutDocker = true)
@RecordApplicationEvents
class NhapTheoLopTest {

    /** Bản sao tạm của data/ của repo: test sửa tài liệu nguồn giữa hai lần nhập mà không đụng repo. */
    private static final Path DATA = saoDuLieu();

    @DynamicPropertySource
    static void nguon(DynamicPropertyRegistry r) {
        r.add("app.content.source", DATA::toString);
    }

    private static Path saoDuLieu() {
        try {
            Path goc = NhapNoiDungChungTest.thuMucData();
            Path tam = Files.createTempDirectory("noi-dung-");
            try (var cay = Files.walk(goc)) {
                for (Path p : cay.toList()) {
                    Path dich = tam.resolve(goc.relativize(p).toString());
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(dich);
                    } else {
                        Files.copy(p, dich);
                    }
                }
            }
            return tam;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired
    private NhapNoiDungChung chung;

    @Autowired
    private NhapTheoLop theoLop;

    @Autowired
    private FormulaSheetRepositoryAdapter sheets;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationEvents suKien;

    private UUID lop;

    @BeforeEach
    void lop() {
        lop = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A1 thử', 12, '2026-2027', now())")
            .params(lop).update();
    }

    @AfterEach
    void hen() {
        KiemGia.DONG_KHONG_DAT = null;
        KiemGia.TRICH_SAI = false;
        KiemGia.TRICH_THEM_HONG = false;
    }

    @Test
    void nhapLopDuTaiLieuKhoaBangKiemVaPhatHanhNhapLaiKhongKiemLai() {
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        assertThat(suKien.stream(BaiDaNhap.class)).hasSize(16);

        NhapTheoLop.KetQua kq = theoLop.nhap(lop, da.bai());
        assertThat(kq.taiLieu()).isEqualTo(5);
        assertThat(kq.phienBanBang()).isEqualTo(1);
        assertThat(kq.daKiem()).isEqualTo(16);
        assertThat(kq.phatHanh()).hasSize(16).containsEntry("DH12-DEMO-CHAN-01", ReleaseStatus.BI_CHAN)
            .containsEntry("DH12-06-VDC-01", ReleaseStatus.CHO_GIAO_VIEN_DUYET).containsEntry("DH12-03-VD-01", ReleaseStatus.DA_PHAT_HANH);
        assertThat(sheets.findCurrent(lop).orElseThrow().isLocked()).isTrue();
        assertThat(suKien.stream(BangCongThucDaKhoa.class)).singleElement().extracting(BangCongThucDaKhoa::lopId).isEqualTo(lop);

        assertThat(dem("select count(*) from documents where class_id = ?")).isEqualTo(5);
        assertThat(dem("select count(*) from document_passages p join documents d on d.id = p.document_id where d.class_id = ?"))
            .isGreaterThan(5);
        // Mọi lượt có tầng 2 DAT đều trích dẫn đoạn của chính lớp này.
        assertThat(dem("""
                select count(*) from verification_runs r where r.class_id = ?
                and exists (select 1 from verification_tier_results t where t.run_id = r.id and t.tier = 2 and t.status = 'DAT')
                and not exists (select 1 from verification_run_citations c where c.run_id = r.id)""")).isZero();
        assertThat(dem("""
                select count(*) from verification_run_citations c join verification_runs r on r.id = c.run_id
                join document_passages p on p.id = c.passage_id join documents d on d.id = p.document_id
                where r.class_id = ? and d.class_id = r.class_id""")).isPositive();
        long luot = dem("select count(*) from verification_runs where class_id = ?");
        assertThat(luot).isEqualTo(16);

        NhapTheoLop.KetQua lai = theoLop.nhap(lop, da.bai());
        assertThat(lai.daKiem()).isZero();
        assertThat(lai.phatHanh()).isEqualTo(kq.phatHanh());
        assertThat(lai.bang()).isEqualTo(kq.bang());
        assertThat(dem("select count(*) from verification_runs where class_id = ?")).isEqualTo(luot);
        assertThat(suKien.stream(BangCongThucDaKhoa.class)).hasSize(1);
        // Cuối test: ràng buộc kiểm lúc commit (căn cứ của tầng đạt, duyệt) đều qua.
        jdbc.sql("set constraints all immediate").update();
    }

    @Test
    void dongBangChuaDatThiKhongKhoaVaKhongKiemBaiNao() {
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        KiemGia.DONG_KHONG_DAT = "d-3";
        assertThatThrownBy(() -> theoLop.nhap(lop, da.bai())).isInstanceOf(IllegalStateException.class).hasMessageContaining("d-3");
        assertThat(sheets.findCurrent(lop)).isEmpty();
        assertThat(dem("select count(*) from verification_runs where class_id = ?")).isZero();
        assertThat(dem("select count(*) from problem_releases where class_id = ?")).isZero();
        assertThat(suKien.stream(BangCongThucDaKhoa.class)).isEmpty();
    }

    @Test
    void trichDanKhongAnhXaDuocThiTang2KhongKiemDuoc() {
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        KiemGia.TRICH_SAI = true;
        NhapTheoLop.KetQua kq = theoLop.nhap(lop, da.bai());
        // Dịch vụ toán nói tầng 2 DAT nhưng chữ trích không có ở vị trí đó: không đoán căn cứ, bài chờ giáo viên duyệt.
        assertThat(kq.phatHanh()).containsEntry("DH12-03-VD-01", ReleaseStatus.CHO_GIAO_VIEN_DUYET)
            .doesNotContainValue(ReleaseStatus.DA_PHAT_HANH);
        assertThat(dem("select count(*) from verification_run_citations c join verification_runs r on r.id = c.run_id where r.class_id = ?"))
            .isZero();
        jdbc.sql("set constraints all immediate").update();
    }

    @Test
    void trichDanThemKhongAnhXaDuocThiDongKhongDatVaKhongKhoa() {
        // Codex #134 (P2): trích dẫn thêm (mệnh đề khác của dòng định lí) chỉ ra ngoài lớp thì dòng không đạt, không khóa.
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        KiemGia.TRICH_THEM_HONG = true;
        assertThatThrownBy(() -> theoLop.nhap(lop, da.bai())).isInstanceOf(IllegalStateException.class).hasMessageContaining("d-5");
        assertThat(sheets.findCurrent(lop)).isEmpty();
        assertThat(dem("select count(*) from verification_runs where class_id = ?")).isZero();
    }

    @Test
    void lopCoBangNhapCuaGiaoVienThiKhongGhiDe() {
        // Codex #134 (P2): bảng nháp giáo viên đang soạn không bị importer thay dòng rồi khóa.
        FormulaSheet nhapGv = FormulaSheet.draft(lop, 1, "Bảng giáo viên đang soạn",
            List.of(Formula.unchecked(1, "gv-1", null, "Dòng của giáo viên", "x", "Phát biểu của giáo viên.")), java.time.Instant.EPOCH);
        sheets.save(nhapGv);
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        assertThatThrownBy(() -> theoLop.nhap(lop, da.bai())).isInstanceOf(IllegalStateException.class).hasMessageContaining("bảng nháp");
        assertThat(sheets.findDraft(lop)).contains(nhapGv);
        assertThat(sheets.findCurrent(lop)).isEmpty();
        assertThat(dem("select count(*) from verification_runs where class_id = ?")).isZero();
    }

    @Test
    void taiLieuNguonDoiThiGiuBanCuLamCanCuKhoaLaiVaKiemLai() throws IOException {
        // Codex #134 (P2): sửa tài liệu nguồn sau lần nhập đầu không sửa tại chỗ tài liệu đang là căn cứ (V5 từ chối);
        // giữ bản cũ (đổi mã), nạp bản mới, khóa bảng phiên bản mới, kiểm lại mọi bài (data-model: nạp phiên bản mới và kiểm lại).
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        NhapTheoLop.KetQua dau = theoLop.nhap(lop, da.bai());
        UUID cu = jdbc.sql("select id from documents where class_id = ? and code = 'v0-don-dieu'").params(lop).query(UUID.class).single();
        String vanBanCu = jdbc.sql("select text_content from documents where id = ?").params(cu).query(String.class).single();

        Path tep = DATA.resolve("v0/tai-lieu.json");
        byte[] goc = Files.readAllBytes(tep);
        try {
            JsonMapper json = JsonMapper.builder().build();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taiLieu = json.readValue(goc, List.class);
            taiLieu.stream().filter(d -> "v0-don-dieu".equals(d.get("ma"))).findFirst().orElseThrow()
                .put("textContent", vanBanCu + " Câu bổ sung của phiên bản mới.");
            Files.writeString(tep, json.writeValueAsString(taiLieu), StandardCharsets.UTF_8);

            NhapTheoLop.KetQua lai = theoLop.nhap(lop, da.bai());
            assertThat(lai.phienBanBang()).isEqualTo(2);
            assertThat(lai.daKiem()).isEqualTo(16);
            assertThat(lai.phatHanh()).isEqualTo(dau.phatHanh());
        } finally {
            Files.write(tep, goc);
        }
        // Bản cũ còn nguyên chữ, nhường mã; bản mới mang mã gốc và chữ mới.
        assertThat(jdbc.sql("select code from documents where id = ?").params(cu).query(String.class).single())
            .startsWith("v0-don-dieu.cu-");
        assertThat(jdbc.sql("select text_content from documents where id = ?").params(cu).query(String.class).single()).isEqualTo(vanBanCu);
        assertThat(jdbc.sql("select text_content from documents where class_id = ? and code = 'v0-don-dieu'").params(lop)
            .query(String.class).single()).endsWith("Câu bổ sung của phiên bản mới.");
        assertThat(dem("select count(*) from documents where class_id = ?")).isEqualTo(6);
        assertThat(dem("select count(*) from verification_runs where class_id = ? and stale")).isEqualTo(16);
        assertThat(suKien.stream(BangCongThucDaKhoa.class)).hasSize(2);
        jdbc.sql("set constraints all immediate").update();
    }

    private long dem(String sql) {
        return jdbc.sql(sql).params(lop).query(Long.class).single();
    }

    /**
     * Hai job kiểm giả. Khóa bảng: mọi dòng DAT hai tầng, trích đoạn đầu của tài liệu đầu ({@link #DONG_KHONG_DAT} thì dòng đó
     * tầng 2 không kiểm được). Kiểm bài: bài không có hàm thì ba tầng không kiểm được; ví dụ cổng chặn (đạo hàm sửa thành 3x)
     * tầng 1 SAI; còn lại ba tầng DAT, tầng 2 trích câu đầu của {@code v0-don-dieu} ({@link #TRICH_SAI} thì chữ trích sai).
     */
    @TestConfiguration
    static class KiemGia {

        static volatile @Nullable String DONG_KHONG_DAT;
        static volatile boolean TRICH_SAI;
        static volatile boolean TRICH_THEM_HONG;

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
                        boolean dat = !d.get("id").equals(DONG_KHONG_DAT);
                        Map<String, Object> tang2 = new LinkedHashMap<>();
                        tang2.put("trang_thai", dat ? "DAT" : "KHONG_KIEM_DUOC");
                        if (dat) {
                            tang2.put("trich_dan", Map.of("tai_lieu", taiLieu.get("id"), "doan", doan.get("id"), "trich", doan.get("text")));
                        }
                        if (TRICH_THEM_HONG && "d-5".equals(d.get("id"))) {
                            tang2.put("trich_dan_them", List.of(Map.of("tai_lieu", taiLieu.get("id"), "doan", UUID.randomUUID().toString(), "trich", "?")));
                        }
                        dong.add(Map.of("id", d.get("id"), "loai", "DANG_THUC", "tang1", Map.of("trang_thai", "DAT", "muc_bang_chung", "CAS"),
                            "tang2", tang2));
                    }
                    return Map.of("dong", dong, "bo_qua", List.of());
                }

                @Override
                @SuppressWarnings("unchecked")
                public Map<String, @Nullable Object> kiemBai(Map<String, ?> yeuCau) {
                    Map<String, Object> donDieu = ((List<Map<String, Object>>) yeuCau.get("tai_lieu")).stream()
                        .filter(t -> "v0-don-dieu".equals(t.get("id"))).findFirst().orElseThrow();
                    String vanBan = (String) donDieu.get("text");
                    String trich = TRICH_SAI ? "chữ không có trong tài liệu" : vanBan.substring(0, 40);
                    Map<String, Object> baiLam = (Map<String, Object>) yeuCau.get("bai_lam");
                    List<String> tang;
                    if (yeuCau.get("ham") == null) {
                        tang = List.of("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC");
                    } else if (baiLam != null && "3*x".equals(baiLam.get("dao_ham"))) {
                        tang = List.of("SAI", "DAT", "DAT");
                    } else {
                        tang = List.of("DAT", "DAT", "DAT");
                    }
                    List<Map<String, @Nullable Object>> kq = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        Map<String, @Nullable Object> t = new LinkedHashMap<>();
                        t.put("tang", i + 1);
                        t.put("trang_thai", tang.get(i));
                        if (i == 1 && tang.get(i).equals("DAT")) {
                            t.put("trich_dan", List.of(Map.of("document_id", "v0-don-dieu", "vi_tri", 0, "trich", trich)));
                        }
                        if (i == 2 && tang.get(i).equals("DAT")) {
                            t.put("cong_thuc", List.of(Map.of("formula_id", "d-1")));
                        }
                        kq.add(t);
                    }
                    return Map.of("tang", kq);
                }
            };
        }
    }
}
