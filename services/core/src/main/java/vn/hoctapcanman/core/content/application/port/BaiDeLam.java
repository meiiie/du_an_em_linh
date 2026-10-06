package vn.hoctapcanman.core.content.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;

/**
 * Bài cho học sinh làm ở một lớp (T020, T021): chỉ bài đang phát hành ở lớp đó ({@code DA_PHAT_HANH}), kèm phiên bản nội
 * dung đọc trong cùng câu lệnh với đề, không bao giờ lời giải (FR-006). Không kiểm người gọi thuộc lớp: nơi gọi kiểm bằng
 * {@code ClassMembership} trước (F-08).
 */
public interface BaiDeLam {

    /** Bài {@code maBai} nếu đang phát hành ở lớp; chưa phát hành, bị chặn hay chờ duyệt thì rỗng, như không có bài. */
    Optional<BaiChoLamBai> bai(UUID lopId, String maBai);

    /** Mọi bài đang phát hành ở lớp, theo mã bài. */
    List<BaiChoLamBai> baiDaPhatHanh(UUID lopId);
}
