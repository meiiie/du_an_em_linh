package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.content.domain.event.BangCongThucDaKhoa;
import vn.hoctapcanman.core.content.domain.model.CheckStatus;
import vn.hoctapcanman.core.content.domain.model.Document;
import vn.hoctapcanman.core.content.domain.model.DocumentKind;
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;
import vn.hoctapcanman.core.content.domain.model.Formula;
import vn.hoctapcanman.core.content.domain.model.FormulaCheck;
import vn.hoctapcanman.core.content.domain.model.FormulaKind;
import vn.hoctapcanman.core.content.domain.model.FormulaSheet;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ProblemRelease;
import vn.hoctapcanman.core.content.domain.model.ReleaseStatus;
import vn.hoctapcanman.core.content.domain.model.SheetStatus;
import vn.hoctapcanman.core.content.domain.model.SubjectKind;
import vn.hoctapcanman.core.content.domain.model.TierResult;
import vn.hoctapcanman.core.content.domain.model.VerificationRun;
import vn.hoctapcanman.core.content.domain.repository.DocumentRepository;
import vn.hoctapcanman.core.content.domain.repository.FormulaSheetRepository;
import vn.hoctapcanman.core.content.domain.repository.ProblemReleaseRepository;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;
import vn.hoctapcanman.core.content.domain.repository.VerificationRunRepository;

/**
 * Nhập nội dung theo lớp (T012b, #85), sau nội dung chung ({@link NhapNoiDungChung}), cho một lớp:
 *
 * <ol>
 *   <li>nạp 5 tài liệu của lớp (3 của v0 ở {@code v0/tai-lieu.json}, {@code sp-tai-lieu-0001}, {@code sp-tai-lieu-0002} của
 *       lab Sư phạm), mỗi câu một đoạn ({@link ChiaDoan});
 *   <li>khóa bảng 6 dòng của v0 ({@code v0/bang-cong-thuc.json}) qua {@code /v1/kiem-dong-cong-thuc}: chỉ khóa khi mọi dòng
 *       đạt tầng 1 và 2 có trích dẫn, ngược lại báo lỗi kèm mã dòng, không khóa thiếu (ADR 013);
 *   <li>chạy {@code /v1/verify} cho từng bài với đúng bài vừa dựng (thang gợi ý đầy đủ như {@code napBai} của
 *       {@code seed.ts}) và kho của lớp, ghi lượt kiểm kèm trích dẫn, rồi áp vào phát hành của lớp.
 * </ol>
 *
 * Trong kho gửi dịch vụ toán, tài liệu và dòng bảng mang mã ổn định ({@code v0-don-dieu}, {@code d-1}…) như tệp vàng của
 * v0 (T013), nên phản hồi so được với v0. Trích dẫn tầng 2 của {@code /v1/verify} (vị trí và chữ trích trong văn bản) ánh
 * xạ về đoạn đã lưu; không ánh xạ được thì tầng 2 ghi {@code KHONG_KIEM_DUOC} (đóng mặc định, không đoán căn cứ). Nhập lại
 * không làm gì thừa: bảng đang dùng cùng nội dung thì giữ, bài có lượt kiểm còn mới thì không kiểm lại.
 */
@Component
public class NhapTheoLop {

    private static final Logger LOG = LoggerFactory.getLogger(NhapTheoLop.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<String> TAI_LIEU_LAB = List.of("supham/tai-lieu/sp-tai-lieu-0001.json", "supham/tai-lieu/sp-tai-lieu-0002.json");
    private static final String GHI_CHU_BANG = "Bảng của v0 (v0/bang-cong-thuc.json), khóa qua kiem-dong-cong-thuc";

    private final NguonNoiDung nguon;
    private final KiemToan toan;
    private final DocumentRepository documents;
    private final FormulaSheetRepository sheets;
    private final ProblemRepository problems;
    private final VerificationRunRepository runs;
    private final ProblemReleaseRepository releases;
    private final TransactionTemplate giaoDich;
    private final ApplicationEventPublisher su;
    private final Clock clock;

    public NhapTheoLop(NguonNoiDung nguon, KiemToan toan, DocumentRepository documents, FormulaSheetRepository sheets,
            ProblemRepository problems, VerificationRunRepository runs, ProblemReleaseRepository releases, PlatformTransactionManager tx,
            ApplicationEventPublisher su, Clock clock) {
        this.nguon = nguon;
        this.toan = toan;
        this.documents = documents;
        this.sheets = sheets;
        this.problems = problems;
        this.runs = runs;
        this.releases = releases;
        this.giaoDich = new TransactionTemplate(tx);
        this.su = su;
        this.clock = clock;
    }

    /** Kết quả cho một lớp: số tài liệu, bảng đang dùng, trạng thái phát hành theo mã bài, số bài đã kiểm lần này. */
    public record KetQua(UUID lop, int taiLieu, UUID bang, int phienBanBang, Map<String, ReleaseStatus> phatHanh, int daKiem) {}

    /** Tài liệu đã lưu của lớp, kèm các đoạn có id thật. */
    record TaiLieuLop(Document taiLieu, List<DocumentPassage> doan) {}

    KetQua nhap(UUID lop, List<NhapNoiDungChung.BaiNhap> bai) {
        Map<String, TaiLieuLop> kho = napTaiLieu(lop);
        FormulaSheet bang = khoaBang(lop, kho);
        Map<String, ReleaseStatus> phatHanh = new LinkedHashMap<>();
        int daKiem = 0;
        for (NhapNoiDungChung.BaiNhap b : bai) {
            if (kiemBai(lop, b, kho, bang)) {
                daKiem++;
            }
            Problem p = problems.findByCode(b.ma()).orElseThrow();
            phatHanh.put(b.ma(), releases.find(lop, p.id()).map(ProblemRelease::status).orElse(ReleaseStatus.NHAP));
        }
        rutBaiRoiNguon(lop, phatHanh.keySet());
        KetQua kq = new KetQua(lop, kho.size(), bang.id(), bang.version(), phatHanh, daKiem);
        LOG.info("Nhập nội dung cho lớp {}: {} tài liệu, bảng phiên bản {}, kiểm {} bài, phát hành {}", lop, kq.taiLieu(),
            kq.phienBanBang(), daKiem, demTheoTrangThai(phatHanh));
        return kq;
    }

    /**
     * Bài do importer tạo ({@link NhapNoiDungChung#NGUON_NHAP}) mà không còn trong lần nhập này (bỏ khỏi nguồn, hay biến thể
     * {@code /v1/generate} nay trả {@code loi}): phát hành của nó ở lớp về {@code NHAP}, học sinh không còn thấy. Bài do giáo
     * viên tạo (nguồn khác) không bị đụng.
     */
    private void rutBaiRoiNguon(UUID lop, Set<String> maDaNhap) {
        List<ProblemRelease> dangMo = releases.findByClass(lop).stream().filter(r -> r.status() != ReleaseStatus.NHAP).toList();
        if (dangMo.isEmpty()) {
            return;
        }
        Set<UUID> rut = new LinkedHashSet<>();
        for (Problem p : problems.findAllById(dangMo.stream().map(ProblemRelease::problemId).toList())) {
            if (NhapNoiDungChung.NGUON_NHAP.contains(p.origin()) && !maDaNhap.contains(p.code())) {
                rut.add(p.id());
            }
        }
        if (rut.isEmpty()) {
            return;
        }
        giaoDich.executeWithoutResult(t -> dangMo.stream().filter(r -> rut.contains(r.problemId()))
            .forEach(r -> releases.save(r.backToDraft(clock.instant()))));
        LOG.info("Lớp {}: rút phát hành {} bài không còn trong nguồn nhập", lop, rut.size());
    }

    // ---- Tài liệu -------------------------------------------------------------------------------------------------

    /**
     * Nạp tài liệu nguồn vào lớp. Tài liệu đã có, cùng nội dung: không ghi gì. Tài liệu đã có mà nguồn đổi (văn bản, loại,
     * quyền dùng, phiên bản…): không sửa tại chỗ, vì đoạn của nó có thể đang là căn cứ của bảng đã khóa hay lượt kiểm (V5
     * từ chối, và căn cứ phải bất biến); bản cũ giữ nguyên làm căn cứ, chỉ đổi mã thành {@code mã.cu-<8 ký tự đầu của id>} để
     * nhường mã, rồi nạp bản mới với mã gốc. Kho của lớp gửi dịch vụ toán chỉ gồm các bản mang mã gốc.
     */
    private Map<String, TaiLieuLop> napTaiLieu(UUID lop) {
        List<Map<String, @Nullable Object>> nguonTaiLieu = new ArrayList<>(nguon.danhSach("v0/tai-lieu.json"));
        TAI_LIEU_LAB.forEach(duong -> nguonTaiLieu.add(nguon.doiTuong(duong)));
        Map<String, TaiLieuLop> kho = new LinkedHashMap<>();
        giaoDich.executeWithoutResult(t -> {
            for (Map<String, @Nullable Object> d : nguonTaiLieu) {
                String ma = chu(d.get("ma"));
                Optional<Document> daCo = documents.findByClassAndCode(lop, ma);
                String vanBan = chu(d.get("textContent"));
                Document moi = new Document(daCo.map(Document::id).orElseGet(UUID::randomUUID), lop, ma, chu(d.get("title")),
                    DocumentKind.parse(chu(d.get("kind"))), chuNeuCo(d.get("source")), chu(d.get("licenseStatus")), null, vanBan,
                    ((Number) Objects.requireNonNull(d.get("version"))).intValue(), null,
                    daCo.map(Document::createdAt).orElseGet(clock::instant));
                if (daCo.isPresent() && daCo.get().equals(moi)) {
                    kho.put(ma, new TaiLieuLop(daCo.get(), documents.findPassages(daCo.get().id())));
                    continue;
                }
                if (daCo.isPresent()) {
                    Document cu = daCo.get();
                    String maCu = ma + ".cu-" + cu.id().toString().substring(0, 8);
                    documents.save(new Document(cu.id(), cu.classId(), maCu, cu.title(), cu.kind(), cu.source(), cu.licenseStatus(),
                        cu.fileRef(), cu.textContent(), cu.version(), cu.uploadedBy(), cu.createdAt()), documents.findPassages(cu.id()));
                    moi = new Document(UUID.randomUUID(), lop, ma, moi.title(), moi.kind(), moi.source(), moi.licenseStatus(), null,
                        vanBan, moi.version(), null, clock.instant());
                    LOG.info("Tài liệu {} của lớp {} đổi ở nguồn: giữ bản cũ làm căn cứ ({}), nạp bản mới", ma, lop, maCu);
                }
                kho.put(ma, new TaiLieuLop(moi, documents.save(moi, ChiaDoan.theoCau(moi.id(), vanBan))));
            }
        });
        return kho;
    }

    /**
     * Ghi chú của bảng importer khóa: nêu nguồn và dấu vân tay của kho đã dùng khi khóa (mã, id, phiên bản của từng tài liệu
     * kho, xếp theo mã). Kho đổi theo bất kỳ cách nào (thay bản, thêm, bỏ một tài liệu) thì dấu vân tay khác, nên bảng được
     * khóa lại và mọi lượt kiểm với bảng cũ thành cũ, phải kiểm lại. Suy từ dữ liệu đã lưu: lần nhập trước lỗi giữa chừng
     * (đã nạp tài liệu mới, chưa khóa được bảng) thì lần sau vẫn khóa lại và kiểm lại.
     */
    private static String ghiChuBang(Map<String, TaiLieuLop> kho) {
        String dau = kho.values().stream().map(TaiLieuLop::taiLieu).sorted(java.util.Comparator.comparing(d -> Objects.requireNonNull(d.code())))
            .map(d -> d.code() + ":" + d.id() + ":" + d.version()).collect(java.util.stream.Collectors.joining("\n"));
        return GHI_CHU_BANG + " · kho " + NhapNoiDungChung.sha256(dau).substring(0, 16);
    }

    // ---- Bảng công thức -------------------------------------------------------------------------------------------

    /**
     * Bảng đang dùng của lớp sau khi nhập. Giữ bảng đang dùng nếu cùng các dòng của v0 và được khóa với đúng kho hiện tại
     * ({@link #ghiChuBang}); ngược lại khóa bảng phiên bản mới với kho hiện tại: lượt kiểm với bảng cũ
     * thành cũ, phát hành giữ «cần kiểm lại» tới khi kiểm lại (ADR 005). Lớp đang có bảng nháp không do importer tạo (giáo viên
     * đang soạn) thì dừng, không ghi đè.
     */
    private FormulaSheet khoaBang(UUID lop, Map<String, TaiLieuLop> kho) {
        List<Formula> dong = dongBangV0();
        Optional<FormulaSheet> dangDung = sheets.findCurrent(lop);
        String ghiChu = ghiChuBang(kho);
        if (dangDung.isPresent() && ghiChu.equals(dangDung.get().note()) && dauVanTay(dangDung.get().rows()).equals(dauVanTay(dong))) {
            return dangDung.get();
        }
        Optional<FormulaSheet> banNhap = sheets.findDraft(lop);
        if (banNhap.isPresent() && !Objects.requireNonNullElse(banNhap.get().note(), "").startsWith(GHI_CHU_BANG)) {
            throw new IllegalStateException("Lớp " + lop + " có bảng nháp đang soạn (" + banNhap.get().id()
                + "), không do importer tạo: không ghi đè, không khóa bảng của v0");
        }
        int phienBan = dangDung.map(b -> b.version() + 1).orElse(1);
        Instant luc = clock.instant();
        FormulaSheet nhap = banNhap
            .map(d -> new FormulaSheet(d.id(), lop, phienBan, SheetStatus.NHAP, ghiChu, null, null, null, d.createdAt(), dong))
            .orElseGet(() -> FormulaSheet.draft(lop, phienBan, ghiChu, dong, luc));

        Map<String, Object> yeuCau = new LinkedHashMap<>();
        yeuCau.put("dong", dong.stream().map(f -> Map.of("id", f.code(), "tieu_de", f.title(), "latex", f.latex(),
            "phat_bieu", f.statement())).toList());
        yeuCau.put("tai_lieu", kho.values().stream().map(t -> {
            Map<String, @Nullable Object> tl = new LinkedHashMap<>();
            tl.put("id", t.taiLieu().id().toString());
            tl.put("ten", t.taiLieu().title());
            tl.put("doan", t.doan().stream().map(p -> {
                Map<String, @Nullable Object> d = new LinkedHashMap<>();
                d.put("id", p.id().toString());
                d.put("trang", p.page());
                d.put("text", p.text());
                return d;
            }).toList());
            tl.put("license_status", t.taiLieu().licenseStatus());
            return tl;
        }).toList());
        yeuCau.put("timeout_s", 20);
        Map<String, @Nullable Object> kq = toan.kiemDongCongThuc(yeuCau);

        Set<UUID> doanCuaLop = new LinkedHashSet<>();
        kho.values().forEach(t -> t.doan().forEach(p -> doanCuaLop.add(p.id())));
        Map<String, Formula> theoMa = new LinkedHashMap<>();
        dong.forEach(f -> theoMa.put(f.code(), f));
        Map<String, FormulaCheck> ketQua = new LinkedHashMap<>();
        for (Map<String, @Nullable Object> r : danhSach(kq.get("dong"))) {
            Formula f = theoMa.get(chu(r.get("id")));
            if (f == null) {
                continue;
            }
            Map<String, @Nullable Object> t1 = doiTuong(r.get("tang1"));
            Map<String, @Nullable Object> t2 = doiTuong(r.get("tang2"));
            UUID trich = null;
            List<UUID> them = new ArrayList<>();
            boolean themHong = false;
            if (t2.get("trich_dan") != null) {
                trich = doanThuocLop(chuNeuCo(doiTuong(t2.get("trich_dan")).get("doan")), doanCuaLop);
            }
            for (Map<String, @Nullable Object> x : danhSach(t2.get("trich_dan_them"))) {
                UUID d = doanThuocLop(chuNeuCo(x.get("doan")), doanCuaLop);
                if (d == null) {
                    themHong = true;
                } else if (!d.equals(trich) && !them.contains(d)) {
                    them.add(d);
                }
            }
            CheckStatus tang2 = trangThai(t2.get("trang_thai"));
            if (tang2 == CheckStatus.DAT && (trich == null || themHong)) {
                // Đạt mà có trích dẫn (chính hay thêm, cho mệnh đề khác của dòng định lí) không chỉ được về đoạn của lớp: không
                // coi là đạt, dòng này chặn khóa (lock báo mã dòng).
                tang2 = CheckStatus.KHONG_KIEM_DUOC;
                trich = null;
                them = List.of();
            }
            ketQua.put(f.code(), FormulaCheck.of(f, FormulaKind.valueOf(chu(r.get("loai"))), trangThai(t1.get("trang_thai")), tang2,
                json(t1), json(t2), trich, them));
        }
        // Mọi dòng phải DAT hai tầng có trích dẫn; không thì lock() ném lỗi kèm mã dòng chưa qua, không khóa thiếu.
        FormulaSheet khoa = nhap.withCheckResults(ketQua).lock(null, luc);
        giaoDich.executeWithoutResult(t -> sheets.save(khoa));
        su.publishEvent(new BangCongThucDaKhoa(lop, khoa.id(), khoa.version()));
        return khoa;
    }

    private List<Formula> dongBangV0() {
        List<Map<String, @Nullable Object>> dong = danhSach(nguon.doiTuong("v0/bang-cong-thuc.json").get("formulas"));
        List<Formula> kq = new ArrayList<>();
        for (int i = 0; i < dong.size(); i++) {
            Map<String, @Nullable Object> f = dong.get(i);
            kq.add(Formula.unchecked(i + 1, chu(f.get("ma")), chuNeuCo(f.get("skillCode")), chu(f.get("title")), chu(f.get("latex")),
                chu(f.get("noiDung"))));
        }
        return kq;
    }

    private static List<String> dauVanTay(List<Formula> dong) {
        return dong.stream().map(f -> f.ordinal() + ":" + f.code() + ":" + f.contentFingerprint()).toList();
    }

    private static @Nullable UUID doanThuocLop(@Nullable String id, Set<UUID> doanCuaLop) {
        if (id == null) {
            return null;
        }
        try {
            UUID d = UUID.fromString(id);
            return doanCuaLop.contains(d) ? d : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- Kiểm và phát hành từng bài -------------------------------------------------------------------------------

    /** Kiểm một bài cho lớp nếu chưa có lượt còn mới; luôn bảo đảm phát hành theo lượt mới nhất. Trả true nếu đã kiểm. */
    private boolean kiemBai(UUID lop, NhapNoiDungChung.BaiNhap b, Map<String, TaiLieuLop> kho, FormulaSheet bang) {
        Problem p = problems.findByCode(b.ma()).orElseThrow(() -> new IllegalStateException("Chưa có bài " + b.ma()));
        int phienBan = problems.findContentVersion(p.id()).orElseThrow();
        Optional<VerificationRun> moiNhat = runs.findLatest(lop, SubjectKind.PROBLEM, p.id());
        // Còn mới: đúng phiên bản nội dung, đúng bảng đang dùng (bảng khóa lại khi kho đổi, nên lượt với kho cũ không còn mới),
        // chưa cũ, và mọi đoạn nó trích dẫn còn thuộc kho hiện tại.
        Set<UUID> doanKho = new java.util.HashSet<>();
        kho.values().forEach(t -> t.doan().forEach(d -> doanKho.add(d.id())));
        boolean conMoi = moiNhat.isPresent() && Objects.equals(moiNhat.get().contentVersion(), phienBan)
            && moiNhat.get().freshnessRefusal(true, p.contentHash(), bang.id()).isEmpty()
            && doanKho.containsAll(moiNhat.get().citationPassageIds());
        if (conMoi) {
            VerificationRun luot = moiNhat.get();
            giaoDich.executeWithoutResult(t -> apPhatHanh(lop, p, luot, bang));
            return false;
        }

        Map<String, @Nullable Object> yeuCau = new LinkedHashMap<>();
        yeuCau.put("ham", b.ham());
        yeuCau.put("bai_lam", b.baiLam());
        yeuCau.put("thang_goi_y", b.thangGoiY());
        if (b.buocBatDau() != null) {
            yeuCau.put("buoc_bat_dau", b.buocBatDau());
        }
        yeuCau.put("de_bai", b.deBai());
        yeuCau.put("tai_lieu", kho.values().stream().map(t -> {
            Map<String, @Nullable Object> tl = new LinkedHashMap<>();
            tl.put("id", t.taiLieu().code());
            tl.put("ten", t.taiLieu().title());
            tl.put("text", t.taiLieu().textContent());
            tl.put("license_status", t.taiLieu().licenseStatus());
            tl.put("phien_ban", t.taiLieu().version());
            return tl;
        }).toList());
        yeuCau.put("cong_thuc", bang.rows().stream().map(f -> Map.of("id", f.code(), "latex", f.latex(), "noi_dung", f.statement(),
            "ten", f.title())).toList());
        Map<String, @Nullable Object> kq = toan.kiemBai(yeuCau);

        List<TierResult> tang = new ArrayList<>();
        Set<UUID> trichDan = new LinkedHashSet<>();
        for (Map<String, @Nullable Object> t : danhSach(kq.get("tang"))) {
            int so = ((Number) Objects.requireNonNull(t.get("tang"))).intValue();
            CheckStatus trangThai = trangThai(t.get("trang_thai"));
            String lyDo = chuNeuCo(t.get("ly_do"));
            Object canCu = t.get("trich_dan") != null ? t.get("trich_dan") : t.get("cong_thuc");
            if (so == 2 && t.get("trich_dan") != null) {
                // Trích dẫn của tầng 2 ghi thành căn cứ bất biến dù tầng DAT hay SAI (căn cứ của bài bị chặn cũng phải giữ).
                Optional<List<UUID>> doan = doanTrichDan(danhSach(t.get("trich_dan")), kho);
                if (doan.isPresent()) {
                    trichDan.addAll(doan.get());
                } else if (trangThai == CheckStatus.DAT) {
                    // Đóng mặc định: dịch vụ toán nói có căn cứ mà không chỉ được về đoạn đã lưu thì không coi là đạt.
                    trangThai = CheckStatus.KHONG_KIEM_DUOC;
                    lyDo = "Trích dẫn của dịch vụ toán không ánh xạ được về đoạn tài liệu đã lưu của lớp.";
                }
            } else if (so == 2 && trangThai == CheckStatus.DAT) {
                trangThai = CheckStatus.KHONG_KIEM_DUOC;
                lyDo = "Tầng 2 đạt mà dịch vụ toán không trích dẫn đoạn nào.";
            }
            Number tinCay = (Number) t.get("do_tin_cay");
            tang.add(new TierResult(so, trangThai, chuNeuCo(t.get("loai_ket_qua")), t.get("buoc_sai") == null ? null : json(t.get("buoc_sai")),
                chuNeuCo(t.get("ma_loi")), tinCay == null ? null : tinCay.doubleValue(), lyDo, canCu == null ? null : json(canCu), json(t)));
        }
        VerificationRun luot = VerificationRun.forProblem(lop, p.id(), p.contentHash(), phienBan, bang.id(), tang,
            List.copyOf(trichDan), clock.instant());
        giaoDich.executeWithoutResult(t -> {
            runs.save(luot);
            apPhatHanh(lop, p, luot, bang);
        });
        return true;
    }

    private void apPhatHanh(UUID lop, Problem p, VerificationRun luot, FormulaSheet bang) {
        ProblemRelease hienTai = releases.find(lop, p.id()).orElseGet(() -> ProblemRelease.draft(lop, p.id(), clock.instant()));
        if (luot.id().equals(hienTai.runId())) {
            return;
        }
        releases.save(hienTai.apply(luot, true, p.contentHash(), bang.id(), clock.instant()));
    }

    /** Đoạn của mọi trích dẫn tầng 2; rỗng nếu danh sách trống hay có trích dẫn không ánh xạ được (không đoán). */
    private static Optional<List<UUID>> doanTrichDan(List<Map<String, @Nullable Object>> trichDan, Map<String, TaiLieuLop> kho) {
        Set<UUID> doan = new LinkedHashSet<>();
        for (Map<String, @Nullable Object> tr : trichDan) {
            TaiLieuLop tl = kho.get(chuNeuCo(tr.get("document_id")));
            Object viTri = tr.get("vi_tri");
            String trich = chuNeuCo(tr.get("trich"));
            if (tl == null || !(viTri instanceof Number so) || trich == null) {
                return Optional.empty();
            }
            Optional<List<DocumentPassage>> trung = ChiaDoan.doanCua(tl.doan(), tl.taiLieu().textContent(), so.intValue(), trich);
            if (trung.isEmpty()) {
                return Optional.empty();
            }
            trung.get().forEach(d -> doan.add(d.id()));
        }
        return doan.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(doan));
    }

    // ---- Tiện ích ---------------------------------------------------------------------------------------------------

    private static Map<ReleaseStatus, Long> demTheoTrangThai(Map<String, ReleaseStatus> phatHanh) {
        Map<ReleaseStatus, Long> dem = new LinkedHashMap<>();
        phatHanh.values().forEach(s -> dem.merge(s, 1L, Long::sum));
        return dem;
    }

    private static CheckStatus trangThai(@Nullable Object o) {
        return CheckStatus.valueOf(chu(o));
    }

    private static String json(Object o) {
        return JSON.writeValueAsString(o);
    }

    private static String chu(@Nullable Object o) {
        return (String) Objects.requireNonNull(o, "thiếu trường bắt buộc");
    }

    private static @Nullable String chuNeuCo(@Nullable Object o) {
        return o == null ? null : (String) o;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> doiTuong(@Nullable Object o) {
        return o == null ? Map.of() : (Map<String, @Nullable Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, @Nullable Object>> danhSach(@Nullable Object o) {
        return o == null ? List.of() : (List<Map<String, @Nullable Object>>) o;
    }
}
