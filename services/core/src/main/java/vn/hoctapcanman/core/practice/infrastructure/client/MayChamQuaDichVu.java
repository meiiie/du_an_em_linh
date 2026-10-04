package vn.hoctapcanman.core.practice.infrastructure.client;

import java.util.Map;
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

    public MayChamQuaDichVu(MathServiceClient math) {
        this.math = math;
    }

    @Override
    public Optional<Map<String, @Nullable Object>> cham(Map<String, ?> yeuCau) {
        return switch (math.call(MathJob.GRADE, yeuCau)) {
            case MathResult.Ok ok -> Optional.of(ok.body());
            case MathResult.Failed loi -> {
                LOG.warn("Chấm bước không được: {}", loi.reason());
                yield Optional.empty();
            }
        };
    }
}
