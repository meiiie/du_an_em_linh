package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.KHONG_KIEM_DUOC;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.SAI;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemReleaseTest {

    private static ProblemRelease ap(ProblemRelease ph, VerificationRun luot) {
        return ph.apply(luot, true, Mau.BAM, Mau.BANG, Mau.LUC);
    }

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
        ProblemRelease ph = ap(nhap, dat);
        assertThat(ph.status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        assertThat(ph.runId()).isEqualTo(dat.id());
        assertThat(ph.visibleToStudents()).isTrue();
        assertThat(ap(nhap, Mau.luot(DAT, SAI, DAT)).status()).isEqualTo(ReleaseStatus.BI_CHAN);
        assertThat(ap(nhap, Mau.luot(DAT, DAT, KHONG_KIEM_DUOC)).status()).isEqualTo(ReleaseStatus.CHO_GIAO_VIEN_DUYET);
    }

    @Test
    void kiemLaiBaiDaPhatHanhMaKetQuaChanThiChan() {
        ProblemRelease daPhatHanh = ap(ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC), Mau.luot(DAT, DAT, DAT));
        assertThat(ap(daPhatHanh, Mau.luot(DAT, DAT, SAI)).visibleToStudents()).isFalse();
    }

    @Test
    void duyetXongThiPhatHanh() {
        VerificationRun cho = Mau.luot(DAT, DAT, KHONG_KIEM_DUOC);
        ProblemRelease choDuyet = ap(ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC), cho);
        VerificationRun daDuyet = cho.approve(Mau.GV, "Đúng.", true, Mau.BAM, Mau.BANG, Mau.LUC).run();
        assertThat(ap(choDuyet, daDuyet).status()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
    }

    @Test
    void khongApLuotCuaLopKhacBaiKhacHayLuotGiaSu() {
        ProblemRelease nhap = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC);
        VerificationRun lopKhac = VerificationRun.forProblem(UUID.randomUUID(), Mau.BAI, Mau.BAM, 1, Mau.BANG, Mau.tang(DAT, DAT, DAT), List.of(Mau.DOAN), Mau.LUC);
        VerificationRun baiKhac = VerificationRun.forProblem(Mau.LOP, UUID.randomUUID(), Mau.BAM, 1, Mau.BANG, Mau.tang(DAT, DAT, DAT), List.of(Mau.DOAN), Mau.LUC);
        VerificationRun giaSu = new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, Mau.BAI, Mau.BAM, null, Mau.BANG,
            SAI, null, false, Mau.LUC, Mau.tang(SAI, SAI, SAI), List.of());
        assertThatThrownBy(() -> ap(nhap, lopKhac)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ap(nhap, baiKhac)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ap(nhap, giaSu)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doiBangThiPhatHanhGiuNguyenVaCanKiemLai() {
        // ADR 005: đổi bảng công thức không gỡ bài đã phát hành; lượt của nó thành stale, phát hành «cần kiểm lại».
        VerificationRun dat = Mau.luot(DAT, DAT, DAT);
        ProblemRelease phatHanh = ap(ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC), dat);
        assertThat(phatHanh.needsRecheck(dat)).isFalse();
        assertThat(phatHanh.needsRecheck(dat.markStale())).isTrue();
        assertThat(phatHanh.visibleToStudents()).isTrue();
        assertThatThrownBy(() -> phatHanh.needsRecheck(Mau.luot(DAT, DAT, DAT))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void khongApLuotDaCuCuaNoiDungCuBangCuHayKhongPhaiMoiNhat() {
        ProblemRelease nhap = ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC);
        VerificationRun dat = Mau.luot(DAT, DAT, DAT);
        assertThatThrownBy(() -> ap(nhap, dat.markStale())).isInstanceOf(IllegalStateException.class).hasMessageContaining("STALE");
        // H3: lượt DAT của nội dung cũ (HASH_A) áp lại khi bài đã đổi nội dung.
        assertThatThrownBy(() -> nhap.apply(dat, true, Mau.BAM_KHAC, Mau.BANG, Mau.LUC))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("CONTENT_CHANGED");
        // H4: lượt kiểm với bảng F1 trong khi bảng hiện tại của lớp là F2 (lượt ghi sau đợt đánh dấu stale).
        assertThatThrownBy(() -> nhap.apply(dat, true, Mau.BAM, UUID.randomUUID(), Mau.LUC))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("SHEET_CHANGED");
        assertThatThrownBy(() -> nhap.apply(dat, false, Mau.BAM, Mau.BANG, Mau.LUC))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOT_LATEST");
    }

    @Test
    void doiNoiDungThiVeNhapNhapKhongGanLuotNgoaiNhapPhaiGanLuot() {
        ProblemRelease daPhatHanh = ap(ProblemRelease.draft(Mau.LOP, Mau.BAI, Mau.LUC), Mau.luot(DAT, DAT, DAT));
        ProblemRelease nhap = daPhatHanh.backToDraft(Mau.LUC);
        assertThat(nhap.status()).isEqualTo(ReleaseStatus.NHAP);
        assertThat(nhap.runId()).isNull();
        assertThatThrownBy(() -> new ProblemRelease(Mau.LOP, Mau.BAI, ReleaseStatus.DA_PHAT_HANH, null, Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProblemRelease(Mau.LOP, Mau.BAI, ReleaseStatus.NHAP, UUID.randomUUID(), Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
