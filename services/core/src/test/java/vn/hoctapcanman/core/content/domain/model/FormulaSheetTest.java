package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormulaSheetTest {

    private static FormulaSheet nhap() {
        return FormulaSheet.draft(Mau.LOP, 1, "Bảng khóa kèm chủ đề", List.of(Mau.dong(2, "d-2"), Mau.dong(1, "d-1")), Mau.LUC);
    }

    @Test
    void bangNhapXepDongTheoThuTuVaTuChoiTrungThuTuHayTrungMa() {
        assertThat(nhap().rows()).extracting(Formula::code).containsExactly("d-1", "d-2");
        assertThatThrownBy(() -> FormulaSheet.draft(Mau.LOP, 1, null, List.of(Mau.dong(1, "d-1"), Mau.dong(1, "d-2")), Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FormulaSheet.draft(Mau.LOP, 1, null, List.of(Mau.dong(1, "d-1"), Mau.dong(2, "d-1")), Mau.LUC))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moiDongDatCaHaiTangThiKhoaDuocCoDauVanTay() {
        FormulaSheet khoa = nhap().withCheckResults(Map.of("d-1", Mau.dat(), "d-2", Mau.dat())).lock(null, Mau.LUC);
        assertThat(khoa.isLocked()).isTrue();
        assertThat(khoa.lockedAt()).isEqualTo(Mau.LUC);
        assertThat(khoa.lockedBy()).isNull();
        assertThat(khoa.fingerprint()).matches("[0-9a-f]{64}");
        assertThat(khoa.fingerprint()).isEqualTo(FormulaSheet.fingerprintOf(nhap().rows()));
        assertThat(khoa.rows()).allMatch(Formula::passes);
    }

    @Test
    void conDongChuaDatThiKhongKhoaThieuVaBaoDungMaDong() {
        FormulaCheck kkd = new FormulaCheck(FormulaKind.DINH_LI, CheckStatus.DAT, CheckStatus.KHONG_KIEM_DUOC, null, "{}", null);
        FormulaSheet mot = nhap().withCheckResults(Map.of("d-1", Mau.dat(), "d-2", kkd));
        assertThat(mot.rowsNotPassing()).containsExactly("d-2");
        assertThatThrownBy(() -> mot.lock(Mau.GV, Mau.LUC)).isInstanceOf(IllegalStateException.class).hasMessageContaining("d-2");
        // Dòng không có kết quả thì chưa kiểm: không khóa được.
        FormulaSheet thieu = nhap().withCheckResults(Map.of("d-1", Mau.dat()));
        assertThat(thieu.rowsNotPassing()).containsExactly("d-2");
        // Bảng trống không khóa được.
        assertThatThrownBy(() -> FormulaSheet.draft(Mau.LOP, 1, null, List.of(), Mau.LUC).lock(Mau.GV, Mau.LUC))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bangDaKhoaKhongSuaDuocSuaLaTaoBangNhapPhienBanMoiChuaKiem() {
        FormulaSheet khoa = nhap().withCheckResults(Map.of("d-1", Mau.dat(), "d-2", Mau.dat())).lock(Mau.GV, Mau.LUC);
        assertThatThrownBy(() -> khoa.withCheckResults(Map.of())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> khoa.lock(Mau.GV, Mau.LUC)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> khoa.newDraft(1, Mau.LUC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> nhap().newDraft(2, Mau.LUC)).isInstanceOf(IllegalStateException.class);

        FormulaSheet moi = khoa.newDraft(2, Mau.LUC);
        assertThat(moi.status()).isEqualTo(SheetStatus.NHAP);
        assertThat(moi.version()).isEqualTo(2);
        assertThat(moi.id()).isNotEqualTo(khoa.id());
        assertThat(moi.rows()).extracting(Formula::code).containsExactly("d-1", "d-2");
        assertThat(moi.rows()).noneMatch(Formula::passes).allMatch(d -> d.tier1Status() == null && d.citationPassageId() == null);
        assertThat(moi.rows()).extracting(Formula::id).doesNotContainAnyElementsOf(khoa.rows().stream().map(Formula::id).toList());
    }

    @Test
    void bangKhoaPhaiCoDongVaMoiDongDat() {
        Formula chuaKiem = Mau.dong(1, "d-1");
        assertThatThrownBy(() -> new FormulaSheet(Mau.BANG, Mau.LOP, 1, SheetStatus.KHOA, null, Mau.BAM, Mau.LUC, null, Mau.LUC,
            List.of(chuaKiem))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FormulaSheet(Mau.BANG, Mau.LOP, 1, SheetStatus.KHOA, null, Mau.BAM, Mau.LUC, null, Mau.LUC,
            List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FormulaSheet(Mau.BANG, Mau.LOP, 1, SheetStatus.NHAP, null, Mau.BAM, null, null, Mau.LUC,
            List.of(chuaKiem))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tang2DatPhaiCoTrichDanDongCongThucKhongDuyetRieng() {
        assertThatThrownBy(() -> new FormulaCheck(FormulaKind.DANG_THUC, CheckStatus.DAT, CheckStatus.DAT, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FormulaCheck(FormulaKind.DANG_THUC, CheckStatus.GV_DUYET, CheckStatus.DAT, null, null, Mau.DOAN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dauVanTayDoiKhiNoiDungDoiKhongDoiTheoKetQuaKiem() {
        List<Formula> goc = List.of(Mau.dong(1, "d-1"));
        String bam = FormulaSheet.fingerprintOf(goc);
        assertThat(FormulaSheet.fingerprintOf(List.of(Mau.dong(1, "d-1")))).isEqualTo(bam);
        assertThat(FormulaSheet.fingerprintOf(nhap().withCheckResults(Map.of("d-1", Mau.dat())).rows()))
            .isEqualTo(FormulaSheet.fingerprintOf(nhap().rows()));
        Formula d = goc.getFirst();
        Formula loiKhac = Formula.unchecked(1, "d-1", d.skillCode(), d.title(), d.latex(), d.statement() + " ");
        assertThat(FormulaSheet.fingerprintOf(List.of(loiKhac))).isNotEqualTo(bam);
        // Độ dài đứng trước mỗi trường: dời chữ giữa hai trường không cho cùng chuỗi.
        Formula a = Formula.unchecked(1, "d-1", null, "ab", "c", "x");
        Formula b = Formula.unchecked(1, "d-1", null, "a", "bc", "x");
        assertThat(FormulaSheet.fingerprintOf(List.of(a))).isNotEqualTo(FormulaSheet.fingerprintOf(List.of(b)));
    }
}
