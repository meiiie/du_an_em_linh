package vn.hoctapcanman.core.practice.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Nghi đoán mò như v0 ({@code nghiDoanMo} của {@code apps/web/lib/learning.ts}): một ô bị đổi từ {@code nguong} lần trở lên
 * trước khi nộp. Đếm mọi sự kiện có ô của bài làm (v0 gửi lại mọi sự kiện từ lúc mở bài ở mỗi lần nộp, core lưu dồn nên
 * đếm trên toàn bộ); sự kiện không gắn ô không tính. Lý do là ô đầu tiên chạm ngưỡng theo thứ tự xuất hiện.
 */
public final class NghiDoanMo {

    private NghiDoanMo() {}

    public static Optional<String> lyDo(List<InputEvent> suKien, int nguong) {
        if (nguong < 1) {
            throw new IllegalArgumentException("Ngưỡng đoán mò phải từ 1");
        }
        Map<String, Integer> dem = new LinkedHashMap<>();
        for (InputEvent e : suKien) {
            if (e.cellRow() != null) {
                dem.merge(e.cellRow() + ":" + e.cellK(), 1, Integer::sum);
            }
        }
        return dem.entrySet().stream().filter(e -> e.getValue() >= nguong).findFirst()
            .map(e -> "Ô " + e.getKey() + " bị đổi " + e.getValue() + " lần trước khi nộp (ngưỡng " + nguong + ").");
    }
}
