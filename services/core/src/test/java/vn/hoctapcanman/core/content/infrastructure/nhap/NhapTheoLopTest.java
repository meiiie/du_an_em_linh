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
        KiemGia.TANG2_SAI = false;
        KiemGia.TRICH_GIUA = false;
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
    void bangDoGiaoVienKhoaThiImporterGiuNguyen() {
        // Codex #134 (P2): giáo viên sửa bảng đã nhập và khóa phiên bản mới; lần nhập sau không khóa đè bảng v0 mới hơn.
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        theoLop.nhap(lop, da.bai());
        UUID giaoVien = UUID.randomUUID();
        jdbc.sql("""
                insert into users (id, email, password_hash, display_name, role, created_at, updated_at)
                values (?, ?, 'x', 'Giáo viên thử', 'TEACHER', now(), now())""").params(giaoVien, "gv." + giaoVien + "@test.local").update();
        UUID doan = jdbc.sql("""
                select p.id from document_passages p join documents d on d.id = p.document_id
                where d.class_id = ? order by d.code, p.char_start limit 1""").params(lop).query(UUID.class).single();
        FormulaSheet nhapGv = sheets.findCurrent(lop).orElseThrow().newDraft(2, java.time.Instant.parse("2026-10-05T00:00:00Z"));
        Map<String, vn.hoctapcanman.core.content.domain.model.FormulaCheck> kiem = new LinkedHashMap<>();
        for (Formula f : nhapGv.rows()) {
            kiem.put(f.code(), vn.hoctapcanman.core.content.domain.model.FormulaCheck.of(f,
                vn.hoctapcanman.core.content.domain.model.FormulaKind.DANG_THUC, vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT,
                vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT, null, null, doan));
        }
        FormulaSheet khoaGv = nhapGv.withCheckResults(kiem).lock(giaoVien, java.time.Instant.parse("2026-10-05T00:00:01Z"));
        sheets.save(khoaGv);

        NhapTheoLop.KetQua lai = theoLop.nhap(lop, da.bai());
        assertThat(lai.bang()).isEqualTo(khoaGv.id());
        assertThat(lai.phienBanBang()).isEqualTo(2);
        assertThat(sheets.findCurrent(lop).orElseThrow().id()).isEqualTo(khoaGv.id());
        assertThat(suKien.stream(BangCongThucDaKhoa.class)).hasSize(1);
        jdbc.sql("set constraints all immediate").update();
    }

    @Test
    void banNhapChepTuBangDaNhapCungKhongBiGhiDe() throws IOException {
        // Codex #134 (P2): giáo viên bắt đầu sửa bảng đã nhập (newDraft giữ ghi chú của importer); nguồn đổi làm importer cần
        // khóa bảng mới thì phải dừng, không thay dòng bảng nháp của giáo viên.
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        theoLop.nhap(lop, da.bai());
        FormulaSheet nhapGv = sheets.findCurrent(lop).orElseThrow().newDraft(2, java.time.Instant.parse("2026-10-05T00:00:00Z"));
        sheets.save(nhapGv);
        Path tep = DATA.resolve("v0/tai-lieu.json");
        byte[] goc = Files.readAllBytes(tep);
        try {
            JsonMapper json = JsonMapper.builder().build();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taiLieu = json.readValue(goc, List.class);
            taiLieu.removeIf(d -> "v0-phuong-phap".equals(d.get("ma")));
            Files.writeString(tep, json.writeValueAsString(taiLieu), StandardCharsets.UTF_8);
            assertThatThrownBy(() -> theoLop.nhap(lop, da.bai())).isInstanceOf(IllegalStateException.class).hasMessageContaining("bảng nháp");
        } finally {
            Files.write(tep, goc);
        }
        assertThat(sheets.findDraft(lop)).contains(nhapGv);
        assertThat(sheets.findCurrent(lop).orElseThrow().version()).isEqualTo(1);
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

            // Codex #134 (P1): lần nhập sau khi đổi tài liệu lỗi giữa chừng (bảng không khóa được) thì lần sau vẫn phải khóa
            // lại và kiểm lại, dù tài liệu mới đã nạp ở lần lỗi.
            KiemGia.DONG_KHONG_DAT = "d-3";
            assertThatThrownBy(() -> theoLop.nhap(lop, da.bai())).isInstanceOf(IllegalStateException.class);
            KiemGia.DONG_KHONG_DAT = null;

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

    @Test
    void taiLieuBiBoKhoiNguonThiKhoaLaiVaKiemLai() throws IOException {
        // Codex #134 (P2): bỏ một tài liệu khỏi nguồn làm kho đổi; bảng và lượt kiểm với kho cũ không còn mới.
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        theoLop.nhap(lop, da.bai());
        Path tep = DATA.resolve("v0/tai-lieu.json");
        byte[] goc = Files.readAllBytes(tep);
        try {
            JsonMapper json = JsonMapper.builder().build();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taiLieu = json.readValue(goc, List.class);
            taiLieu.removeIf(d -> "v0-phuong-phap".equals(d.get("ma")));
            Files.writeString(tep, json.writeValueAsString(taiLieu), StandardCharsets.UTF_8);
            NhapTheoLop.KetQua lai = theoLop.nhap(lop, da.bai());
            assertThat(lai.taiLieu()).isEqualTo(4);
            assertThat(lai.phienBanBang()).isEqualTo(2);
            assertThat(lai.daKiem()).isEqualTo(16);
        } finally {
            Files.write(tep, goc);
        }
        assertThat(dem("select count(*) from verification_runs where class_id = ? and stale")).isEqualTo(16);
        jdbc.sql("set constraints all immediate").update();
    }

    @Test
    void baiRoiNguonThiRutPhatHanh() {
        // Codex #134 (P2): bài do importer tạo mà lần nhập sau không còn (bỏ khỏi nguồn, generate trả loi) thì không còn
        // DA_PHAT_HANH ở lớp; bài khác giữ nguyên.
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        NhapTheoLop.KetQua dau = theoLop.nhap(lop, da.bai());
        assertThat(dau.phatHanh()).containsEntry("DH12-03-VD-01", ReleaseStatus.DA_PHAT_HANH);
        List<NhapNoiDungChung.BaiNhap> conLai = da.bai().stream().filter(b -> !b.ma().equals("DH12-03-VD-01")).toList();
        NhapTheoLop.KetQua lai = theoLop.nhap(lop, conLai);
        assertThat(lai.phatHanh()).doesNotContainKey("DH12-03-VD-01").containsEntry("DH12-NB-01", ReleaseStatus.DA_PHAT_HANH);
        assertThat(jdbc.sql("""
                select r.status from problem_releases r join problems p on p.id = r.problem_id
                where r.class_id = ? and p.code = 'DH12-03-VD-01'""").params(lop).query(String.class).single()).isEqualTo("NHAP");
        jdbc.sql("set constraints all immediate").update();
    }

    @Test
    void taiLieuDangToHopVanAnhXaTrichDanSauChuanHoa() throws IOException {
        // Codex #134 (P2): verify.py chuẩn hóa NFC rồi mới tính vi_tri. Nguồn viết dạng tổ hợp (NFD) thì importer lưu và gửi
        // NFC, nên câu trích ở giữa văn bản vẫn ánh xạ về đúng đoạn.
        Path tep = DATA.resolve("v0/tai-lieu.json");
        byte[] goc = Files.readAllBytes(tep);
        try {
            JsonMapper json = JsonMapper.builder().build();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taiLieu = json.readValue(goc, List.class);
            Map<String, Object> donDieu = taiLieu.stream().filter(d -> "v0-don-dieu".equals(d.get("ma"))).findFirst().orElseThrow();
            donDieu.put("textContent", java.text.Normalizer.normalize((String) donDieu.get("textContent"), java.text.Normalizer.Form.NFD));
            Files.writeString(tep, json.writeValueAsString(taiLieu), StandardCharsets.UTF_8);

            NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
            KiemGia.TRICH_GIUA = true;
            NhapTheoLop.KetQua kq = theoLop.nhap(lop, da.bai());
            assertThat(kq.phatHanh()).containsEntry("DH12-03-VD-01", ReleaseStatus.DA_PHAT_HANH);
        } finally {
            Files.write(tep, goc);
        }
        String luu = jdbc.sql("select text_content from documents where class_id = ? and code = 'v0-don-dieu'").params(lop)
            .query(String.class).single();
        assertThat(java.text.Normalizer.isNormalized(luu, java.text.Normalizer.Form.NFC)).isTrue();
        jdbc.sql("set constraints all immediate").update();
    }

    @Test
    void tang2SaiCoTrichDanThiGhiTrichDan() {
        // Codex #134 (P2): căn cứ của tầng 2 SAI (bài bị chặn) cũng ghi vào verification_run_citations, bất biến như căn cứ đạt.
        NhapNoiDungChung.DaNhap da = chung.nhapGiuBai();
        KiemGia.TANG2_SAI = true;
        NhapTheoLop.KetQua kq = theoLop.nhap(lop, da.bai());
        assertThat(kq.phatHanh()).containsEntry("DH12-03-VD-01", ReleaseStatus.BI_CHAN);
        assertThat(dem("""
                select count(*) from verification_runs r where r.class_id = ?
                and exists (select 1 from verification_tier_results t where t.run_id = r.id and t.tier = 2 and t.status = 'SAI')
                and not exists (select 1 from verification_run_citations c where c.run_id = r.id)""")).isZero();
        assertThat(dem("select count(*) from verification_run_citations c join verification_runs r on r.id = c.run_id where r.class_id = ?"))
            .isPositive();
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
        static volatile boolean TANG2_SAI;
        static volatile boolean TRICH_GIUA;

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
                    // Như verify.py: chuẩn hóa NFC rồi tính vị trí theo code point.
                    String vanBan = java.text.Normalizer.normalize((String) donDieu.get("text"), java.text.Normalizer.Form.NFC);
                    int batDau = TRICH_GIUA ? vanBan.indexOf(". ") + 2 : 0;
                    String trich = TRICH_SAI ? "chữ không có trong tài liệu" : vanBan.substring(batDau, batDau + 30);
                    int viTri = vanBan.codePointCount(0, batDau);
                    Map<String, Object> baiLam = (Map<String, Object>) yeuCau.get("bai_lam");
                    List<String> tang;
                    if (yeuCau.get("ham") == null) {
                        tang = List.of("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC");
                    } else if (baiLam != null && "3*x".equals(baiLam.get("dao_ham"))) {
                        tang = List.of("SAI", "DAT", "DAT");
                    } else if (TANG2_SAI) {
                        tang = List.of("DAT", "SAI", "DAT");
                    } else {
                        tang = List.of("DAT", "DAT", "DAT");
                    }
                    List<Map<String, @Nullable Object>> kq = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        Map<String, @Nullable Object> t = new LinkedHashMap<>();
                        t.put("tang", i + 1);
                        t.put("trang_thai", tang.get(i));
                        if (i == 1 && !tang.get(i).equals("KHONG_KIEM_DUOC")) {
                            t.put("trich_dan", List.of(Map.of("document_id", "v0-don-dieu", "vi_tri", viTri, "trich", trich)));
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
