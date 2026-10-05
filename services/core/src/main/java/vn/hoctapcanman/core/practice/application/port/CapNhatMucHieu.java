package vn.hoctapcanman.core.practice.application.port;

import java.util.List;
import vn.hoctapcanman.core.practice.application.dto.BaiDaNop;
import vn.hoctapcanman.core.practice.application.dto.ThayDoiMucHieu;

/**
 * Mức hiểu sau khi nộp bài: cổng ra của practice, module mức hiểu (T050) hiện thực; practice không biết gì về BKT. Nộp bài
 * gọi mọi hiện thực (hôm nay chưa có, nên {@code mucHieu} rỗng) trong giao dịch nộp: ném thì bài làm không nộp. Gửi nộp lại
 * sau khi đã nộp gọi lại với cùng {@link BaiDaNop}, nên hiện thực phải idempotent theo {@code baiLamId}: không ghi lần hai,
 * trả đúng các thay đổi lần đầu.
 */
public interface CapNhatMucHieu {

    /** Mức hiểu từng kỹ năng đổi thế nào do bài vừa nộp; rỗng khi không kỹ năng nào đổi. */
    List<ThayDoiMucHieu> sauKhiNop(BaiDaNop bai);
}
