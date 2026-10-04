package vn.hoctapcanman.core.practice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.practice.domain.model.SignTable;
import vn.hoctapcanman.core.practice.domain.model.StepLine;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.TableCell;

/**
 * Payload {@code /v1/grade} dựng từ các bước đã lưu phải trùng từng byte với {@code JSON.stringify} của payload v0
 * ({@code payload()} ở {@code apps/web/components/solve-client.tsx}, lọc bước bắt đầu ở {@code nopBuoc}), và băm là SHA-256
 * của đúng các byte đó.
 */
class YeuCauChamTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String HAM = "x**3 - 3*x**2 + 2";
    private static final List<String> KHUNG = List.of("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN");
    private static final List<String> HOI_CUC_TRI = List.of("dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu");
    private static final List<String> CHI_DON_DIEU = List.of("dong_bien", "nghich_bien");

    private static final StepWork TXD = new StepWork("B.DH.TXD", List.of(new StepLine(0, "D = \\mathbb{R}", null)), null);
    private static final StepWork DAOHAM = new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "y' = 3x^2 - 6x", null)), null);
    private static final StepWork NGHIEM = new StepWork("B.DH.NGHIEM",
        List.of(new StepLine(0, "x = 0", "NGHIEM"), new StepLine(1, "x = 2", "NGHIEM")), null);
    private static final StepWork XETDAU = new StepWork("B.DH.XETDAU", List.of(), new SignTable("XET_DAU", List.of(
        new TableCell("X", 0, "0"), new TableCell("X", 1, "2"), new TableCell("DAU_YPHAY", 0, "+"), new TableCell("DAU_YPHAY", 2, "-"),
        new TableCell("DAU_YPHAY", 4, "+"), new TableCell("DAU_YPHAY", 1, "0"), new TableCell("DAU_YPHAY", 3, "0"),
        new TableCell("BIEN_THIEN", 0, "↗"), new TableCell("BIEN_THIEN", 2, "↘"), new TableCell("BIEN_THIEN", 4, "↗"))));
    private static final StepWork KETLUAN = new StepWork("B.DH.KETLUAN", List.of(
        new StepLine(0, "(-\\infty; 0), (2; +\\infty)", "DONG_BIEN"), new StepLine(1, "(0; 2)", "NGHICH_BIEN"),
        new StepLine(2, "x = 0, y = 2", "CUC_DAI"), new StepLine(3, "x = 2, y = -2", "CUC_TIEU")), null);

    /** JSON.stringify của payload v0 khi nộp tới bước kết luận, đề hỏi cả cực trị. */
    private static final String V0_KET_LUAN = "{\"ham\":\"x**3 - 3*x**2 + 2\",\"nop_toi\":\"B.DH.KETLUAN\",\"cac_buoc\":["
        + "{\"ma_buoc\":\"B.DH.TXD\",\"cac_dong\":[{\"dong\":0,\"latex\":\"D = \\\\mathbb{R}\"}]},"
        + "{\"ma_buoc\":\"B.DH.DAOHAM\",\"cac_dong\":[{\"dong\":0,\"latex\":\"y' = 3x^2 - 6x\"}]},"
        + "{\"ma_buoc\":\"B.DH.NGHIEM\",\"cac_dong\":[{\"dong\":0,\"latex\":\"x = 0\",\"loai\":\"NGHIEM\"},"
        + "{\"dong\":1,\"latex\":\"x = 2\",\"loai\":\"NGHIEM\"}]},"
        + "{\"ma_buoc\":\"B.DH.XETDAU\",\"bang\":{\"loai_bang\":\"XET_DAU\",\"cac_o\":["
        + "{\"hang\":\"X\",\"k\":0,\"gia_tri\":\"0\"},{\"hang\":\"X\",\"k\":1,\"gia_tri\":\"2\"},"
        + "{\"hang\":\"DAU_YPHAY\",\"k\":0,\"gia_tri\":\"+\"},{\"hang\":\"DAU_YPHAY\",\"k\":2,\"gia_tri\":\"-\"},"
        + "{\"hang\":\"DAU_YPHAY\",\"k\":4,\"gia_tri\":\"+\"},{\"hang\":\"DAU_YPHAY\",\"k\":1,\"gia_tri\":\"0\"},"
        + "{\"hang\":\"DAU_YPHAY\",\"k\":3,\"gia_tri\":\"0\"},{\"hang\":\"BIEN_THIEN\",\"k\":0,\"gia_tri\":\"↗\"},"
        + "{\"hang\":\"BIEN_THIEN\",\"k\":2,\"gia_tri\":\"↘\"},{\"hang\":\"BIEN_THIEN\",\"k\":4,\"gia_tri\":\"↗\"}]}},"
        + "{\"ma_buoc\":\"B.DH.KETLUAN\",\"khai_bao\":[\"dong_bien\",\"nghich_bien\",\"cuc_dai\",\"cuc_tieu\"],\"cac_dong\":["
        + "{\"dong\":0,\"latex\":\"(-\\\\infty; 0), (2; +\\\\infty)\",\"loai\":\"DONG_BIEN\"},"
        + "{\"dong\":1,\"latex\":\"(0; 2)\",\"loai\":\"NGHICH_BIEN\"},"
        + "{\"dong\":2,\"latex\":\"x = 0, y = 2\",\"loai\":\"CUC_DAI\"},"
        + "{\"dong\":3,\"latex\":\"x = 2, y = -2\",\"loai\":\"CUC_TIEU\"}]}]}";

    @Test
    void trungTungByteVoiPayloadV0VaBamLaSha256CuaChinhCacByteDo() throws Exception {
        // Thứ tự lưu không quan trọng: payload theo thứ tự khung.
        Map<String, ?> yeuCau = YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.DH.KETLUAN", List.of(KETLUAN, XETDAU, TXD, NGHIEM, DAOHAM));
        assertThat(JSON.writeValueAsString(yeuCau)).isEqualTo(V0_KET_LUAN);
        assertThat(YeuCauCham.bam(yeuCau)).isEqualTo(HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(V0_KET_LUAN.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void chiGuiCacBuocTuBuocBatDauToiBuocNop() {
        Map<String, ?> yeuCau = YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, "B.DH.NGHIEM", "B.DH.XETDAU", List.of(TXD, DAOHAM, NGHIEM, XETDAU, KETLUAN));
        assertThat(JSON.writeValueAsString(yeuCau)).startsWith("{\"ham\":\"x**3 - 3*x**2 + 2\",\"nop_toi\":\"B.DH.XETDAU\","
            + "\"cac_buoc\":[{\"ma_buoc\":\"B.DH.NGHIEM\"").endsWith("]}}],\"buoc_bat_dau\":\"B.DH.NGHIEM\"}");
        assertThat(((List<?>) yeuCau.get("cac_buoc")).stream().map(b -> (Object) ((Map<?, ?>) b).get("ma_buoc")).toList())
            .containsExactly("B.DH.NGHIEM", "B.DH.XETDAU");
    }

    @Test
    void khaiBaoLayTuBaiKhongTuNhanDongHocSinhGui() {
        // Codex #140 (P1): đề hỏi cực trị mà bài làm bỏ hai ô cực trị: khai_bao vẫn đủ bốn ô, bộ chấm vẫn chấm cực trị.
        StepWork boCucTri = new StepWork("B.DH.KETLUAN",
            List.of(new StepLine(0, "(2; +\\infty)", "DONG_BIEN"), new StepLine(1, "(-\\infty; 2)", "NGHICH_BIEN")), null);
        assertThat(JSON.writeValueAsString(YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.DH.KETLUAN", List.of(boCucTri))))
            .contains("\"khai_bao\":[\"dong_bien\",\"nghich_bien\",\"cuc_dai\",\"cuc_tieu\"],\"cac_dong\"");
        assertThat(JSON.writeValueAsString(YeuCauCham.dung(HAM, KHUNG, CHI_DON_DIEU, null, "B.DH.KETLUAN", List.of(KETLUAN))))
            .contains("\"khai_bao\":[\"dong_bien\",\"nghich_bien\"],\"cac_dong\"");
        // Chỉ bước kết luận (bước cuối của khung) mang khai_bao.
        assertThat(JSON.writeValueAsString(YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.DH.NGHIEM", List.of(NGHIEM))))
            .doesNotContain("khai_bao");
    }

    @Test
    void bamDoiKhiNoiDungDoiVaGiuNguyenKhiNoiDungGiuNguyen() {
        String goc = YeuCauCham.bam(YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.DH.DAOHAM", List.of(TXD, DAOHAM)));
        assertThat(YeuCauCham.bam(YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.DH.DAOHAM", List.of(DAOHAM, TXD)))).isEqualTo(goc);
        StepWork khac = new StepWork("B.DH.DAOHAM", List.of(new StepLine(0, "y' = 3x^2 - 6", null)), null);
        assertThat(YeuCauCham.bam(YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.DH.DAOHAM", List.of(TXD, khac)))).isNotEqualTo(goc);
        assertThat(YeuCauCham.bam(YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.DH.TXD", List.of(TXD, DAOHAM)))).isNotEqualTo(goc);
        assertThat(goc).matches("[0-9a-f]{64}");
    }

    @Test
    void buocNopNgoaiKhungHayTruocBuocBatDauBiTuChoi() {
        assertThatThrownBy(() -> YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, null, "B.KHAC", List.of(TXD)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> YeuCauCham.dung(HAM, KHUNG, HOI_CUC_TRI, "B.DH.NGHIEM", "B.DH.DAOHAM", List.of(DAOHAM)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(YeuCauCham.batDau(KHUNG, "B.KHAC")).isZero();
        assertThat(YeuCauCham.batDau(KHUNG, null)).isZero();
    }
}
