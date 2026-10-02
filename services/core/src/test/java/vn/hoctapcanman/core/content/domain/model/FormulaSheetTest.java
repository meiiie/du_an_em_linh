package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FormulaSheetTest {

    private static FormulaSheet nhap() {
        return FormulaSheet.draft(Mau.LOP, 1, "Bảng khóa kèm chủ đề", List.of(Mau.dong(2, "d-2"), Mau.dong(1, "d-1")), Mau.LUC);
    }

    private static FormulaSheet khoa() {
        FormulaSheet bang = nhap();
        return bang.withCheckResults(Mau.datCaBang(bang)).lock(Mau.GV, Mau.LUC);
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
        FormulaSheet bang = khoa();
        assertThat(bang.isLocked()).isTrue();
        assertThat(bang.lockedAt()).isEqualTo(Mau.LUC);
        assertThat(bang.fingerprint()).matches("[0-9a-f]{64}");
        assertThat(bang.fingerprint()).isEqualTo(FormulaSheet.fingerprintOf(nhap().rows()));
        assertThat(bang.rows()).allMatch(Formula::passes);
        FormulaSheet boiImporter = nhap().withCheckResults(Mau.datCaBang(nhap())).lock(null, Mau.LUC);
        assertThat(boiImporter.lockedBy()).isNull();
    }

    @Test
    void conDongChuaDatThiKhongKhoaThieuVaBaoDungMaDong() {
        FormulaSheet bang = nhap();
        FormulaCheck kkd = FormulaCheck.of(bang.rows().get(1), FormulaKind.DINH_LI, CheckStatus.DAT, CheckStatus.KHONG_KIEM_DUOC,
            null, "{}", null);
        FormulaSheet mot = bang.withCheckResults(Map.of("d-1", Mau.dat(bang.rows().get(0)), "d-2", kkd));
        assertThat(mot.rowsNotPassing()).containsExactly("d-2");
        assertThatThrownBy(() -> mot.lock(Mau.GV, Mau.LUC)).isInstanceOf(IllegalStateException.class).hasMessageContaining("d-2");
        // Dòng không có kết quả thì chưa kiểm: không khóa được.
        FormulaSheet thieu = bang.withCheckResults(Map.of("d-1", Mau.dat(bang.rows().get(0))));
        assertThat(thieu.rowsNotPassing()).containsExactly("d-2");
        // Bảng trống không khóa được.
        assertThatThrownBy(() -> FormulaSheet.draft(Mau.LOP, 1, null, List.of(), Mau.LUC).lock(Mau.GV, Mau.LUC))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ketQuaKiemCuaNoiDungCuKhongApVaoDongDaSua() {
        // H6: kiểm dòng d-1 với nội dung cũ, rồi dòng bị sửa thành công thức sai trước khi ghi kết quả.
        Formula cu = Mau.dong(1, "d-1");
        Formula daSua = new Formula(cu.id(), 1, "d-1", cu.skillCode(), cu.title(), "(u+v)' = u'v'", cu.statement(),
            null, null, null, null, null, null);
        FormulaSheet bang = FormulaSheet.draft(Mau.LOP, 1, null, List.of(daSua), Mau.LUC);
        FormulaSheet sauKiem = bang.withCheckResults(Map.of("d-1", Mau.dat(cu)));
        assertThat(sauKiem.rowsNotPassing()).containsExactly("d-1");
        assertThat(sauKiem.rows().getFirst().tier1Status()).isNull();
        assertThatThrownBy(() -> sauKiem.lock(Mau.GV, Mau.LUC)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bangKhoaNapLaiLechDauVanTayThiTuChoi() {
        FormulaSheet bang = khoa();
        // H7: dấu vân tay không ứng với các dòng (dòng bị sửa ở CSDL sau khi khóa).
        assertThatThrownBy(() -> new FormulaSheet(bang.id(), bang.classId(), bang.version(), SheetStatus.KHOA, bang.note(),
            "0".repeat(64), bang.lockedAt(), bang.lockedBy(), bang.createdAt(), bang.rows()))
            .isInstanceOf(IllegalArgumentException.class);
        // Bảng khóa nạp lại đúng thì nhận.
        assertThat(new FormulaSheet(bang.id(), bang.classId(), bang.version(), SheetStatus.KHOA, bang.note(), bang.fingerprint(),
            bang.lockedAt(), bang.lockedBy(), bang.createdAt(), bang.rows()).isLocked()).isTrue();
    }

    @Test
    void bangDaKhoaKhongSuaDuocSuaLaTaoBangNhapPhienBanMoiChuaKiem() {
        FormulaSheet bang = khoa();
        assertThatThrownBy(() -> bang.withCheckResults(Map.of())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> bang.lock(Mau.GV, Mau.LUC)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> bang.newDraft(1, Mau.LUC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> nhap().newDraft(2, Mau.LUC)).isInstanceOf(IllegalStateException.class);

        FormulaSheet moi = bang.newDraft(2, Mau.LUC);
        assertThat(moi.status()).isEqualTo(SheetStatus.NHAP);
        assertThat(moi.version()).isEqualTo(2);
        assertThat(moi.id()).isNotEqualTo(bang.id());
        assertThat(moi.rows()).extracting(Formula::code).containsExactly("d-1", "d-2");
        assertThat(moi.rows()).noneMatch(Formula::passes).allMatch(d -> d.tier1Status() == null && d.citationPassageId() == null);
        assertThat(moi.rows()).extracting(Formula::id).doesNotContainAnyElementsOf(bang.rows().stream().map(Formula::id).toList());
    }

    @Test
    void bangKhoaPhaiCoDongVaMoiDongDat() {
        Formula chuaKiem = Mau.dong(1, "d-1");
        String bam = FormulaSheet.fingerprintOf(List.of(chuaKiem));
        assertThatThrownBy(() -> new FormulaSheet(Mau.BANG, Mau.LOP, 1, SheetStatus.KHOA, null, bam, Mau.LUC, null, Mau.LUC,
            List.of(chuaKiem))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FormulaSheet(Mau.BANG, Mau.LOP, 1, SheetStatus.KHOA, null, Mau.BAM, Mau.LUC, null, Mau.LUC,
            List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FormulaSheet(Mau.BANG, Mau.LOP, 1, SheetStatus.NHAP, null, Mau.BAM, null, null, Mau.LUC,
            List.of(chuaKiem))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dongKiemTang2DatPhaiCoTrichDanCoLoaiThuTuVaChuHopLe() {
        Formula d = Mau.dong(1, "d-1");
        assertThatThrownBy(() -> FormulaCheck.of(d, FormulaKind.DANG_THUC, CheckStatus.DAT, CheckStatus.DAT, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FormulaCheck.of(d, FormulaKind.DANG_THUC, CheckStatus.GV_DUYET, CheckStatus.DAT, null, null, Mau.DOAN))
            .isInstanceOf(IllegalArgumentException.class);
        // Dòng có kết quả kiểm mà không có loại; số thứ tự quá smallint; surrogate lẻ.
        assertThatThrownBy(() -> new Formula(UUID.randomUUID(), 1, "d-1", null, "Tổng", "x", "y", null, CheckStatus.DAT, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Formula.unchecked(40000, "d-1", null, "Tổng", "x", "y")).isInstanceOf(IllegalArgumentException.class);
        String le = "a" + (char) 0xD800;
        assertThatThrownBy(() -> Formula.unchecked(1, "d-1", null, "Tổng", le, "y")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dauVanTayDoiKhiNoiDungDoiKhongDoiTheoKetQuaKiem() {
        List<Formula> goc = List.of(Mau.dong(1, "d-1"));
        String bam = FormulaSheet.fingerprintOf(goc);
        assertThat(FormulaSheet.fingerprintOf(List.of(Mau.dong(1, "d-1")))).isEqualTo(bam);
        FormulaSheet bang = nhap();
        assertThat(FormulaSheet.fingerprintOf(bang.withCheckResults(Mau.datCaBang(bang)).rows()))
            .isEqualTo(FormulaSheet.fingerprintOf(bang.rows()));
        Formula d = goc.getFirst();
        Formula loiKhac = Formula.unchecked(1, "d-1", d.skillCode(), d.title(), d.latex(), d.statement() + " ");
        assertThat(FormulaSheet.fingerprintOf(List.of(loiKhac))).isNotEqualTo(bam);
        // Độ dài đứng trước mỗi trường: dời chữ giữa hai trường không cho cùng chuỗi.
        Formula a = Formula.unchecked(1, "d-1", null, "ab", "c", "x");
        Formula b = Formula.unchecked(1, "d-1", null, "a", "bc", "x");
        assertThat(FormulaSheet.fingerprintOf(List.of(a))).isNotEqualTo(FormulaSheet.fingerprintOf(List.of(b)));
    }
}
