package vn.hoctapcanman.core.content.application.port;

import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.application.dto.KhungCham;

/** Kho nội dung chấm phía máy chủ, khóa bằng đúng bài và phiên bản nội dung. */
public interface KhungChamTheoBai {
    Optional<KhungCham> doc(UUID problemId, int phienBan);
    /** Khung cùng phiên bản nội dung bất biến; đổi khung phải tăng phiên bản nội dung của bài. */
    void ghi(UUID problemId, int phienBan, KhungCham khung);
}
