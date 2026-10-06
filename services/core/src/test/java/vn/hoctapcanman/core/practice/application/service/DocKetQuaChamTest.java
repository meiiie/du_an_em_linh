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
import vn.hoctapcanman.core.practice.application.dto.hocsinh.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ViTriSai;
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
        GradingResult g = DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, phanHoiSai(), LUC);
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
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, sai, LUC).mathOk()).isTrue();
        Map<String, @Nullable Object> dat = phanHoiDat("B.DH.KETLUAN");
        dat.put("toan_dung", true);
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", KHUNG, BAM, dat, LUC).mathOk()).isNull();
        Map<String, @Nullable Object> khongPhaiBool = phanHoiSai();
        khongPhaiBool.put("toan_dung", "true");
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, khongPhaiBool, LUC).mathOk()).isNull();
    }

    @Test
    void khongCoPhanHoiHayPhanHoiHongThiKhongChamDuoc() {
        Map<String, @Nullable Object> ketQuaLa = phanHoiDat("B.DH.TXD");
        ketQuaLa.put("ket_qua", "DAT_ROI");
        Map<String, @Nullable Object> khongChamDuoc = phanHoiDat("B.DH.TXD");
        khongChamDuoc.put("ket_qua", "KHONG_CHAM_DUOC");
        Map<String, @Nullable Object> perBuocSaiKieu = phanHoiDat("B.DH.TXD");
        perBuocSaiKieu.put("per_buoc", Map.of("B.DH.TXD", 1));
        Map<String, @Nullable Object> perBuocKhongPhaiDoiTuong = phanHoiDat("B.DH.TXD");
        perBuocKhongPhaiDoiTuong.put("per_buoc", List.of("DAT"));
        Map<String, @Nullable Object> tinCayNgoaiKhoang = phanHoiSai();
        tinCayNgoaiKhoang.put("do_tin_cay", 1.5);
        Map<String, @Nullable Object> maLoiSaiKieu = phanHoiSai();
        maLoiSaiKieu.put("ma_loi", 5);
        Map<String, @Nullable Object> thieuKetQua = phanHoiDat("B.DH.TXD");
        thieuKetQua.remove("ket_qua");
        // Phán quyết độc lập #140 (ghi chú 7): mỗi ca chấm ở đúng bước của phong bì gốc, để bị từ chối vì đúng lỗi của ca, không
        // vì nop_toi lệch bước. Phong bì gốc ở mỗi bước thì được nhận.
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.TXD", KHUNG, BAM, phanHoiDat("B.DH.TXD"), LUC).result()).isEqualTo(GradeStatus.DAT);
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, phanHoiSai(), LUC).result()).isEqualTo(GradeStatus.SAI);
        Map<Map<String, @Nullable Object>, String> buocCua = new java.util.IdentityHashMap<>();
        buocCua.put(tinCayNgoaiKhoang, "B.DH.XETDAU");
        buocCua.put(maLoiSaiKieu, "B.DH.XETDAU");
        Stream.of(null, ketQuaLa, khongChamDuoc, perBuocSaiKieu, perBuocKhongPhaiDoiTuong, tinCayNgoaiKhoang, maLoiSaiKieu, thieuKetQua)
            .map(p -> DocKetQuaCham.ketQua(BAI_LAM, p == null ? "B.DH.TXD" : buocCua.getOrDefault(p, "B.DH.TXD"), KHUNG, BAM, p, LUC))
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
        List<Map<String, @Nullable Object>> hongSai = new java.util.ArrayList<>();
        Map<String, @Nullable Object> chiKetQua = new LinkedHashMap<>();
        chiKetQua.put("ket_qua", "DAT");
        hong.add(chiKetQua);
        // 12 khóa _pack luôn trả (grader.py, hàm _pack), viết tay ở đây, không đọc từ mã sản phẩm. Thiếu khóa nào cũng không đạt,
        // ở phong bì DAT lẫn SAI.
        for (String khoa : List.of("cac_van_de", "ket_qua", "loai_ket_qua", "buoc_sai", "ma_loi", "do_tin_cay", "per_buoc", "chuan_hoa",
                "phien_ban_chuan_hoa", "thong_bao", "chua_xong", "nop_toi")) {
            Map<String, @Nullable Object> thieu = phanHoiDat("B.DH.DAOHAM");
            thieu.remove(khoa);
            hong.add(thieu);
            Map<String, @Nullable Object> saiThieu = phanHoiSai();
            saiThieu.remove(khoa);
            hongSai.add(saiThieu);
        }
        Map<String, @Nullable Object> chuanHoaSaiKieu = phanHoiSai();
        chuanHoaSaiKieu.put("chuan_hoa", "OK");
        hongSai.add(chuanHoaSaiKieu);
        Map<String, @Nullable Object> saiKhacBuoc = phanHoiSai();
        saiKhacBuoc.put("nop_toi", "B.DH.KETLUAN");
        hongSai.add(saiKhacBuoc);
        for (Map<String, @Nullable Object> p : hongSai) {
            assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, p, LUC).result()).as("%s", p).isEqualTo(GradeStatus.KHONG_CHAM_DUOC);
        }
        Map<String, @Nullable Object> datMaCoBuocSai = phanHoiDat("B.DH.DAOHAM");
        datMaCoBuocSai.put("buoc_sai", Map.of("ma_buoc", "B.DH.TXD"));
        hong.add(datMaCoBuocSai);
        Map<String, @Nullable Object> datMaCoVanDe = phanHoiDat("B.DH.DAOHAM");
        datMaCoVanDe.put("cac_van_de", List.of(Map.of("id", "VD1")));
        hong.add(datMaCoVanDe);
        Map<String, @Nullable Object> datLoaiKhac = phanHoiDat("B.DH.DAOHAM");
        datLoaiKhac.put("loai_ket_qua", "SAI_BIEN_DOI");
        hong.add(datLoaiKhac);
        Map<String, @Nullable Object> sauBuoc = phanHoiDat("B.DH.DAOHAM");
        sauBuoc.put("nop_toi", "B.DH.KETLUAN");
        hong.add(sauBuoc);
        Map<String, @Nullable Object> chuaXongChu = phanHoiDat("B.DH.DAOHAM");
        chuaXongChu.put("chua_xong", "true");
        hong.add(chuaXongChu);
        // Codex #140 (P1, vòng hai): per_buoc và chua_xong của DAT phải đúng _per(den) và bước nộp.
        Map<String, @Nullable Object> buocNopSai = phanHoiDat("B.DH.DAOHAM");
        buocNopSai.put("per_buoc", Map.of("B.DH.TXD", "DAT", "B.DH.DAOHAM", "SAI"));
        hong.add(buocNopSai);
        Map<String, @Nullable Object> perRong = phanHoiDat("B.DH.DAOHAM");
        perRong.put("per_buoc", Map.of());
        hong.add(perRong);
        Map<String, @Nullable Object> thieuBuocDau = phanHoiDat("B.DH.DAOHAM");
        thieuBuocDau.put("per_buoc", Map.of("B.DH.DAOHAM", "DAT"));
        hong.add(thieuBuocDau);
        Map<String, @Nullable Object> quaBuocNop = phanHoiDat("B.DH.DAOHAM");
        quaBuocNop.put("per_buoc", Map.of("B.DH.TXD", "DAT", "B.DH.DAOHAM", "DAT", "B.DH.NGHIEM", "DAT"));
        hong.add(quaBuocNop);
        Map<String, @Nullable Object> xongGiuaChung = phanHoiDat("B.DH.DAOHAM");
        xongGiuaChung.put("chua_xong", false);
        hong.add(xongGiuaChung);
        for (Map<String, @Nullable Object> p : hong) {
            assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", KHUNG, BAM, p, LUC).result()).as("%s", p).isEqualTo(GradeStatus.KHONG_CHAM_DUOC);
        }
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", KHUNG, BAM, phanHoiDat("B.DH.DAOHAM"), LUC).result()).isEqualTo(GradeStatus.DAT);
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", KHUNG, BAM, phanHoiDat("B.DH.KETLUAN"), LUC).result())
            .isEqualTo(GradeStatus.DAT);
        // Codex #140 (vòng ba): ở bước cuối, DAT của _pack luôn có chua_xong=false (nhánh chua_xong=True cần lỗi ở bước sau).
        Map<String, @Nullable Object> cuoiChuaXong = phanHoiDat("B.DH.KETLUAN");
        cuoiChuaXong.put("chua_xong", true);
        assertThat(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", KHUNG, BAM, cuoiChuaXong, LUC).result()).isEqualTo(GradeStatus.KHONG_CHAM_DUOC);
    }

    @Test
    void choHocSinhChoSaiKhongTrungVaKhongCoGiaTriDung() {
        KetQuaNopBuoc kq = DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, phanHoiSai(), LUC), KHUNG, false);
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
    void choHocSinhToDongLienQuanCuaVanDeGocKhiSaiNhuV0() {
        // Như cham-v0.json (DH12-03-TH-01, thua_diem_xuyen_buoc): mốc thừa 100 ở ô X k=2, dòng nghiệm 2 chứa mốc đó.
        Map<String, @Nullable Object> p = phanHoiSai();
        Map<String, @Nullable Object> moc = theoThuTu("ma_buoc", "B.DH.NGHIEM", "dong", null, "o", theoThuTu("hang", "X", "k", 2));
        p.put("buoc_sai", moc);
        p.put("cac_van_de", List.of(
            theoThuTu("id", "VD1", "loai_ket_qua", "DIEM_THUA", "buoc_sai", moc, "dong_lien_quan", 2),
            theoThuTu("id", "VD2", "buoc_sai", theoThuTu("ma_buoc", "B.DH.XETDAU", "dong", null, "o", theoThuTu("hang", "DAU_YPHAY", "k", 4)),
                "nguyen_nhan", "VD1", "dong_lien_quan", 1)));
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, p, LUC), KHUNG, false).oSai())
            .containsExactly(new ViTriSai("B.DH.NGHIEM", null, "X", 2), new ViTriSai("B.DH.NGHIEM", 2, null, null),
                new ViTriSai("B.DH.XETDAU", null, "DAU_YPHAY", 4));
        // Không SAI thì v0 không tô từ danh sách vấn đề, nên không có dòng liên quan.
        p.put("ket_qua", "KHONG_KIEM_DUOC");
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.XETDAU", KHUNG, BAM, p, LUC), KHUNG, false).oSai())
            .containsExactly(new ViTriSai("B.DH.NGHIEM", null, "X", 2), new ViTriSai("B.DH.XETDAU", null, "DAU_YPHAY", 4));
    }

    @Test
    void datThiCoBuocKeTruBuocCuoi() {
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", KHUNG, BAM, phanHoiDat("B.DH.DAOHAM"), LUC), KHUNG, false).buocKe())
            .isEqualTo("B.DH.NGHIEM");
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.KETLUAN", KHUNG, BAM, phanHoiDat("B.DH.KETLUAN"), LUC), KHUNG, false).buocKe())
            .isNull();
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", KHUNG, BAM, null, LUC), KHUNG, false))
            .isEqualTo(new KetQuaNopBuoc("KHONG_CHAM_DUOC", DocKetQuaCham.MAY_BAN, List.of(), null, null));
    }

    @Test
    void nghiDoanMoThemCauNhuV0() {
        assertThat(DocKetQuaCham.choHocSinh(DocKetQuaCham.ketQua(BAI_LAM, "B.DH.DAOHAM", KHUNG, BAM, phanHoiDat("B.DH.DAOHAM"), LUC), KHUNG, true).thongBao())
            .isEqualTo("Đúng rồi. " + DocKetQuaCham.DOAN_MO);
    }

    /** Phong bì DAT như _pack("DAT", ..., _per(den), ..., chua_xong=den != KETLUAN, nop_toi=den) của grader.py. */
    private static Map<String, @Nullable Object> phanHoiDat(String nopToi) {
        Map<String, @Nullable Object> p = new LinkedHashMap<>();
        Map<String, @Nullable Object> per = new LinkedHashMap<>();
        KHUNG.subList(0, KHUNG.indexOf(nopToi) + 1).forEach(b -> per.put(b, "DAT"));
        p.put("ket_qua", "DAT");
        p.put("loai_ket_qua", "DAT");
        p.put("buoc_sai", null);
        p.put("ma_loi", null);
        p.put("do_tin_cay", null);
        p.put("per_buoc", per);
        p.put("thong_bao", "Đúng rồi.");
        p.put("cac_van_de", List.of());
        p.put("chua_xong", !nopToi.equals(KHUNG.getLast()));
        p.put("nop_toi", nopToi);
        p.put("chuan_hoa", List.of());
        p.put("phien_ban_chuan_hoa", "norm-0.2");
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
        p.put("nop_toi", "B.DH.XETDAU");
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
