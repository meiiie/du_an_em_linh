package vn.hoctapcanman.mau.tot.content.application.port;

import java.util.Optional;
import java.util.UUID;

/** Mẫu: cổng lời giải sau khi nộp của module nội dung. */
public interface LoiGiaiSauKhiNop {

    Optional<String> vanBan(UUID lopId, UUID problemId, int phienBan);
}
