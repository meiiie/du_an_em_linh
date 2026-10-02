package vn.hoctapcanman.core.classroom.application.port;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Ghi cảnh báo cho giáo viên của lớp học sinh đang học (FR-025): kẹt do mastery phát hiện ({@code KET}) và «Gửi thầy
 * cô» do học sinh bấm ở gia sư ({@code NHO_GV}). Lớp lấy từ ghi danh của học sinh, không nhận từ bên gọi. Không ghi
 * trùng khi đã có cảnh báo cùng loại, cùng kỹ năng, cùng bài chưa xử lý (như v0 kiểm trước khi ghi).
 */
public interface CanhBaoGiaoVien {

    /** Trả {@code false} khi học sinh chưa thuộc lớp nào hoặc đã có cảnh báo mở trùng. */
    boolean ghiKet(UUID hocSinhId, String kyNang, @Nullable String maBai, @Nullable String maBuoc, String lyDo);

    /** Trả {@code false} khi học sinh chưa thuộc lớp nào hoặc đã có cảnh báo mở trùng. */
    boolean ghiNhoGiaoVien(UUID hocSinhId, String kyNang, String maBai, @Nullable String maBuoc, String lyDo);
}
