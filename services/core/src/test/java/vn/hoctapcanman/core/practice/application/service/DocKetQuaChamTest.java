package vn.hoctapcanman.core.practice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.dto.ViTriSai;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;

/** Đọc phản hồi {@code /v1/grade} đóng mặc định (FR-009) và phản hồi cho học sinh không mang giá trị đúng (FR-006). */
class DocKetQuaChamTest {

    private static final UUID BAI_LAM = UUID.randomUUID();
    private static final String BAM = "c".repeat(64);
    private static final Instant LUC = Instant.parse("2026-10-05T08:00:00Z");
    private static final List<String> KHUNG = List.of("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN");

    @Test
    void docDuPhanHoiSaiNhuV0() {
        GradingResult g = DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", BAM, phanHoiSai(), LUC);
        assertThat(g.result()).isEqualTo(GradeStatus.SAI);
        assertThat(g.stepCode()).isEqualTo("B.DH.XETDAU");
        assertThat(g.requestHash()).isEqualTo(BAM);
        assertThat(g.resultType()).isEqualTo("DAU_DOI_TRONG_KHOANG");
        assertThat(g.wrongStepsJson()).isEqualTo("{\"ma_buoc\":\"B.DH.XETDAU\",\"dong\":null,\"o\":{\"hang\":\"DAU_YPHAY\",\"k\":2}}");
        assertThat(g.errorCode()).isEqualTo("ERR.DH.05");
        assertThat(g.confidence()).isEqualTo(0.8);
        assertThat(g.perStep()).containsExactlyInAnyOrderEntriesOf(Map.of("B.DH.NGHIEM", "DAT", "B.DH.XETDAU", "SAI"));
        assertThat(g.message()).isEqualTo("Bước Xét dấu, ô dấu thứ 2 cần xem lại.");
        assertThat(g.issuesJson()).startsWith("[{\"id\":\"v1\"");
        assertThat(g.mathOk()).isNull();
        assertThat(g.unfinished()).isFalse();
        assertThat(g.normalizerVersion()).isEqualTo("norm-0.2");
        assertThat(g.normalizationJson()).isEqualTo("[{\"ma_buoc\":\"B.DH.XETDAU\",\"dong\":0,\"trang_thai_chuan_hoa\":\"OK\"}]");
    }

    @Test
    void coDauUChiCoNghiaKhiSai() {
        Map<String, @Nullable Object> sai = phanHoiSai();
        sai.put("toan_dung", true);
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", BAM, sai, LUC).mathOk()).isTrue();
        Map<String, @Nullable Object> dat = phanHoiDat();
        dat.put("toan_dung", true);
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", BAM, dat, LUC).mathOk()).isNull();
        Map<String, @Nullable Object> khongPhaiBool = phanHoiSai();
        khongPhaiBool.put("toan_dung", "true");
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", BAM, khongPhaiBool, LUC).mathOk()).isNull();
    }

    @Test
    void khongCoPhanHoiHayPhanHoiHongThiKhongChamDuoc() {
        Map<String, @Nullable Object> ketQuaLa = phanHoiDat();
        ketQuaLa.put("ket_qua", "DAT_ROI");
        Map<String, @Nullable Object> khongChamDuoc = phanHoiDat();
        khongChamDuoc.put("ket_qua", "KHONG_CHAM_DUOC");
        Map<String, @Nullable Object> perBuocSaiKieu = phanHoiDat();
        perBuocSaiKieu.put("per_buoc", Map.of("B.DH.TXD", 1));
        Map<String, @Nullable Object> perBuocKhongPhaiDoiTuong = phanHoiDat();
        perBuocKhongPhaiDoiTuong.put("per_buoc", List.of("DAT"));
        Map<String, @Nullable Object> tinCayNgoaiKhoang = phanHoiSai();
        tinCayNgoaiKhoang.put("do_tin_cay", 1.5);
        Map<String, @Nullable Object> maLoiSaiKieu = phanHoiSai();
        maLoiSaiKieu.put("ma_loi", 5);
        Map<String, @Nullable Object> thieuKetQua = phanHoiDat();
        thieuKetQua.remove("ket_qua");
        Stream.of(null, ketQuaLa, khongChamDuoc, perBuocSaiKieu, perBuocKhongPhaiDoiTuong, tinCayNgoaiKhoang, maLoiSaiKieu, thieuKetQua)
            .map(p -> DocKetQuaCham.ketQua(BAI_LAM, "B.DH.TXD", BAM, p, LUC))
            .forEach(g -> {
                assertThat(g.result()).isEqualTo(GradeStatus.KHONG_CHAM_DUOC);
                assertThat(g.message()).isEqualTo(DocKetQuaCham.MAY_BAN);
                assertThat(g.perStep()).isEmpty();
                assertThat(g.errorCode()).isNull();
            });
    }

    @Test
    void phongBiThieuHayKhongNhatQuanThiKhongChamDuoc() {
        // Codex #140 (P1): phản hồi thiếu như {"ket_qua":"DAT"} không được thành đạt.
        List<Map<String, @Nullable Object>> hong = new java.util.ArrayList<>();
        Map<String, @Nullable Object> chiKetQua = new LinkedHashMap<>();
        chiKetQua.put("ket_qua", "DAT");
        hong.add(chiKetQua);
        for (String khoa : List.of("loai_ket_qua", "per_buoc", "thong_bao", "chua_xong", "cac_van_de", "buoc_sai", "ma_loi", "do_tin_cay")) {
            Map<String, @Nullable Object> thieu = phanHoiDat();
            thieu.remove(khoa);
            hong.add(thieu);
        }
        Map<String, @Nullable Object> datMaCoBuocSai = phanHoiDat();
        datMaCoBuocSai.put("buoc_sai", Map.of("ma_buoc", "B.DH.TXD"));
        hong.add(datMaCoBuocSai);
        Map<String, @Nullable Object> datMaCoVanDe = phanHoiDat();
        datMaCoVanDe.put("cac_van_de", List.of(Map.of("id", "VD1")));
        hong.add(datMaCoVanDe);
        Map<String, @Nullable Object> datLoaiKhac = phanHoiDat();
        datLoaiKhac.put("loai_ket_qua", "SAI_BIEN_DOI");
        hong.add(datLoaiKhac);
        Map<String, @Nullable Object> sauBuoc = phanHoiDat();
        sauBuoc.put("nop_toi", "B.DH.KETLUAN");
        hong.add(sauBuoc);
        Map<String, @Nullable Object> chuaXongChu = phanHoiDat();
        chuaXongChu.put("chua_xong", "false");
        hong.add(chuaXongChu);
        for (Map<String, @Nullable Object> p : hong) {
            assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", BAM, p, LUC).result()).as("%s", p).isEqualTo(GradeStatus.KHONG_CHAM_DUOC);
        }
        Map<String, @Nullable Object> dungBuoc = phanHoiDat();
        dungBuoc.put("nop_toi", "B.DH.DAOHAM");
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", BAM, dungBuoc, LUC).result()).isEqualTo(GradeStatus.DAT);
    }

    @Test
    void choHocSinhChoSaiKhongTrungVaKhongCoGiaTriDung() {
        KetQuaNopBuoc kq = DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", BAM, phanHoiSai(), LUC), KHUNG, false);
        assertThat(kq.ketQua()).isEqualTo("SAI");
        assertThat(kq.thongBao()).isEqualTo("Bước Xét dấu, ô dấu thứ 2 cần xem lại.");
        assertThat(kq.maLoi()).isEqualTo("ERR.DH.05");
        assertThat(kq.buocKe()).isNull();
        // Bước sai gốc rồi bước sai của từng vấn đề; ô trùng chỉ một lần.
        assertThat(kq.oSai()).containsExactly(new ViTriSai("B.DH.XETDAU", null, "DAU_YPHAY", 2),
            new ViTriSai("B.DH.XETDAU", null, "BIEN_THIEN", 2));
        assertThat(Stream.of(KetQuaNopBuoc.class, ViTriSai.class).flatMap(c -> Arrays.stream(c.getRecordComponents()))
                .map(RecordComponent::getName))
            .containsExactly("ketQua", "thongBao", "oSai", "maLoi", "buocKe", "maBuoc", "dong", "hang", "k")
            .noneMatch(ten -> ten.toLowerCase().contains("dung") || ten.toLowerCase().contains("giai"));
    }

    @Test
    void datThiCoBuocKeTruBuocCuoi() {
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", BAM, phanHoiDat(), LUC), KHUNG, false).buocKe())
            .isEqualTo("B.DH.NGHIEM");
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", BAM, phanHoiDat(), LUC), KHUNG, false).buocKe())
            .isNull();
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", BAM, null, LUC), KHUNG, false))
            .isEqualTo(new KetQuaNopBuoc("KHONG_CHAM_DUOC", DocKetQuaCham.MAY_BAN, List.of(), null, null));
    }

    @Test
    void nghiDoanMoThemCauNhuV0() {
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", BAM, phanHoiDat(), LUC), KHUNG, true).thongBao())
            .isEqualTo("Đúng rồi. " + DocKetQuaCham.DOAN_MO);
    }

    private static Map<String, @Nullable Object> phanHoiDat() {
        Map<String, @Nullable Object> p = new LinkedHashMap<>();
        p.put("ket_qua", "DAT");
        p.put("loai_ket_qua", "DAT");
        p.put("buoc_sai", null);
        p.put("ma_loi", null);
        p.put("do_tin_cay", null);
        p.put("per_buoc", Map.of("B.DH.DAOHAM", "DAT"));
        p.put("thong_bao", "Đúng rồi.");
        p.put("cac_van_de", List.of());
        p.put("chua_xong", false);
        return p;
    }

    private static Map<String, @Nullable Object> phanHoiSai() {
        Map<String, @Nullable Object> p = new HashMap<>();
        Map<String, @Nullable Object> o = new LinkedHashMap<>();
        o.put("hang", "DAU_YPHAY");
        o.put("k", 2);
        Map<String, @Nullable Object> buocSai = new LinkedHashMap<>();
        buocSai.put("ma_buoc", "B.DH.XETDAU");
        buocSai.put("dong", null);
        buocSai.put("o", o);
        Map<String, @Nullable Object> oHeQua = new LinkedHashMap<>();
        oHeQua.put("hang", "BIEN_THIEN");
        oHeQua.put("k", 2);
        Map<String, @Nullable Object> buocSaiHeQua = new LinkedHashMap<>(buocSai);
        buocSaiHeQua.put("o", oHeQua);
        p.put("ket_qua", "SAI");
        p.put("loai_ket_qua", "DAU_DOI_TRONG_KHOANG");
        p.put("buoc_sai", buocSai);
        p.put("cac_van_de", List.of(theoThuTu("id", "v1", "buoc_sai", buocSai), theoThuTu("id", "v2", "buoc_sai", buocSaiHeQua)));
        p.put("ma_loi", "ERR.DH.05");
        p.put("do_tin_cay", 0.8);
        p.put("per_buoc", Map.of("B.DH.NGHIEM", "DAT", "B.DH.XETDAU", "SAI"));
        p.put("thong_bao", "Bước Xét dấu, ô dấu thứ 2 cần xem lại.");
        p.put("phien_ban_chuan_hoa", "norm-0.2");
        p.put("chuan_hoa", List.of(theoThuTu("ma_buoc", "B.DH.XETDAU", "dong", 0, "trang_thai_chuan_hoa", "OK")));
        p.put("chua_xong", false);
        return p;
    }

    private static Map<String, Object> theoThuTu(Object... khoaGiaTri) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < khoaGiaTri.length; i += 2) {
            m.put((String) khoaGiaTri[i], khoaGiaTri[i + 1]);
        }
        return m;
    }
}
