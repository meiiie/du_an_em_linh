package vn.hoctapcanman.core.practice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Bất biến của model practice, không CSDL. */
class PracticeModelTest {

    private static final Instant LUC = Instant.parse("2026-10-05T08:00:00Z");
    private static final UUID LOP = UUID.randomUUID();
    private static final UUID HS = UUID.randomUUID();
    private static final UUID BAI = UUID.randomUUID();

    @Test
    void baiLamChiNopMotLanVaNghiDoanMoChiBat() {
        Submission dangLam = Submission.open(LOP, HS, BAI, 3, LUC);
        assertThat(dangLam.isOpen()).isTrue();
        Submission nghi = dangLam.suspectGuess("Đổi ô 6 lần.");
        assertThat(nghi.suspectGuess("Lý do khác").guessReason()).isEqualTo("Đổi ô 6 lần.");
        Submission daNop = nghi.submit(GradeStatus.SAI, LUC.plusSeconds(5));
        assertThat(daNop.status()).isEqualTo(SubmissionStatus.DA_NOP);
        assertThat(daNop.guessSuspected()).isTrue();
        assertThatThrownBy(() -> daNop.submit(GradeStatus.DAT, LUC)).isInstanceOf(IllegalStateException.class);
        // Dịch vụ toán lỗi lúc nộp: không đóng bài làm (Codex #136).
        assertThatThrownBy(() -> dangLam.submit(GradeStatus.KHONG_CHAM_DUOC, LUC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> daNop.suspectGuess("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void baiLamKhongHopLe() {
        assertThatThrownBy(() -> Submission.open(LOP, HS, BAI, 0, LUC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Submission(UUID.randomUUID(), LOP, HS, BAI, 1, SubmissionStatus.DANG_LAM, false, null,
            GradeStatus.DAT, LUC, null)).as("đang làm mà có kết quả").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Submission(UUID.randomUUID(), LOP, HS, BAI, 1, SubmissionStatus.DA_NOP, false, null, null, LUC,
            LUC)).as("đã nộp mà không có kết quả").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Submission(UUID.randomUUID(), LOP, HS, BAI, 1, SubmissionStatus.DANG_LAM, false, "lý do", null,
            LUC, null)).as("lý do mà không nghi").isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buocVaBangKhongHopLe() {
        assertThatThrownBy(() -> new StepWork("B.DH.DAOHAM", List.of(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StepWork("B.DH.NGHIEM", List.of(new StepLine(0, "x=0", "NGHIEM"), new StepLine(0, "x=2", "NGHIEM")),
            null)).as("trùng số dòng").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StepWork("B.DH.NGHIEM", List.of(new StepLine(1, "x=0", null), new StepLine(0, "x=2", null)), null))
            .as("sai thứ tự dòng").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StepWork("b.dh.daoham", List.of(new StepLine(0, "x", null)), null))
            .as("mã bước").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StepLine(0, "x", "nghiem")).as("nhãn dòng").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SignTable("XET_DAU", List.of(new TableCell("X", 0, "0"), new TableCell("X", 0, "2"))))
            .as("trùng ô").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TableCell("X", -1, "0")).as("k âm").isInstanceOf(IllegalArgumentException.class);
        // v0 gửi dòng trống khi học sinh chưa viết gì: hợp lệ.
        assertThat(new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "", null)), null).lines()).hasSize(1);
    }

    @Test
    void khongChamDuocKhongMangPhanQuyet() {
        String bam = "a".repeat(64);
        assertThat(GradingResult.notGraded(UUID.randomUUID(), "B.DH.DAOHAM", bam, "Máy chấm đang bận.", LUC).result())
            .isEqualTo(GradeStatus.KHONG_CHAM_DUOC);
        assertThatThrownBy(() -> new GradingResult(UUID.randomUUID(), UUID.randomUUID(), "B.DH.DAOHAM", bam, GradeStatus.KHONG_CHAM_DUOC,
            null, null, "ERR.DH.02", null, Map.of(), null, null, null, false, null, null, LUC)).isInstanceOf(IllegalArgumentException.class);
        // Codex #136: kết quả từng bước, loại kết quả, vấn đề cũng là phán quyết.
        assertThatThrownBy(() -> new GradingResult(UUID.randomUUID(), UUID.randomUUID(), "B.DH.DAOHAM", bam, GradeStatus.KHONG_CHAM_DUOC,
            null, null, null, null, Map.of("B.DH.TXD", "DAT"), null, null, null, false, null, null, LUC))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GradingResult(UUID.randomUUID(), UUID.randomUUID(), "B.DH.DAOHAM", bam, GradeStatus.KHONG_CHAM_DUOC,
            "SAI_BUOC", null, null, null, Map.of(), null, "[]", null, false, null, null, LUC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GradingResult(UUID.randomUUID(), UUID.randomUUID(), "B.DH.DAOHAM", "A".repeat(64), GradeStatus.DAT,
            null, null, null, null, Map.of(), null, null, null, false, null, null, LUC)).as("băm").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GradingResult(UUID.randomUUID(), UUID.randomUUID(), "B.DH.DAOHAM", bam, GradeStatus.SAI, null, null,
            null, 1.5, Map.of(), null, null, null, false, null, null, LUC)).as("độ tin cậy").isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maKetQuaCuaDichVuToan() {
        assertThat(GradeStatus.tuDichVuToan("DAT")).contains(GradeStatus.DAT);
        assertThat(GradeStatus.tuDichVuToan("SAI")).contains(GradeStatus.SAI);
        assertThat(GradeStatus.tuDichVuToan("KHONG_KIEM_DUOC")).contains(GradeStatus.KHONG_KIEM_DUOC);
        // Mã của core và mã lạ không phải phản hồi hợp lệ của dịch vụ toán.
        assertThat(GradeStatus.tuDichVuToan("KHONG_CHAM_DUOC")).isEmpty();
        assertThat(GradeStatus.tuDichVuToan("dat")).isEmpty();
        assertThat(GradeStatus.tuDichVuToan(null)).isEmpty();
        assertThat(GradeStatus.tuDichVuToan(1)).isEmpty();
    }
}
