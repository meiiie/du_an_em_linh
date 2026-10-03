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
        UUID lop = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A1 thử', 12, '2026-2027', now())")
            .params(lop).update();
        Document d = new Document(UUID.randomUUID(), lop, "v0-don-dieu", "Ghi chú tự soạn", DocumentKind.TU_SOAN, "giáo viên thử",
            "tu_soan", null, "Đoạn một. Đoạn hai.", 1, null, LUC);
        List<DocumentPassage> lan1 = documents.save(d, List.of(DocumentPassage.of(d.id(), 1, 0, "Đoạn một."),
            DocumentPassage.of(d.id(), 1, 10, "Đoạn hai.")));
        assertThat(lan1).extracting(DocumentPassage::textFolded).containsExactly("doan mot.", "doan hai.");

        List<DocumentPassage> lan2 = documents.save(d, List.of(DocumentPassage.of(d.id(), 1, 0, "Đoạn một (sửa).")));
        assertThat(lan2).hasSize(1);
        assertThat(lan2.getFirst().id()).isEqualTo(lan1.getFirst().id());
        assertThat(lan2.getFirst().text()).isEqualTo("Đoạn một (sửa).");

        assertThat(documents.findByClassAndCode(lop, "v0-don-dieu")).contains(
            new Document(d.id(), lop, "v0-don-dieu", "Ghi chú tự soạn", DocumentKind.TU_SOAN, "giáo viên thử", "tu_soan", null,
                "Đoạn một. Đoạn hai.", 1, null, Instant.parse("2026-10-03T08:00:00.123456Z")));
        assertThat(documents.findByClass(lop)).hasSize(1);
        assertThatThrownBy(() -> documents.save(d, List.of(DocumentPassage.of(UUID.randomUUID(), 1, 0, "lạc"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doanDangLaCanCuThiKhongSuaChuDuoc() {
        // Codex #120 (P1): nạp lại tài liệu đổi chữ của đoạn đang được trích dẫn thì bảng vẫn mang kết quả DAT cũ.
        UUID lop = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A2 thử', 12, '2026-2027', now())")
            .params(lop).update();
        Document d = new Document(UUID.randomUUID(), lop, "sp-tai-lieu-0001", "Quy tắc đạo hàm", DocumentKind.TU_SOAN, null,
            "tu_soan", null, "Đạo hàm của tổng bằng tổng các đạo hàm. Đoạn khác.", 1, null, LUC);
        List<DocumentPassage> doan = documents.save(d, List.of(
            DocumentPassage.of(d.id(), 1, 0, "Đạo hàm của tổng bằng tổng các đạo hàm."),
            DocumentPassage.of(d.id(), 1, 41, "Đoạn khác.")));
        UUID bang = UUID.randomUUID();
        jdbc.sql("insert into formula_sheets (id, class_id, version, status, created_at) values (?, ?, 1, 'NHAP', now())")
            .params(bang, lop).update();
        jdbc.sql("""
                insert into formulas (id, formula_sheet_id, ordinal, code, title, latex, statement, kind, tier1_status,
                    tier2_status, citation_passage_id, checked_fingerprint)
                values (?, ?, 1, 'd-2', 'Đạo hàm tổng', 'x', 'y', 'DANG_THUC', 'DAT', 'DAT', ?, ?)""")
            .params(UUID.randomUUID(), bang, doan.getFirst().id(), "b".repeat(64)).update();

        // Đoạn không được trích dẫn vẫn sửa được.
        documents.save(d, List.of(DocumentPassage.of(d.id(), 1, 0, "Đạo hàm của tổng bằng tổng các đạo hàm."),
            DocumentPassage.of(d.id(), 1, 41, "Đoạn khác (sửa).")));
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        assertThatThrownBy(() -> documents.save(d, List.of(DocumentPassage.of(d.id(), 1, 0, "Đạo hàm của tổng bằng tích các đạo hàm."),
            DocumentPassage.of(d.id(), 1, 41, "Đoạn khác (sửa).")))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
