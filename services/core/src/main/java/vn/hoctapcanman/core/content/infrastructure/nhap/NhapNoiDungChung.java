package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.hoctapcanman.core.content.domain.event.BaiDaNhap;
import vn.hoctapcanman.core.content.domain.model.BloomLevel;
import vn.hoctapcanman.core.content.domain.model.ErrorType;
import vn.hoctapcanman.core.content.domain.model.HintLevel;
import vn.hoctapcanman.core.content.domain.model.InputKind;
import vn.hoctapcanman.core.content.domain.model.Level3;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.SkillPrerequisite;
import vn.hoctapcanman.core.content.domain.model.Solution;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.model.Topic;
import vn.hoctapcanman.core.content.domain.repository.HintLevelRepository;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;
import vn.hoctapcanman.core.content.domain.repository.SolutionRepository;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

/**
 * Nhập nội dung chung của chủ đề (T012a, #85): chủ đề, kỹ năng và tiên quyết, mã lỗi, khung bước, bài cùng lời giải và
 * thang gợi ý, đúng nguồn và cách dựng của {@code apps/web/scripts/seed.ts}:
 * <ul>
 *   <li>bài ví dụ của lab Sư phạm ({@code 03-vi-du-bai-tap.json}): hàm máy giải được thì giải bằng {@code /v1/solve};</li>
 *   <li>hai bài máy giải ({@code DH12-NB-01}, {@code DH12-TH-02}), ba biến thể {@code /v1/generate} hạt giống cố định;</li>
 *   <li>tám bài khung ngắn ({@code bai-khung-ngan.seed-v01.json}) và bài ví dụ cổng chặn {@code DH12-DEMO-CHAN-01}.</li>
 * </ul>
 * Gọi dịch vụ toán trước, ghi sau, cả lần ghi trong một giao dịch: dịch vụ toán lỗi giữa chừng thì không ghi gì (khác
 * seed của v0, #119). Chạy lại không nhân bản: bài nhận theo mã, giữ id và thời điểm tạo. Sau khi ghi, mỗi bài phát
 * {@link BaiDaNhap}. Nội dung theo lớp (tài liệu, bảng công thức, kiểm 3 tầng, phát hành) ở {@link NhapTheoLop} (T012b),
 * kiểm đúng các bài vừa dựng ({@link DaNhap#bai()}).
 */
@Component
public class NhapNoiDungChung {

    static final String CHU_DE = "DH12";

    /** Nguồn bài ({@code problems.origin}) do importer này tạo: chỉ các bài này mới bị importer rút phát hành khi rời nguồn. */
    static final Set<String> NGUON_NHAP = Set.of("SUPHAM", "MAY_GIAI", "THAM_SO_HOA", "SUPHAM_KHUNG_NGAN", "VI_DU_CONG");

    private static final Logger LOG = LoggerFactory.getLogger(NhapNoiDungChung.class);

    /** Mã mức của lab và của v0 → 4 mức (bảng {@code MUC4} của {@code seed.ts}). */
    private static final Map<String, String> MUC4 = Map.of("NB", "NHAN_BIET", "TH", "THONG_HIEU", "VD", "VAN_DUNG",
        "VDC", "VAN_DUNG_CAO", "NHAN_BIET", "NHAN_BIET", "THONG_HIEU", "THONG_HIEU", "VAN_DUNG", "VAN_DUNG",
        "VAN_DUNG_CAO", "VAN_DUNG_CAO");
    /** 4 mức → 3 mức CV 7991 khi lab không ghi (bảng {@code MUC3} của {@code seed.ts}; không có khóa VAN_DUNG_CAO). */
    private static final Map<String, String> MUC3 = Map.of("NB", "BIET", "TH", "HIEU", "VD", "VAN_DUNG", "VDC", "VAN_DUNG",
        "BIET", "BIET", "HIEU", "HIEU", "VAN_DUNG", "VAN_DUNG");

    private final NguonNoiDung nguon;
    private final GiaiToan toan;
    private final TopicCatalogRepository catalog;
    private final ProblemRepository problems;
    private final SolutionRepository solutions;
    private final HintLevelRepository hints;
    private final TransactionTemplate giaoDich;
    private final ApplicationEventPublisher su;
    private final Clock clock;

    public NhapNoiDungChung(NguonNoiDung nguon, GiaiToan toan, TopicCatalogRepository catalog, ProblemRepository problems,
            SolutionRepository solutions, HintLevelRepository hints, PlatformTransactionManager tx, ApplicationEventPublisher su,
            Clock clock) {
        this.nguon = nguon;
        this.toan = toan;
        this.catalog = catalog;
        this.problems = problems;
        this.solutions = solutions;
        this.hints = hints;
        this.giaoDich = new TransactionTemplate(tx);
        this.su = su;
        this.clock = clock;
    }

    /** Kết quả một lần nhập: mã các bài đã ghi, theo thứ tự nhập. */
    public record KetQua(int kyNang, int maLoi, int buoc, List<String> bai) {}

    /** Bài đã dựng xong (sau khi gọi dịch vụ toán), chờ ghi. */
    record BaiNhap(String ma, String kyNang, List<String> kyNangPhu, Level4 muc4, @Nullable Level3 muc3, @Nullable BloomLevel bloom,
            String deBai, String latex, @Nullable String ham, String dangTraLoi, @Nullable String buocBatDau, String nguonBai,
            @Nullable Object baiLam, Object suKien, List<Map<String, @Nullable Object>> thangGoiY) {}

    /** Kết quả nhập kèm các bài đã dựng, để nhập theo lớp kiểm đúng nội dung vừa ghi, như {@code napBai} của seed.ts. */
    record DaNhap(KetQua ketQua, List<BaiNhap> bai) {}

    public KetQua nhap() {
        return nhapGiuBai().ketQua();
    }

    DaNhap nhapGiuBai() {
        Map<String, @Nullable Object> danhMuc = nguon.doiTuong("supham/danh-muc-ky-nang-DH.json");
        List<List<String>> maLoi = nguon.csv("supham/ma-loi-DH.csv");
        List<Map<String, @Nullable Object>> khung = nguon.danhSach("v0/khung-buoc.json");
        List<BaiNhap> bai = dungBai();
        Integer[] dem = new Integer[3];
        giaoDich.executeWithoutResult(trangThai -> {
            dem[0] = ghiKyNang(danhMuc);
            dem[1] = ghiMaLoi(maLoi);
            dem[2] = ghiKhungBuoc(khung);
            Instant luc = clock.instant();
            bai.forEach(b -> ghiBai(b, luc));
        });
        KetQua kq = new KetQua(dem[0], dem[1], dem[2], bai.stream().map(BaiNhap::ma).toList());
        LOG.info("Nhập nội dung chung: {} kỹ năng, {} mã lỗi, {} bước, {} bài", kq.kyNang(), kq.maLoi(), kq.buoc(), kq.bai().size());
        // Sau commit: nơi nghe (T034b) đọc được bài đã ghi.
        for (BaiNhap b : bai) {
            Problem p = problems.findByCode(b.ma()).orElseThrow();
            su.publishEvent(new BaiDaNhap(p.id(), b.ma(), problems.findContentVersion(p.id()).orElseThrow()));
        }
        return new DaNhap(kq, bai);
    }

    // ---- Dựng bài (gọi dịch vụ toán) ---------------------------------------------------------------------------

    List<BaiNhap> dungBai() {
        List<BaiNhap> bai = new ArrayList<>();
        for (Map<String, @Nullable Object> ex : nguon.danhSach("supham/03-vi-du-bai-tap.json")) {
            bai.add(baiViDu(ex));
        }
        bai.add(baiMayGiai("DH12-NB-01", "x**2"));
        bai.add(baiMayGiai("DH12-TH-02", "x**3 - 3*x"));
        for (Map.Entry<String, Integer> gen : List.of(Map.entry("bac_ba", 11), Map.entry("trung_phuong", 7), Map.entry("huu_ti", 5))) {
            baiSinh(gen.getKey(), gen.getValue()).ifPresent(bai::add);
        }
        for (Map<String, @Nullable Object> ex : nguon.danhSach("supham/bai-khung-ngan.seed-v01.json")) {
            bai.add(baiKhungNgan(ex));
        }
        bai.add(baiViDuCongChan());
        return bai;
    }

    /**
     * Bài ví dụ của lab: giải bằng máy khi có hàm không tham số; máy trả lời không giải được thì vẫn nhập, không có lời
     * giải. Dịch vụ toán không trả lời thì {@link DichVuToanKhongTraLoi} dừng cả lần nhập.
     */
    private BaiNhap baiViDu(Map<String, @Nullable Object> ex) {
        Map<String, @Nullable Object> de = doiTuong(ex.get("de_bai"));
        String hamDe = chuNeuCo(de.get("ham_so_sympy"));
        List<Map<String, @Nullable Object>> thang = danhSach(ex.get("thang_goi_y"));
        String ham = null;
        Object baiLam = null;
        Object suKien = List.of();
        if (hamDe != null && !hamDe.contains("m")) {
            Map<String, @Nullable Object> giai = toan.giai(Map.of("ham", hamDe));
            if (Boolean.TRUE.equals(giai.get("dat")) && giai.get("bai_lam") != null) {
                ham = hamDe;
                baiLam = giai.get("bai_lam");
                suKien = Objects.requireNonNullElse(giai.get("su_kien"), List.of());
                if (thang.isEmpty()) {
                    thang = danhSach(giai.get("thang_goi_y"));
                }
            }
        }
        String dangCau = Objects.requireNonNullElse(chuNeuCo(ex.get("dang_cau")), "TU_LUAN");
        String dang = ham != null && dangCau.equals("TU_LUAN") ? Problem.TU_LUAN_5_BUOC : dangCau;
        String muc4 = chu(ex.get("muc_do_4"));
        return new BaiNhap(chu(ex.get("id")), chu(ex.get("ky_nang_chinh")), danhSachChu(ex.get("ky_nang_phu")), muc4(muc4),
            muc3(chuNeuCo(ex.get("muc_do_bo_3")), muc4), bloom(chuNeuCo(ex.get("muc_bloom"))), chu(de.get("van_ban")),
            chu(de.get("latex")), ham, dang, null, "SUPHAM", baiLam, suKien, thang);
    }

    /** Hai bài máy giải của v0: đề dựng từ LaTeX máy trả về. */
    private BaiNhap baiMayGiai(String ma, String ham) {
        Map<String, @Nullable Object> giai = giaiBatBuoc(Map.of("ham", ham), ma);
        String latex = Objects.requireNonNullElse(chuNeuCo(giai.get("latex")), ham);
        return new BaiNhap(ma, "T12.DH.03", List.of(), Level4.VAN_DUNG, Level3.VAN_DUNG, BloomLevel.VAN_DUNG,
            "Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số y = " + latex + ".", latex, ham, Problem.TU_LUAN_5_BUOC,
            null, "MAY_GIAI", giai.get("bai_lam"), Objects.requireNonNullElse(giai.get("su_kien"), List.of()),
            danhSach(giai.get("thang_goi_y")));
    }

    /**
     * Biến thể {@code /v1/generate}; máy trả lời không dùng được ({@code loi}, thiếu hàm hay lời giải) thì bỏ qua như v0.
     * Dịch vụ toán không trả lời thì {@link DichVuToanKhongTraLoi} dừng cả lần nhập, không bỏ biến thể trong im lặng.
     */
    private Optional<BaiNhap> baiSinh(String dang, int hatGiong) {
        Map<String, @Nullable Object> g = toan.sinh(Map.of("dang", dang, "seed", hatGiong));
        if (g.get("loi") != null || g.get("ham") == null || g.get("bai_lam") == null) {
            LOG.warn("Biến thể {} hạt giống {} không dùng được", dang, hatGiong);
            return Optional.empty();
        }
        String ham = chu(g.get("ham"));
        String muc4 = Objects.requireNonNullElse(chuNeuCo(g.get("muc_do_4")), "VAN_DUNG");
        return Optional.of(new BaiNhap("GEN-" + dang + "-" + hatGiong,
            Objects.requireNonNullElse(chuNeuCo(g.get("ky_nang_chinh")), "T12.DH.03"), List.of(), muc4(muc4),
            muc3(Objects.requireNonNullElse(chuNeuCo(g.get("muc_do_bo_3")), "VAN_DUNG"), muc4), bloom(chuNeuCo(g.get("muc_bloom"))),
            Objects.requireNonNullElse(chuNeuCo(g.get("de_bai")), "").replace("$", ""),
            Objects.requireNonNullElse(chuNeuCo(g.get("latex")), ham), ham, Problem.TU_LUAN_5_BUOC, null, "THAM_SO_HOA",
            g.get("bai_lam"), Objects.requireNonNullElse(g.get("su_kien"), List.of()), danhSach(g.get("thang_goi_y"))));
    }

    /** Bài khung ngắn của lab: máy giải cùng đề và bước bắt đầu (để dữ kiện bảo vệ có dòng y' đề cho); thang của lab. */
    private BaiNhap baiKhungNgan(Map<String, @Nullable Object> ex) {
        Map<String, @Nullable Object> de = doiTuong(ex.get("de_bai"));
        String ma = chu(ex.get("id"));
        String ham = chu(de.get("ham_so_sympy"));
        String buoc = chu(ex.get("buoc_bat_dau"));
        Map<String, Object> yeuCau = new LinkedHashMap<>();
        yeuCau.put("ham", ham);
        yeuCau.put("de_bai", chu(de.get("van_ban")));
        yeuCau.put("buoc_bat_dau", buoc);
        Map<String, @Nullable Object> giai = giaiBatBuoc(yeuCau, ma);
        String dangCau = Objects.requireNonNullElse(chuNeuCo(ex.get("dang_cau")), "TU_LUAN");
        String muc4 = chu(ex.get("muc_do_4"));
        return new BaiNhap(ma, chu(ex.get("ky_nang_chinh")), danhSachChu(ex.get("ky_nang_phu")), muc4(muc4),
            muc3(chuNeuCo(ex.get("muc_do_bo_3")), muc4), bloom(chuNeuCo(ex.get("muc_bloom"))), chu(de.get("van_ban")),
            chu(de.get("latex")), ham, dangCau.equals("TU_LUAN") ? Problem.TU_LUAN_5_BUOC : dangCau, buoc, "SUPHAM_KHUNG_NGAN",
            giai.get("bai_lam"), Objects.requireNonNullElse(giai.get("su_kien"), List.of()), danhSach(ex.get("thang_goi_y")));
    }

    /** Ví dụ cổng chặn của v0: lời giải của y = x² bị sửa đạo hàm thành 3x (không giao cho học sinh). */
    private BaiNhap baiViDuCongChan() {
        Map<String, @Nullable Object> giai = giaiBatBuoc(Map.of("ham", "x**2"), "DH12-DEMO-CHAN-01");
        Map<String, @Nullable Object> baiLam = new LinkedHashMap<>(doiTuong(giai.get("bai_lam")));
        baiLam.put("dao_ham", "3*x");
        return new BaiNhap("DH12-DEMO-CHAN-01", "T12.DH.03", List.of(), Level4.NHAN_BIET, Level3.BIET, bloom("NHAN_BIET"),
            "Ví dụ cổng chặn (không giao cho học sinh): lời giải của y = x² bị sửa đạo hàm thành 3x.", "x^{2}", "x**2",
            Problem.TU_LUAN_5_BUOC, null, "VI_DU_CONG", baiLam, Objects.requireNonNullElse(giai.get("su_kien"), List.of()),
            danhSach(giai.get("thang_goi_y")));
    }

    private Map<String, @Nullable Object> giaiBatBuoc(Map<String, ?> yeuCau, String ma) {
        Map<String, @Nullable Object> giai = toan.giai(yeuCau);
        if (!Boolean.TRUE.equals(giai.get("dat")) || giai.get("bai_lam") == null) {
            throw new IllegalStateException("Không giải được " + ma);
        }
        return giai;
    }

    // ---- Ghi --------------------------------------------------------------------------------------------------

    private int ghiKyNang(Map<String, @Nullable Object> danhMuc) {
        catalog.saveTopic(new Topic(CHU_DE, chu(danhMuc.get("chu_de")), 12));
        List<Map<String, @Nullable Object>> kyNang = danhSach(danhMuc.get("ky_nang"));
        for (Map<String, @Nullable Object> k : kyNang) {
            catalog.saveSkill(new Skill(chu(k.get("ma")), CHU_DE, chu(k.get("ten")), chuNeuCo(k.get("yccd_gdpt2018")),
                ((Number) Objects.requireNonNull(k.get("lop"))).intValue(), Boolean.TRUE.equals(k.get("la_cot_loi_chu_de"))));
        }
        // Tiên quyết sau khi mọi kỹ năng đã có (khóa ngoại).
        for (Map<String, @Nullable Object> k : kyNang) {
            String ma = chu(k.get("ma"));
            catalog.replacePrerequisites(ma, danhSach(k.get("tien_quyet")).stream()
                .map(t -> new SkillPrerequisite(ma, chu(t.get("ma")), chuNeuCo(t.get("muc_toi_thieu")))).toList());
        }
        return kyNang.size();
    }

    /** CSV mã lỗi đọc theo tên cột như v0; loại kết quả liên quan tách theo «|». */
    private int ghiMaLoi(List<List<String>> csv) {
        List<String> cot = csv.getFirst().stream().map(String::strip).toList();
        int ma = viTri(cot, "ma_loi");
        int kyNang = viTri(cot, "ky_nang_chinh");
        int buoc = viTri(cot, "ma_buoc");
        int moTa = viTri(cot, "mo_ta");
        int goiY = viTri(cot, "goi_y_sua");
        int loai = viTri(cot, "loai_ket_qua_lien_quan");
        int dem = 0;
        for (List<String> hang : csv.subList(1, csv.size())) {
            if (o(hang, ma).isBlank()) {
                continue;
            }
            List<String> loaiKetQua = List.of(o(hang, loai).split("\\|")).stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
            catalog.saveErrorType(new ErrorType(o(hang, ma), rongThanhNull(o(hang, kyNang)), rongThanhNull(o(hang, buoc)), o(hang, moTa),
                rongThanhNull(o(hang, goiY)), loaiKetQua));
            dem++;
        }
        return dem;
    }

    private int ghiKhungBuoc(List<Map<String, @Nullable Object>> khung) {
        for (Map<String, @Nullable Object> b : khung) {
            catalog.saveStepTemplate(new StepTemplate(chu(b.get("maBuoc")), chu(b.get("topicCode")),
                ((Number) Objects.requireNonNull(b.get("thuTu"))).intValue(), InputKind.valueOf(chu(b.get("dangNhap"))),
                chuNeuCo(b.get("skillCode")), chu(b.get("moTa"))));
        }
        return khung.size();
    }

    private void ghiBai(BaiNhap b, Instant luc) {
        Optional<Problem> daCo = problems.findByCode(b.ma());
        UUID id = daCo.map(Problem::id).orElseGet(UUID::randomUUID);
        problems.save(new Problem(id, b.ma(), b.kyNang(), b.kyNangPhu(), b.muc4(), b.muc3(), b.bloom(), 0.5, b.deBai(), b.latex(),
            b.ham(), b.dangTraLoi(), b.buocBatDau(), b.nguonBai(), dauVanTay(b), null, daCo.map(Problem::createdAt).orElse(luc), luc));
        solutions.save(new Solution(id, b.baiLam() == null ? null : JsonKieuJs.stringify(b.baiLam()), JsonKieuJs.stringify(b.suKien()),
            null));
        List<HintLevel> cap = new ArrayList<>();
        for (Map<String, @Nullable Object> khoi : b.thangGoiY()) {
            String buoc = chu(khoi.get("ma_buoc"));
            for (Map<String, @Nullable Object> c : danhSach(khoi.get("cac_cap"))) {
                // SP-08: cấp chỉ có ly_do_trong (ghi chú cho người soạn) không thành dòng gợi ý.
                String noiDung = Objects.requireNonNullElse(chuNeuCo(c.get("noi_dung")), "").strip();
                if (!noiDung.isEmpty()) {
                    cap.add(new HintLevel(id, buoc, ((Number) Objects.requireNonNull(c.get("cap"))).intValue(), noiDung));
                }
            }
        }
        hints.replaceForProblem(id, cap);
    }

    /**
     * Dấu vân tay nội dung của bài (data-model §problems): mọi đầu vào của {@code /v1/verify} và mọi thứ học sinh thấy (đề,
     * LaTeX, hàm, dạng trả lời, bước bắt đầu, lời giải, dữ kiện bảo vệ, thang gợi ý), JSON viết như {@code JSON.stringify}.
     */
    static String dauVanTay(BaiNhap b) {
        Map<String, @Nullable Object> noiDung = new LinkedHashMap<>();
        noiDung.put("de", b.deBai());
        noiDung.put("latex", b.latex());
        noiDung.put("ham", b.ham());
        noiDung.put("dang", b.dangTraLoi());
        noiDung.put("buoc", b.buocBatDau());
        noiDung.put("bl", b.baiLam());
        noiDung.put("su_kien", b.suKien());
        noiDung.put("hints", b.thangGoiY());
        return sha256(JsonKieuJs.stringify(noiDung));
    }

    static String sha256(String chu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(chu.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e);
        }
    }

    // ---- Quy đổi và đọc trường ---------------------------------------------------------------------------------

    private static Level4 muc4(String ma) {
        return Level4.valueOf(Objects.requireNonNullElse(MUC4.get(ma), ma));
    }

    private static @Nullable Level3 muc3(@Nullable String muc3, String muc4) {
        String ma = muc3 != null ? muc3 : MUC3.get(muc4);
        return ma == null ? null : Level3.valueOf(ma);
    }

    /** Bloom 6 mức của lab; hai mã khác của v0: {@code APPLY} ({@code /v1/generate}) → VAN_DUNG, {@code NHAN_BIET} → NHO. */
    static @Nullable BloomLevel bloom(@Nullable String ma) {
        if (ma == null) {
            return null;
        }
        return switch (ma) {
            case "APPLY" -> BloomLevel.VAN_DUNG;
            case "NHAN_BIET" -> BloomLevel.NHO;
            default -> BloomLevel.valueOf(ma);
        };
    }

    private static int viTri(List<String> cot, String ten) {
        int i = cot.indexOf(ten);
        if (i < 0) {
            throw new IllegalStateException("CSV mã lỗi thiếu cột " + ten);
        }
        return i;
    }

    private static String o(List<String> hang, int i) {
        return i < hang.size() ? hang.get(i) : "";
    }

    private static @Nullable String rongThanhNull(String s) {
        return s.isBlank() ? null : s;
    }

    private static String chu(@Nullable Object o) {
        return (String) Objects.requireNonNull(o, "thiếu trường bắt buộc");
    }

    private static @Nullable String chuNeuCo(@Nullable Object o) {
        return o == null ? null : (String) o;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> doiTuong(@Nullable Object o) {
        return (Map<String, @Nullable Object>) Objects.requireNonNull(o, "thiếu đối tượng");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, @Nullable Object>> danhSach(@Nullable Object o) {
        return o == null ? List.of() : (List<Map<String, @Nullable Object>>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<String> danhSachChu(@Nullable Object o) {
        return o == null ? List.of() : (List<String>) o;
    }
}
