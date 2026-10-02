package vn.hoctapcanman.core.classroom.application.port;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Ghi cảnh báo cho giáo viên của lớp học sinh đang học (FR-025): kẹt do mastery phát hiện ({@code KET}) và «Gửi thầy
 * cô» do học sinh bấm ở gia sư ({@code NHO_GV}). Lớp lấy từ ghi danh của học sinh, không nhận từ bên gọi. Không ghi
 * trùng khi đã có cảnh báo cùng loại, cùng kỹ năng, cùng bài chưa xử lý (như v0 kiểm trước khi ghi).
 *
 * <p>Bên gọi bảo đảm: {@code hocSinhId} là người đã xác thực (access token) hay chủ bài làm, không lấy từ thân yêu cầu;
 * mã kỹ năng, bài, bước là của bài đã phát hành cho lớp học sinh. {@code lyDo} do hệ thống soạn theo mẫu (như v0:
 * «Kẹt 3 lượt ở …», «Em nhờ thầy cô ở bước …»), <strong>không</strong> chứa câu học sinh gõ hay câu của mô hình: lý do
 * hiện thẳng cho giáo viên và được giữ lâu.
 */
public interface CanhBaoGiaoVien {

    /** Kết quả ghi, để bên gọi nói đúng cho học sinh lời nhờ đã tới thầy cô hay chưa. */
    enum KetQua {
        /** Đã ghi một cảnh báo mới cho giáo viên của lớp. */
        DA_GHI,
        /** Đã có cảnh báo mở cùng loại, cùng kỹ năng, cùng bài: không ghi thêm. */
        DA_CO_CANH_BAO_MO,
        /** Học sinh chưa thuộc lớp nào: không có ai để báo. */
        CHUA_THUOC_LOP,
        /** Lớp chưa có giáo viên: không ghi, vì không ai đọc. */
        LOP_CHUA_CO_GIAO_VIEN
    }

    KetQua ghiKet(UUID hocSinhId, String kyNang, @Nullable String maBai, @Nullable String maBuoc, String lyDo);

    KetQua ghiNhoGiaoVien(UUID hocSinhId, String kyNang, String maBai, @Nullable String maBuoc, String lyDo);
}
