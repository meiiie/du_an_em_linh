package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.KHONG_KIEM_DUOC;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.SAI;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemReleaseTest {

    @Test
    void baiMoiNapLaNhapHocSinhChuaThay() {
        ProblemRelease ph = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC);
        assertThat(ph.status()).isEqualTo(ReleaseStatus.NHAP);
        assertThat(ph.runId()).isNull();
        assertThat(ph.visibleToStudents()).isFalse();
    }

    @Test
    void apLuotKiemTheoTrangThaiPhatHanhCuaLuot() {
        ProblemRelease nhap = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC);
        VerificationRun dat = Mau.luot(DAT, DAT, DAT);
        ProblemRelease ph = nhap.apply(dat, Mau.LUC);
        assertThat(ph.status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        assertThat(ph.runId()).isEqualTo(dat.id());
        assertThat(ph.visibleToStudents()).isTrue();
        assertThat(nhap.apply(Mau.luot(DAT, SAI, DAT), Mau.LUC).status()).isEqualTo(ReleaseStatus.BI_CHAN);
        assertThat(nhap.apply(Mau.luot(DAT, DAT, KHONG_KIEM_DUOC), Mau.LUC).status()).isEqualTo(ReleaseStatus.CHO_GIAO_VIEN_DUYET);
    }

    @Test
    void kiemLaiBaiDaPhatHanhMaKetQuaChanThiChan() {
        ProblemRelease daPhatHanh = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC).apply(Mau.luot(DAT, DAT, DAT), Mau.LUC);
        assertThat(daPhatHanh.apply(Mau.luot(DAT, DAT, SAI), Mau.LUC).visibleToStudents()).isFalse();
    }

    @Test
    void duyetXongThiPhatHanh() {
        VerificationRun cho = Mau.luot(DAT, DAT, KHONG_KIEM_DUOC);
        ProblemRelease choDuyet = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC).apply(cho, Mau.LUC);
        VerificationRun daDuyet = cho.approve(Mau.GV, "Đúng.", true, Mau.BAM, Mau.BANG, Mau.LUC).run();
        assertThat(choDuyet.apply(daDuyet, Mau.LUC).status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
    }

    @Test
    void khongApLuotCuaLopKhacBaiKhacHayLuotDaCu() {
        ProblemRelease nhap = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC);
        VerificationRun lopKhac = VerificationRun.forProblem(UUID.randomUUID(), Mau.BAI, Mau.BAM, Mau.BANG, Mau.tang(DAT, DAT, DAT), Mau.LUC);
        VerificationRun baiKhac = VerificationRun.forProblem(Mau.LOP, UUID.randomUUID(), Mau.BAM, Mau.BANG, Mau.tang(DAT, DAT, DAT), Mau.LUC);
        assertThatThrownBy(() -> nhap.apply(lopKhac, Mau.LUC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> nhap.apply(baiKhac, Mau.LUC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> nhap.apply(Mau.luot(DAT, DAT, DAT).markStale(), Mau.LUC)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void doiNoiDungThiVeNhapVaNgoaiNhapPhaiGanLuotKiem() {
        ProblemRelease daPhatHanh = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC).apply(Mau.luot(DAT, DAT, DAT), Mau.LUC);
        ProblemRelease nhap = daPhatHanh.backToDraft(Mau.LUC);
        assertThat(nhap.status()).isEqualTo(ReleaseStatus.NHAP);
        assertThat(nhap.runId()).isNull();
        assertThatThrownBy(() -> new ProblemRelease(Mau.LOP, Mau.BAI, ReleaseStatus.DA_PHAT_HANH, null, Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
