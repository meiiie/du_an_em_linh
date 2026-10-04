package vn.hoctapcanman.core.content.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.application.dto.hocsinh.DeBaiChoHocSinh;

/**
 * Đọc đề bài cho học sinh của một lớp (T015): chỉ bài đã phát hành ở lớp đó ({@code DA_PHAT_HANH}), không bao giờ lời giải
 * (FR-006). Không kiểm người gọi thuộc lớp: nơi gọi kiểm bằng {@code ClassMembership} của module lớp học trước (F-08).
 */
public interface DeBaiHocSinh {

    /** Các bài đã phát hành ở lớp, theo mã bài. */
    List<DeBaiChoHocSinh> baiDaPhatHanh(UUID lopId);

    /** Bài {@code maBai} nếu đã phát hành ở lớp; chưa phát hành, bị chặn hay chờ duyệt thì rỗng, như không có bài. */
    Optional<DeBaiChoHocSinh> bai(UUID lopId, String maBai);
}
