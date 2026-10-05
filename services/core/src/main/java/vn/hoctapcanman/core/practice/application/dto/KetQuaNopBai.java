package vn.hoctapcanman.core.practice.application.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Phản hồi nộp bài (contracts/api-core.md, {@code /nop}): kết quả cả bài, mức hiểu đổi thế nào (rỗng khi chưa có module
 * mức hiểu), lời giải mẫu viết cho học sinh chỉ khi được mở (FR-006), không thì {@code null}. Gửi lại sau khi đã nộp thì
 * {@code ketQua} và {@code mucHieu} y hệt lần đầu; {@code loiGiai} tính lúc trả lời, theo cờ lớp lúc đó.
 */
public record KetQuaNopBai(KetQuaBai ketQua, List<ThayDoiMucHieu> mucHieu, @Nullable String loiGiai) {

    public KetQuaNopBai {
        mucHieu = List.copyOf(mucHieu);
    }
}
