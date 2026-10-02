package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.GV_DUYET;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.KHONG_KIEM_DUOC;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.SAI;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificationRunTest {

    @Test
    void trangThaiTongVaPhatHanhNhuCongPhatHanhCuaV0() {
        assertThat(Mau.luot(DAT, DAT, DAT)).satisfies(r -> {
            assertThat(r.overallStatus()).isEqualTo(DAT);
            assertThat(r.publishStatus()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        });
        // SAI thắng KHONG_KIEM_DUOC: bài có tầng sai không bao giờ chờ duyệt.
        assertThat(Mau.luot(DAT, KHONG_KIEM_DUOC, SAI).publishStatus()).isEqualTo(ReleaseStatus.BI_CHAN);
        assertThat(Mau.luot(SAI, DAT, DAT).overallStatus()).isEqualTo(SAI);
        assertThat(Mau.luot(DAT, DAT, KHONG_KIEM_DUOC)).satisfies(r -> {
            assertThat(r.overallStatus()).isEqualTo(KHONG_KIEM_DUOC);
            assertThat(r.publishStatus()).isEqualTo(ReleaseStatus.CHO_GIAO_VIEN_DUYET);
        });
    }

    @Test
    void thieuTangHayKhongCoTangThiKhongTuPhatHanh() {
        assertThat(VerificationRun.overallOf(List.of(TierResult.of(1, DAT), TierResult.of(2, DAT)))).isEqualTo(KHONG_KIEM_DUOC);
        assertThat(VerificationRun.overallOf(List.of())).isEqualTo(KHONG_KIEM_DUOC);
        assertThat(VerificationRun.overallOf(List.of(TierResult.of(2, SAI)))).isEqualTo(SAI);
    }

    @Test
    void motTangGhiHaiLanHayTrangThaiPhatHanhLechThiTuChoi() {
        assertThatThrownBy(() -> VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, Mau.BANG,
            List.of(TierResult.of(1, DAT), TierResult.of(1, DAT), TierResult.of(2, DAT), TierResult.of(3, DAT)), Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.PROBLEM, Mau.BAI, Mau.BAM, Mau.BANG,
            SAI, ReleaseStatus.DA_PHAT_HANH, false, Mau.LUC, Mau.tang(SAI, DAT, DAT))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.PROBLEM, Mau.BAI, Mau.BAM, Mau.BANG,
            DAT, null, false, Mau.LUC, Mau.tang(DAT, DAT, DAT))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TierResult.of(1, GV_DUYET)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TierResult.of(4, DAT)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void congThucTrongLoiGiaSuKhongPhatHanhKhongDuyetRieng() {
        VerificationRun luot = new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(), Mau.BAM,
            null, SAI, null, false, Mau.LUC, List.of(TierResult.of(1, SAI)));
        assertThat(luot.approvalRefusal(true, Mau.BAM, null)).contains(ApprovalRefusal.NOT_A_PROBLEM);
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(),
            Mau.BAM, null, KHONG_KIEM_DUOC, ReleaseStatus.CHO_GIAO_VIEN_DUYET, false, Mau.LUC, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(),
            Mau.BAM, null, GV_DUYET, null, false, Mau.LUC, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chiDuyetLuotKhongKiemDuocMoiNhatChuaCuDungNoiDungVaDungBang() {
        VerificationRun cho = Mau.luot(DAT, DAT, KHONG_KIEM_DUOC);
        assertThat(cho.approvalRefusal(true, Mau.BAM, Mau.BANG)).isEmpty();
        assertThat(Mau.luot(SAI, DAT, KHONG_KIEM_DUOC).approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.BLOCKED);
        assertThat(Mau.luot(DAT, DAT, DAT).approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.NOTHING_TO_APPROVE);
        assertThat(cho.markStale().approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.STALE);
        assertThat(cho.approvalRefusal(false, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.NOT_LATEST);
        assertThat(cho.approvalRefusal(true, Mau.BAM_KHAC, Mau.BANG)).contains(ApprovalRefusal.CONTENT_CHANGED);
        assertThat(cho.approvalRefusal(true, Mau.BAM, UUID.randomUUID())).contains(ApprovalRefusal.SHEET_CHANGED);
        assertThat(cho.approvalRefusal(true, Mau.BAM, null)).contains(ApprovalRefusal.SHEET_CHANGED);
        // Lớp chưa có bảng khóa: lượt kiểm không có bảng, bảng hiện tại cũng trống → duyệt được.
        VerificationRun khongBang = VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, null, Mau.tang(DAT, DAT, KHONG_KIEM_DUOC), Mau.LUC);
        assertThat(khongBang.approvalRefusal(true, Mau.BAM, null)).isEmpty();
    }

    @Test
    void duyetThanhGvDuyetVaPhatHanhCoBanGhiDuyet() {
        VerificationRun cho = Mau.luot(DAT, DAT, KHONG_KIEM_DUOC);
        VerificationRun.Approval kq = cho.approve(Mau.GV, "  Đã xem bảng xét dấu, đúng.  ", true, Mau.BAM, Mau.BANG, Mau.LUC);
        assertThat(kq.run().id()).isEqualTo(cho.id());
        assertThat(kq.run().overallStatus()).isEqualTo(GV_DUYET);
        assertThat(kq.run().publishStatus()).isEqualTo(ReleaseStatus.DA_PHAT_HANH);
        assertThat(kq.run().tiers()).isEqualTo(cho.tiers());
        assertThat(kq.review().runId()).isEqualTo(cho.id());
        assertThat(kq.review().contentHash()).isEqualTo(Mau.BAM);
        assertThat(kq.review().note()).isEqualTo("Đã xem bảng xét dấu, đúng.");
        assertThat(kq.run().approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.ALREADY_APPROVED);

        assertThatThrownBy(() -> Mau.luot(SAI, DAT, DAT).approve(Mau.GV, "Thấy đúng.", true, Mau.BAM, Mau.BANG, Mau.LUC))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BLOCKED");
    }

    @Test
    void danhDauCuGiuNguyenKetQua() {
        VerificationRun luot = Mau.luot(DAT, DAT, DAT);
        VerificationRun cu = luot.markStale();
        assertThat(cu.stale()).isTrue();
        assertThat(cu.overallStatus()).isEqualTo(DAT);
        assertThat(cu.markStale()).isSameAs(cu);
    }
}
