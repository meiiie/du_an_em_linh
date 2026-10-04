package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.shared.infrastructure.math.MathJob;
import vn.hoctapcanman.core.shared.infrastructure.math.MathResult;
import vn.hoctapcanman.core.shared.infrastructure.math.MathServiceClient;

/** {@link GiaiToan} qua {@link MathServiceClient} (đóng mặc định, #82). */
@Component
public class GiaiToanQuaDichVu implements GiaiToan {

    private final MathServiceClient math;

    public GiaiToanQuaDichVu(MathServiceClient math) {
        this.math = math;
    }

    @Override
    public Optional<Map<String, @Nullable Object>> giai(Map<String, ?> yeuCau) {
        return ok(math.call(MathJob.SOLVE, yeuCau));
    }

    @Override
    public Optional<Map<String, @Nullable Object>> sinh(Map<String, ?> yeuCau) {
        return ok(math.call(MathJob.GENERATE, yeuCau));
    }

    private static Optional<Map<String, @Nullable Object>> ok(MathResult kq) {
        return kq instanceof MathResult.Ok ok ? Optional.of(ok.body()) : Optional.empty();
    }
}
