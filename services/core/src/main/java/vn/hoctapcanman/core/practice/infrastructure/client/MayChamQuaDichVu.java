package vn.hoctapcanman.core.practice.infrastructure.client;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.practice.application.port.MayCham;
import vn.hoctapcanman.core.shared.infrastructure.math.MathJob;
import vn.hoctapcanman.core.shared.infrastructure.math.MathResult;
import vn.hoctapcanman.core.shared.infrastructure.math.MathServiceClient;

/**
 * {@link MayCham} qua {@link MathServiceClient} ({@link MathJob#GRADE}, hết giờ 12 s, đóng mặc định #82). Thất bại thì rỗng;
 * log chỉ ghi lý do, không ghi bài làm của học sinh.
 */
@Component
public class MayChamQuaDichVu implements MayCham {

    private static final Logger LOG = LoggerFactory.getLogger(MayChamQuaDichVu.class);

    private final MathServiceClient math;
    private final vn.hoctapcanman.core.tutor.application.port.TermFeedback terms;

    public MayChamQuaDichVu(MathServiceClient math, vn.hoctapcanman.core.tutor.application.port.TermFeedback terms) {
        this.terms = terms;
        this.math = math;
    }

    @Override
    public Optional<Map<String, @Nullable Object>> cham(Map<String, ?> yeuCau) {
        return switch (math.call(MathJob.GRADE, yeuCau)) {
            case MathResult.Ok ok -> Optional.of(phanHoi(ok.body(), yeuCau));
            case MathResult.Failed loi -> {
                LOG.warn("Chấm bước không được: {}", loi.reason());
                yield Optional.empty();
            }
        };
    }
    private Map<String, @Nullable Object> phanHoi(Map<String, @Nullable Object> body, Map<String, ?> request) {
        if (!"SAI".equals(body.get("ket_qua")) || !(body.get("buoc_sai") instanceof Map<?,?> wrong)
            || !"B.DH.DAOHAM".equals(wrong.get("ma_buoc")) || !(wrong.get("dong") instanceof Number line)
            || !(request.get("ham") instanceof String function) || !(request.get("cac_buoc") instanceof List<?> steps)) return body;
        for (var step : steps) {
            if (!(step instanceof Map<?,?> s) || !"B.DH.DAOHAM".equals(s.get("ma_buoc"))
                || !(s.get("cac_dong") instanceof List<?> lines)) continue;
            for (var row : lines) {
                if (!(row instanceof Map<?,?> r) || !line.equals(r.get("dong")) || !(r.get("latex") instanceof String expression)) continue;
                var feedback = terms.derivative(function, expression);
                if (feedback.isEmpty()) return body;
                var notes = new java.util.ArrayList<String>();
                for (var term : feedback.get().terms()) {
                    if (term.status().equals("sai")) notes.add("Số hạng thứ " + term.index() + " «" + term.text() + "» cần xem lại.");
                    if (term.status().equals("thua")) notes.add("Số hạng thứ " + term.index() + " «" + term.text() + "» đang thừa.");
                }
                if (feedback.get().missingCount() > 0) notes.add("Dòng này còn thiếu " + feedback.get().missingCount() + " số hạng.");
                if (notes.isEmpty()) return body;
                var result = new java.util.LinkedHashMap<String, @Nullable Object>(body);
                result.put("thong_bao", "Bước đạo hàm, dòng " + (line.intValue() + 1) + ": " + String.join(" ", notes));
                return result;
            }
        }
        return body;
    }

}
