package vn.hoctapcanman.mau.xau.content.application.port;

import java.util.Optional;
import java.util.UUID;

/** Mẫu: cổng lời giải sau khi nộp của module nội dung (như cổng thật). */
public interface LoiGiaiSauKhiNop {

    Optional<String> vanBan(UUID lopId, UUID problemId, int phienBan);
}
