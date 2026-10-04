package vn.hoctapcanman.core.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.content.domain.model.BloomLevel;
import vn.hoctapcanman.core.content.domain.model.Document;
import vn.hoctapcanman.core.content.domain.model.DocumentKind;
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;
import vn.hoctapcanman.core.content.domain.model.ErrorType;
import vn.hoctapcanman.core.content.domain.model.HintLevel;
import vn.hoctapcanman.core.content.domain.model.InputKind;
import vn.hoctapcanman.core.content.domain.model.Level3;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ReleasedProblem;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.SkillPrerequisite;
import vn.hoctapcanman.core.content.domain.model.Solution;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.model.Topic;

/** Flyway V4 trên PostgreSQL 18 thật + adapter JDBC của nội dung: ghi đè theo khóa, mảng, jsonb, real, thứ tự đọc. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    TopicCatalogRepositoryAdapter.class,
    ProblemRepositoryAdapter.class,
    SolutionRepositoryAdapter.class,
    HintLevelRepositoryAdapter.class,
    DocumentRepositoryAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class ContentPersistenceTest {

    private static final Instant LUC = Instant.parse("2026-10-03T08:00:00.123456789Z");
    private static final String BAM = "a".repeat(64);

    @Autowired
    private TopicCatalogRepositoryAdapter catalog;

    @Autowired
    private ProblemRepositoryAdapter problems;

    @Autowired
    private SolutionRepositoryAdapter solutions;

    @Autowired
    private HintLevelRepositoryAdapter hints;

    @Autowired
    private DocumentRepositoryAdapter documents;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void danhMuc() {
        catalog.saveTopic(new Topic("DH12", "Đơn điệu và cực trị", 12));
        catalog.saveSkill(new Skill("T12.DH.02", "DH12", "Tính đạo hàm", "YCCĐ trang 12", 12, true));
        catalog.saveSkill(new Skill("T12.DH.03", "DH12", "Xét dấu đạo hàm", null, null, false));
        catalog.saveStepTemplate(new StepTemplate("B.DH.DAOHAM", "DH12", 2, InputKind.DONG, "T12.DH.02", "Tính đạo hàm"));
        catalog.saveStepTemplate(new StepTemplate("B.DH.TXD", "DH12", 1, InputKind.DONG, null, "Tập xác định"));
    }

    @Test
    void danhMucGhiDeTheoMaKhongNhanBan() {
        catalog.saveTopic(new Topic("DH12", "Đơn điệu và cực trị (sửa)", 12));
        catalog.saveSkill(new Skill("T12.DH.03", "DH12", "Xét dấu", "mô tả", 12, true));
        catalog.replacePrerequisites("T12.DH.03", List.of(new SkillPrerequisite("T12.DH.03", "T12.DH.02", "TH")));
        catalog.replacePrerequisites("T12.DH.03", List.of(new SkillPrerequisite("T12.DH.03", "T12.DH.02", null)));
        catalog.saveErrorType(new ErrorType("E-DH-01", "T12.DH.02", "B.DH.DOCBANG", "Quên hằng số", "Xem lại", List.of("DAO_HAM", "BIEU_THUC")));

        assertThat(catalog.findTopic("DH12")).contains(new Topic("DH12", "Đơn điệu và cực trị (sửa)", 12));
        assertThat(catalog.findSkills("DH12")).extracting(Skill::code).containsExactly("T12.DH.02", "T12.DH.03");
        assertThat(catalog.findSkills("DH12").get(1)).isEqualTo(new Skill("T12.DH.03", "DH12", "Xét dấu", "mô tả", 12, true));
        assertThat(catalog.findPrerequisites("T12.DH.03")).containsExactly(new SkillPrerequisite("T12.DH.03", "T12.DH.02", null));
        assertThat(catalog.findStepTemplates("DH12")).extracting(StepTemplate::stepCode).containsExactly("B.DH.TXD", "B.DH.DAOHAM");
        assertThat(catalog.findErrorTypes("T12.DH.02")).containsExactly(
            new ErrorType("E-DH-01", "T12.DH.02", "B.DH.DOCBANG", "Quên hằng số", "Xem lại", List.of("DAO_HAM", "BIEU_THUC")));
        assertThatThrownBy(() -> catalog.replacePrerequisites("T12.DH.03",
            List.of(new SkillPrerequisite("T12.DH.02", "T12.DH.03", null)))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void baiLoiGiaiGoiYDocLaiDung() {
        Problem p = new Problem(UUID.randomUUID(), "DH12-03-VD-01", "T12.DH.03", List.of("T12.DH.02"), Level4.VAN_DUNG, Level3.VAN_DUNG,
            BloomLevel.VAN_DUNG, 0.35, "Xét tính đơn điệu", "y = x^3 - 6x^2 + 9x + 2", "x**3-6*x**2+9*x+2", Problem.TU_LUAN_5_BUOC,
            null, "SUPHAM", BAM, null, LUC, LUC);
        Problem khongPhu = new Problem(UUID.randomUUID(), "DH12-NB-01", "T12.DH.02", List.of(), Level4.NHAN_BIET, null, null, null,
            "Đề", "y", null, "TN_DUNG_SAI", "B.DH.DAOHAM", "SUPHAM", BAM, null, LUC, LUC);
        problems.save(p);
        problems.save(p);
        problems.save(khongPhu);

        Problem doc = problems.findById(p.id()).orElseThrow();
        assertThat(doc.difficulty()).isEqualTo(0.35);
        assertThat(doc.createdAt()).isEqualTo(Instant.parse("2026-10-03T08:00:00.123456Z"));
        assertThat(doc).usingRecursiveComparison().ignoringFields("createdAt", "updatedAt").isEqualTo(p);
        assertThat(problems.findByCode("DH12-NB-01")).contains(new Problem(khongPhu.id(), "DH12-NB-01", "T12.DH.02", List.of(),
            Level4.NHAN_BIET, null, null, null, "Đề", "y", null, "TN_DUNG_SAI", "B.DH.DAOHAM", "SUPHAM", BAM, null, doc.createdAt(),
            doc.updatedAt()));
        assertThat(problems.findAllById(List.of())).isEmpty();
        assertThat(problems.findAllById(List.of(khongPhu.id(), p.id()))).extracting(Problem::code)
            .containsExactly("DH12-03-VD-01", "DH12-NB-01");

        solutions.save(new Solution(p.id(), "{\"buoc\": [1, 2]}", "[\"x = 1\", \"x = 3\"]", "đồng biến trên (−∞; 1)"));
        Solution s = solutions.findByProblemId(p.id()).orElseThrow();
        assertThat(s.protectedFactsJson()).isEqualTo("[\"x = 1\", \"x = 3\"]");
        assertThat(s.workedSolutionJson()).isEqualTo("{\"buoc\": [1, 2]}");
        assertThat(s.finalAnswer()).isEqualTo("đồng biến trên (−∞; 1)");

        hints.replaceForProblem(p.id(), List.of(new HintLevel(p.id(), "B.DH.DAOHAM", 2, "cấp 2"),
            new HintLevel(p.id(), "B.DH.KHAC", 1, "bước ngoài khung"), new HintLevel(p.id(), "B.DH.DAOHAM", 1, "cấp 1"),
            new HintLevel(p.id(), "B.DH.TXD", 1, "txd")));
        hints.replaceForProblem(p.id(), hints.findByProblemId(p.id()));
        assertThat(hints.findByProblemId(p.id())).extracting(h -> h.stepCode() + "#" + h.level())
            .containsExactly("B.DH.TXD#1", "B.DH.DAOHAM#1", "B.DH.DAOHAM#2", "B.DH.KHAC#1");

        // Cuối test: lỗi ràng buộc hủy giao dịch của test, lệnh sau đó không chạy được nữa.
        Problem trungMa = new Problem(UUID.randomUUID(), p.code(), "T12.DH.03", List.of(), Level4.VAN_DUNG, null, null, null, "Đề",
            "y", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", BAM, null, LUC, LUC);
        assertThatThrownBy(() -> problems.save(trungMa)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void doanTaiLieuGiuIdKhiNapLaiVaXoaDoanBoDi() {
        UUID lop = lopMoi("12A1 thử");
        Document d = taiLieu(lop, "v0-don-dieu", "Đoạn một. Đoạn hai.");
        List<DocumentPassage> lan1 = documents.save(d, List.of(doan(d, "Đoạn một."), doan(d, "Đoạn hai.")));
        assertThat(lan1).extracting(DocumentPassage::textFolded).containsExactly("doan mot.", "doan hai.");

        // Tài liệu chưa là căn cứ: nạp bản sửa được; đoạn cùng vị trí giữ id, đoạn bỏ đi bị xóa.
        Document sua = taiLieu(d, "Đoạn một (sửa).", 2);
        List<DocumentPassage> lan2 = documents.save(sua, List.of(doan(sua, "Đoạn một (sửa).")));
        assertThat(lan2).hasSize(1);
        assertThat(lan2.getFirst().id()).isEqualTo(lan1.getFirst().id());
        assertThat(lan2.getFirst().text()).isEqualTo("Đoạn một (sửa).");

        assertThat(documents.findByClassAndCode(lop, "v0-don-dieu")).contains(new Document(d.id(), lop, "v0-don-dieu",
            "Ghi chú tự soạn", DocumentKind.TU_SOAN, "giáo viên thử", "tu_soan", null, "Đoạn một (sửa).", 2, null,
            Instant.parse("2026-10-03T08:00:00.123456Z")));
        assertThat(documents.findByClass(lop)).hasSize(1);
    }

    @Test
    void doanPhaiNamDungTrongVanBanCuaTaiLieu() {
        // Codex #120 (P2): đoạn của tài liệu khác, vị trí lệch, vị trí ngoài văn bản, chữ gập bịa đều không ghi được.
        Document d = taiLieu(lopMoi("12A3 thử"), "v0-doan", "Đoạn một. Đoạn hai.");
        assertThatThrownBy(() -> documents.save(d, List.of(DocumentPassage.of(UUID.randomUUID(), 1, 0, "Đoạn một."))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> documents.save(d, List.of(DocumentPassage.of(d.id(), 1, 0, "Đoạn hai."))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> documents.save(d, List.of(DocumentPassage.of(d.id(), 1, 15, "Đoạn hai. Thêm"))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> documents.save(d, List.of(new DocumentPassage(UUID.randomUUID(), d.id(), 1, 0, 9, "Đoạn một.", "bia"))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(documents.findById(d.id())).isEmpty();
    }

    @Test
    void doanDangLaCanCuThiKhongSuaChuDuoc() {
        // Codex #120 (P1): nạp lại tài liệu đổi chữ của đoạn đang được trích dẫn thì bảng vẫn mang kết quả DAT cũ.
        Document d = taiLieuDuocTrichDan();
        // Nạp lại đúng như cũ thì được (importer chạy lại).
        documents.save(d, documents.findPassages(d.id()));
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        Document sua = taiLieu(d, "Đạo hàm của tổng bằng tích các đạo hàm. Đoạn khác.", 1);
        assertThatThrownBy(() -> documents.save(sua, List.of(doan(sua, "Đạo hàm của tổng bằng tích các đạo hàm."),
            doan(sua, "Đoạn khác.")))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void taiLieuDangLaCanCuThiKhongDoiVanBanDuDoanCuGiuNguyen() {
        // Codex #120 (P1): đổi văn bản và phiên bản tài liệu mà giữ đoạn cũ đang là căn cứ.
        Document d = taiLieuDuocTrichDan();
        Document sua = taiLieu(d, "Đạo hàm của tổng bằng tổng các đạo hàm. Đoạn khác. Thêm câu.", 2);
        assertThatThrownBy(() -> documents.save(sua, List.of(doan(sua, "Đạo hàm của tổng bằng tổng các đạo hàm."),
            doan(sua, "Đoạn khác.")))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void suaNoiDungBaiThiLuotKiemCuVaPhatHanhVeNhap() {
        // Codex #120 (P1): ghi lại bài với content_hash mới thì lượt kiểm cũ thành cũ, phát hành ở mọi lớp về NHAP.
        UUID lop = lopMoi("12A4 thử");
        Problem p = new Problem(UUID.randomUUID(), "DH12-TH-09", "T12.DH.03", List.of(), Level4.THONG_HIEU, null, null, null, "Đề",
            "y", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", BAM, null, LUC, LUC);
        problems.save(p);
        UUID luot = UUID.randomUUID();
        jdbc.sql("""
                insert into verification_runs (id, class_id, subject_kind, subject_id, content_hash, overall_status,
                    publish_status, content_version, created_at)
                values (?, ?, 'PROBLEM', ?, ?, 'DAT', 'DA_PHAT_HANH', 1, now())""").params(luot, lop, p.id(), BAM).update();
        jdbc.sql("insert into problem_releases (class_id, problem_id, status, run_id, updated_at) values (?, ?, 'DA_PHAT_HANH', ?, now())")
            .params(lop, p.id(), luot).update();

        problems.save(p);
        assertThat(trangThaiPhatHanh(p.id())).isEqualTo("DA_PHAT_HANH");

        problems.save(new Problem(p.id(), p.code(), p.skillCode(), p.extraSkillCodes(), p.level4(), null, null, null, "Đề đã sửa",
            "y", null, p.answerForm(), null, p.origin(), "c".repeat(64), null, LUC, LUC));
        assertThat(trangThaiPhatHanh(p.id())).isEqualTo("NHAP");
        assertThat(jdbc.sql("select stale from verification_runs where id = ?").params(luot).query(Boolean.class).single()).isTrue();
        assertThat(jdbc.sql("select content_version from problems where id = ?").params(p.id()).query(Integer.class).single()).isEqualTo(2);
        // Cuối test: phát hành lại theo lượt cũ bị từ chối (lỗi hủy giao dịch của test).
        assertThatThrownBy(() -> jdbc.sql("update problem_releases set status = 'DA_PHAT_HANH', run_id = ? where problem_id = ?")
            .params(luot, p.id()).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void luotKiemChoPhienBanNoiDungCuBiTuChoi() {
        // Codex #120 (P1): bên kiểm đọc nội dung cũ trong lúc có người sửa thì lượt của nó không được ghi.
        UUID lop = lopMoi("12A6 thử");
        Problem p = new Problem(UUID.randomUUID(), "DH12-TH-11", "T12.DH.03", List.of(), Level4.THONG_HIEU, null, null, null, "Đề",
            "y", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", BAM, null, LUC, LUC);
        problems.save(p);
        hints.replaceForProblem(p.id(), List.of(new HintLevel(p.id(), "B.DH.DAOHAM", 1, "cấp 1")));
        int phienBan = jdbc.sql("select content_version from problems where id = ?").params(p.id()).query(Integer.class).single();
        assertThat(phienBan).isEqualTo(2);
        assertThatThrownBy(() -> jdbc.sql("""
                insert into verification_runs (id, class_id, subject_kind, subject_id, content_hash, overall_status,
                    publish_status, content_version, created_at)
                values (?, ?, 'PROBLEM', ?, ?, 'DAT', 'DA_PHAT_HANH', 1, now())""")
            .params(UUID.randomUUID(), lop, p.id(), BAM).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void suaLoiGiaiHayGoiYThiPhatHanhVeNhapGhiLaiYNhuCuThiKhong() {
        // Codex #120 (P1): lời giải và thang gợi ý là nội dung của bài nhưng ở bảng riêng.
        UUID lop = lopMoi("12A5 thử");
        Problem p = new Problem(UUID.randomUUID(), "DH12-TH-10", "T12.DH.03", List.of(), Level4.THONG_HIEU, null, null, null, "Đề",
            "y", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", BAM, null, LUC, LUC);
        problems.save(p);
        Solution loiGiai = new Solution(p.id(), "{\"buoc\": [1]}", "[\"x = 1\"]", "x = 1");
        List<HintLevel> thang = List.of(new HintLevel(p.id(), "B.DH.DAOHAM", 1, "cấp 1"), new HintLevel(p.id(), "B.DH.DAOHAM", 2, "cấp 2"));
        solutions.save(loiGiai);
        hints.replaceForProblem(p.id(), thang);
        phatHanh(lop, p.id());

        solutions.save(loiGiai);
        hints.replaceForProblem(p.id(), thang);
        assertThat(trangThaiPhatHanh(p.id())).isEqualTo("DA_PHAT_HANH");

        hints.replaceForProblem(p.id(), List.of(new HintLevel(p.id(), "B.DH.DAOHAM", 1, "cấp 1 đã sửa")));
        assertThat(trangThaiPhatHanh(p.id())).isEqualTo("NHAP");
        assertThat(hints.findByProblemId(p.id())).extracting(HintLevel::text).containsExactly("cấp 1 đã sửa");

        phatHanh(lop, p.id());
        solutions.save(new Solution(p.id(), "{\"buoc\": [1]}", "[\"x = 1\", \"x = 3\"]", "x = 1"));
        assertThat(trangThaiPhatHanh(p.id())).isEqualTo("NHAP");
    }

    @Test
    void baiDangPhatHanhCuaLopDocCungMotAnhChup() {
        // Codex #133 (P1): bài và phát hành đọc trong một câu lệnh; sửa đề rút phát hành nên không còn trả bài đó.
        UUID lop = lopMoi("12A7 thử");
        UUID lopKhac = lopMoi("12A8 thử");
        Problem p = new Problem(UUID.randomUUID(), "DH12-TH-12", "T12.DH.03", List.of(), Level4.THONG_HIEU, null, null, null, "Đề",
            "y", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", BAM, null, LUC, LUC);
        Problem chuaMo = new Problem(UUID.randomUUID(), "DH12-TH-13", "T12.DH.03", List.of(), Level4.THONG_HIEU, null, null, null,
            "Đề khác", "y", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", BAM, null, LUC, LUC);
        problems.save(p);
        problems.save(chuaMo);
        phatHanh(lop, p.id());

        assertThat(problems.findReleasedInClass(lop)).extracting(Problem::code).containsExactly("DH12-TH-12");
        assertThat(problems.findReleasedInClass(lop, "DH12-TH-12")).isPresent();
        assertThat(problems.findReleasedInClass(lop, "DH12-TH-13")).isEmpty();
        assertThat(problems.findReleasedInClass(lopKhac)).isEmpty();
        assertThat(problems.findReleasedInClass(lopKhac, "DH12-TH-12")).isEmpty();
        // T020: cho bài làm, kèm phiên bản nội dung hiện tại và chủ đề của kỹ năng chính, cùng câu lệnh.
        assertThat(problems.findReleasedForWork(lop, "DH12-TH-12")).map(r -> List.of(r.problem().id(), r.contentVersion(), r.topicCode()))
            .contains(List.of(p.id(), 1, "DH12"));
        assertThat(problems.findReleasedForWork(lop, "DH12-TH-13")).isEmpty();
        assertThat(problems.findReleasedForWork(lopKhac, "DH12-TH-12")).isEmpty();

        problems.save(new Problem(p.id(), p.code(), p.skillCode(), p.extraSkillCodes(), p.level4(), null, null, null, "Đề đã sửa",
            "y", null, p.answerForm(), null, p.origin(), "d".repeat(64), null, LUC, LUC));
        assertThat(problems.findReleasedInClass(lop)).isEmpty();
        assertThat(problems.findReleasedInClass(lop, "DH12-TH-12")).isEmpty();
        assertThat(problems.findReleasedForWork(lop, "DH12-TH-12")).isEmpty();
        phatHanh(lop, p.id());
        assertThat(problems.findReleasedForWork(lop, "DH12-TH-12")).map(ReleasedProblem::contentVersion).contains(2);
    }

    /** Lượt kiểm DAT mới cho bài ở lớp, rồi phát hành theo lượt đó. */
    private void phatHanh(UUID lop, UUID baiId) {
        UUID luot = UUID.randomUUID();
        jdbc.sql("""
                insert into verification_runs (id, class_id, subject_kind, subject_id, content_hash, overall_status,
                    publish_status, content_version, created_at)
                values (?, ?, 'PROBLEM', ?, ?, 'DAT', 'DA_PHAT_HANH', (select content_version from problems where id = ?), clock_timestamp())""")
            .params(luot, lop, baiId, BAM, baiId).update();
        jdbc.sql("""
                insert into problem_releases (class_id, problem_id, status, run_id, updated_at) values (?, ?, 'DA_PHAT_HANH', ?, now())
                on conflict (class_id, problem_id) do update set status = excluded.status, run_id = excluded.run_id""")
            .params(lop, baiId, luot).update();
    }

    private String trangThaiPhatHanh(UUID baiId) {
        return jdbc.sql("select status from problem_releases where problem_id = ?").params(baiId).query(String.class).single();
    }

    private UUID lopMoi(String ten) {
        UUID lop = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, ?, 12, '2026-2027', now())")
            .params(lop, ten).update();
        return lop;
    }

    private static Document taiLieu(UUID lop, String ma, String vanBan) {
        return new Document(UUID.randomUUID(), lop, ma, "Ghi chú tự soạn", DocumentKind.TU_SOAN, "giáo viên thử", "tu_soan", null,
            vanBan, 1, null, LUC);
    }

    /** Bản khác của cùng tài liệu: văn bản và phiên bản mới, mọi trường khác giữ. */
    private static Document taiLieu(Document d, String vanBan, int phienBan) {
        return new Document(d.id(), d.classId(), d.code(), d.title(), d.kind(), d.source(), d.licenseStatus(), d.fileRef(), vanBan,
            phienBan, d.uploadedBy(), d.createdAt());
    }

    /** Đoạn {@code chu} ở đúng vị trí của nó trong văn bản tài liệu. */
    private static DocumentPassage doan(Document d, String chu) {
        int viTri = d.textContent().indexOf(chu);
        assertThat(viTri).as("đoạn có trong văn bản").isNotNegative();
        return DocumentPassage.of(d.id(), 1, viTri, chu);
    }

    @Test
    void taiLieuDangLaCanCuThiKhongHaQuyenDungDuoc() {
        // Codex #120 (P1): hạ quyền dùng xuống chua_ro thì tài liệu không còn là căn cứ (R9) mà bảng vẫn DAT.
        Document d = taiLieuDuocTrichDan();
        Document chuaRo = new Document(d.id(), d.classId(), d.code(), d.title(), d.kind(), d.source(), Document.CHUA_RO,
            d.fileRef(), d.textContent(), d.version(), d.uploadedBy(), d.createdAt());
        assertThatThrownBy(() -> documents.save(chuaRo, documents.findPassages(d.id())))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Lớp mới, tài liệu hai đoạn, bảng nháp có một dòng DAT trích dẫn đoạn đầu. */
    private Document taiLieuDuocTrichDan() {
        Document d = taiLieu(lopMoi("12A2 thử"), "sp-tai-lieu-0001", "Đạo hàm của tổng bằng tổng các đạo hàm. Đoạn khác.");
        List<DocumentPassage> doan = documents.save(d, List.of(doan(d, "Đạo hàm của tổng bằng tổng các đạo hàm."),
            doan(d, "Đoạn khác.")));
        UUID bang = UUID.randomUUID();
        jdbc.sql("insert into formula_sheets (id, class_id, version, status, created_at) values (?, ?, 1, 'NHAP', now())")
            .params(bang, d.classId()).update();
        jdbc.sql("""
                insert into formulas (id, formula_sheet_id, ordinal, code, title, latex, statement, kind, tier1_status,
                    tier2_status, citation_passage_id, checked_fingerprint)
                values (?, ?, 1, 'd-2', 'Đạo hàm tổng', 'x', 'y', 'DANG_THUC', 'DAT', 'DAT', ?, ?)""")
            .params(UUID.randomUUID(), bang, doan.getFirst().id(), "b".repeat(64)).update();
        return d;
    }
}
