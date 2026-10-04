package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.shared.infrastructure.math.MathJob;
import vn.hoctapcanman.core.shared.infrastructure.math.MathResult;
import vn.hoctapcanman.core.shared.infrastructure.math.MathServiceClient;

/** {@link GiaiToan} qua {@link MathServiceClient} (đóng mặc định, #82); {@link MathResult.Failed} thành ngoại lệ. */
@Component
public class GiaiToanQuaDichVu implements GiaiToan {

    private final MathServiceClient math;

    public GiaiToanQuaDichVu(MathServiceClient math) {
        this.math = math;
    }

    @Override
    public Map<String, @Nullable Object> giai(Map<String, ?> yeuCau) {
        return than(MathJob.SOLVE, math.call(MathJob.SOLVE, yeuCau));
    }

    @Override
    public Map<String, @Nullable Object> sinh(Map<String, ?> yeuCau) {
        return than(MathJob.GENERATE, math.call(MathJob.GENERATE, yeuCau));
    }

    private static Map<String, @Nullable Object> than(MathJob job, MathResult kq) {
        return switch (kq) {
            case MathResult.Ok ok -> ok.body();
            case MathResult.Failed loi -> throw new DichVuToanKhongTraLoi(job, loi.reason());
        };
    }
}
