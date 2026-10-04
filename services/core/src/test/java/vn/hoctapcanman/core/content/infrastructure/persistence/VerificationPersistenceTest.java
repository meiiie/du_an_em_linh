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
        releases.save(ProblemRelease.draft(lop, bai.id(), LUC).apply(luot, true, BAM, bang1.id(), LUC));
        assertThat(releases.find(lop, bai.id()).orElseThrow().status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);

        // Khóa bảng mới thì lượt cũ thành cũ ngay trong giao dịch khóa bảng (V6), nhưng bài đã phát hành giữ nguyên và
        // «cần kiểm lại»: ADR 005, data-model §content (Codex #121, lần 3).
        FormulaSheet bang2 = daKiem(bang1.newDraft(2, LUC)).lock(giaoVien, LUC);
        sheets.save(bang2);
        assertThat(sheets.findCurrent(lop).orElseThrow().id()).isEqualTo(bang2.id());
        VerificationRun cu = runs.findById(luot.id()).orElseThrow();
        assertThat(cu.stale()).isTrue();
        ProblemRelease giu = releases.find(lop, bai.id()).orElseThrow();
        assertThat(giu.status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        assertThat(giu.runId()).isEqualTo(luot.id());
        assertThat(giu.needsRecheck(cu)).isTrue();

        // Kiểm lại với bảng mới: ghi lượt mới rút phát hành theo lượt cũ, áp lượt mới thì hết «cần kiểm lại».
        VerificationRun moi = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang2.id(), ba(CheckStatus.DAT), List.of(doanTong),
            LUC.plusSeconds(1));
        runs.save(moi);
        assertThat(releases.find(lop, bai.id()).orElseThrow().status()).isEqualTo(ReleaseStatus.NHAP);
        releases.save(releases.find(lop, bai.id()).orElseThrow().apply(moi, true, BAM, bang2.id(), LUC));
        ProblemRelease kiemLai = releases.find(lop, bai.id()).orElseThrow();
        assertThat(kiemLai.status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        assertThat(kiemLai.needsRecheck(runs.findById(moi.id()).orElseThrow())).isFalse();
        // Cuối test: lượt mới kiểm với bảng cũ bị từ chối (lỗi ràng buộc hủy giao dịch của test).
        VerificationRun voiBangCu = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang1.id(), ba(CheckStatus.DAT), List.of(doanTong),
            LUC.plusSeconds(2));
        assertThatThrownBy(() -> runs.save(voiBangCu)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void luotKiemTrenPhienBanNoiDungCuBiTuChoi() {
        hints.replaceForProblem(bai.id(), List.of(new HintLevel(bai.id(), "B.DH.DAOHAM", 1, "Đạo hàm từng hạng tử.")));
        assertThat(problems.findContentVersion(bai.id())).contains(2);
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        VerificationRun cu = VerificationRun.forProblem(lop, bai.id(), BAM, 1, null, ba(CheckStatus.KHONG_KIEM_DUOC), List.of(), LUC);
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
        // Khóa bảng mới đã làm A thành cũ (V6), nên lần ghi duyệt không còn lượt chờ duyệt nào để chuyển.
        assertThat(runs.findById(a.id()).orElseThrow().stale()).isTrue();
        VerificationRun.Approval duyetA = a.approve(giaoVien, "Duyệt với bảng cũ", true, BAM, bang1.id(), LUC);
        assertThatThrownBy(() -> runs.saveApproval(duyetA))
            .isInstanceOfAny(IllegalStateException.class, DataIntegrityViolationException.class);
    }

    @Test
    void luotMoiRutPhatHanhTheoLuotCu() {
        // Codex #121 (P1): A đang phát hành; ghi B (SAI) thì A thôi phát hành ngay, kể cả khi nơi gọi chưa kịp áp B.
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun a = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.DAT), List.of(doanTong), LUC);
        runs.save(a);
        releases.save(ProblemRelease.draft(lop, bai.id(), LUC).apply(a, true, BAM, bang.id(), LUC));
        assertThat(releases.find(lop, bai.id()).orElseThrow().status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);

        VerificationRun b = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.SAI), List.of(), LUC.plusSeconds(1));
        runs.save(b);
        assertThat(releases.find(lop, bai.id()).orElseThrow().status()).isEqualTo(ReleaseStatus.NHAP);
        releases.save(releases.find(lop, bai.id()).orElseThrow().apply(b, true, BAM, bang.id(), LUC));
        assertThat(releases.find(lop, bai.id()).orElseThrow().status()).isEqualTo(ReleaseStatus.BI_CHAN);
    }

    @Test
    void luotGhiSauMaCuHonKhongRutPhatHanhCuaLuotMoiNhat() {
        // Codex #121 (P1, lần 3): A tạo trước nhưng ghi sau khi B đã ghi và áp; A không phải lượt mới nhất nên không rút B.
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun b = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.DAT), List.of(doanTong),
            LUC.plusSeconds(1));
        runs.save(b);
        releases.save(ProblemRelease.draft(lop, bai.id(), LUC).apply(b, true, BAM, bang.id(), LUC));

        VerificationRun a = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.SAI), List.of(), LUC);
        runs.save(a);
        assertThat(runs.findLatest(lop, SubjectKind.PROBLEM, bai.id())).contains(b);
        ProblemRelease r = releases.find(lop, bai.id()).orElseThrow();
        assertThat(r.status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        assertThat(r.runId()).isEqualTo(b.id());
        // Cuối test: áp A (không mới nhất) bị CSDL từ chối (lỗi ràng buộc hủy giao dịch của test).
        assertThatThrownBy(() -> releases.save(new ProblemRelease(lop, bai.id(), ReleaseStatus.BI_CHAN, a.id(), LUC)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void luotCongThucGiaSuTang2DatPhaiCoTrichDan() {
        // Codex #121 (P1, lần 3): tầng 2 DAT bắt buộc trích dẫn ở mọi loại lượt (ADR 005, 013), không chỉ lượt kiểm bài.
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun coTrichDan = new VerificationRun(UUID.randomUUID(), lop, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(), BAM,
            null, bang.id(), CheckStatus.DAT, null, false, LUC, ba(CheckStatus.DAT), List.of(doanTong));
        runs.save(coTrichDan);
        jdbc.sql("set constraints all immediate").update();
        assertThat(runs.findById(coTrichDan.id())).contains(coTrichDan);
        jdbc.sql("set constraints all deferred").update();

        // Domain không dựng được lượt thiếu trích dẫn, nên ghi thẳng bằng SQL như một nơi ghi khác.
        UUID thieu = UUID.randomUUID();
        jdbc.sql("""
                insert into verification_runs (id, class_id, subject_kind, subject_id, content_hash, formula_sheet_id, overall_status,
                    created_at)
                values (?, ?, 'TUTOR_FORMULA', ?, ?, ?, 'DAT', now())""").params(thieu, lop, UUID.randomUUID(), BAM, bang.id()).update();
        for (int tang = 1; tang <= 3; tang++) {
            jdbc.sql("insert into verification_tier_results (run_id, tier, status) values (?, ?, 'DAT')").params(thieu, tang).update();
        }
        // Cuối test: kiểm lúc commit (ép ngay bằng SET CONSTRAINTS), lỗi ràng buộc hủy giao dịch của test.
        assertThatThrownBy(() -> jdbc.sql("set constraints all immediate").update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lopChuaKhoaBangThiTang3KhongDatDuoc() {
        // Codex #121 (P1, lần 4): lớp chưa khóa bảng nào thì lượt kiểm không có bảng; tầng 3 chỉ KHONG_KIEM_DUOC.
        VerificationRun cho = VerificationRun.forProblem(lop, bai.id(), BAM, 1, null,
            List.of(TierResult.of(1, CheckStatus.DAT), TierResult.of(2, CheckStatus.DAT), TierResult.of(3, CheckStatus.KHONG_KIEM_DUOC)),
            List.of(doanTong), LUC);
        runs.save(cho);
        jdbc.sql("set constraints all immediate").update();
        assertThat(runs.findById(cho.id())).contains(cho);
        jdbc.sql("set constraints all deferred").update();
        assertThatThrownBy(() -> VerificationRun.forProblem(lop, bai.id(), BAM, 1, null, ba(CheckStatus.DAT), List.of(doanTong), LUC))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Tầng 3");

        // Nơi ghi khác (SQL thẳng): lượt không bảng mà thêm tầng 3 DAT bị từ chối lúc commit.
        UUID khongBang = UUID.randomUUID();
        jdbc.sql("""
                insert into verification_runs (id, class_id, subject_kind, subject_id, content_hash, overall_status, publish_status,
                    content_version, created_at)
                values (?, ?, 'PROBLEM', ?, ?, 'KHONG_KIEM_DUOC', 'CHO_GIAO_VIEN_DUYET', 1, clock_timestamp())""")
            .params(khongBang, lop, bai.id(), BAM).update();
        jdbc.sql("insert into verification_tier_results (run_id, tier, status) values (?, 3, 'DAT')").params(khongBang).update();
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        assertThatThrownBy(() -> jdbc.sql("set constraints all immediate").update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void themTang2DatSauMaKhongTrichDanBiTuChoi() {
        // Codex #121 (P1, lần 4): ghi lượt trước, thêm tầng 2 DAT sau (đã qua lần kiểm của lượt) vẫn bị kiểm căn cứ.
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun motTang = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), List.of(TierResult.of(1, CheckStatus.DAT)),
            List.of(), LUC);
        runs.save(motTang);
        jdbc.sql("set constraints all immediate").update();
        jdbc.sql("set constraints all deferred").update();
        jdbc.sql("insert into verification_tier_results (run_id, tier, status) values (?, 2, 'DAT')").params(motTang.id()).update();
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        assertThatThrownBy(() -> jdbc.sql("set constraints all immediate").update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ketQuaTangKhongSuaDuoc() {
        // Kết quả tầng chỉ thêm như lượt và trích dẫn: không đổi phán quyết SAI thành DAT sau khi đã ghi.
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun sai = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.SAI), List.of(), LUC);
        runs.save(sai);
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        assertThatThrownBy(() -> jdbc.sql("update verification_tier_results set status = 'DAT' where run_id = ? and tier = 1")
            .params(sai.id()).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void xoaLuotThiKetQuaTangXoaTheoDayChuyenXoaRiengThiKhong() {
        FormulaSheet bang = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        sheets.save(bang);
        VerificationRun xoa = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.SAI), List.of(), LUC);
        VerificationRun con = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang.id(), ba(CheckStatus.SAI), List.of(),
            LUC.plusSeconds(1));
        runs.save(xoa);
        runs.save(con);
        jdbc.sql("delete from verification_runs where id = ?").params(xoa.id()).update();
        assertThat(jdbc.sql("select count(*) from verification_tier_results where run_id = ?").params(xoa.id()).query(Long.class).single())
            .isZero();
        // Cuối test: xóa riêng một tầng của lượt còn đó bị từ chối (lỗi ràng buộc hủy giao dịch của test).
        assertThatThrownBy(() -> jdbc.sql("delete from verification_tier_results where run_id = ? and tier = 1").params(con.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void khongGhiDeBangNhapCuaLopKhac() {
        // Codex #121 (P2): id trùng bảng nháp của lớp khác thì từ chối, không đổi ghi chú hay dòng của lớp đó.
        FormulaSheet cuaLop = bangNhap(1);
        sheets.save(cuaLop);
        UUID lopKhac = UUID.randomUUID();
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A3 thử', 12, '2026-2027', now())")
            .params(lopKhac).update();
        FormulaSheet maoDanh = new FormulaSheet(cuaLop.id(), lopKhac, 1, cuaLop.status(), "ghi đè", null, null, null, LUC,
            List.of(Formula.unchecked(1, "d-9", "T12.DH.03", "Lạ", "x", "Dòng lạ.")));
        assertThatThrownBy(() -> sheets.save(maoDanh)).isInstanceOf(IllegalStateException.class).hasMessageContaining("lớp khác");
        assertThat(sheets.findDraft(lop)).contains(cuaLop);
        assertThat(sheets.findDraft(lopKhac)).isEmpty();
    }

    @Test
    void khongKhoaDuocBangCuHonBangDangDung() {
        // Codex #121 (P2): khóa muộn một bảng phiên bản thấp hơn không được làm lượt của bảng mới nhất thành cũ.
        FormulaSheet bang2 = daKiem(bangNhap(2)).lock(giaoVien, LUC);
        sheets.save(bang2);
        VerificationRun luot = VerificationRun.forProblem(lop, bai.id(), BAM, 1, bang2.id(), ba(CheckStatus.DAT), List.of(doanTong), LUC);
        runs.save(luot);
        FormulaSheet bang1 = daKiem(bangNhap(1)).lock(giaoVien, LUC);
        // Cuối test: lỗi ràng buộc hủy giao dịch của test.
        assertThatThrownBy(() -> sheets.save(bang1)).isInstanceOf(DataIntegrityViolationException.class);
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
