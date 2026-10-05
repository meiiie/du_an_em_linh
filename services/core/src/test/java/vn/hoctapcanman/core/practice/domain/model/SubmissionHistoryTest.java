package vn.hoctapcanman.core.practice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Luật trên lịch sử bài làm của một học sinh cho một bài: lúc mở lời giải (FR-006) và lần nộp để phát lại. Không CSDL. */
class SubmissionHistoryTest {

    private static final Instant LUC = Instant.parse("2026-10-05T08:00:00Z");
    private static final UUID HS = UUID.randomUUID();
    private static final UUID BAI = UUID.randomUUID();
    private static final UUID LOP_A = UUID.randomUUID();
    private static final UUID LOP_B = UUID.randomUUID();

    @Test
    void loiGiaiMoKhiDaNopVaKhongConLamLaiCungPhienBanOLopNao() {
        Submission.DaNop daNop = nop(LOP_A, 2, LUC);
        assertThat(lichSu(daNop.baiLam()).choMoLoiGiai(daNop)).isTrue();
        // Làm lại cùng phiên bản, kể cả ở lớp khác: đang làm, lời giải đóng.
        assertThat(lichSu(daNop.baiLam(), Submission.open(LOP_A, HS, BAI, 2, LUC.plusSeconds(60))).choMoLoiGiai(daNop)).isFalse();
        assertThat(lichSu(daNop.baiLam(), Submission.open(LOP_B, HS, BAI, 2, LUC.plusSeconds(60))).choMoLoiGiai(daNop)).isFalse();
        // Bài làm dở của phiên bản khác không chặn.
        assertThat(lichSu(Submission.open(LOP_A, HS, BAI, 1, LUC.minusSeconds(60)), daNop.baiLam()).choMoLoiGiai(daNop)).isTrue();
        // Chứng nhận của bài làm mà lịch sử (CSDL) không thấy đã nộp: đóng.
        assertThat(lichSu().choMoLoiGiai(daNop)).isFalse();
        Submission cungIdDangLam = new Submission(daNop.baiLam().id(), LOP_A, HS, BAI, 2, SubmissionStatus.DANG_LAM, false, null, null,
            null, LUC, null);
        assertThat(lichSu(cungIdDangLam).choMoLoiGiai(daNop)).isFalse();
    }

    @Test
    void phatLaiLanNopMoiNhatCuaDungLopVaPhienBan() {
        Submission.DaNop dau = nop(LOP_A, 2, LUC);
        Submission.DaNop sau = nop(LOP_A, 2, LUC.plusSeconds(60));
        Submission.DaNop lopKhac = nop(LOP_B, 2, LUC.plusSeconds(120));
        Submission.DaNop deCu = nop(LOP_A, 1, LUC.plusSeconds(180));
        SubmissionHistory h = lichSu(sau.baiLam(), deCu.baiLam(), dau.baiLam(), lopKhac.baiLam(),
            Submission.open(LOP_A, HS, BAI, 2, LUC.plusSeconds(240)));
        assertThat(h.daNopMoiNhat(LOP_A, 2)).contains(sau);
        assertThat(h.daNopMoiNhat(LOP_A, 1)).contains(deCu);
        assertThat(h.daNopMoiNhat(LOP_B, 2)).contains(lopKhac);
        assertThat(h.daNopMoiNhat(LOP_A, 3)).isEmpty();
    }

    @Test
    void dangLamDeKhacKhiBaiLamDoOLopLaPhienBanKhac() {
        Submission doCu = Submission.open(LOP_A, HS, BAI, 1, LUC);
        assertThat(lichSu(doCu).dangLamDeKhac(LOP_A, 2)).isTrue();
        assertThat(lichSu(doCu).dangLamDeKhac(LOP_A, 1)).isFalse();
        assertThat(lichSu(doCu).dangLamDeKhac(LOP_B, 2)).isFalse();
        assertThat(lichSu(nop(LOP_A, 1, LUC).baiLam()).dangLamDeKhac(LOP_A, 2)).isFalse();
    }

    @Test
    void chiGomBaiLamCuaMotHocSinhChoMotBai() {
        assertThatThrownBy(() -> new SubmissionHistory(HS, BAI, List.of(Submission.open(LOP_A, UUID.randomUUID(), BAI, 1, LUC))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubmissionHistory(HS, BAI, List.of(Submission.open(LOP_A, HS, UUID.randomUUID(), 1, LUC))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static SubmissionHistory lichSu(Submission... baiLam) {
        return new SubmissionHistory(HS, BAI, List.of(baiLam));
    }

    private static Submission.DaNop nop(UUID lop, int phienBan, Instant luc) {
        Submission dangLam = Submission.open(lop, HS, BAI, phienBan, luc);
        GradingResult canCu = new GradingResult(UUID.randomUUID(), dangLam.id(), "B.DH.KETLUAN", "c".repeat(64), GradeStatus.DAT, null,
            null, null, null, Map.of(), null, null, null, false, null, null, luc);
        return dangLam.submit(canCu, luc.plusSeconds(1));
    }
}
