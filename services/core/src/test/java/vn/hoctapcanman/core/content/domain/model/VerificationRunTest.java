package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.DAT;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.GV_DUYET;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.KHONG_KIEM_DUOC;
import static vn.hoctapcanman.core.content.domain.model.CheckStatus.SAI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        assertThat(Mau.luot(DAT, DAT, KHONG_KIEM_DUOC).publishStatus()).isEqualTo(ReleaseStatus.CHO_GIAO_VIEN_DUYET);
    }

    /**
     * Cả 64 tổ hợp (mỗi tầng DAT, SAI, KHONG_KIEM_DUOC hay thiếu) so với tệp vàng xuất từ {@code cong_phat_hanh} của v0
     * ({@code specs/001-lat-cat-doc/doi-chieu/cong-phat-hanh-v0.py}). Đủ ba tầng: trùng v0. Thiếu tầng: v0 phát hành khi
     * các tầng có mặt đều DAT, còn core giữ ở chờ duyệt (và không cho duyệt, {@link ApprovalRefusal#INCOMPLETE}).
     */
    @Test
    void caSauMuoiTuToHopSoVoiTepVangCuaV0() throws IOException {
        List<String[]> dong = new ArrayList<>();
        try (BufferedReader doc = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/content/cong-phat-hanh-v0.csv")), StandardCharsets.UTF_8))) {
            doc.lines().filter(l -> !l.startsWith("#") && !l.startsWith("tang1")).map(l -> l.split(",")).forEach(dong::add);
        }
        assertThat(dong).hasSize(64);
        int duTang = 0;
        int thieuTang = 0;
        for (String[] d : dong) {
            List<TierResult> tang = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                if (!d[i].equals("THIEU")) {
                    tang.add(TierResult.of(i + 1, CheckStatus.valueOf(d[i])));
                }
            }
            VerificationRun luot = VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 1, Mau.BANG, tang, List.of(Mau.DOAN), Mau.LUC);
            String toHop = String.join(",", d[0], d[1], d[2]);
            if (tang.size() == 3) {
                duTang++;
                assertThat(luot.publishStatus().name()).as(toHop).isEqualTo(d[3]);
                assertThat(luot.overallStatus().name()).as(toHop).isEqualTo(d[4]);
            } else {
                thieuTang++;
                boolean coSai = tang.stream().anyMatch(t -> t.status() == SAI);
                assertThat(luot.overallStatus()).as(toHop).isEqualTo(coSai ? SAI : KHONG_KIEM_DUOC);
                assertThat(luot.publishStatus()).as(toHop).isNotEqualTo(ReleaseStatus.DA_PHAT_HANH);
                if (!coSai) {
                    assertThat(luot.approvalRefusal(true, Mau.BAM, Mau.BANG)).as(toHop).contains(ApprovalRefusal.INCOMPLETE);
                }
            }
        }
        assertThat(duTang).isEqualTo(27);
        assertThat(thieuTang).isEqualTo(37);
    }

    @Test
    void luotNapLaiLechCacTangThiTuChoi() {
        // H1a: tổng DAT, phát hành, nhưng ba tầng SAI.
        assertThatThrownBy(() -> Mau.napLai(DAT, ReleaseStatus.DA_PHAT_HANH, Mau.tang(SAI, SAI, SAI)))
            .isInstanceOf(IllegalArgumentException.class);
        // H1b: tổng KHONG_KIEM_DUOC, chờ duyệt, nhưng tầng 1 SAI.
        assertThatThrownBy(() -> Mau.napLai(KHONG_KIEM_DUOC, ReleaseStatus.CHO_GIAO_VIEN_DUYET, Mau.tang(SAI, DAT, KHONG_KIEM_DUOC)))
            .isInstanceOf(IllegalArgumentException.class);
        // H1c: GV_DUYET với ba tầng SAI; và GV_DUYET khi thiếu tầng.
        assertThatThrownBy(() -> Mau.napLai(GV_DUYET, ReleaseStatus.DA_PHAT_HANH, Mau.tang(SAI, SAI, SAI)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Mau.napLai(GV_DUYET, ReleaseStatus.DA_PHAT_HANH, List.of(TierResult.of(1, DAT))))
            .isInstanceOf(IllegalArgumentException.class);
        // Hợp lệ: GV_DUYET khi đủ ba tầng và tổng từ các tầng là KHONG_KIEM_DUOC.
        assertThat(Mau.napLai(GV_DUYET, ReleaseStatus.DA_PHAT_HANH, Mau.tang(DAT, DAT, KHONG_KIEM_DUOC)).overallStatus()).isEqualTo(GV_DUYET);
        // Phát hành lệch trạng thái tổng.
        assertThatThrownBy(() -> Mau.napLai(SAI, ReleaseStatus.DA_PHAT_HANH, Mau.tang(SAI, DAT, DAT)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void motTangGhiHaiLanHayTangKhongHopLeThiTuChoi() {
        assertThatThrownBy(() -> VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 1, Mau.BANG,
            List.of(TierResult.of(1, DAT), TierResult.of(1, DAT), TierResult.of(2, DAT), TierResult.of(3, DAT)), List.of(Mau.DOAN), Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TierResult.of(1, GV_DUYET)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TierResult.of(4, DAT)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void congThucTrongLoiGiaSuKhongPhatHanhKhongDuyetRieng() {
        VerificationRun luot = new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(), Mau.BAM, null,
            null, SAI, null, false, Mau.LUC, List.of(TierResult.of(1, SAI)), List.of());
        assertThat(luot.approvalRefusal(true, Mau.BAM, null)).contains(ApprovalRefusal.NOT_A_PROBLEM);
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(),
            Mau.BAM, null, null, KHONG_KIEM_DUOC, ReleaseStatus.CHO_GIAO_VIEN_DUYET, false, Mau.LUC, List.of(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(),
            Mau.BAM, null, null, GV_DUYET, null, false, Mau.LUC, List.of(), List.of())).isInstanceOf(IllegalArgumentException.class);
        // Rà lần 2 (N1): lượt công thức gia sư cũng phải có trạng thái tổng khớp các tầng.
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(),
            Mau.BAM, null, null, DAT, null, false, Mau.LUC, Mau.tang(SAI, SAI, SAI), List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.TUTOR_FORMULA, UUID.randomUUID(),
            Mau.BAM, null, null, DAT, null, false, Mau.LUC, List.of(TierResult.of(1, DAT)), List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chiDuyetLuotKhongKiemDuocDuTangMoiNhatChuaCuDungNoiDungVaDungBang() {
        VerificationRun cho = Mau.luot(DAT, DAT, KHONG_KIEM_DUOC);
        assertThat(cho.approvalRefusal(true, Mau.BAM, Mau.BANG)).isEmpty();
        assertThat(Mau.luot(SAI, DAT, KHONG_KIEM_DUOC).approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.BLOCKED);
        assertThat(Mau.luot(DAT, DAT, DAT).approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.NOTHING_TO_APPROVE);
        VerificationRun thieu = VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 1, Mau.BANG, List.of(TierResult.of(1, DAT)), List.of(), Mau.LUC);
        assertThat(thieu.approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.INCOMPLETE);
        VerificationRun khongTang = VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 1, Mau.BANG, List.of(), List.of(), Mau.LUC);
        assertThat(khongTang.approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.INCOMPLETE);
        assertThat(cho.markStale().approvalRefusal(true, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.STALE);
        assertThat(cho.approvalRefusal(false, Mau.BAM, Mau.BANG)).contains(ApprovalRefusal.NOT_LATEST);
        assertThat(cho.approvalRefusal(true, Mau.BAM_KHAC, Mau.BANG)).contains(ApprovalRefusal.CONTENT_CHANGED);
        assertThat(cho.approvalRefusal(true, Mau.BAM, UUID.randomUUID())).contains(ApprovalRefusal.SHEET_CHANGED);
        assertThat(cho.approvalRefusal(true, Mau.BAM, null)).contains(ApprovalRefusal.SHEET_CHANGED);
        // Lớp chưa có bảng khóa: lượt kiểm không có bảng, bảng hiện tại cũng trống → duyệt được.
        VerificationRun khongBang = VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 1, null, Mau.tang(DAT, DAT, KHONG_KIEM_DUOC), List.of(Mau.DOAN), Mau.LUC);
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
    void danhDauCuGiuNguyenKetQuaVaToStringKhongInCanCu() {
        VerificationRun luot = Mau.luot(DAT, DAT, DAT);
        VerificationRun cu = luot.markStale();
        assertThat(cu.stale()).isTrue();
        assertThat(cu.overallStatus()).isEqualTo(DAT);
        assertThat(cu.markStale()).isSameAs(cu);

        TierResult coRaw = new TierResult(1, DAT, null, null, null, null, "lý do", null, "{\"dao_ham\": \"3*x**2 - 12*x + 9\"}");
        VerificationRun coCanCu = VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 1, Mau.BANG,
            List.of(coRaw, TierResult.of(2, DAT), TierResult.of(3, DAT)), List.of(Mau.DOAN), Mau.LUC);
        assertThat(coRaw.toString()).doesNotContain("3*x**2").doesNotContain("lý do");
        assertThat(coCanCu.toString()).doesNotContain("3*x**2");
    }

    @Test
    void luotKiemBaiGhiPhienBanNoiDungVaDoanTrichDan() {
        // V5: lượt kiểm bài phải mang phiên bản nội dung đã kiểm; đoạn trích dẫn không trùng và đi theo khi duyệt, cũ.
        assertThatThrownBy(() -> new VerificationRun(UUID.randomUUID(), Mau.LOP, SubjectKind.PROBLEM, Mau.BAI, Mau.BAM, null,
            Mau.BANG, DAT, ReleaseStatus.DA_PHAT_HANH, false, Mau.LUC, Mau.tang(DAT, DAT, DAT), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 0, Mau.BANG, Mau.tang(DAT, DAT, DAT), List.of(Mau.DOAN), Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
        UUID doan = UUID.randomUUID();
        VerificationRun cho = VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 3, Mau.BANG, Mau.tang(DAT, DAT, KHONG_KIEM_DUOC),
            List.of(doan), Mau.LUC);
        assertThat(cho.contentVersion()).isEqualTo(3);
        assertThat(cho.citationPassageIds()).containsExactly(doan);
        assertThat(cho.markStale().citationPassageIds()).containsExactly(doan);
        VerificationRun.Approval duyet = cho.approve(Mau.GV, "Đã đối chiếu", true, Mau.BAM, Mau.BANG, Mau.LUC);
        assertThat(duyet.run().contentVersion()).isEqualTo(3);
        assertThat(duyet.run().citationPassageIds()).containsExactly(doan);
        assertThatThrownBy(() -> VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 3, Mau.BANG, Mau.tang(DAT, DAT, DAT),
            List.of(doan, doan), Mau.LUC)).isInstanceOf(IllegalArgumentException.class);
        // Codex #121: tầng 2 DAT mà không có đoạn trích dẫn thì không dựng được lượt kiểm bài.
        assertThatThrownBy(() -> VerificationRun.forProblem(Mau.LOP, Mau.BAI, Mau.BAM, 3, Mau.BANG, Mau.tang(DAT, DAT, DAT),
            List.of(), Mau.LUC)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tầng 2 DAT");
    }
}
