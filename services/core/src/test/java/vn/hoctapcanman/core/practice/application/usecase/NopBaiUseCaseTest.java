package vn.hoctapcanman.core.practice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.service.ClassMembershipService;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.ClassSettingsRepositoryAdapter;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.EnrollmentRepositoryAdapter;
import vn.hoctapcanman.core.content.application.service.BaiDeLamService;
import vn.hoctapcanman.core.content.application.service.LoiGiaiSauKhiNopService;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.SolutionRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.TopicCatalogRepositoryAdapter;
import vn.hoctapcanman.core.practice.application.dto.BaiDaNop;
import vn.hoctapcanman.core.practice.application.dto.DongNop;
import vn.hoctapcanman.core.practice.application.dto.KetQuaBai;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBai;
import vn.hoctapcanman.core.practice.application.dto.NopBuocRequest;
import vn.hoctapcanman.core.practice.application.dto.ONop;
import vn.hoctapcanman.core.practice.application.dto.ThayDoiMucHieu;
import vn.hoctapcanman.core.practice.application.dto.ViTriSai;
import vn.hoctapcanman.core.practice.application.exception.BaiChuaNopDuocException;
import vn.hoctapcanman.core.practice.application.exception.BaiChuaNopDuocException.LyDo;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;
import vn.hoctapcanman.core.practice.application.port.CapNhatMucHieu;
import vn.hoctapcanman.core.practice.application.service.MoLoiGiai;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;
import vn.hoctapcanman.core.practice.infrastructure.persistence.GradingResultRepositoryAdapter;
import vn.hoctapcanman.core.practice.infrastructure.persistence.SubmissionRepositoryAdapter;
import vn.hoctapcanman.core.shared.infrastructure.ClockConfig;

/**
 * Nộp bài trên PostgreSQL 18 thật (T020 phần 2): kết quả là phán quyết đã ghi của bước kết luận trên nội dung hiện tại, ghim
 * làm căn cứ; thiếu bước hay không có phán quyết đó thì 409 và không ghi gì; gửi lại phát lại; không gọi dịch vụ toán; lời giải chỉ khi lớp
 * bật cờ, đúng đề, và học sinh không làm lại; mức hiểu gọi trong giao dịch nộp. Bước nộp qua {@link NopBuocUseCase} với máy
 * chấm giả. Use case tự mở giao dịch, nên test không chạy trong giao dịch của test và tự dọn dữ liệu.
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
    SolutionRepositoryAdapter.class,
    BaiDeLamService.class,
    LoiGiaiSauKhiNopService.class,
    SubmissionRepositoryAdapter.class,
    GradingResultRepositoryAdapter.class,
    NopBuocUseCaseTest.MayChamGia.class,
    NopBuocUseCase.class,
    MoLoiGiai.class,
    NopBaiUseCase.class
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NopBaiUseCaseTest {

    /** Lời giải mẫu của bài thử và chữ v0 viết cho học sinh từ nó (như tệp vàng loi-giai-v0.json). */
    private static final String LOI_GIAI_MAU = """
            {"TXD": "D = \\\\mathbb{R}", "ket_luan": {"dong_bien": ["(-oo; 0)", "(2; +oo)"], "nghich_bien": ["(0; 2)"]}}""";
    private static final String LOI_GIAI = "Tập xác định: D = \\mathbb{R}. Đồng biến trên (-oo; 0) và (2; +oo). Nghịch biến trên (0; 2).";
    private static final KetQuaNopBai DAT_KHONG_LOI_GIAI = new KetQuaNopBai(KetQuaBai.DAT, List.of(), null);

    @Autowired
    private NopBaiUseCase nopBai;

    @Autowired
    private NopBuocUseCase nopBuoc;

    @Autowired
    private NopBuocUseCaseTest.MayChamGia mayCham;

    @Autowired
    private MoLoiGiai moLoiGiai;

    @Autowired
    private ClassMembershipService membership;

    @Autowired
    private BaiDeLamService baiDeLam;

    @Autowired
    private SubmissionRepositoryAdapter submissions;

    @Autowired
    private GradingResultRepositoryAdapter grades;

    @Autowired
    private LoiGiaiSauKhiNopService loiGiaiCong;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private PlatformTransactionManager giaoDich;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcClient jdbc;

    private UUID lop;
    private UUID an;
    private UUID giaoVien;
    private UUID bai;
    private String ma;
    private final List<UUID> nguoi = new ArrayList<>();

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
        jdbc.sql("insert into solutions (problem_id, worked_solution, protected_facts) values (?, cast(? as jsonb), '[\"x = 0\"]')")
            .params(bai, LOI_GIAI_MAU).update();
        DuLieuPractice.phatHanh(jdbc, lop, bai);
        ma = jdbc.sql("select code from problems where id = ?").params(bai).query(String.class).single();
    }

    @AfterEach
    void don() {
        jdbc.sql("delete from classes where id = ?").params(lop).update();
        jdbc.sql("delete from problems where id = ?").params(bai).update();
        nguoi.forEach(id -> jdbc.sql("delete from users where id = ?").params(id).update());
    }

    @Test
    void nopSauKhiBuocKetLuanCoPhanQuyetGhimCanCuVaGuiLaiPhatLai() {
        lamDuBuoc();
        int lanCham = mayCham.yeuCau.size();
        KetQuaNopBai dau = nopBai.execute(an, lop, ma);
        assertThat(dau).isEqualTo(DAT_KHONG_LOI_GIAI);
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(dau);
        assertThat(mayCham.yeuCau).as("nộp bài không gọi dịch vụ toán").hasSize(lanCham);
        assertThat(jdbc.sql("""
                select s.status || ' ' || g.step_code || ' ' || g.result from submissions s join grading_results g on g.id = s.result_grading_id
                where s.class_id = ?""").params(lop).query(String.class).list()).containsExactly("DA_NOP B.DH.KETLUAN DAT");
    }

    @Test
    void chuaLamDuBuocThi409KhongGhiGi() {
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
        nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R"));
        nopBuoc.execute(an, lop, ma, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x"));
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
        lamTruocKetLuan();
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
        assertThat(trangThai()).containsExactly("DANG_LAM");
    }

    @Test
    void suaBuocSauKhiChamKetLuanThi409NopLaiKetLuanThiNopDuoc() {
        lamDuBuoc();
        nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = \\mathbb{R}"));
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_CHAM_BUOC_KET_LUAN);
        nopBuoc.execute(an, lop, ma, ketLuan());
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
    }

    /** Codex #142: chỉ nộp bước kết luận thì máy chấm ghi KHONG_KIEM_DUOC (thiếu dòng), nhưng đó không phải bài đã làm. */
    @Test
    void thieuBuocTruocKetLuanThi409KhongNopKhongMoLoiGiai() {
        datCo(true);
        mayCham.traLoi = NopBaiUseCaseTest::thieuDongTapXacDinh;
        nopBuoc.execute(an, lop, ma, ketLuan());
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
        assertThat(trangThai()).containsExactly("DANG_LAM");
    }

    /** Phán quyết #142 (vòng 2): dòng rỗng, dòng chỉ khoảng trắng, bảng không ô là chưa viết gì vào bước, không phải đã làm. */
    @Test
    void buocKhongCoChuKhongTinhLaDaLam() {
        datCo(true);
        mayCham.traLoi = NopBaiUseCaseTest::thieuDongTapXacDinh;
        nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", ""));
        nopBuoc.execute(an, lop, ma, dong("B.DH.DAOHAM", "   "));
        nopBuoc.execute(an, lop, ma, new NopBuocRequest("B.DH.NGHIEM", null, List.of(), null));
        nopBuoc.execute(an, lop, ma, new NopBuocRequest("B.DH.XETDAU", null, List.of(new ONop("DAU_YPHAY", 0, " ")), null));
        nopBuoc.execute(an, lop, ma, ketLuan());
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
        assertThat(trangThai()).containsExactly("DANG_LAM");
    }

    @Test
    void dichVuToanLoiOBuocKetLuanThi409() {
        lamTruocKetLuan();
        mayCham.traLoi = y -> Optional.empty();
        nopBuoc.execute(an, lop, ma, ketLuan());
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_CHAM_BUOC_KET_LUAN);
        assertThat(trangThai()).containsExactly("DANG_LAM");
    }

    @Test
    void ngoaiLopGiaoVienBaiChuaPhatHanhHayKhongCoNhuKhongCoBai() {
        nopBuoc.execute(an, lop, ma, ketLuan());
        UUID ngoai = nguoiMoi("STUDENT");
        UUID chuaPhatHanh = DuLieuPractice.bai(jdbc, "NB-NHAP");
        String maChua = jdbc.sql("select code from problems where id = ?").params(chuaPhatHanh).query(String.class).single();
        try {
            assertThatThrownBy(() -> nopBai.execute(ngoai, lop, ma)).isInstanceOf(BaiKhongTimThayException.class);
            assertThatThrownBy(() -> nopBai.execute(giaoVien, lop, ma)).isInstanceOf(BaiKhongTimThayException.class);
            assertThatThrownBy(() -> nopBai.execute(an, lop, maChua)).isInstanceOf(BaiKhongTimThayException.class);
            assertThatThrownBy(() -> nopBai.execute(an, lop, "KHONG-CO")).isInstanceOf(BaiKhongTimThayException.class);
            assertThat(trangThai()).containsExactly("DANG_LAM");
        } finally {
            jdbc.sql("delete from problems where id = ?").params(chuaPhatHanh).update();
        }
    }

    @Test
    void coLopDocLucGoiTatThiKhongCoLoiGiaiBatThiCoNhuV0() {
        lamDuBuoc();
        assertThat(nopBai.execute(an, lop, ma)).as("lớp chưa có dòng cài: tắt").isEqualTo(DAT_KHONG_LOI_GIAI);
        datCo(false);
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
        datCo(true);
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(new KetQuaNopBai(KetQuaBai.DAT, List.of(), LOI_GIAI));
        datCo(false);
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
    }

    @Test
    void lamLaiThiLoiGiaiDongVaNopLaiNhamBaiLamMoi() {
        datCo(true);
        lamDuBuoc();
        assertThat(nopBai.execute(an, lop, ma).loiGiai()).isEqualTo(LOI_GIAI);
        Submission.DaNop daNop = daNop();
        // Nộp một bước mở bài làm mới cùng phiên bản: em đang làm lại, lời giải đóng (FR-006).
        nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R"));
        assertThat(moLoiGiai.cho(daNop)).isEmpty();
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
        assertThat(trangThai()).containsExactly("DA_NOP", "DANG_LAM");
    }

    @Test
    void deDoiKhiDangLamThi409DeDaDoi() {
        nopBuoc.execute(an, lop, ma, ketLuan());
        doiDe();
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.DE_DA_DOI);
        assertThat(trangThai()).containsExactly("DANG_LAM");
    }

    /**
     * Phán quyết #142: đề đổi trong lúc nộp bài làm thứ hai chờ khóa dòng bài. Khóa bỏ qua bài làm đó (phiên bản cũ), nhưng lần
     * nộp trước ở phiên bản cũ không phải kết quả của nó: 409 DE_DA_DOI, không phát lại DAT của bài làm đầu.
     */
    @Test
    void deDoiTrongLucChoKhoaThi409DeDaDoiKhongPhatLaiLanNopCu() throws Exception {
        lamDuBuoc();
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
        nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R"));
        CountDownLatch daGhi = new CountDownLatch(1);
        CompletableFuture<Void> suaDe = CompletableFuture.runAsync(() -> tx.executeWithoutResult(s -> {
            jdbc.sql("update problems set content_hash = ? where id = ?").params("b".repeat(64), bai).update();
            daGhi.countDown();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }));
        assertThat(daGhi.await(20, TimeUnit.SECONDS)).isTrue();
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.DE_DA_DOI);
        suaDe.get(30, TimeUnit.SECONDS);
        assertThat(trangThai()).containsExactly("DA_NOP", "DANG_LAM");
    }

    /**
     * Phán quyết #142 (vòng 2): tab khác mở bài làm mới giữa lúc khóa không thấy bài làm đang làm và lúc đọc lịch sử. Đề không
     * đổi, nên không được báo DE_DA_DOI: kết quả phải như chạy tuần tự (bài làm mới chưa đủ bước).
     */
    @Test
    void lamLaiChenGiuaKhoaVaLichSuThiNopBaiLamMoiKhongBaoDeDoi() {
        lamDuBuoc();
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
        SubmissionRepository coChen = chen(submissions, "history", 1, false, () -> nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R")));
        NopBaiUseCase uc = new NopBaiUseCase(membership, baiDeLam, coChen, grades, List.of(), moLoiGiai, tx, clock);
        ketQua409(() -> uc.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
        assertThat(trangThai()).containsExactly("DA_NOP", "DANG_LAM");
    }

    /** Phán quyết #142 (vòng 3): tab khác mở và làm xong bài làm mới ở cùng chỗ chen: khóa lại thấy nó thì nộp nó. */
    @Test
    void khoaLaiThayBaiLamTabKhacVuaLamXongThiNopNo() {
        lamDuBuoc();
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
        NopBaiUseCase uc = new NopBaiUseCase(membership, baiDeLam, chen(submissions, "history", 1, false, this::lamDuBuoc), grades,
            List.of(), moLoiGiai, tx, clock);
        assertThat(uc.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
        assertThat(trangThai()).containsExactly("DA_NOP", "DA_NOP");
        assertThat(jdbc.sql("select count(distinct result_grading_id) from submissions where class_id = ?").params(lop)
            .query(Integer.class).single()).isEqualTo(2);
    }

    /**
     * Phán quyết #142 (vòng 3), bốn tác nhân: tab khác mở bài làm mới trước lúc đọc lịch sử, rồi làm xong và nộp nó trước lần
     * khóa lại. Đề không đổi: phát lại lần nộp mới nhất, không báo DE_DA_DOI (trước đây suy đề đổi từ «khóa trượt hai lần»).
     */
    @Test
    void tabKhacNopBaiLamGiuaHaiLanKhoaThiPhatLaiKhongBaoDeDoi() {
        lamDuBuoc();
        assertThat(nopBai.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
        SubmissionRepository moBaiLam = chen(submissions, "history", 1, false, () -> nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R")));
        SubmissionRepository nopBaiLam = chen(moBaiLam, "lockOpen", 2, false, () -> {
            lamDuBuoc();
            nopBai.execute(an, lop, ma);
        });
        int phienBan = phienBanBai();
        NopBaiUseCase uc = new NopBaiUseCase(membership, baiDeLam, nopBaiLam, grades, List.of(), moLoiGiai, tx, clock);
        assertThat(uc.execute(an, lop, ma)).isEqualTo(DAT_KHONG_LOI_GIAI);
        assertThat(phienBanBai()).isEqualTo(phienBan);
        assertThat(trangThai()).containsExactly("DA_NOP", "DA_NOP");
    }

    /**
     * Phán quyết #142 (vòng 2), ba tác nhân: sau khi {@code cho} thấy «không làm lại», học sinh mở bài làm mới rồi giáo viên bật
     * cờ. Đọc trong một ảnh chụp thì kết quả là của một thời điểm (chưa bật cờ): không có lời giải.
     */
    @Test
    void moLoiGiaiDocMotAnhChupKhongGhepLamLaiVaCoBatLucKhac() {
        lamDuBuoc();
        nopBai.execute(an, lop, ma);
        Submission.DaNop daNop = daNop();
        assertThat(new MoLoiGiai(lamLaiRoiBatCo(), membership, loiGiaiCong, giaoDich).cho(daNop)).isEmpty();
        assertThat(moLoiGiai.cho(daNop)).as("đang làm lại").isEmpty();
    }

    /**
     * Phán quyết #142 (vòng 3): gọi {@code cho} từ trong một giao dịch khác (READ COMMITTED) thì ảnh chụp vẫn là giao dịch riêng
     * REPEATABLE READ, không nhập vào giao dịch ngoài và lặng lẽ mất mức cách ly.
     */
    @Test
    void moLoiGiaiTrongGiaoDichNgoaiVanDocAnhChupRieng() {
        lamDuBuoc();
        nopBai.execute(an, lop, ma);
        Submission.DaNop daNop = daNop();
        MoLoiGiai coChen = new MoLoiGiai(lamLaiRoiBatCo(), membership, loiGiaiCong, giaoDich);
        Optional<String> loiGiai = tx.execute(s -> coChen.cho(daNop));
        assertThat(loiGiai).isEmpty();
    }

    /** Ngay sau lần đọc lịch sử đầu: học sinh mở bài làm mới, rồi giáo viên bật cờ. */
    private SubmissionRepository lamLaiRoiBatCo() {
        return chen(submissions, "history", 1, true, () -> {
            nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R"));
            datCo(true);
        });
    }

    @Test
    void deDoiSauKhiNopThiLoiGiaiCuDongVaKhongPhatLaiChoDeMoi() {
        datCo(true);
        lamDuBuoc();
        assertThat(nopBai.execute(an, lop, ma).loiGiai()).isEqualTo(LOI_GIAI);
        Submission.DaNop daNop = daNop();
        doiDe();
        assertThat(moLoiGiai.cho(daNop)).isEmpty();
        ketQua409(() -> nopBai.execute(an, lop, ma), LyDo.CHUA_LAM_DU_BUOC);
    }

    @Test
    void mucHieuNhanBaiDaNopTuCanCuVaPhatLaiGoiLaiCungDuKien() {
        lamTruocKetLuan();
        mayCham.traLoi = NopBaiUseCaseTest::saiOKetLuan;
        List<BaiDaNop> daNhan = new ArrayList<>();
        CapNhatMucHieu ghiLai = b -> {
            daNhan.add(b);
            return List.of(new ThayDoiMucHieu("T12.DH.02", "THONG_HIEU", "VAN_DUNG"));
        };
        NopBaiUseCase coMucHieu = new NopBaiUseCase(membership, baiDeLam, submissions, grades, List.of(ghiLai), moLoiGiai, tx, clock);
        nopBuoc.execute(an, lop, ma, ketLuan());
        KetQuaNopBai kq = coMucHieu.execute(an, lop, ma);
        assertThat(kq).isEqualTo(new KetQuaNopBai(KetQuaBai.SAI, List.of(new ThayDoiMucHieu("T12.DH.02", "THONG_HIEU", "VAN_DUNG")), null));
        Submission nop = daNop().baiLam();
        assertThat(daNhan).containsExactly(new BaiDaNop(nop.id(), an, lop, bai, "T12.DH.02", "THONG_HIEU", KetQuaBai.SAI,
            new ViTriSai("B.DH.KETLUAN", 1, null, null), "ERR.DH.06", 0.9, false, false, nop.submittedAt()));
        // Codex #142 (P2): sửa kỹ năng, mức của bài không đổi content_hash nên không tăng phiên bản; phát lại vẫn dựng y hệt.
        jdbc.sql("insert into skills (code, topic_code, name, is_core) values ('T12.DH.03', 'DH12', 'Cực trị', true) on conflict do nothing")
            .update();
        int phienBan = phienBanBai();
        jdbc.sql("update problems set skill_code = 'T12.DH.03', level4 = 'VAN_DUNG' where id = ?").params(bai).update();
        assertThat(phienBanBai()).isEqualTo(phienBan);
        assertThat(coMucHieu.execute(an, lop, ma)).isEqualTo(kq);
        assertThat(daNhan).hasSize(2).containsOnly(daNhan.getFirst());
    }

    @Test
    void mucHieuLoiThiBaiLamKhongNop() {
        CapNhatMucHieu loi = b -> {
            throw new IllegalStateException("mức hiểu lỗi");
        };
        NopBaiUseCase coLoi = new NopBaiUseCase(membership, baiDeLam, submissions, grades, List.of(loi), moLoiGiai, tx, clock);
        lamDuBuoc();
        assertThatThrownBy(() -> coLoi.execute(an, lop, ma)).hasMessage("mức hiểu lỗi");
        assertThat(trangThai()).containsExactly("DANG_LAM");
    }

    @Test
    void haiTabNopCungLucCungMotKetQua() throws Exception {
        lamDuBuoc();
        CompletableFuture<KetQuaNopBai> tab1 = CompletableFuture.supplyAsync(() -> nopBai.execute(an, lop, ma));
        CompletableFuture<KetQuaNopBai> tab2 = CompletableFuture.supplyAsync(() -> nopBai.execute(an, lop, ma));
        assertThat(List.of(tab1.get(30, TimeUnit.SECONDS), tab2.get(30, TimeUnit.SECONDS)))
            .containsExactly(DAT_KHONG_LOI_GIAI, DAT_KHONG_LOI_GIAI);
        assertThat(trangThai()).containsExactly("DA_NOP");
    }

    /**
     * Kho bài làm {@code goc} chạy {@code viec} ở luồng khác (giao dịch riêng, đã commit) ở lần gọi thứ {@code lan} của
     * {@code ham}: trước lần gọi đó, hay sau khi nó đã đọc ({@code sau}: ảnh chụp của giao dịch đang chạy đã chụp). Lồng được
     * để chèn ở nhiều chỗ.
     */
    private static SubmissionRepository chen(SubmissionRepository goc, String ham, int lan, boolean sau, Runnable viec) {
        AtomicInteger dem = new AtomicInteger();
        return (SubmissionRepository) Proxy.newProxyInstance(NopBaiUseCaseTest.class.getClassLoader(),
            new Class<?>[] {SubmissionRepository.class}, (p, m, a) -> {
                boolean dung = m.getName().equals(ham) && dem.incrementAndGet() == lan;
                if (dung && !sau) {
                    CompletableFuture.runAsync(viec).get(30, TimeUnit.SECONDS);
                }
                Object ra;
                try {
                    ra = m.invoke(goc, a);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
                if (dung && sau) {
                    CompletableFuture.runAsync(viec).get(30, TimeUnit.SECONDS);
                }
                return ra;
            });
    }

    private int phienBanBai() {
        return jdbc.sql("select content_version from problems where id = ?").params(bai).query(Integer.class).single();
    }

    private Submission.DaNop daNop() {
        return submissions.findLatest(an, lop, bai).flatMap(Submission::daNop).orElseThrow();
    }

    private List<String> trangThai() {
        return jdbc.sql("select status from submissions where class_id = ? order by started_at").params(lop).query(String.class).list();
    }

    private void datCo(boolean mo) {
        jdbc.sql("""
                insert into class_settings (class_id, reveal_solution_after_submit, updated_at) values (?, ?, now())
                on conflict (class_id) do update set reveal_solution_after_submit = excluded.reveal_solution_after_submit""")
            .params(lop, mo).update();
    }

    /** Sửa đề: phiên bản nội dung tăng, phát hành về NHAP; kiểm lại rồi phát hành phiên bản mới. */
    private void doiDe() {
        jdbc.sql("update problems set content_hash = ? where id = ?").params("b".repeat(64), bai).update();
        DuLieuPractice.phatHanh(jdbc, lop, bai);
    }

    private UUID nguoiMoi(String vaiTro) {
        UUID id = DuLieuPractice.nguoi(jdbc, vaiTro);
        nguoi.add(id);
        return id;
    }

    private static void ketQua409(ThrowingCallable nop, LyDo lyDo) {
        assertThatThrownBy(nop).isInstanceOfSatisfying(BaiChuaNopDuocException.class, e -> assertThat(e.lyDo()).isEqualTo(lyDo));
    }

    /** Bốn bước trước kết luận của khung 5 bước. */
    private void lamTruocKetLuan() {
        nopBuoc.execute(an, lop, ma, dong("B.DH.TXD", "D = R"));
        nopBuoc.execute(an, lop, ma, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x"));
        nopBuoc.execute(an, lop, ma, dong("B.DH.NGHIEM", "x = 0, x = 2"));
        nopBuoc.execute(an, lop, ma, new NopBuocRequest("B.DH.XETDAU", null,
            List.of(new ONop("X", 0, "0"), new ONop("DAU_YPHAY", 2, "+")), null));
    }

    private void lamDuBuoc() {
        lamTruocKetLuan();
        nopBuoc.execute(an, lop, ma, ketLuan());
    }

    private static NopBuocRequest dong(String maBuoc, String latex) {
        return new NopBuocRequest(maBuoc, List.of(new DongNop(0, latex, null)), null, null);
    }

    /** Bước kết luận đúng các ô đề hỏi (bài thử chỉ hỏi đơn điệu). */
    private static NopBuocRequest ketLuan() {
        return new NopBuocRequest("B.DH.KETLUAN", List.of(new DongNop(0, "(-\\infty; 0), (2; +\\infty)", "DONG_BIEN"),
            new DongNop(1, "(0; 2)", "NGHICH_BIEN")), null, null);
    }

    /** Phong bì của bộ chấm khi thiếu dòng tập xác định ({@code services/math/app/grader.py}, nhánh {@code thieu_dong}). */
    private static Optional<Map<String, @Nullable Object>> thieuDongTapXacDinh(Map<String, ?> y) {
        Map<String, @Nullable Object> p = new LinkedHashMap<>();
        p.put("cac_van_de", List.of());
        p.put("ket_qua", "KHONG_KIEM_DUOC");
        p.put("loai_ket_qua", "KHONG_KIEM_DUOC");
        p.put("buoc_sai", null);
        p.put("ma_loi", null);
        p.put("do_tin_cay", null);
        p.put("per_buoc", Map.of("B.DH.TXD", "KHONG_KIEM_DUOC"));
        p.put("chuan_hoa", List.of());
        p.put("phien_ban_chuan_hoa", "norm-0.2");
        p.put("thong_bao", "Chưa kiểm được bước Tập xác định.");
        p.put("chua_xong", false);
        p.put("nop_toi", y.get("nop_toi"));
        return Optional.of(p);
    }

    /** Phong bì SAI của bộ chấm ở bước kết luận, dòng 1 sai, lỗi toán (không phải dấu U). */
    private static Optional<Map<String, @Nullable Object>> saiOKetLuan(Map<String, ?> y) {
        Map<String, @Nullable Object> buocSai = new LinkedHashMap<>();
        buocSai.put("ma_buoc", "B.DH.KETLUAN");
        buocSai.put("dong", 1);
        buocSai.put("o", null);
        Map<String, @Nullable Object> p = new LinkedHashMap<>();
        p.put("ket_qua", "SAI");
        p.put("loai_ket_qua", "KHOANG_SAI");
        p.put("buoc_sai", buocSai);
        p.put("ma_loi", "ERR.DH.06");
        p.put("do_tin_cay", 0.9);
        p.put("per_buoc", Map.of("B.DH.KETLUAN", "SAI"));
        p.put("thong_bao", "Bước Kết luận, dòng 2 cần xem lại.");
        p.put("cac_van_de", List.of());
        p.put("chua_xong", false);
        p.put("chuan_hoa", List.of());
        p.put("phien_ban_chuan_hoa", "norm-0.2");
        p.put("nop_toi", y.get("nop_toi"));
        p.put("toan_dung", false);
        return Optional.of(p);
    }
}
