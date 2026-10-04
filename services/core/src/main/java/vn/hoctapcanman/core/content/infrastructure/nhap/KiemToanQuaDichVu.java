package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.shared.infrastructure.math.MathJob;
import vn.hoctapcanman.core.shared.infrastructure.math.MathResult;
import vn.hoctapcanman.core.shared.infrastructure.math.MathServiceClient;

/** {@link KiemToan} qua {@link MathServiceClient} (đóng mặc định, #82); {@link MathResult.Failed} thành ngoại lệ. */
@Component
public class KiemToanQuaDichVu implements KiemToan {

    private final MathServiceClient math;

    public KiemToanQuaDichVu(MathServiceClient math) {
        this.math = math;
    }

    @Override
    public Map<String, @Nullable Object> kiemDongCongThuc(Map<String, ?> yeuCau) {
        return than(MathJob.KIEM_DONG_CONG_THUC, math.call(MathJob.KIEM_DONG_CONG_THUC, yeuCau));
    }

    @Override
    public Map<String, @Nullable Object> kiemBai(Map<String, ?> yeuCau) {
        return than(MathJob.VERIFY, math.call(MathJob.VERIFY, yeuCau));
    }

    private static Map<String, @Nullable Object> than(MathJob job, MathResult kq) {
        return switch (kq) {
            case MathResult.Ok ok -> ok.body();
            case MathResult.Failed loi -> throw new DichVuToanKhongTraLoi(job, loi.reason());
        };
    }
}
