package vn.hoctapcanman.core.practice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.service.ClassMembershipService;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.ClassSettingsRepositoryAdapter;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.EnrollmentRepositoryAdapter;
import vn.hoctapcanman.core.content.application.service.BaiDeLamService;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.TopicCatalogRepositoryAdapter;
import vn.hoctapcanman.core.practice.application.dto.DongNop;
import vn.hoctapcanman.core.practice.application.dto.NopBuocRequest;
import vn.hoctapcanman.core.practice.application.dto.ONop;
import vn.hoctapcanman.core.practice.application.dto.SuKienNop;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ViTriSai;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;
import vn.hoctapcanman.core.practice.application.port.MayCham;
import vn.hoctapcanman.core.practice.application.service.DocKetQuaCham;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;
import vn.hoctapcanman.core.practice.infrastructure.persistence.GradingResultRepositoryAdapter;
import vn.hoctapcanman.core.practice.infrastructure.persistence.SubmissionRepositoryAdapter;
import vn.hoctapcanman.core.shared.infrastructure.ClockConfig;

/**
 * Nộp bước trên PostgreSQL 18 thật (T020, T022), máy chấm giả đếm lần gọi: dựng yêu cầu chấm từ các bước đã lưu, tải lại
 * giữ bài làm, hai tab nộp cùng bước chỉ một lần chấm có phán quyết, dịch vụ toán lỗi thì {@code KHONG_CHAM_DUOC} và nộp lại
 * chấm lại, chấm sai không lộ đáp án, người ngoài lớp hay bài chưa phát hành như không có bài, nghi đoán mò. Use case tự mở
 * giao dịch và gọi máy chấm ngoài giao dịch, nên test không chạy trong giao dịch của test và tự dọn dữ liệu.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    ClockConfig.class,
    EnrollmentRepositoryAdapter.class,
    ClassSettingsRepositoryAdapter.class,
    ClassMembershipService.class,
    ProblemRepositoryAdapter.class,
    TopicCatalogRepositoryAdapter.class,
    BaiDeLamService.class,
    SubmissionRepositoryAdapter.class,
    GradingResultRepositoryAdapter.class,
    NopBuocUseCaseTest.MayChamGia.class,
    NopBuocUseCase.class
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NopBuocUseCaseTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant LUC = Instant.parse("2026-10-05T08:00:00Z");
    private static final String BI_MAT = "GIA_TRI_DUNG_KHONG_DUOC_LO";

    @Autowired
    private NopBuocUseCase nopBuoc;

    @Autowired
    private MayChamGia mayCham;

    @Autowired
    private JdbcClient jdbc;

    private UUID lop;
    private UUID an;
    private UUID giaoVien;
    private UUID bai;
    private String ma;
    private final List<UUID> nguoi = new ArrayList<>();
    private final List<UUID> baiThem = new ArrayList<>();

    @BeforeEach
    void duLieu() {
        mayCham.datLai();
        DuLieuPractice.danhMuc(jdbc);
        lop = DuLieuPractice.lop(jdbc);
        an = nguoiMoi("STUDENT");
        giaoVien = nguoiMoi("TEACHER");
        DuLieuPractice.ghiDanh(jdbc, lop, an, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, giaoVien, "TEACHER");
        bai = DuLieuPractice.bai(jdbc, "NB");
        DuLieuPractice.phatHanh(jdbc, lop, bai);
        ma = jdbc.sql("select code from problems where id = ?").params(bai).query(String.class).single();
    }

    @AfterEach
    void don() {
        jdbc.sql("delete from classes where id = ?").params(lop).update();
        jdbc.sql("delete from problems where id = ?").params(bai).update();
        baiThem.forEach(id -> jdbc.sql("delete from problems where id = ?").params(id).update());
        nguoi.forEach(id -> jdbc.sql("delete from users where id = ?").params(id).update());
    }

    @Test
    void taiLaiGiuBaiLamVaYeuCauChamDungTuCacBuocDaLuu() {
        KetQuaNopBuoc txd = nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = \\mathbb{R}"));
        assertThat(txd).isEqualTo(new KetQuaNopBuoc("DAT", "Đúng rồi.", List.of(), null, "B.DH.DAOHAM"));
        // «Tải lại»: lần nộp sau (yêu cầu mới, không mang bước trước) vẫn chấm trên bài làm cũ, gồm bước đã lưu.
        nopBuoc.execute(an, lop, ma, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x"));
        assertThat(JSON.writeValueAsString(mayCham.yeuCau.getLast())).isEqualTo("{\"ham\":\"x**3 - 3*x**2 + 2\","
            + "\"nop_toi\":\"B.DH.DAOHAM\",\"cac_buoc\":[{\"ma_buoc\":\"B.DH.TXD\",\"cac_dong\":[{\"dong\":0,\"latex\":\"D = \\\\mathbb{R}\"}]},"
            + "{\"ma_buoc\":\"B.DH.DAOHAM\",\"cac_dong\":[{\"dong\":0,\"latex\":\"y' = 3x^2 - 6x\"}]}]}");
        assertThat(jdbc.sql("select count(*) from submissions where class_id = ? and student_id = ?").params(lop, an).query(Integer.class)
            .single()).isOne();
        assertThat(ketQuaDaGhi()).containsExactly("B.DH.TXD DAT", "B.DH.DAOHAM DAT");
    }

    @Test
    void nopLaiCungBuocCungNoiDungChiChamMotLan() {
        KetQuaNopBuoc dau = nopBuoc.execute(an, lop, ma, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x"));
        KetQuaNopBuoc lai = nopBuoc.execute(an, lop, ma, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x"));
        assertThat(lai).isEqualTo(dau);
        assertThat(mayCham.yeuCau).hasSize(1);
        assertThat(ketQuaDaGhi()).containsExactly("B.DH.DAOHAM DAT");
        // Sửa nội dung rồi nộp lại: yêu cầu khác, chấm lại.
        nopBuoc.execute(an, lop, ma, dong("B.DH.DAOHAM", "y' = 3x^2 - 6"));
        assertThat(mayCham.yeuCau).hasSize(2);
    }

    @Test
    void haiTabNopCungBuocCungLucChiMotLanChamCoPhanQuyet() throws Exception {
        // Máy chấm giữ lần gọi đầu tới khi lần thứ hai tới: cả hai tab đều chưa thấy kết quả nào nên đều gọi máy chấm.
        CountDownLatch haiLanGoi = new CountDownLatch(2);
        mayCham.cho = haiLanGoi;
        CompletableFuture<KetQuaNopBuoc> tab1 = CompletableFuture.supplyAsync(() -> nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R")));
        CompletableFuture<KetQuaNopBuoc> tab2 = CompletableFuture.supplyAsync(() -> nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R")));
        assertThat(tab1.get(30, TimeUnit.SECONDS)).isEqualTo(tab2.get(30, TimeUnit.SECONDS));
        assertThat(mayCham.yeuCau).hasSize(2);
        assertThat(ketQuaDaGhi()).containsExactly("B.DH.TXD DAT");
    }

    @Test
    void dichVuToanLoiThiKhongChamDuocVaNopLaiThiChamLai() {
        mayCham.traLoi = y -> Optional.empty();
        KetQuaNopBuoc loi = nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R"));
        assertThat(loi).isEqualTo(new KetQuaNopBuoc("KHONG_CHAM_DUOC", DocKetQuaCham.MAY_BAN, List.of(), null, null));
        mayCham.traLoi = MayChamGia::dat;
        KetQuaNopBuoc lai = nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R"));
        assertThat(lai.ketQua()).isEqualTo("DAT");
        assertThat(mayCham.yeuCau).hasSize(2);
        assertThat(ketQuaDaGhi()).containsExactly("B.DH.TXD KHONG_CHAM_DUOC", "B.DH.TXD DAT");
        assertThat(jdbc.sql("select status from submissions where class_id = ? and student_id = ?").params(lop, an).query(String.class)
            .single()).isEqualTo("DANG_LAM");
    }

    @Test
    void chamSaiKhongLoDapAn() {
        mayCham.traLoi = y -> {
            Map<String, @Nullable Object> p = new LinkedHashMap<>();
            Map<String, @Nullable Object> o = new LinkedHashMap<>();
            o.put("hang", "DAU_YPHAY");
            o.put("k", 2);
            Map<String, @Nullable Object> buocSai = new LinkedHashMap<>();
            buocSai.put("ma_buoc", "B.DH.XETDAU");
            buocSai.put("dong", null);
            buocSai.put("o", o);
            p.put("ket_qua", "SAI");
            p.put("loai_ket_qua", "DAU_DOI_TRONG_KHOANG");
            p.put("buoc_sai", buocSai);
            p.put("ma_loi", "ERR.DH.05");
            p.put("do_tin_cay", 0.8);
            p.put("per_buoc", Map.of("B.DH.XETDAU", "SAI"));
            p.put("thong_bao", "Bước Xét dấu, ô dấu thứ 2 cần xem lại.");
            // Trường lạ mang giá trị đúng: không bao giờ được tới học sinh.
            p.put("gia_tri_dung", BI_MAT);
            p.put("cac_van_de", List.of(Map.of("buoc_sai", buocSai, "goi_y_dung", BI_MAT)));
            p.put("chua_xong", false);
            p.put("chuan_hoa", List.of());
            p.put("phien_ban_chuan_hoa", "norm-0.2");
            p.put("nop_toi", y.get("nop_toi"));
            return Optional.of(p);
        };
        KetQuaNopBuoc kq = nopBuoc.execute(an, lop, ma, bang(List.of(new ONop("X", 0, "0"), new ONop("DAU_YPHAY", 2, "+")), List.of()));
        assertThat(kq).isEqualTo(new KetQuaNopBuoc("SAI", "Bước Xét dấu, ô dấu thứ 2 cần xem lại.",
            List.of(new ViTriSai("B.DH.XETDAU", null, "DAU_YPHAY", 2)), "ERR.DH.05", null));
        assertThat(JSON.writeValueAsString(kq)).doesNotContain(BI_MAT);
    }

    @Test
    void ngoaiLopGiaoVienBaiChuaPhatHanhHayKhongCoNhuKhongCoBai() {
        UUID ngoai = nguoiMoi("STUDENT");
        UUID chuaPhatHanh = DuLieuPractice.bai(jdbc, "NB-NHAP");
        String maChua = jdbc.sql("select code from problems where id = ?").params(chuaPhatHanh).query(String.class).single();
        try {
            assertThatThrownBy(() -> nopBuoc.execute(ngoai, lop, ma, dong("B.DH.TXD", "D = R"))).isInstanceOf(BaiKhongTimThayException.class);
            assertThatThrownBy(() -> nopBuoc.execute(giaoVien, lop, ma, dong("B.DH.TXD", "D = R")))
                .isInstanceOf(BaiKhongTimThayException.class);
            assertThatThrownBy(() -> nopBuoc.execute(an, lop, maChua, dong("B.DH.TXD", "D = R"))).isInstanceOf(BaiKhongTimThayException.class);
            assertThatThrownBy(() -> nopBuoc.execute(an, lop, "KHONG-CO", dong("B.DH.TXD", "D = R")))
                .isInstanceOf(BaiKhongTimThayException.class);
            assertThatThrownBy(() -> nopBuoc.execute(an, lop, ma, dong("B.KHAC", "x"))).isInstanceOf(IllegalArgumentException.class);
            assertThat(mayCham.yeuCau).isEmpty();
            assertThat(jdbc.sql("select count(*) from submissions where class_id = ?").params(lop).query(Integer.class).single()).isZero();
        } finally {
            jdbc.sql("delete from problems where id = ?").params(chuaPhatHanh).update();
        }
    }

    @Test
    void motODoiTuNguongLanThiNghiDoanMo() {
        List<SuKienNop> doi = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            doi.add(new SuKienNop("B.DH.XETDAU", "DAU_YPHAY", 1, i % 2 == 0 ? "+" : "-", i % 2 == 0 ? "-" : "+", LUC.plusSeconds(i)));
        }
        KetQuaNopBuoc ba = nopBuoc.execute(an, lop, ma, bang(List.of(new ONop("DAU_YPHAY", 1, "-")), doi.subList(0, 3)));
        assertThat(ba.thongBao()).doesNotContain(DocKetQuaCham.DOAN_MO);
        KetQuaNopBuoc bon = nopBuoc.execute(an, lop, ma, bang(List.of(new ONop("DAU_YPHAY", 1, "+")), doi.subList(3, 4)));
        assertThat(bon.thongBao()).endsWith(DocKetQuaCham.DOAN_MO);
        assertThat(jdbc.sql("select guess_reason from submissions where class_id = ? and student_id = ?").params(lop, an)
            .query(String.class).single()).isEqualTo("Ô DAU_YPHAY:1 bị đổi 4 lần trước khi nộp (ngưỡng 4).");
    }

    @Test
    void guiLaiCungSuKienKhongGhiTrungVaKhongThanhDoanMo() {
        // Codex #140 (P2): mất phản hồi rồi gửi lại cùng yêu cầu: ba lần đổi ô không thành sáu.
        List<SuKienNop> doi = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            doi.add(new SuKienNop("B.DH.XETDAU", "DAU_YPHAY", 1, i % 2 == 0 ? "+" : "-", i % 2 == 0 ? "-" : "+",
                LUC.plusMillis(1001 * i).plusNanos(123)));
        }
        NopBuocRequest yeuCau = bang(List.of(new ONop("DAU_YPHAY", 1, "-")), doi);
        KetQuaNopBuoc dau = nopBuoc.execute(an, lop, ma, yeuCau);
        KetQuaNopBuoc lai = nopBuoc.execute(an, lop, ma, yeuCau);
        assertThat(lai).isEqualTo(dau);
        assertThat(lai.thongBao()).doesNotContain(DocKetQuaCham.DOAN_MO);
        assertThat(jdbc.sql("""
                select count(*) from input_events e join submissions s on s.id = e.submission_id where s.class_id = ?""")
            .params(lop).query(Integer.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("select guess_suspected from submissions where class_id = ?").params(lop).query(Boolean.class).single()).isFalse();
    }

    @Test
    void ketLuanKhaiBaoTheoDeVaChiNhanODeHoi() {
        // Codex #140 (P1): khai_bao của bước kết luận lấy từ đề. Đề của bài mẫu chỉ hỏi đơn điệu.
        NopBuocRequest coCucDai = new NopBuocRequest("B.DH.KETLUAN", List.of(new DongNop(0, "(2; +\\infty)", "DONG_BIEN"),
            new DongNop(1, "(0; 2)", "NGHICH_BIEN"), new DongNop(2, "x = 0", "CUC_DAI")), null, null);
        assertThatThrownBy(() -> nopBuoc.execute(an, lop, ma, coCucDai)).isInstanceOf(IllegalArgumentException.class);
        assertThat(mayCham.yeuCau).isEmpty();
        // Bài hỏi cực trị: học sinh bỏ hai ô cực trị, khai_bao vẫn đủ bốn ô.
        UUID hoiCucTri = DuLieuPractice.bai(jdbc, "NB-CT");
        try {
            jdbc.sql("update problems set statement_text = 'Tìm khoảng đơn điệu và cực trị của y = x^3 - 3x^2 + 2.' where id = ?")
                .params(hoiCucTri).update();
            DuLieuPractice.phatHanh(jdbc, lop, hoiCucTri);
            String maCt = jdbc.sql("select code from problems where id = ?").params(hoiCucTri).query(String.class).single();
            nopBuoc.execute(an, lop, maCt, new NopBuocRequest("B.DH.KETLUAN", List.of(new DongNop(0, "(2; +\\infty)", "DONG_BIEN"),
                new DongNop(1, "(0; 2)", "NGHICH_BIEN")), null, null));
            assertThat(JSON.writeValueAsString(mayCham.yeuCau.getLast()))
                .contains("\"khai_bao\":[\"dong_bien\",\"nghich_bien\",\"cuc_dai\",\"cuc_tieu\"]");
        } finally {
            baiThem.add(hoiCucTri);
        }
    }

    private List<String> ketQuaDaGhi() {
        return jdbc.sql("""
                select g.step_code || ' ' || g.result from grading_results g join submissions s on s.id = g.submission_id
                where s.class_id = ? order by g.graded_at, g.id""").params(lop).query(String.class).list();
    }

    private UUID nguoiMoi(String vaiTro) {
        UUID id = DuLieuPractice.nguoi(jdbc, vaiTro);
        nguoi.add(id);
        return id;
    }

    private static NopBuocRequest dong(String maBuoc, String latex) {
        return new NopBuocRequest(maBuoc, List.of(new DongNop(0, latex, null)), null, null);
    }

    private static NopBuocRequest bang(List<ONop> o, List<SuKienNop> suKien) {
        return new NopBuocRequest("B.DH.XETDAU", null, o, suKien);
    }

    /** Máy chấm giả: ghi lại yêu cầu, trả lời theo hàm đặt được, có thể giữ lần gọi tới khi đủ số lần gọi. */
    static class MayChamGia implements MayCham {

        final List<Map<String, ?>> yeuCau = new CopyOnWriteArrayList<>();
        volatile Function<Map<String, ?>, Optional<Map<String, @Nullable Object>>> traLoi = MayChamGia::dat;
        volatile @Nullable CountDownLatch cho;

        void datLai() {
            yeuCau.clear();
            traLoi = MayChamGia::dat;
            cho = null;
        }

        @Override
        public Optional<Map<String, @Nullable Object>> cham(Map<String, ?> y) {
            yeuCau.add(y);
            CountDownLatch c = cho;
            if (c != null) {
                c.countDown();
                try {
                    if (!c.await(20, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Tab kia không tới máy chấm");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            return traLoi.apply(y);
        }

        static Optional<Map<String, @Nullable Object>> dat(Map<String, ?> y) {
            Map<String, @Nullable Object> p = new LinkedHashMap<>();
            p.put("ket_qua", "DAT");
            p.put("loai_ket_qua", "DAT");
            p.put("buoc_sai", null);
            p.put("ma_loi", null);
            p.put("do_tin_cay", null);
            List<String> khung = List.of("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN");
            String nopToi = (String) y.get("nop_toi");
            Map<String, @Nullable Object> per = new LinkedHashMap<>();
            khung.subList(0, khung.indexOf(nopToi) + 1).forEach(b -> per.put(b, "DAT"));
            p.put("per_buoc", per);
            p.put("thong_bao", "Đúng rồi.");
            p.put("cac_van_de", List.of());
            p.put("chua_xong", !nopToi.equals(khung.getLast()));
            p.put("chuan_hoa", List.of());
            p.put("phien_ban_chuan_hoa", "norm-0.2");
            p.put("nop_toi", y.get("nop_toi"));
            return Optional.of(p);
        }
    }
}
