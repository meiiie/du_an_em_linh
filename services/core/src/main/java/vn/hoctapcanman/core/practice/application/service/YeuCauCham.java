package vn.hoctapcanman.core.practice.application.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.practice.domain.model.SignTable;
import vn.hoctapcanman.core.practice.domain.model.StepLine;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.TableCell;

/**
 * Yêu cầu chấm {@code /v1/grade} dựng lại từ các bước đã lưu của bài làm, trùng payload của v0 ({@code nopBuoc} ở
 * {@code apps/web/lib/actions/hs.ts}, {@code payload()} ở {@code components/solve-client.tsx}): {@code ham}, {@code nop_toi},
 * {@code cac_buoc} từ bước bắt đầu tới bước nộp theo thứ tự khung, {@code buoc_bat_dau} khi bài khung ngắn (lấy từ bài, không
 * tin máy học sinh). Mỗi bước: {@code ma_buoc}; {@code khai_bao} khi bước có dòng kết luận (khóa của nhãn theo thứ tự dòng,
 * như v0 dựng từ các ô kết luận); {@code cac_dong} ({@code dong}, {@code latex}, {@code loai} khi có); {@code bang}
 * ({@code loai_bang}, {@code cac_o} theo thứ tự gửi: {@code hang}, {@code k}, {@code gia_tri}).
 *
 * <p>Băm là SHA-256 của JSON payload (UTF-8, khóa theo thứ tự dựng): cùng nội dung các bước tới cùng bước nộp thì cùng băm,
 * nên hai tab nộp cùng bước chỉ có một lần chấm (V7).
 */
public final class YeuCauCham {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    /** Nhãn ô kết luận của v0 (SP-03) và khóa {@code khai_bao} tương ứng. */
    private static final Map<String, String> KHAI_BAO = Map.of(
        "DONG_BIEN", "dong_bien", "NGHICH_BIEN", "nghich_bien", "CUC_DAI", "cuc_dai", "CUC_TIEU", "cuc_tieu");

    private YeuCauCham() {}

    /** Vị trí bước bắt đầu trong khung; không có hay không thuộc khung thì 0, như v0. */
    public static int batDau(List<String> khung, @Nullable String buocBatDau) {
        return buocBatDau == null ? 0 : Math.max(0, khung.indexOf(buocBatDau));
    }

    /**
     * Payload chấm tới bước {@code nopToi}. Bước nộp phải thuộc khung và không trước bước bắt đầu, không thì
     * {@link IllegalArgumentException}.
     */
    public static Map<String, @Nullable Object> dung(String ham, List<String> khung, @Nullable String buocBatDau, String nopToi,
            List<StepWork> daLuu) {
        int dau = batDau(khung, buocBatDau);
        int den = khung.indexOf(nopToi);
        if (den < dau) {
            throw new IllegalArgumentException("Bước nộp không thuộc khung của bài: " + nopToi);
        }
        List<Map<String, @Nullable Object>> cacBuoc = daLuu.stream()
            .filter(b -> khung.indexOf(b.stepCode()) >= dau && khung.indexOf(b.stepCode()) <= den)
            .sorted(Comparator.comparingInt(b -> khung.indexOf(b.stepCode())))
            .map(YeuCauCham::buoc)
            .toList();
        Map<String, @Nullable Object> yeuCau = new LinkedHashMap<>();
        yeuCau.put("ham", ham);
        yeuCau.put("nop_toi", nopToi);
        yeuCau.put("cac_buoc", cacBuoc);
        if (dau > 0) {
            yeuCau.put("buoc_bat_dau", khung.get(dau));
        }
        return yeuCau;
    }

    /** SHA-256 hex chữ thường của JSON payload. */
    public static String bam(Map<String, ?> yeuCau) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(yeuCau)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK thiếu SHA-256", e);
        }
    }

    private static Map<String, @Nullable Object> buoc(StepWork b) {
        Map<String, @Nullable Object> m = new LinkedHashMap<>();
        m.put("ma_buoc", b.stepCode());
        List<String> khaiBao = new ArrayList<>();
        for (StepLine l : b.lines()) {
            String khoa = l.kind() == null ? null : KHAI_BAO.get(l.kind());
            if (khoa != null) {
                khaiBao.add(khoa);
            }
        }
        if (!khaiBao.isEmpty()) {
            m.put("khai_bao", khaiBao);
        }
        if (!b.lines().isEmpty()) {
            m.put("cac_dong", b.lines().stream().map(YeuCauCham::dong).toList());
        }
        SignTable bang = b.table();
        if (bang != null) {
            Map<String, @Nullable Object> t = new LinkedHashMap<>();
            t.put("loai_bang", bang.kind());
            t.put("cac_o", bang.cells().stream().map(YeuCauCham::o).toList());
            m.put("bang", t);
        }
        return m;
    }

    private static Map<String, @Nullable Object> dong(StepLine l) {
        Map<String, @Nullable Object> m = new LinkedHashMap<>();
        m.put("dong", l.lineNo());
        m.put("latex", l.latex());
        if (l.kind() != null) {
            m.put("loai", l.kind());
        }
        return m;
    }

    private static Map<String, @Nullable Object> o(TableCell c) {
        Map<String, @Nullable Object> m = new LinkedHashMap<>();
        m.put("hang", c.row());
        m.put("k", c.k());
        m.put("gia_tri", c.value());
        return m;
    }
}
