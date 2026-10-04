package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Hai job kiểm của {@code services/math} mà lần nhập theo lớp dùng (T012b): {@code /v1/kiem-dong-cong-thuc} (tầng 1 và 2 của
 * từng dòng bảng, lúc khóa bảng; ADR 013) và {@code /v1/verify} (cổng 3 tầng của một bài với kho của lớp). Dịch vụ toán lỗi,
 * hết giờ hay trả phong bì lỗi thì ném {@link DichVuToanKhongTraLoi}: lần nhập của lớp dừng, không khóa bảng hay phát hành
 * gì theo phán quyết không có.
 */
public interface KiemToan {

    Map<String, @Nullable Object> kiemDongCongThuc(Map<String, ?> yeuCau);

    Map<String, @Nullable Object> kiemBai(Map<String, ?> yeuCau);
}
