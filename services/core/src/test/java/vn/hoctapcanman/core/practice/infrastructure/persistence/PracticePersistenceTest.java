package vn.hoctapcanman.core.practice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.practice.domain.model.Assignment;
import vn.hoctapcanman.core.practice.domain.model.AssignmentStatus;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;
import vn.hoctapcanman.core.practice.domain.model.InputEvent;
import vn.hoctapcanman.core.practice.domain.model.SignTable;
import vn.hoctapcanman.core.practice.domain.model.StepLine;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.SkillLevel;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.model.SubmissionStatus;
import vn.hoctapcanman.core.practice.domain.model.TableCell;

/**
 * Flyway V7, V8 trên PostgreSQL 18 thật + adapter JDBC của practice: bất biến của bài làm (học sinh của lớp, bài đã phát
 * hành, đúng phiên bản nội dung, một bài làm đang làm, đã nộp thì không ghi thêm, nộp có căn cứ là lần chấm có phán quyết
 * của chính bài làm), nội dung bước thay trọn và giữ thứ tự, kết quả chấm chỉ thêm và không ghi lần hai cho cùng yêu cầu,
 * giao bài. Mỗi ca CSDL từ chối là lệnh cuối của test (lỗi làm hỏng giao dịch của test).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    SubmissionRepositoryAdapter.class,
    GradingResultRepositoryAdapter.class,
    AssignmentRepositoryAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class PracticePersistenceTest {

    private static final Instant LUC = Instant.parse("2026-10-05T08:00:00.123456789Z");
    private static final SkillLevel PHAN_LOAI = new SkillLevel("T12.DH.02", "THONG_HIEU");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SubmissionRepositoryAdapter submissions;

    @Autowired
    private GradingResultRepositoryAdapter grades;

    @Autowired
    private AssignmentRepositoryAdapter assignments;

    @Autowired
    private JdbcClient jdbc;

    private UUID lop;
    private UUID giaoVien;
    private UUID an;
    private UUID bai;

    @BeforeEach
    void duLieu() {
        DuLieuPractice.danhMuc(jdbc);
        lop = DuLieuPractice.lop(jdbc);
        giaoVien = DuLieuPractice.nguoi(jdbc, "TEACHER");
        an = DuLieuPractice.nguoi(jdbc, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, giaoVien, "TEACHER");
        DuLieuPractice.ghiDanh(jdbc, lop, an, "STUDENT");
        bai = DuLieuPractice.bai(jdbc, "PR-01");
        DuLieuPractice.phatHanh(jdbc, lop, bai);
    }

    @Test
    void moHaiLanLaCungMotBaiLam() {
        Submission dau = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        Submission sau = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC.plusSeconds(5)));
        assertThat(sau).isEqualTo(dau);
        assertThat(dau.status()).isEqualTo(SubmissionStatus.DANG_LAM);
        assertThat(dau.startedAt()).isEqualTo(LUC.truncatedTo(ChronoUnit.MICROS));
        assertThat(submissions.findOpen(an, lop, bai, 1)).contains(dau);
        assertThat(submissions.findById(dau.id())).contains(dau);
    }

    @Test
    void noiDungDoiThiMoBaiLamMoiConBaiLamDoCuGiuNguyen() {
        Submission cu = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        // Đổi nội dung bài: V5 tăng content_version và rút phát hành; kiểm lại rồi phát hành phiên bản mới.
        jdbc.sql("update problems set content_hash = ? where id = ?").params("b".repeat(64), bai).update();
        assertThat(jdbc.sql("select content_version from problems where id = ?").params(bai).query(Integer.class).single()).isEqualTo(2);
        DuLieuPractice.phatHanh(jdbc, lop, bai);
        Submission moi = submissions.openOrGet(Submission.open(lop, an, bai, 2, LUC.plusSeconds(60)));
        assertThat(moi.id()).isNotEqualTo(cu.id());
        assertThat(submissions.findOpen(an, lop, bai, 2)).contains(moi);
        assertThat(submissions.findOpen(an, lop, bai, 1)).contains(cu);
        assertThat(submissions.findLatest(an, lop, bai)).contains(moi);
    }

    @Test
    void khoaChiBaiLamDangLamOPhienBanHienTai() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        assertThat(submissions.lockOpen(an, lop, bai, 1)).contains(dangLam);
        assertThat(submissions.lockOpen(an, lop, bai, 2)).isEmpty();
        submissions.update(nop(dangLam, GradeStatus.SAI, LUC.plusSeconds(10)));
        assertThat(submissions.lockOpen(an, lop, bai, 1)).as("đã nộp").isEmpty();
        Submission lamLai = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC.plusSeconds(20)));
        assertThat(submissions.lockOpen(an, lop, bai, 1)).contains(lamLai);
        jdbc.sql("update problems set content_hash = ? where id = ?").params("b".repeat(64), bai).update();
        assertThat(submissions.lockOpen(an, lop, bai, 1)).as("đề đã đổi").isEmpty();
    }

    @Test
    void lichSuGomMoiBaiLamCuaHocSinhChoBai() {
        // Mỗi học sinh một lớp (V3), nên lịch sử nhiều lớp chỉ thử được ở SubmissionHistoryTest.
        UUID binh = DuLieuPractice.nguoi(jdbc, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, binh, "STUDENT");
        UUID baiKhac = DuLieuPractice.bai(jdbc, "PR-04");
        DuLieuPractice.phatHanh(jdbc, lop, baiKhac);
        Submission daNop = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.update(nop(daNop, GradeStatus.DAT, LUC.plusSeconds(10)));
        Submission lamLai = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC.plusSeconds(20)));
        submissions.openOrGet(Submission.open(lop, binh, bai, 1, LUC.plusSeconds(30)));
        submissions.openOrGet(Submission.open(lop, an, baiKhac, 1, LUC.plusSeconds(40)));
        assertThat(submissions.history(an, bai).attempts())
            .containsExactly(submissions.findById(daNop.id()).orElseThrow(), lamLai);
    }

    @Test
    void giaoVienKhongMoDuocBaiLam() {
        assertThatThrownBy(() -> submissions.openOrGet(Submission.open(lop, giaoVien, bai, 1, LUC)))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("không là học sinh");
    }

    @Test
    void baiChuaPhatHanhKhongMoDuoc() {
        UUID chuaPhatHanh = DuLieuPractice.bai(jdbc, "PR-02");
        assertThatThrownBy(() -> submissions.openOrGet(Submission.open(lop, an, chuaPhatHanh, 1, LUC)))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chưa phát hành");
    }

    @Test
    void phienBanNoiDungCuKhongMoDuoc() {
        assertThatThrownBy(() -> submissions.openOrGet(Submission.open(lop, an, bai, 2, LUC)))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("phiên bản nội dung");
    }

    @Test
    void nopLaiBuocThayTronNoiDungVaGiuThuTuKhungVaThuTuO() {
        UUID bl = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC)).id();
        submissions.saveStep(bl, new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "3x^2-6x", null)), null));
        submissions.saveStep(bl, new StepWork("B.DH.NGHIEM",
            List.of(new StepLine(0, "x=0", "NGHIEM"), new StepLine(1, "x=2", "NGHIEM")), null));
        // Ô gửi không theo thứ tự hàng: đọc lại phải đúng thứ tự gửi (payload chấm trùng v0).
        List<TableCell> o = List.of(new TableCell("X", 0, "0"), new TableCell("X", 1, "2"), new TableCell("DAU_YPHAY", 0, "+"),
            new TableCell("DAU_YPHAY", 2, "-"), new TableCell("DAU_YPHAY", 1, "0"), new TableCell("BIEN_THIEN", 0, "↗"));
        submissions.saveStep(bl, new StepWork("B.DH.XETDAU", List.of(), new SignTable("XET_DAU", o)));
        submissions.saveStep(bl, new StepWork("B.DH.TXD", List.of(new StepLine(0, "D = \\mathbb{R}", null)), null));

        assertThat(submissions.steps(bl)).extracting(StepWork::stepCode)
            .containsExactly("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU");
        StepWork xetDau = submissions.steps(bl).get(3);
        assertThat(xetDau.lines()).isEmpty();
        assertThat(xetDau.table()).isEqualTo(new SignTable("XET_DAU", o));
        assertThat(submissions.steps(bl).get(2).lines())
            .containsExactly(new StepLine(0, "x=0", "NGHIEM"), new StepLine(1, "x=2", "NGHIEM"));

        // Nộp lại: thay trọn, không gộp với nội dung cũ.
        submissions.saveStep(bl, new StepWork("B.DH.NGHIEM", List.of(new StepLine(0, "x=0", "NGHIEM")), null));
        submissions.saveStep(bl, new StepWork("B.DH.XETDAU", List.of(), new SignTable("XET_DAU", o.subList(0, 2))));
        assertThat(submissions.steps(bl).get(2).lines()).containsExactly(new StepLine(0, "x=0", "NGHIEM"));
        assertThat(submissions.steps(bl).get(3).table()).isEqualTo(new SignTable("XET_DAU", o.subList(0, 2)));
    }

    @Test
    void nopBaiRoiThiKhongGhiThemDuoc() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.saveStep(dangLam.id(), new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "3x^2-6x", null)), null));
        GradingResult canCu = grades.record(ketQua(dangLam.id(), "1".repeat(64), GradeStatus.SAI));
        Submission daNop = dangLam.submit(canCu, PHAN_LOAI, LUC.plusSeconds(60)).baiLam();
        submissions.update(daNop);
        assertThat(grades.findById(canCu.id())).contains(canCu);

        assertThat(submissions.findById(dangLam.id()).orElseThrow().status()).isEqualTo(SubmissionStatus.DA_NOP);
        assertThat(submissions.findOpen(an, lop, bai, 1)).isEmpty();
        assertThat(submissions.findLatest(an, lop, bai)).contains(new Submission(dangLam.id(), lop, an, bai, 1, SubmissionStatus.DA_NOP,
            false, null, GradeStatus.SAI, canCu.id(), LUC.truncatedTo(ChronoUnit.MICROS), LUC.plusSeconds(60).truncatedTo(ChronoUnit.MICROS),
            PHAN_LOAI));
        StepWork buoc = new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "3x^2", null)), null);
        assertThatThrownBy(() -> submissions.saveStep(dangLam.id(), buoc)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> submissions.addEvents(dangLam.id(), List.of(suKien("a", LUC))))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> grades.record(ketQua(dangLam.id(), "b".repeat(64), GradeStatus.DAT)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> submissions.update(daNop)).isInstanceOf(IllegalStateException.class);
        assertThat(submissions.steps(dangLam.id()).getFirst().lines()).containsExactly(new StepLine(0, "3x^2-6x", null));

        // Làm lại: bài làm mới, bài làm đã nộp giữ nguyên.
        Submission lanHai = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC.plusSeconds(120)));
        assertThat(lanHai.id()).isNotEqualTo(dangLam.id());
        assertThat(submissions.findLatest(an, lop, bai)).contains(lanHai);
    }

    @Test
    void csdlChanSuaBuocCuaBaiLamDaNop() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.saveStep(dangLam.id(), new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "3x^2-6x", null)), null));
        submissions.update(nop(dangLam, GradeStatus.SAI, LUC));
        assertThatThrownBy(() -> jdbc.sql("update submission_steps set latex = '3x^2' where submission_id = ?").params(dangLam.id())
                .update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("đã nộp");
    }

    @Test
    void moLaiKhiTabKhacNopBaiLamGiuaLanGhiVaLanDoc() {
        // Codex #136 (P2): lần ghi trùng bài làm đang mở nên không ghi; tab khác nộp bài làm đó trước lần đọc; lần đọc trượt.
        Submission cu = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        int[] lanDoc = {0};
        SubmissionRepositoryAdapter xenNop = new SubmissionRepositoryAdapter(jdbc) {
            @Override
            public Optional<Submission> findOpen(UUID studentId, UUID classId, UUID problemId, int contentVersion) {
                if (lanDoc[0]++ == 0) {
                    submissions.update(nop(cu, GradeStatus.SAI, LUC.plusSeconds(30)));
                }
                return super.findOpen(studentId, classId, problemId, contentVersion);
            }
        };
        Submission moi = Submission.open(lop, an, bai, 1, LUC.plusSeconds(60));
        Submission moRa = xenNop.openOrGet(moi);
        assertThat(lanDoc[0]).isEqualTo(2);
        assertThat(moRa.id()).isEqualTo(moi.id()).isNotEqualTo(cu.id());
        assertThat(moRa.status()).isEqualTo(SubmissionStatus.DANG_LAM);
        assertThat(submissions.findById(cu.id()).orElseThrow().status()).isEqualTo(SubmissionStatus.DA_NOP);
    }

    @Test
    void moBaoLoiSauSoVongCoHan() {
        int[] lanDoc = {0};
        SubmissionRepositoryAdapter luonTruot = new SubmissionRepositoryAdapter(jdbc) {
            @Override
            public Optional<Submission> findOpen(UUID studentId, UUID classId, UUID problemId, int contentVersion) {
                lanDoc[0]++;
                return Optional.empty();
            }
        };
        assertThatThrownBy(() -> luonTruot.openOrGet(Submission.open(lop, an, bai, 1, LUC)))
            .isInstanceOf(IllegalStateException.class).hasMessage("Không mở được bài làm");
        assertThat(lanDoc[0]).isEqualTo(SubmissionRepositoryAdapter.SO_LAN_MO);
    }

    @Test
    void csdlChanChuyenBuocCuaBaiLamDaNopSangBaiLamKhac() {
        // Codex #136 (P2): UPDATE đổi submission_id không được rút dòng khỏi bài làm đã nộp.
        Submission daNop = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.saveStep(daNop.id(), new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "3x^2-6x", null)), null));
        submissions.update(nop(daNop, GradeStatus.SAI, LUC));
        Submission moi = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC.plusSeconds(60)));
        assertThatThrownBy(() -> jdbc.sql("update submission_steps set submission_id = ? where submission_id = ?")
                .params(moi.id(), daNop.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("không chuyển");
    }

    @Test
    void csdlChanNopBaiVoiKetQuaKhongChamDuoc() {
        // Codex #136 (P2): lần chấm cuối lỗi dịch vụ toán không được đóng bài làm.
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        GradingResult loi = grades.record(GradingResult.notGraded(dangLam.id(), "B.DH.KETLUAN", "3".repeat(64), "bận", LUC));
        assertThatThrownBy(() -> jdbc.sql("""
                update submissions set status = 'DA_NOP', result = 'KHONG_CHAM_DUOC', result_grading_id = ?, submitted_at = now(),
                    skill_code = 'T12.DH.02', level4 = 'THONG_HIEU'
                where id = ?""").params(loi.id(), dangLam.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void csdlChanNopBaiKhongCanCu() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        assertThatThrownBy(() -> jdbc.sql("""
                update submissions set status = 'DA_NOP', result = 'DAT', submitted_at = now(), skill_code = 'T12.DH.02',
                    level4 = 'THONG_HIEU' where id = ?""")
                .params(dangLam.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("submissions_nop_co_can_cu");
    }

    @Test
    void csdlChanNopBaiKhongGhimPhanLoai() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        GradingResult dat = grades.record(ketQua(dangLam.id(), "7".repeat(64), GradeStatus.DAT));
        assertThatThrownBy(() -> jdbc.sql("""
                update submissions set status = 'DA_NOP', result = 'DAT', result_grading_id = ?, submitted_at = now(),
                    skill_code = 'T12.DH.02' where id = ?""").params(dat.id(), dangLam.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("submissions_nop_ghim_phan_loai");
    }

    @Test
    void csdlChanNopBaiThieuMaKyNang() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        GradingResult dat = grades.record(ketQua(dangLam.id(), "8".repeat(64), GradeStatus.DAT));
        assertThatThrownBy(() -> jdbc.sql("""
                update submissions set status = 'DA_NOP', result = 'DAT', result_grading_id = ?, submitted_at = now(),
                    level4 = 'THONG_HIEU' where id = ?""").params(dat.id(), dangLam.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("submissions_nop_ghim_phan_loai");
    }

    @Test
    void csdlChanBaiLamDangLamCoPhanLoai() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        assertThatThrownBy(() -> jdbc.sql("update submissions set skill_code = 'T12.DH.02', level4 = 'THONG_HIEU' where id = ?")
                .params(dangLam.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("submissions_nop_ghim_phan_loai");
    }

    @Test
    void csdlChanCanCuCuaBaiLamKhac() {
        UUID binh = DuLieuPractice.nguoi(jdbc, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, binh, "STUDENT");
        Submission cuaAn = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        Submission cuaBinh = submissions.openOrGet(Submission.open(lop, binh, bai, 1, LUC));
        GradingResult chamBinh = grades.record(ketQua(cuaBinh.id(), "4".repeat(64), GradeStatus.DAT));
        assertThat(ghimCanCu(cuaBinh.id(), "DAT", chamBinh.id())).isOne();
        assertThatThrownBy(() -> ghimCanCu(cuaAn.id(), "DAT", chamBinh.id()))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("submissions_can_cu_cua_chinh_bai_lam");
    }

    @Test
    void csdlChanCanCuKhongCoPhanQuyet() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        GradingResult loi = grades.record(GradingResult.notGraded(dangLam.id(), "B.DH.KETLUAN", "5".repeat(64), "bận", LUC));
        assertThatThrownBy(() -> ghimCanCu(dangLam.id(), "DAT", loi.id()))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("submissions_can_cu_cua_chinh_bai_lam");
    }

    @Test
    void csdlChanCanCuLechKetQua() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        GradingResult sai = grades.record(ketQua(dangLam.id(), "6".repeat(64), GradeStatus.SAI));
        assertThatThrownBy(() -> ghimCanCu(dangLam.id(), "DAT", sai.id()))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("submissions_can_cu_cua_chinh_bai_lam");
    }

    @Test
    void xoaLopXoaCaBaiLamDaNopVaCanCu() {
        // Khóa ngoại vòng submissions ⇄ grading_results (V8) không chặn xóa dây chuyền.
        Submission daNop = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        grades.record(ketQua(daNop.id(), "7".repeat(64), GradeStatus.SAI));
        submissions.update(nop(daNop, GradeStatus.DAT, LUC.plusSeconds(10)));
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC.plusSeconds(20)));
        grades.record(ketQua(dangLam.id(), "8".repeat(64), GradeStatus.SAI));
        assertThat(jdbc.sql("delete from classes where id = ?").params(lop).update()).isOne();
        assertThat(jdbc.sql("select count(*) from submissions where id in (?, ?)").params(daNop.id(), dangLam.id()).query(Integer.class)
            .single()).isZero();
        assertThat(jdbc.sql("select count(*) from grading_results where submission_id in (?, ?)").params(daNop.id(), dangLam.id())
            .query(Integer.class).single()).isZero();
    }

    @Test
    void csdlChanDoiPhienBanCuaBaiLam() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        assertThatThrownBy(() -> jdbc.sql("update submissions set content_version = 2 where id = ?").params(dangLam.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void chamLaiCungYeuCauKhongGhiLanHaiTruKhongChamDuoc() throws Exception {
        UUID bl = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC)).id();
        String bam = "c".repeat(64);
        GradingResult dau = grades.record(new GradingResult(UUID.randomUUID(), bl, "B.DH.DAOHAM", bam, GradeStatus.SAI, "SAI_BUOC",
            "{\"ma_buoc\": \"B.DH.DAOHAM\", \"dong\": 0, \"o\": null}", "ERR.DH.02", 0.35,
            Map.of("B.DH.TXD", "DAT", "B.DH.DAOHAM", "SAI"), "Đạo hàm của x^3 chưa đúng.", "[]", null, false, "norm-0.2",
            "[{\"ma_buoc\": \"B.DH.DAOHAM\", \"dong\": 0, \"trang_thai_chuan_hoa\": \"OK\"}]", LUC));
        GradingResult tabHai = grades.record(ketQua(bl, bam, GradeStatus.DAT));
        assertThat(tabHai).isEqualTo(dau);
        assertThat(dau.confidence()).isEqualTo(0.35);
        assertThat(dau.perStep()).isEqualTo(Map.of("B.DH.TXD", "DAT", "B.DH.DAOHAM", "SAI"));
        assertThat(JSON.readTree(dau.wrongStepsJson())).isEqualTo(JSON.readTree("{\"ma_buoc\":\"B.DH.DAOHAM\",\"dong\":0,\"o\":null}"));
        assertThat(grades.bySubmission(bl)).hasSize(1);

        // Dịch vụ toán lỗi: ghi mỗi lần, không chặn lần chấm lại có phán quyết.
        String bam2 = "d".repeat(64);
        grades.record(GradingResult.notGraded(bl, "B.DH.NGHIEM", bam2, "Máy chấm đang bận.", LUC.plusSeconds(1)));
        grades.record(GradingResult.notGraded(bl, "B.DH.NGHIEM", bam2, "Máy chấm đang bận.", LUC.plusSeconds(2)));
        assertThat(grades.findByRequest(bl, bam2)).isEmpty();
        GradingResult coPhanQuyet = grades.record(ketQua(bl, bam2, GradeStatus.DAT, LUC.plusSeconds(3)));
        assertThat(grades.findByRequest(bl, bam2)).contains(coPhanQuyet);
        assertThat(grades.bySubmission(bl)).extracting(GradingResult::result)
            .containsExactly(GradeStatus.SAI, GradeStatus.KHONG_CHAM_DUOC, GradeStatus.KHONG_CHAM_DUOC, GradeStatus.DAT);
    }

    @Test
    void tabGiuAnhCuNopBaiKhongXoaCoNghiDoanMo() {
        // Codex #136 (P2): tab A bật cờ nghi đoán mò; tab B giữ ảnh cũ (chưa nghi) nộp bài: cờ và lý do đầu phải còn.
        Submission anhCu = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.update(anhCu.suspectGuess("Đổi một ô dấu 6 lần trong 30 giây."));
        submissions.update(nop(anhCu, GradeStatus.SAI, LUC.plusSeconds(60)));
        Submission daNop = submissions.findById(anhCu.id()).orElseThrow();
        assertThat(daNop.status()).isEqualTo(SubmissionStatus.DA_NOP);
        assertThat(daNop.guessSuspected()).isTrue();
        assertThat(daNop.guessReason()).isEqualTo("Đổi một ô dấu 6 lần trong 30 giây.");
    }

    @Test
    void csdlChanTatCoNghiDoanMo() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.update(dangLam.suspectGuess("Đổi ô nhiều lần."));
        assertThatThrownBy(() -> jdbc.sql("update submissions set guess_suspected = false, guess_reason = null where id = ?")
                .params(dangLam.id()).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("nghi đoán mò");
    }

    @Test
    void baiLamCuaPhienBanCuThoiDuocGhiChamVaNop() {
        // Codex #136 (P1): đổi đề khi học sinh đang làm; tab cũ không được ghi bước, kết quả chấm hay nộp cho đề cũ.
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.saveStep(dangLam.id(), new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "3x^2-6x", null)), null));
        GradingResult canCu = grades.record(ketQua(dangLam.id(), "2".repeat(64), GradeStatus.DAT));
        jdbc.sql("update problems set content_hash = ? where id = ?").params("b".repeat(64), bai).update();
        StepWork buoc = new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "3x^2", null)), null);
        assertThatThrownBy(() -> submissions.saveStep(dangLam.id(), buoc)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("phiên bản nội dung cũ");
        assertThatThrownBy(() -> submissions.addEvents(dangLam.id(), List.of(suKien("+", LUC)))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> grades.record(ketQua(dangLam.id(), "a".repeat(64), GradeStatus.DAT)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> submissions.update(dangLam.submit(canCu, PHAN_LOAI, LUC).baiLam())).isInstanceOf(IllegalStateException.class);
        assertThat(submissions.findById(dangLam.id()).orElseThrow().status()).isEqualTo(SubmissionStatus.DANG_LAM);
        assertThatThrownBy(() -> jdbc.sql("""
                insert into grading_results (id, submission_id, step_code, request_hash, result, graded_at)
                values (?, ?, 'B.DH.DAOHAM', ?, 'DAT', now())""").params(UUID.randomUUID(), dangLam.id(), "a".repeat(64)).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("phiên bản nội dung cũ");
    }

    @Test
    void daCoPhanQuyetThiLanKhongChamDuocSauTraPhanQuyet() {
        // Codex #136 (P2): lần chấm lại lỗi dịch vụ toán sau khi đã có phán quyết không thêm dòng KHONG_CHAM_DUOC.
        UUID bl = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC)).id();
        String bam = "9".repeat(64);
        GradingResult phanQuyet = grades.record(ketQua(bl, bam, GradeStatus.SAI));
        assertThat(grades.record(GradingResult.notGraded(bl, "B.DH.DAOHAM", bam, "Máy chấm đang bận.", LUC.plusSeconds(5))))
            .isEqualTo(phanQuyet);
        assertThat(grades.bySubmission(bl)).containsExactly(phanQuyet);
        assertThatThrownBy(() -> jdbc.sql("""
                insert into grading_results (id, submission_id, step_code, request_hash, result, message, graded_at)
                values (?, ?, 'B.DH.DAOHAM', ?, 'KHONG_CHAM_DUOC', 'bận', now())""").params(UUID.randomUUID(), bl, bam).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("đã có phán quyết");
    }

    @Test
    void csdlChanKhongChamDuocMangPhanQuyet() {
        // Codex #136 (P2): kết quả từng bước của một lần không chấm được là phán quyết lẻn vào lịch sử.
        UUID bl = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC)).id();
        assertThatThrownBy(() -> jdbc.sql("""
                insert into grading_results (id, submission_id, step_code, request_hash, result, per_step, message, graded_at)
                values (?, ?, 'B.DH.DAOHAM', ?, 'KHONG_CHAM_DUOC', '{"B.DH.TXD": "DAT"}', 'bận', now())""")
                .params(UUID.randomUUID(), bl, "8".repeat(64)).update())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ketQuaChamChiThem() {
        UUID bl = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC)).id();
        grades.record(ketQua(bl, "e".repeat(64), GradeStatus.SAI));
        assertThatThrownBy(() -> jdbc.sql("update grading_results set result = 'DAT' where submission_id = ?").params(bl).update())
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chỉ thêm");
    }

    @Test
    void suKienNhapVaNghiDoanMo() {
        Submission dangLam = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC));
        submissions.addEvents(dangLam.id(), List.of(suKien("+", LUC.plusSeconds(2)), suKien("-", LUC.plusSeconds(1)),
            new InputEvent("B.DH.DAOHAM", null, null, null, "3x^2", LUC)));
        assertThat(submissions.events(dangLam.id())).extracting(InputEvent::newValue).containsExactly("3x^2", "-", "+");

        submissions.update(dangLam.suspectGuess("Đổi một ô dấu 6 lần trong 30 giây."));
        Submission nghi = submissions.findById(dangLam.id()).orElseThrow();
        assertThat(nghi.guessSuspected()).isTrue();
        assertThat(nghi.guessReason()).isEqualTo("Đổi một ô dấu 6 lần trong 30 giây.");
        assertThat(nghi.status()).isEqualTo(SubmissionStatus.DANG_LAM);
    }

    @Test
    void giaoBaiChoHocSinhCuaLopVaGiaoLaiGiuDong() {
        Assignment a = Assignment.of(lop, bai, an, "Bộ 1", LUC.plusSeconds(86_400), giaoVien, LUC);
        assignments.saveAll(List.of(a));
        Assignment lai = Assignment.of(lop, bai, an, "Bộ 2", null, giaoVien, LUC.plusSeconds(10));
        assignments.saveAll(List.of(lai));
        assertThat(assignments.forStudent(lop, an)).singleElement().satisfies(g -> {
            assertThat(g.id()).isEqualTo(a.id());
            assertThat(g.setName()).isEqualTo("Bộ 2");
            assertThat(g.dueAt()).isNull();
        });
        assignments.saveAll(List.of(new Assignment(a.id(), lop, bai, an, AssignmentStatus.DA_HUY, null, null, giaoVien, LUC)));
        assertThat(assignments.forStudent(lop, an)).isEmpty();
    }

    @Test
    void khongGiaoBaiChuaPhatHanh() {
        UUID chuaPhatHanh = DuLieuPractice.bai(jdbc, "PR-03");
        assertThatThrownBy(() -> assignments.saveAll(List.of(Assignment.of(lop, chuaPhatHanh, an, null, null, giaoVien, LUC))))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chưa phát hành");
    }

    @Test
    void khongGiaoBaiChoGiaoVien() {
        assertThatThrownBy(() -> assignments.saveAll(List.of(Assignment.of(lop, bai, giaoVien, null, null, giaoVien, LUC))))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("không là học sinh");
    }

    /** Ghi một lần chấm có phán quyết {@code kq} cho bài làm rồi trả bài làm đã nộp với căn cứ là lần đó. */
    private Submission nop(Submission dangLam, GradeStatus kq, Instant luc) {
        GradingResult canCu = grades.record(ketQua(dangLam.id(), "0".repeat(64), kq, luc));
        return dangLam.submit(canCu, PHAN_LOAI, luc).baiLam();
    }

    private int ghimCanCu(UUID baiLam, String ketQua, UUID canCu) {
        return jdbc.sql("""
                update submissions set status = 'DA_NOP', result = ?, result_grading_id = ?, submitted_at = now(),
                    skill_code = 'T12.DH.02', level4 = 'THONG_HIEU' where id = ?""")
            .params(ketQua, canCu, baiLam).update();
    }

    private static InputEvent suKien(String giaTri, Instant luc) {
        return new InputEvent("B.DH.XETDAU", "DAU_YPHAY", 0, null, giaTri, luc);
    }

    private static GradingResult ketQua(UUID bl, String bam, GradeStatus kq) {
        return ketQua(bl, bam, kq, LUC);
    }

    private static GradingResult ketQua(UUID bl, String bam, GradeStatus kq, Instant luc) {
        return new GradingResult(UUID.randomUUID(), bl, "B.DH.DAOHAM", bam, kq, null, null, null, null, Map.of(), "thông báo", null, null,
            false, null, null, luc);
    }
}
