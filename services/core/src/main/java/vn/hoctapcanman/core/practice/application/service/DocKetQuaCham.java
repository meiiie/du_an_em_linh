package vn.hoctapcanman.core.practice.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
 * <p>Phong bì bắt buộc là phong bì mà {@code _pack} của {@code services/math/app/grader.py} luôn trả (Codex #140: phản hồi
 * thiếu như {@code {"ket_qua":"DAT"}} không được thành đạt): {@code ket_qua}, {@code loai_ket_qua} (chữ), {@code per_buoc}
 * (đối tượng chữ → chữ), {@code thong_bao} (chữ), {@code chua_xong} (boolean), {@code cac_van_de} (danh sách), và có mặt
 * {@code buoc_sai} (đối tượng hay null), {@code ma_loi} (chữ hay null), {@code do_tin_cay} (số hay null). {@code nop_toi} có
 * thì phải đúng bước đã nộp. {@code DAT} phải trùng trọn phong bì mà {@code _pack} sinh cho bước đã nộp (xem
 * {@link #datDungPhongBi}).
 */
public final class DocKetQuaCham {

    /** Thông báo khi dịch vụ toán không chấm được, như v0 (B-20: học sinh nhận câu chung). */
    public static final String MAY_BAN = "Máy chấm đang bận. Em thử lại sau ít giây nhé.";
    /** Câu thêm vào thông báo khi bài làm bị nghi đoán mò, như v0. */
    public static final String DOAN_MO = "Bài này bị đánh dấu đoán mò nên không tính lên mức.";

    private static final Logger LOG = LoggerFactory.getLogger(DocKetQuaCham.class);
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
        GradeStatus ketQua = GradeStatus.tuDichVuToan(p.get("ket_qua"))
            .orElseThrow(() -> new IllegalArgumentException("ket_qua lạ"));
        for (String khoa : List.of("buoc_sai", "ma_loi", "do_tin_cay")) {
            if (!p.containsKey(khoa)) {
                throw new IllegalArgumentException("Phong bì chấm thiếu " + khoa);
            }
        }
        if (!(p.get("loai_ket_qua") instanceof String loai) || !(p.get("thong_bao") instanceof String)
                || !(p.get("chua_xong") instanceof Boolean) || !(p.get("cac_van_de") instanceof List<?> vanDe)
                || !(p.get("per_buoc") instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("Phong bì chấm thiếu trường bắt buộc");
        }
        if (p.get("buoc_sai") != null && !(p.get("buoc_sai") instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("buoc_sai không phải đối tượng");
        }
        if (p.get("nop_toi") != null && !nopToi.equals(p.get("nop_toi"))) {
            throw new IllegalArgumentException("nop_toi khác bước đã nộp");
        }
        Map<String, String> perBuoc = new LinkedHashMap<>();
        m.forEach((k, v) -> perBuoc.put((String) k, (String) v));
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
     * {@code _per(den)}, tức mọi bước từ đầu khung tới bước nộp, đều {@code DAT}; chưa tới bước cuối thì {@code chua_xong}. So
     * trọn phong bì, không thêm từng điều kiện (Codex #140 hai vòng chỉ ra phần kiểm từng trường còn sót).
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
            && vanDe.isEmpty() && nopToi.equals(p.get("nop_toi")) && perBuoc.equals(perDat)
            && (den == khung.size() - 1 || Boolean.TRUE.equals(p.get("chua_xong")));
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
