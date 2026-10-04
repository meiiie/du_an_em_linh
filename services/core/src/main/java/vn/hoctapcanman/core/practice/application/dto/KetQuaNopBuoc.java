package vn.hoctapcanman.core.practice.application.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Kết quả nộp một bước (contracts/api-core.md): {@code DAT}, {@code SAI}, {@code KHONG_KIEM_DUOC} hay {@code KHONG_CHAM_DUOC}
 * (dịch vụ toán lỗi, không bao giờ là đạt), thông báo của bộ chấm, chỗ sai để tô, mã lỗi, bước kế khi đạt. Không có giá trị
 * đúng nào (FR-006).
 */
public record KetQuaNopBuoc(String ketQua, String thongBao, List<ViTriSai> oSai, @Nullable String maLoi, @Nullable String buocKe) {

    public KetQuaNopBuoc {
        oSai = List.copyOf(oSai);
    }
}
