package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Hai job của {@code services/math} mà importer dùng: {@code /v1/solve} (máy giải một hàm thành lời giải 5 bước, dữ kiện
 * bảo vệ, thang gợi ý) và {@code /v1/generate} (biến thể có hạt giống cố định). Trả thân JSON khi dịch vụ toán trả lời,
 * kể cả câu trả lời «không dùng được» ({@code dat=false}, {@code loi}). Dịch vụ toán lỗi, hết giờ hay trả phong bì lỗi
 * thì ném {@link DichVuToanKhongTraLoi}: importer dừng cả lần nhập như seed v0, không ghi gì, không bao giờ bịa lời giải.
 */
public interface GiaiToan {

    Map<String, @Nullable Object> giai(Map<String, ?> yeuCau);

    Map<String, @Nullable Object> sinh(Map<String, ?> yeuCau);
}
