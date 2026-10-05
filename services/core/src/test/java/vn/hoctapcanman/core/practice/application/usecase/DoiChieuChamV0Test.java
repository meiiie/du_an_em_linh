package vn.hoctapcanman.core.practice.application.usecase;

import static java.util.Map.entry;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.service.ClassMembershipService;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.ClassSettingsRepositoryAdapter;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.EnrollmentRepositoryAdapter;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.content.application.service.BaiDeLamService;
import vn.hoctapcanman.core.content.infrastructure.nhap.NhapNoiDungChungTest;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.TopicCatalogRepositoryAdapter;
import vn.hoctapcanman.core.practice.application.dto.DongNop;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.dto.NopBuocRequest;
import vn.hoctapcanman.core.practice.application.dto.ONop;
import vn.hoctapcanman.core.practice.application.dto.ViTriSai;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;
import vn.hoctapcanman.core.practice.infrastructure.persistence.GradingResultRepositoryAdapter;
import vn.hoctapcanman.core.practice.infrastructure.persistence.SubmissionRepositoryAdapter;
import vn.hoctapcanman.core.shared.infrastructure.ClockConfig;

/**
 * SC-006 (T023): chấm từng bước trên toàn ngân hàng cho cùng kết quả như v0. Tệp vàng {@code cham-v0.json} do
 * {@code specs/001-lat-cat-doc/doi-chieu/cham-v0.ts} sinh bằng chính mã v0 (thân {@code SolveClient}, {@code nopBuoc},
 * {@code mathJob}, cổng trang làm bài) trên dịch vụ toán thật. Mỗi kịch bản là một học sinh mới nộp từng bước như lúc ghi;
 * máy chấm giả trả đúng phản hồi đã ghi. Mỗi lần nộp phải: gọi máy chấm đúng một lần; gửi yêu cầu trùng cây JSON rồi trùng
 * từng byte (SHA-256) với v0, và ghi đúng băm đó vào {@code grading_results.request_hash}; trả học sinh kết quả khớp phép chiếu
 * từ ngữ nghĩa giao diện v0 ({@link #KY_VONG}, không gọi mã của core); ghi hàng chấm khớp hàng v0 ghi ({@link #COT}).
 *
 * <p>Bảng kín hai phía: mọi khóa v0 trả ({@link #TRA_VE}) hay ghi, mọi cột {@code grading_results} (information_schema), mọi
 * thành phần {@link KetQuaNopBuoc} (phản chiếu record) đều phải được phân loại. Khác biệt có chủ đích khai ở
 * {@link #KHAC_BIET}: lệch mà không mục nào giải thích thì đỏ, mục mà không còn lệch hay không gặp lần nào cũng đỏ. Ngân hàng
 * nạp bằng SQL từ {@code v0-bai.json} (T014 đã đối chiếu nhập của core với mọi cột đó). Cùng ngữ cảnh Spring với
 * {@link NopBuocUseCaseTest} (cùng @Import, cùng máy chấm giả), nên dùng chung một PostgreSQL.
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
class DoiChieuChamV0Test {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Path DOI_CHIEU = NhapNoiDungChungTest.thuMucData().getParent().resolve("specs/001-lat-cat-doc/doi-chieu");
    private static final List<String> KHUNG = List.of("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN");
    private static final List<String> KET_QUA = List.of("DAT", "SAI", "KHONG_KIEM_DUOC");
    private static final String KHONG_MANG = "không mang cho học sinh: ";

    /** Số đếm không phụ thuộc bộ chấm (ngân hàng và điều kiện áp dụng của biến thể). Sinh lại mà đổi thì sửa ở đây. */
    private static final Dem DEM = new Dem(12, 5, 21, 187, 65);

    /** Khóa của {@code tra_ve} (giá trị {@code nopBuoc} của v0 trả máy học sinh) và chỗ so ở core. Khóa không có ở đây thì đỏ. */
    private static final Map<String, String> TRA_VE = Map.ofEntries(
        entry("ok", "ketQua khác KHONG_CHAM_DUOC"),
        entry("ket_qua", "ketQua"),
        entry("thong_bao", "thongBao"),
        entry("ma_loi", "maLoi"),
        entry("buoc_sai", "oSai"),
        entry("cac_van_de", "oSai: bước sai từng vấn đề, và dòng liên quan của vấn đề gốc khi SAI"),
        entry("finished", "ketQua DAT và buocKe null"),
        entry("loai_ket_qua", KHONG_MANG + "học sinh không thấy loại kết quả; so ở cột result_type"),
        entry("per_buoc", KHONG_MANG + "thanh bước v2 dựng từ lịch sử chấm; so ở cột per_step"),
        entry("toan_dung", KHONG_MANG + "cờ dấu U cho mức hiểu (T050); so ở cột math_ok"));

    /**
     * Mỗi thành phần của {@link KetQuaNopBuoc} và kỳ vọng dựng từ v0. {@code oSai} so như tập: v0 tô bước sai gốc, bước sai
     * của mọi vấn đề (ô hệ quả nét đứt), và dòng {@code dong_lien_quan} của vấn đề gốc khi SAI ({@code lineBad} của
     * {@code solve-client.tsx}). Phần mở dần (chỉ vấn đề gốc đầu, «còn N chỗ») là việc của giao diện, không của API.
     */
    private static final Map<String, BiFunction<LanCham, LanNop, @Nullable Object>> KY_VONG = Map.of(
        "ketQua", (b, ln) -> chu(b.traVe(), "ket_qua"),
        "thongBao", (b, ln) -> chu(b.traVe(), "thong_bao"),
        "oSai", (b, ln) -> oSaiV0(b.traVe()),
        "maLoi", (b, ln) -> chu(b.traVe(), "ma_loi"),
        "buocKe", (b, ln) -> ln.buocSau());

    /** Mọi cột {@code grading_results} của core và cách so. Cột không có ở đây thì đỏ; khóa {@code ghi} của v0 cũng vậy. */
    private static final Map<String, CachSo> COT = Map.ofEntries(
        entry("result", CachSo.ghi("ketQua")),
        entry("result_type", CachSo.ghi("loaiKetQua")),
        entry("wrong_steps", CachSo.ghi("buocSai")),
        entry("error_code", CachSo.ghi("maLoi")),
        entry("confidence", CachSo.ghi("doTinCay")),
        entry("per_step", CachSo.ghi("perBuoc")),
        entry("message", CachSo.ghi("thongBao")),
        entry("issues", CachSo.ghi("cacVanDe")),
        entry("math_ok", CachSo.ghi("toanDung")),
        entry("request_hash", CachSo.tu("SHA-256 thân yêu cầu v0 gửi (bam)", (b, ln) -> ln.bam())),
        entry("step_code", CachSo.tu("nop_toi của yêu cầu", (b, ln) -> b.yeuCau().get("nop_toi"))),
        entry("unfinished", CachSo.tu("chua_xong của phản hồi; v0 không lưu", (b, ln) -> b.phanHoi().get("chua_xong"))),
        entry("normalizer_version", CachSo.tu("phien_ban_chuan_hoa của phản hồi; v0 lưu ở submission_steps",
            (b, ln) -> b.phanHoi().get("phien_ban_chuan_hoa"))),
        entry("normalization", CachSo.tu("chuan_hoa của phản hồi; v0 lưu từng dòng ở submission_steps",
            (b, ln) -> b.phanHoi().get("chuan_hoa"))),
        entry("id", CachSo.khong("khóa của core")),
        entry("submission_id", CachSo.khong("bài làm của core; v0 tạo một submissions mỗi lần nộp")),
        entry("graded_at", CachSo.khong("thời điểm chấm")));

    /** Khác biệt có chủ đích giữa core và v0: mỗi mục kèm điều kiện, giá trị core phải trả và lý do. */
    private static final List<KhacBiet> KHAC_BIET = List.of();

    @Autowired
    private NopBuocUseCase nopBuoc;

    @Autowired
    private NopBuocUseCaseTest.MayChamGia mayCham;

    @Autowired
    private BaiDeLam baiDeLam;

    @Autowired
    private JdbcClient jdbc;

    private final List<UUID> nguoi = new ArrayList<>();
    private final List<UUID> bai = new ArrayList<>();
    private @Nullable UUID lop;

    @TestFactory
    Stream<DynamicTest> chamToanNganHangNhuV0() {
        mayCham.datLai();
        TepVang vang = TepVang.doc(DOI_CHIEU.resolve("cham-v0.json"));
        List<BaiV0> v0 = BaiV0.doc(DOI_CHIEU.resolve("v0-bai.json"));
        UUID l = DuLieuPractice.lop(jdbc);
        lop = l;
        Map<String, String> maCore = napNganHang(v0, l);
        Map<String, String> kieuCot = new LinkedHashMap<>();
        jdbc.sql("""
                select column_name, data_type from information_schema.columns
                where table_schema = current_schema() and table_name = 'grading_results' order by ordinal_position""")
            .query((rs, n) -> entry(rs.getString(1), rs.getString(2))).list().forEach(e -> kieuCot.put(e.getKey(), e.getValue()));
        Map<String, Integer> gap = new HashMap<>();
        int[] xong = {0};
        return Stream.of(
            Stream.of(
                dynamicTest("tệp vàng đủ bộ ca", () -> kiemBoCa(vang, v0)),
                dynamicTest("bảng kín: khóa v0, cột grading_results, thành phần KetQuaNopBuoc", () -> kiemBangKin(vang, kieuCot)),
                dynamicTest("sổ khác biệt tự kiểm", DoiChieuChamV0Test::soKhacBietTuKiem),
                dynamicTest("bài core cho chấm đúng là bài cổng v0 cho làm", () -> kiemCong(vang, maCore, l))),
            vang.chay().map(kb -> dynamicTest(kb.ma() + " / " + kb.bienThe(), () -> {
                chay(kb, vang, Objects.requireNonNull(maCore.get(kb.ma())), l, kieuCot, gap);
                xong[0]++;
            })),
            Stream.of(dynamicTest("mọi khác biệt có chủ đích vẫn còn", () -> {
                assumeTrue(xong[0] == DEM.chay(), "chỉ kiểm khi mọi kịch bản đã chạy xanh");
                assertThat(mucChet(KHAC_BIET, gap)).as("mục KHAC_BIET không gặp lần nào: xóa").isEmpty();
            })))
            .flatMap(Function.identity());
    }

    @AfterEach
    void don() {
        if (lop != null) {
            jdbc.sql("delete from classes where id = ?").params(lop).update();
        }
        bai.forEach(id -> jdbc.sql("delete from problems where id = ?").params(id).update());
        nguoi.forEach(id -> jdbc.sql("delete from users where id = ?").params(id).update());
    }

    /** 17 bài của v0-bài.json; bài v0 đã phát hành thì phát hành ở lớp. Trả mã v0 → mã trong CSDL. */
    private Map<String, String> napNganHang(List<BaiV0> v0, UUID l) {
        DuLieuPractice.danhMuc(jdbc);
        Map<String, String> maCore = new LinkedHashMap<>();
        for (BaiV0 b : v0) {
            DuLieuPractice.kyNang(jdbc, b.kyNang());
            DuLieuPractice.BaiDaGhi g = DuLieuPractice.baiV0(jdbc, b.ma(), b.kyNang(), b.muc4(), b.de(), b.deLatex(), b.ham(),
                b.dangTraLoi(), b.buocBatDau(), b.nguon());
            bai.add(g.id());
            maCore.put(b.ma(), g.ma());
            if ("DA_PHAT_HANH".equals(b.trangThaiPhatHanh())) {
                DuLieuPractice.phatHanh(jdbc, l, g.id());
            }
        }
        return maCore;
    }

    private UUID hocSinhMoi(UUID l) {
        UUID hs = DuLieuPractice.nguoi(jdbc, "STUDENT");
        nguoi.add(hs);
        DuLieuPractice.ghiDanh(jdbc, l, hs, "STUDENT");
        return hs;
    }

    /** DEM, tích chéo bài × biến thể, mỗi biến thể chạy ít nhất một bài, sàn DAT/SAI/KHONG_KIEM_DUOC theo bước, lời giải mẫu đạt trọn bài. */
    private static void kiemBoCa(TepVang vang, List<BaiV0> v0) {
        Set<String> baiCham = vang.kichBan().stream().map(KichBan::ma).collect(toCollection(TreeSet::new));
        List<KichBan.Chay> chay = vang.chay().toList();
        assertThat(new Dem(baiCham.size(), vang.khongCham().size(), vang.bienThe().size(), chay.size(), vang.kichBan().size() - chay.size()))
            .as("số đếm của tệp vàng (DEM)").isEqualTo(DEM);
        assertThat(vang.kichBan().stream().map(k -> k.ma() + " / " + k.bienThe()))
            .as("mỗi (bài, biến thể) đúng một kịch bản").doesNotHaveDuplicates().hasSize(baiCham.size() * vang.bienThe().size());
        assertThat(vang.bienThe()).as("biến thể nào cũng chạy ít nhất một bài")
            .allSatisfy(bt -> assertThat(chay).anyMatch(k -> k.bienThe().equals(bt)));
        Set<String> moi = new TreeSet<>(baiCham);
        vang.khongCham().forEach(k -> assertThat(moi.add(k.ma())).as("bài vừa chấm được vừa không: %s", k.ma()).isTrue());
        assertThat(moi).as("bài chấm được ∪ bài không chấm = ngân hàng v0").isEqualTo(v0.stream().map(BaiV0::ma).collect(toSet()));
        List<LanNop> nop = chay.stream().flatMap(k -> k.lanNop().stream()).toList();
        for (String buoc : KHUNG) {
            for (String kq : KET_QUA) {
                assertThat(nop).as("có lần nộp %s %s", buoc, kq).anyMatch(n -> n.nopToi().equals(buoc) && n.ketQua().equals(kq));
            }
        }
        for (BaiV0 b : v0) {
            chay.stream().filter(k -> k.ma().equals(b.ma()) && k.bienThe().equals("dung")).findFirst().ifPresent(k -> {
                assertThat(k.lanNop()).as("%s: lời giải mẫu DAT từng bước", b.ma())
                    .hasSize(KHUNG.size() - (b.buocBatDau() == null ? 0 : KHUNG.indexOf(b.buocBatDau())))
                    .allMatch(n -> n.ketQua().equals("DAT"));
                assertThat(vang.lanCham().get(k.lanNop().getLast().bam()).traVe().get("finished").booleanValue())
                    .as("%s: lời giải mẫu xong bài", b.ma()).isTrue();
            });
        }
    }

    private static void kiemBangKin(TepVang vang, Map<String, String> kieuCot) {
        Set<String> khoaGhi = COT.values().stream().map(CachSo::khoaGhi).filter(Objects::nonNull).collect(toSet());
        for (LanCham b : vang.lanCham().values()) {
            assertThat(b.traVe().propertyNames()).as("khóa tra_ve của v0 phải có trong TRA_VE (so hay không mang, kèm lý do)")
                .containsExactlyInAnyOrderElementsOf(TRA_VE.keySet());
            assertThat(b.ghi().propertyNames()).as("khóa ghi của v0 phải có cột so trong COT")
                .containsExactlyInAnyOrderElementsOf(khoaGhi);
        }
        assertThat(kieuCot.keySet()).as("cột grading_results phải có trong COT (so với v0, hay không so kèm lý do)")
            .containsExactlyInAnyOrderElementsOf(COT.keySet());
        assertThat(Arrays.stream(KetQuaNopBuoc.class.getRecordComponents()).map(RecordComponent::getName))
            .as("thành phần KetQuaNopBuoc phải có kỳ vọng từ v0 trong KY_VONG").containsExactlyInAnyOrderElementsOf(KY_VONG.keySet());
    }

    /**
     * Tập bài core cho chấm từng bước ({@link BaiDeLam} có hàm) bằng tập bài cổng trang v0 cho làm; bài v0 không cho làm thì
     * nộp ở core như không có bài, không gọi máy chấm.
     */
    private void kiemCong(TepVang vang, Map<String, String> maCore, UUID l) {
        Set<String> core = maCore.entrySet().stream()
            .filter(e -> baiDeLam.bai(l, e.getValue()).filter(b -> b.ham() != null).isPresent())
            .map(Map.Entry::getKey).collect(toSet());
        assertThat(core).as("bài core cho chấm").isEqualTo(vang.kichBan().stream().map(KichBan::ma).collect(toSet()));
        UUID hs = hocSinhMoi(l);
        for (KhongCham k : vang.khongCham()) {
            int truoc = mayCham.yeuCau.size();
            NopBuocRequest txd = new NopBuocRequest("B.DH.TXD", List.of(new DongNop(0, "\\mathbb{R}", null)), null, List.of());
            assertThatThrownBy(() -> nopBuoc.execute(hs, l, Objects.requireNonNull(maCore.get(k.ma())), txd))
                .as("%s (%s, %s)", k.ma(), k.trangThai(), k.dangTraLoi()).isInstanceOf(BaiKhongTimThayException.class);
            assertThat(mayCham.yeuCau).as("%s: không gọi máy chấm", k.ma()).hasSize(truoc);
        }
    }

    /** Một học sinh mới nộp lần lượt các lần nộp của kịch bản; mỗi lần so yêu cầu, kết quả cho học sinh và hàng chấm. */
    private void chay(KichBan.Chay kb, TepVang vang, String maBai, UUID l, Map<String, String> kieuCot, Map<String, Integer> gap) {
        UUID hs = hocSinhMoi(l);
        for (int i = 0; i < kb.lanNop().size(); i++) {
            LanNop ln = kb.lanNop().get(i);
            String noi = "%s / %s, lần nộp %d (%s)".formatted(kb.ma(), kb.bienThe(), i + 1, ln.nopToi());
            LanCham b = vang.lanCham().get(ln.bam());
            assertThat(b).as("%s: bam có trong lan_cham", noi).isNotNull();
            assertThat(chu(b.yeuCau(), "nop_toi")).as(noi).isEqualTo(ln.nopToi());
            assertThat(chu(b.traVe(), "ket_qua")).as(noi).isEqualTo(ln.ketQua());
            int truoc = mayCham.yeuCau.size();
            mayCham.traLoi = y -> Optional.of(banSao(b.phanHoi()));
            KetQuaNopBuoc thuc = nopBuoc.execute(hs, l, maBai, b.buocNop());
            assertThat(mayCham.yeuCau).as("%s: gọi máy chấm đúng một lần", noi).hasSize(truoc + 1);
            byte[] guiDi = JSON.writeValueAsBytes(mayCham.yeuCau.getLast());
            assertThat(JSON.readTree(guiDi)).as("%s: yêu cầu chấm như v0", noi).isEqualTo(b.yeuCau());
            assertThat(sha256(guiDi)).as("%s: cùng cây mà khác byte (thứ tự khóa, thoát ký tự, số)", noi).isEqualTo(ln.bam());
            List<String> loi = lechChuaGiaiThich(KHAC_BIET, b, so(thuc, b, ln, hangCham(hs, ln.bam(), kieuCot), kieuCot), gap);
            if (!loi.isEmpty()) {
                fail(noi + "\n  " + String.join("\n  ", loi));
            }
        }
    }

    /** Hàng grading_results của học sinh có request_hash = bam: đúng một hàng, nghĩa là băm của core trùng băm v0. */
    private Map<String, @Nullable Object> hangCham(UUID hs, String bam, Map<String, String> kieuCot) {
        String cot = kieuCot.entrySet().stream()
            .map(e -> "jsonb".equals(e.getValue()) ? "g." + e.getKey() + "::text as " + e.getKey() : "g." + e.getKey())
            .collect(joining(", "));
        List<Map<String, @Nullable Object>> hang = jdbc.sql("select " + cot
                + " from grading_results g join submissions s on s.id = g.submission_id where s.student_id = ? and g.request_hash = ?")
            .params(hs, bam).query().listOfRows();
        assertThat(hang).as("hàng grading_results có request_hash = bam của v0").hasSize(1);
        return hang.getFirst();
    }

    /** Mọi phép so của một lần nộp: thành phần KetQuaNopBuoc, hai giá trị suy ra, mọi cột so của hàng chấm. */
    private static List<So> so(KetQuaNopBuoc thuc, LanCham b, LanNop ln, Map<String, @Nullable Object> hang, Map<String, String> kieuCot) {
        List<So> so = new ArrayList<>();
        for (RecordComponent c : KetQuaNopBuoc.class.getRecordComponents()) {
            Object core;
            try {
                core = c.getAccessor().invoke(thuc);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            BiFunction<LanCham, LanNop, @Nullable Object> kyVong = Objects.requireNonNull(KY_VONG.get(c.getName()), c.getName());
            so.add(new So(c.getName(), kyVong.apply(b, ln), core instanceof List<?> ds ? Set.copyOf(ds) : core));
        }
        so.add(new So("finished", b.traVe().get("finished").booleanValue(), "DAT".equals(thuc.ketQua()) && thuc.buocKe() == null));
        so.add(new So("ok", b.traVe().get("ok").booleanValue(), !"KHONG_CHAM_DUOC".equals(thuc.ketQua())));
        COT.forEach((cot, cach) -> {
            if (cach.kyVong() != null) {
                String kieu = Objects.requireNonNull(kieuCot.get(cot), cot);
                so.add(new So("grading_results." + cot, nut(cach.kyVong().apply(b, ln), kieu), nut(hang.get(cot), kieu)));
            }
        });
        return so;
    }

    /**
     * Lệch không mục nào của sổ giải thích. Lệch mà đúng một mục (cùng trường, điều kiện đúng, giá trị core đúng như mục ghi)
     * thì ghi một lần gặp; bằng nhau mà vẫn có mục đúng điều kiện thì mục đó đã hết.
     */
    private static List<String> lechChuaGiaiThich(List<KhacBiet> so, LanCham b, List<So> cacSo, Map<String, Integer> gap) {
        List<String> loi = new ArrayList<>();
        for (So s : cacSo) {
            List<KhacBiet> ap = so.stream().filter(k -> k.truong().equals(s.truong()) && k.khi().test(b)).toList();
            if (Objects.equals(s.v0(), s.core())) {
                ap.forEach(k -> loi.add("khác biệt " + k.ma() + " đã hết ở " + s.truong() + ": xóa mục"));
                continue;
            }
            List<KhacBiet> khop = ap.stream().filter(k -> Objects.equals(k.coreThay().apply(b), s.core())).toList();
            if (khop.size() == 1) {
                gap.merge(khop.getFirst().ma(), 1, Integer::sum);
            } else {
                loi.add(s.truong() + ": v0 " + s.v0() + ", core " + s.core());
            }
        }
        return loi;
    }

    private static List<String> mucChet(List<KhacBiet> so, Map<String, Integer> gap) {
        return so.stream().map(KhacBiet::ma).filter(ma -> !gap.containsKey(ma)).toList();
    }

    /** Sổ khác biệt với một mục thử: lệch được giải thích, lệch lạ, mục đã hết, mục chết. */
    private static void soKhacBietTuKiem() {
        LanCham b = new LanCham(NullNode.getInstance(), NullNode.getInstance(), NullNode.getInstance(), NullNode.getInstance());
        List<KhacBiet> so = List.of(new KhacBiet("thu", "x", c -> true, c -> "core", "thử"),
            new KhacBiet("chet", "z", c -> true, c -> "core", "thử"));
        Map<String, Integer> gap = new HashMap<>();
        assertThat(lechChuaGiaiThich(so, b, List.of(new So("x", "v0", "core")), gap)).isEmpty();
        assertThat(gap).isEqualTo(Map.of("thu", 1));
        assertThat(lechChuaGiaiThich(so, b, List.of(new So("x", "v0", "khac")), gap)).containsExactly("x: v0 v0, core khac");
        assertThat(lechChuaGiaiThich(so, b, List.of(new So("y", "v0", "core")), gap)).containsExactly("y: v0 v0, core core");
        assertThat(lechChuaGiaiThich(so, b, List.of(new So("x", "a", "a")), gap)).containsExactly("khác biệt thu đã hết ở x: xóa mục");
        assertThat(mucChet(so, gap)).containsExactly("chet");
    }

    /** Chỗ v0 tô cho lần chấm (xem {@link #KY_VONG}). */
    private static Set<ViTriSai> oSaiV0(JsonNode traVe) {
        Set<ViTriSai> o = new HashSet<>();
        viTriV0(traVe.get("buoc_sai")).ifPresent(o::add);
        boolean sai = "SAI".equals(chu(traVe, "ket_qua"));
        for (JsonNode v : traVe.get("cac_van_de")) {
            viTriV0(v.get("buoc_sai")).ifPresent(o::add);
            JsonNode lienQuan = v.path("dong_lien_quan");
            if (sai && v.path("nguyen_nhan").asString("").isEmpty() && lienQuan.isInt()) {
                o.add(new ViTriSai(v.get("buoc_sai").get("ma_buoc").stringValue(), lienQuan.intValue(), null, null));
            }
        }
        return o;
    }

    private static Optional<ViTriSai> viTriV0(@Nullable JsonNode b) {
        if (b == null || b.isNull()) {
            return Optional.empty();
        }
        JsonNode o = b.path("o");
        return Optional.of(new ViTriSai(b.get("ma_buoc").stringValue(), nguyen(b.path("dong")), o.isObject() ? chu(o, "hang") : null,
            o.isObject() ? nguyen(o.path("k")) : null));
    }

    private static @Nullable Integer nguyen(JsonNode n) {
        return n.isInt() ? n.intValue() : null;
    }

    private static @Nullable String chu(JsonNode n, String khoa) {
        JsonNode v = n.get(khoa);
        return v == null || v.isNull() ? null : v.stringValue();
    }

    /** Giá trị đưa về JsonNode theo kiểu cột: jsonb đọc như cây, real so như float (v0 và core đều lưu float4). */
    private static JsonNode nut(@Nullable Object v, String kieu) {
        JsonNode n = v == null ? NullNode.getInstance()
            : v instanceof JsonNode j ? j
            : "jsonb".equals(kieu) ? JSON.readTree((String) v)
            : JSON.valueToTree(v);
        return "real".equals(kieu) && n.isNumber() ? JsonNodeFactory.instance.numberNode(n.floatValue()) : n;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> banSao(JsonNode phanHoi) {
        return JSON.convertValue(phanHoi, LinkedHashMap.class);
    }

    private static String sha256(byte[] du) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(du));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode docJson(Path tep) {
        try {
            return JSON.readTree(Files.readString(tep));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record Dem(int baiCham, int baiKhongCham, int bienThe, int chay, int boQua) {}

    /** Cách so một cột: với khóa {@code ghi} của v0, với một giá trị khác của tệp vàng, hay không so (kèm lý do). */
    record CachSo(String moTa, @Nullable String khoaGhi, @Nullable BiFunction<LanCham, LanNop, @Nullable Object> kyVong) {
        static CachSo ghi(String khoa) {
            return new CachSo("ghi." + khoa + " của v0", khoa, (b, ln) -> b.ghi().get(khoa));
        }

        static CachSo tu(String moTa, BiFunction<LanCham, LanNop, @Nullable Object> kyVong) {
            return new CachSo(moTa, null, kyVong);
        }

        static CachSo khong(String lyDo) {
            return new CachSo(lyDo, null, null);
        }
    }

    record KhacBiet(String ma, String truong, Predicate<LanCham> khi, Function<LanCham, @Nullable Object> coreThay, String lyDo) {}

    record So(String truong, @Nullable Object v0, @Nullable Object core) {}

    /** Tệp vàng đã đọc qua ranh giới: dữ liệu có kiểu, phần còn lại của test tin nó. */
    record TepVang(List<String> bienThe, List<KhongCham> khongCham, List<KichBan> kichBan, Map<String, LanCham> lanCham) {

        static TepVang doc(Path tep) {
            JsonNode g = docJson(tep);
            List<KichBan> kichBan = g.get("kich_ban").valueStream().<KichBan>map(k -> {
                String ma = k.get("ma").stringValue();
                String bt = k.get("bien_the").stringValue();
                return k.has("bo_qua") ? new KichBan.BoQua(ma, bt, k.get("bo_qua").stringValue())
                    : new KichBan.Chay(ma, bt, k.get("lan_nop").valueStream()
                        .map(n -> new LanNop(n.get("bam").stringValue(), n.get("nop_toi").stringValue(), n.get("ket_qua").stringValue(),
                            chu(n, "buoc_sau")))
                        .toList());
            }).toList();
            Map<String, LanCham> lanCham = new LinkedHashMap<>();
            g.get("lan_cham").forEachEntry((bam, c) -> lanCham.put(bam,
                new LanCham(c.get("yeu_cau"), c.get("phan_hoi"), c.get("tra_ve"), c.get("ghi"))));
            return new TepVang(
                g.get("bien_the").valueStream().map(b -> b.get("ma").stringValue()).toList(),
                g.get("khong_cham").valueStream()
                    .map(k -> new KhongCham(k.get("ma").stringValue(), k.get("trang_thai").stringValue(), chu(k, "dang_tra_loi")))
                    .toList(),
                kichBan, lanCham);
        }

        Stream<KichBan.Chay> chay() {
            return kichBan.stream().filter(KichBan.Chay.class::isInstance).map(KichBan.Chay.class::cast);
        }
    }

    record KhongCham(String ma, String trangThai, @Nullable String dangTraLoi) {}

    /** Kịch bản chạy có ít nhất một lần nộp; kịch bản bỏ qua chỉ có lý do. */
    sealed interface KichBan permits KichBan.Chay, KichBan.BoQua {
        String ma();

        String bienThe();

        record Chay(String ma, String bienThe, List<LanNop> lanNop) implements KichBan {
            public Chay {
                if (lanNop.isEmpty()) {
                    throw new IllegalArgumentException("kịch bản chạy không có lần nộp: " + ma + " / " + bienThe);
                }
            }
        }

        record BoQua(String ma, String bienThe, String lyDo) implements KichBan {}
    }

    record LanNop(String bam, String nopToi, String ketQua, @Nullable String buocSau) {}

    record LanCham(JsonNode yeuCau, JsonNode phanHoi, JsonNode traVe, JsonNode ghi) {

        /** Bước học sinh nộp lần này: mục cuối của cac_buoc, đúng như máy học sinh v0 gửi (cham-v0.ts kiểm lúc sinh). */
        NopBuocRequest buocNop() {
            JsonNode s = yeuCau.get("cac_buoc").get(yeuCau.get("cac_buoc").size() - 1);
            String ma = s.get("ma_buoc").stringValue();
            if (!ma.equals(chu(yeuCau, "nop_toi"))) {
                throw new IllegalStateException("bước cuối của yêu cầu không phải bước nộp: " + ma);
            }
            List<DongNop> dong = s.has("cac_dong") ? s.get("cac_dong").valueStream()
                .map(d -> new DongNop(d.get("dong").intValue(), d.get("latex").stringValue(), chu(d, "loai"))).toList() : null;
            List<ONop> bang = s.has("bang") ? s.get("bang").get("cac_o").valueStream()
                .map(o -> new ONop(o.get("hang").stringValue(), nguyen(o.path("k")), o.get("gia_tri").stringValue())).toList() : null;
            return new NopBuocRequest(ma, dong, bang, List.of());
        }
    }

    /** Một bài của v0-bai.json với các cột core đọc khi chấm. */
    record BaiV0(String ma, String trangThaiPhatHanh, String dangTraLoi, String nguon, String kyNang, String muc4, String de,
            String deLatex, @Nullable String ham, @Nullable String buocBatDau) {

        static List<BaiV0> doc(Path tep) {
            return docJson(tep).get("bai").valueStream().map(b -> {
                JsonNode c = b.get("cot_v0");
                return new BaiV0(b.get("ma").stringValue(), b.get("trang_thai_phat_hanh").stringValue(), b.get("dang_tra_loi").stringValue(),
                    b.get("nguon_bai").stringValue(), c.get("skillCode").stringValue(), c.get("mucDo4").stringValue(),
                    c.get("statementText").stringValue(), c.get("statementLatex").stringValue(), chu(c, "hamSympy"), chu(c, "buocBatDau"));
            }).toList();
        }
    }
}
