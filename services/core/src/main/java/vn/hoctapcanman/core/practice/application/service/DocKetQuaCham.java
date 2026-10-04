package vn.hoctapcanman.core.practice.application.service;

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
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.dto.ViTriSai;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;

/**
 * Đọc phản hồi {@code /v1/grade} thành {@link GradingResult} như v0 ghi {@code grading_results}, và kết quả chấm thành phản
 * hồi cho học sinh. Đóng mặc định (FR-009): không có phản hồi, mã {@code ket_qua} lạ, thiếu trường của phong bì hay trường
 * sai kiểu thì {@code KHONG_CHAM_DUOC} với thông báo chung (chi tiết chỉ vào log, không kèm nội dung bài làm), không bao giờ
 * là đạt.
 *
 * <p>Phong bì bắt buộc là {@link #PHONG_BI}: đúng 12 khóa mà {@code _pack} của {@code services/math/app/grader.py} luôn trả,
 * mỗi khóa đúng kiểu, và {@code nop_toi} đúng bước đã nộp (mọi lần gọi {@code _pack} đều truyền {@code nop_toi=den}). Phản
 * hồi thiếu như {@code {"ket_qua":"DAT"}} không bao giờ thành đạt (Codex #140, ba vòng). {@code DAT} còn phải trùng trọn phong
 * bì mà {@code _pack} sinh cho bước đã nộp (xem {@link #datDungPhongBi}). Khóa thêm của hậu xử lý ({@code toan_dung},
 * {@code ly_do}, {@code chi_tiet_tu_choi}) được phép.
 */
public final class DocKetQuaCham {

    /** Thông báo khi dịch vụ toán không chấm được, như v0 (B-20: học sinh nhận câu chung). */
    public static final String MAY_BAN = "Máy chấm đang bận. Em thử lại sau ít giây nhé.";
    /** Câu thêm vào thông báo khi bài làm bị nghi đoán mò, như v0. */
    public static final String DOAN_MO = "Bài này bị đánh dấu đoán mò nên không tính lên mức.";

    private static final Logger LOG = LoggerFactory.getLogger(DocKetQuaCham.class);
    /** Khóa và kiểu của phong bì {@code _pack} ({@code grader.py}, hàm {@code _pack}): null chỉ hợp lệ ở khóa ghi «hay null». */
    private static final Map<String, Predicate<@Nullable Object>> PHONG_BI = Map.ofEntries(
        Map.entry("ket_qua", v -> v instanceof String),
        Map.entry("loai_ket_qua", v -> v instanceof String),
        Map.entry("buoc_sai", v -> v == null || v instanceof Map<?, ?>),
        Map.entry("ma_loi", v -> v == null || v instanceof String),
        Map.entry("do_tin_cay", v -> v == null || v instanceof Number),
        Map.entry("per_buoc", v -> v instanceof Map<?, ?> m && m.keySet().stream().allMatch(String.class::isInstance)
            && m.values().stream().allMatch(String.class::isInstance)),
        Map.entry("cac_van_de", v -> v instanceof List<?>),
        Map.entry("chuan_hoa", v -> v instanceof List<?>),
        Map.entry("phien_ban_chuan_hoa", v -> v instanceof String),
        Map.entry("thong_bao", v -> v instanceof String),
        Map.entry("chua_xong", v -> v instanceof Boolean),
        Map.entry("nop_toi", v -> v instanceof String));
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private DocKetQuaCham() {}

    /** Kết quả chấm bài làm {@code baiLam} tới bước {@code nopToi} của khung {@code khung} cho yêu cầu có băm {@code bam}. */
    public static GradingResult ketQua(UUID baiLam, String nopToi, List<String> khung, String bam,
            @Nullable Map<String, @Nullable Object> phanHoi, Instant luc) {
        if (phanHoi == null) {
            return GradingResult.notGraded(baiLam, nopToi, bam, MAY_BAN, luc);
        }
        try {
            return doc(baiLam, nopToi, khung, bam, phanHoi, luc);
        } catch (RuntimeException e) {
            LOG.warn("Phản hồi chấm không đọc được ({}), coi như không chấm được", e.getClass().getSimpleName());
            return GradingResult.notGraded(baiLam, nopToi, bam, MAY_BAN, luc);
        }
    }

    private static GradingResult doc(UUID baiLam, String nopToi, List<String> khung, String bam, Map<String, @Nullable Object> p,
            Instant luc) {
        PHONG_BI.forEach((khoa, dungKieu) -> {
            if (!p.containsKey(khoa) || !dungKieu.test(p.get(khoa))) {
                throw new IllegalArgumentException("Phong bì chấm thiếu hay sai kiểu " + khoa);
            }
        });
        GradeStatus ketQua = GradeStatus.tuDichVuToan(p.get("ket_qua"))
            .orElseThrow(() -> new IllegalArgumentException("ket_qua lạ"));
        if (!nopToi.equals(p.get("nop_toi"))) {
            throw new IllegalArgumentException("nop_toi khác bước đã nộp");
        }
        String loai = (String) Objects.requireNonNull(p.get("loai_ket_qua"));
        List<?> vanDe = (List<?>) Objects.requireNonNull(p.get("cac_van_de"));
        Map<String, String> perBuoc = new LinkedHashMap<>();
        ((Map<?, ?>) Objects.requireNonNull(p.get("per_buoc"))).forEach((k, v) -> perBuoc.put((String) k, (String) v));
        if (ketQua == GradeStatus.DAT && !datDungPhongBi(p, loai, vanDe, perBuoc, nopToi, khung)) {
            throw new IllegalArgumentException("Phong bì DAT khác phong bì _pack sinh cho bước đã nộp");
        }
        Number tinCay = (Number) p.get("do_tin_cay");
        // Cờ dấu U (0002c) chỉ có nghĩa khi SAI và là boolean, như laDauU của v0.
        Boolean toanDung = ketQua == GradeStatus.SAI && p.get("toan_dung") instanceof Boolean b ? b : null;
        return new GradingResult(UUID.randomUUID(), baiLam, nopToi, bam, ketQua, (String) p.get("loai_ket_qua"), json(p.get("buoc_sai")),
            (String) p.get("ma_loi"), tinCay == null ? null : tinCay.doubleValue(), perBuoc, (String) p.get("thong_bao"),
            json(p.get("cac_van_de")), toanDung, Boolean.TRUE.equals(p.get("chua_xong")), (String) p.get("phien_ban_chuan_hoa"),
            json(p.get("chuan_hoa")), luc);
    }

    /**
     * Phong bì {@code DAT} mà {@code _pack(..., _per(den), ..., nop_toi=den)} của {@code grader.py} sinh cho bước {@code nopToi}:
     * loại {@code DAT}; không bước sai, mã lỗi, độ tin cậy, vấn đề; {@code nop_toi} đúng bước; {@code per_buoc} đúng
     * {@code _per(den)}, tức mọi bước từ đầu khung tới bước nộp, đều {@code DAT}; {@code chua_xong} đúng bằng «bước nộp chưa phải
     * bước cuối» (nhánh {@code chua_xong=True} còn lại của {@code grader.py} cần một lỗi ở bước sau bước nộp, nên không xảy ra ở
     * bước cuối). So trọn phong bì, không thêm từng điều kiện.
     */
    private static boolean datDungPhongBi(Map<String, @Nullable Object> p, String loai, List<?> vanDe, Map<String, String> perBuoc,
            String nopToi, List<String> khung) {
        int den = khung.indexOf(nopToi);
        if (den < 0) {
            return false;
        }
        Map<String, String> perDat = new LinkedHashMap<>();
        khung.subList(0, den + 1).forEach(b -> perDat.put(b, "DAT"));
        return "DAT".equals(loai) && p.get("buoc_sai") == null && p.get("ma_loi") == null && p.get("do_tin_cay") == null
            && vanDe.isEmpty() && perBuoc.equals(perDat) && Boolean.valueOf(den != khung.size() - 1).equals(p.get("chua_xong"));
    }

    /**
     * Phản hồi cho học sinh: kết quả, thông báo (thêm câu đoán mò khi bài làm bị nghi), chỗ sai để tô (bước sai gốc rồi bước
     * sai của từng vấn đề, không trùng), mã lỗi, bước kế trong khung khi đạt mà chưa xong khung. Không có giá trị đúng nào.
     */
    public static KetQuaNopBuoc choHocSinh(GradingResult g, List<String> khung, boolean nghiDoanMo) {
        String thongBao = g.message() == null ? "" : g.message();
        if (nghiDoanMo) {
            thongBao = thongBao.isEmpty() ? DOAN_MO : thongBao + " " + DOAN_MO;
        }
        Set<ViTriSai> oSai = new LinkedHashSet<>();
        viTri(cay(g.wrongStepsJson())).ifPresent(oSai::add);
        JsonNode vanDe = cay(g.issuesJson());
        if (vanDe != null && vanDe.isArray()) {
            for (JsonNode v : vanDe) {
                viTri(v.get("buoc_sai")).ifPresent(oSai::add);
            }
        }
        String buocKe = null;
        int vt = khung.indexOf(g.stepCode());
        if (g.result() == GradeStatus.DAT && vt >= 0 && vt + 1 < khung.size()) {
            buocKe = khung.get(vt + 1);
        }
        return new KetQuaNopBuoc(g.result().name(), thongBao, new ArrayList<>(oSai), g.errorCode(), buocKe);
    }

    private static Optional<ViTriSai> viTri(@Nullable JsonNode b) {
        if (b == null || !b.isObject() || !b.path("ma_buoc").isString()) {
            return Optional.empty();
        }
        JsonNode o = b.path("o");
        Integer dong = b.path("dong").isInt() ? b.path("dong").intValue() : null;
        String hang = o.path("hang").isString() ? o.path("hang").stringValue() : null;
        Integer k = o.path("k").isInt() ? o.path("k").intValue() : null;
        return Optional.of(new ViTriSai(b.path("ma_buoc").stringValue(), dong, hang, k));
    }

    private static @Nullable String json(@Nullable Object giaTri) {
        return giaTri == null ? null : JSON.writeValueAsString(giaTri);
    }

    private static @Nullable JsonNode cay(@Nullable String json) {
        return json == null ? null : JSON.readTree(json);
    }
}
