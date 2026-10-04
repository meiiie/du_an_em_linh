package vn.hoctapcanman.core.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import vn.hoctapcanman.core.content.domain.model.CheckStatus;
import vn.hoctapcanman.core.content.domain.model.Document;
import vn.hoctapcanman.core.content.domain.model.DocumentKind;
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;
import vn.hoctapcanman.core.content.domain.model.Formula;
import vn.hoctapcanman.core.content.domain.model.FormulaCheck;
import vn.hoctapcanman.core.content.domain.model.FormulaKind;
import vn.hoctapcanman.core.content.domain.model.FormulaSheet;
import vn.hoctapcanman.core.content.domain.model.HintLevel;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ProblemRelease;
import vn.hoctapcanman.core.content.domain.model.ReleaseStatus;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.SubjectKind;
import vn.hoctapcanman.core.content.domain.model.TierResult;
import vn.hoctapcanman.core.content.domain.model.Topic;
import vn.hoctapcanman.core.content.domain.model.VerificationRun;

/**
 * Bảng công thức, lượt kiểm, phát hành, duyệt trên PostgreSQL 18 thật (Flyway V1–V5): đi đúng thứ tự mà khóa ngoại và
 * trigger đòi, đọc lại đúng, và các bất biến của CSDL đứng sau adapter.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    TopicCatalogRepositoryAdapter.class,
    ProblemRepositoryAdapter.class,
    HintLevelRepositoryAdapter.class,
    DocumentRepositoryAdapter.class,
    FormulaSheetRepositoryAdapter.class,
    VerificationRunRepositoryAdapter.class,
    ProblemReleaseRepositoryAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class VerificationPersistenceTest {

    private static final Instant LUC = Instant.parse("2026-10-04T08:00:00.123456Z");
    private static final String BAM = "a".repeat(64);
    private static final String VAN_BAN = "Đạo hàm của tổng bằng tổng các đạo hàm. Nếu đạo hàm đổi dấu từ dương sang âm thì đạt cực đại.";

    @Autowired
    private TopicCatalogRepositoryAdapter catalog;

    @Autowired
    private ProblemRepositoryAdapter problems;

    @Autowired
    private HintLevelRepositoryAdapter hints;

    @Autowired
    private DocumentRepositoryAdapter documents;

    @Autowired
    private FormulaSheetRepositoryAdapter sheets;

    @Autowired
    private VerificationRunRepositoryAdapter runs;

    @Autowired
    private ProblemReleaseRepositoryAdapter releases;

    @Autowired
    private JdbcClient jdbc;

    private UUID lop;
    private UUID giaoVien;
    private UUID doanTong;
    private UUID doanCucDai;
    private Problem bai;

    @BeforeEach
    void nen() {
        catalog.saveTopic(new Topic("DH12", "Đơn điệu và cực trị", 12));
        catalog.saveSkill(new Skill("T12.DH.03", "DH12", "Xét dấu đạo hàm", null, 12, true));
        lop = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A1 thử', 12, '2026-2027', now())")
            .params(lop).update();
        giaoVien = UUID.randomUUID();
        jdbc.sql("""
                insert into users (id, email, password_hash, display_name, role, created_at, updated_at)
                values (?, 'gv.thu@demo.local', 'x', 'Giáo viên thử', 'TEACHER', now(), now())""").params(giaoVien).update();
        Document d = new Document(UUID.randomUUID(), lop, "sp-tai-lieu-0001", "Quy tắc đạo hàm", DocumentKind.TU_SOAN, null, "tu_soan",
            null, VAN_BAN, 1, null, LUC);
        List<DocumentPassage> doan = documents.save(d, List.of(doan(d, "Đạo hàm của tổng bằng tổng các đạo hàm."),
            doan(d, "Nếu đạo hàm đổi dấu từ dương sang âm thì đạt cực đại.")));
        doanTong = doan.get(0).id();
        doanCucDai = doan.get(1).id();
        bai = new Problem(UUID.randomUUID(), "DH12-03-VD-01", "T12.DH.03", List.of(), Level4.VAN_DUNG, null, null, null,
            "Xét tính đơn điệu của y = x^3 - 6x^2 + 9x + 2.", "y = x^3 - 6x^2 + 9x + 2", "x**3-6*x**2+9*x+2", Problem.TU_LUAN_5_BUOC,
            null, "SUPHAM", BAM, null, LUC, LUC);
        problems.save(bai);
    }

    @Test
    void bangNhapRoiKhoaDocLaiDungVaKhongGhiKhacDiDuoc() {
        FormulaSheet nhap = bangNhap(1);
        sheets.save(nhap);
        assertThat(sheets.findDraft(lop)).contains(nhap);

        FormulaSheet khoa = daKiem(nhap).lock(giaoVien, LUC);
        sheets.save(khoa);
        FormulaSheet doc = sheets.findCurrent(lop).orElseThrow();
        assertThat(doc).isEqualTo(khoa);
        assertThat(doc.rows().get(1).extraCitationPassageIds()).containsExactly(doanTong);
        assertThat(sheets.findDraft(lop)).isEmpty();

        sheets.save(khoa);
        assertThatThrownBy(() -> sheets.save(nhap)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void luotKiemPhatHanhDuyetVaDoiBangThiLuotCu() {
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        int phienBan = problems.findContentVersion(bai.id()).orElseThrow();
        VerificationRun cho = VerificationRun.forProblem(lop, bai.id(), BAM, phienBan, bang.id(),
                List.of(new TierResult(1, CheckStatus.DAT, "DAO_HAM", null, null, 0.9, null, null, "{\"buoc\": 2}"),
                    TierResult.of(2, CheckStatus.DAT), TierResult.of(3, CheckStatus.KHONG_KIEM_DUOC)), List.of(doanCucDai), LUC);
        runs.save(cho);
        assertThat(runs.findLatest(lop, SubjectKind.PROBLEM, bai.id())).contains(cho);

        ProblemRelease phatHanh = ProblemRelease.draft(lop, bai.id(), LUC).apply(cho, true, BAM, bang.id(), LUC);
        releases.save(phatHanh);
        assertThat(releases.find(lop, bai.id())).contains(phatHanh);
        assertThat(phatHanh.status()).isEqualTo(ReleaseStatus.CHO_GIAO_VIEN_DUYET);

        VerificationRun.Approval duyet = cho.approve(giaoVien, "Đã đối chiếu SGK trang 12", true, BAM, bang.id(), LUC);
        runs.saveApproval(duyet);
        assertThat(runs.findById(cho.id())).contains(duyet.run());
        assertThat(runs.findReview(cho.id())).contains(duyet.review());
        assertThat(releases.find(lop, bai.id()).orElseThrow().status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        assertThat(releases.findByClass(lop)).hasSize(1);
        assertThatThrownBy(() -> runs.saveApproval(duyet)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void doiBangCongThucThiLuotKiemVoiBangCuThanhCu() {
        FormulaSheet bang1 = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang1);
        VerificationRun luot = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang1.id(), ba(CheckStatus.DAT), List.of(doanTong), LUC);
        runs.save(luot);
        FormulaSheet bang2 = daKiem(bang1.newDraft(2, LUC)).lock(giaoVien, LUC);
        sheets.save(bang2);
        assertThat(sheets.findCurrent(lop).orElseThrow().id()).isEqualTo(bang2.id());

        assertThat(runs.markStaleExceptSheet(lop, bang2.id())).isEqualTo(1);
        assertThat(runs.findById(luot.id()).orElseThrow().stale()).isTrue();
        assertThat(runs.markStaleExceptSheet(lop, bang2.id())).isZero();
    }

    @Test
    void luotKiemTrenPhienBanNoiDungCuBiTuChoi() {
        hints.replaceForProblem(bai.id(), List.of(new HintLevel(bai.id(), "B.DH.DAOHAM", 1, "Đạo hàm từng hạng tử.")));
        assertThat(problems.findContentVersion(bai.id())).contains(2);
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        VerificationRun cu = VerificationRun.forProblem(lop, bai.id(), BAM, 1, null, ba(CheckStatus.DAT), List.of(doanTong), LUC);
        assertThatThrownBy(() -> runs.save(cu)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void phatHanhChiTheoLuotMoiNhat() {
        // Codex #121 (P1): lượt A còn chờ, lượt B (SAI) mới hơn; phát hành chậm theo A không được đè kết quả của B.
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun a = choDuyet(bang, LUC);
        VerificationRun b = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.SAI), List.of(), LUC.plusSeconds(1));
        runs.save(a);
        runs.save(b);
        releases.save(ProblemRelease.draft(lop, bai.id(), LUC).apply(b, true, BAM, bang.id(), LUC));
        assertThat(releases.find(lop, bai.id()).orElseThrow().status()).isEqualTo(ReleaseStatus.BI_CHAN);
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        assertThatThrownBy(() -> releases.save(new ProblemRelease(lop, bai.id(), ReleaseStatus.CHO_GIAO_VIEN_DUYET, a.id(), LUC)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duyetChiLuotMoiNhat() {
        // Codex #121 (P2): lượt mới hơn đã có thì không duyệt được lượt cũ dù nó chưa bị đánh dấu cũ.
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun a = choDuyet(bang, LUC);
        runs.save(a);
        runs.save(choDuyet(bang, LUC.plusSeconds(1)));
        VerificationRun.Approval duyetA = a.approve(giaoVien, "Duyệt theo lượt cũ", true, BAM, bang.id(), LUC);
        assertThatThrownBy(() -> runs.saveApproval(duyetA)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duyetChiLuotKiemVoiBangDangDung() {
        // Codex #121 (P2): lớp đã khóa bảng mới thì không duyệt được lượt kiểm với bảng cũ.
        FormulaSheet bang1 = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang1);
        VerificationRun a = choDuyet(bang1, LUC);
        runs.save(a);
        sheets.save(daKiem(bang1.newDraft(2, LUC)).lock(giaoVien, LUC));
        VerificationRun.Approval duyetA = a.approve(giaoVien, "Duyệt với bảng cũ", true, BAM, bang1.id(), LUC);
        assertThatThrownBy(() -> runs.saveApproval(duyetA)).isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Lượt kiểm bài chờ giáo viên duyệt (tầng 3 không kiểm được), tầng 2 trích dẫn đoạn cực đại. */
    private VerificationRun choDuyet(FormulaSheet bang, Instant luc) {
        return VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(),
            List.of(TierResult.of(1, CheckStatus.DAT), TierResult.of(2, CheckStatus.DAT), TierResult.of(3, CheckStatus.KHONG_KIEM_DUOC)),
            List.of(doanCucDai), luc);
    }

    /** Bảng nháp hai dòng: tổng (đẳng thức) và cực đại (định lí). */
    private FormulaSheet bangNhap(int phienBan) {
        return FormulaSheet.draft(lop, phienBan, "bảng của v0", List.of(
            Formula.unchecked(1, "d-2", "T12.DH.03", "Đạo hàm tổng", "(u+v)' = u' + v'", "Đạo hàm của tổng bằng tổng các đạo hàm."),
            Formula.unchecked(2, "d-5", "T12.DH.03", "Cực trị", "+ \\to - : \\text{cực đại}", "Đổi dấu từ dương sang âm thì cực đại.")),
            LUC);
    }

    /** Kết quả DAT hai tầng cho mọi dòng; dòng định lí có thêm một đoạn trích. */
    private FormulaSheet daKiem(FormulaSheet bang) {
        Formula tong = bang.rows().get(0);
        Formula cucDai = bang.rows().get(1);
        return bang.withCheckResults(Map.of(
            tong.code(), FormulaCheck.of(tong, FormulaKind.DANG_THUC, CheckStatus.DAT, CheckStatus.DAT, "{\"muc\": \"CAS\"}", null, doanTong),
            cucDai.code(), FormulaCheck.of(cucDai, FormulaKind.DINH_LI, CheckStatus.DAT, CheckStatus.DAT, null, "{\"doan\": 2}",
                doanCucDai, List.of(doanTong))));
    }

    private static List<TierResult> ba(CheckStatus trangThai) {
        return List.of(TierResult.of(1, trangThai), TierResult.of(2, trangThai), TierResult.of(3, trangThai));
    }

    private static DocumentPassage doan(Document d, String chu) {
        return DocumentPassage.of(d.id(), 1, d.textContent().indexOf(chu), chu);
    }
}
