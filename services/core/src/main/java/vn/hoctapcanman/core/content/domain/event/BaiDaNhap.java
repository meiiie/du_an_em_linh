package vn.hoctapcanman.core.content.domain.event;

import java.util.UUID;

/**
 * Nội dung chung của một bài vừa được ghi (mới hay ghi lại), ở phiên bản nội dung {@code phienBanNoiDung}. Gợi ý của bài phải
 * kiểm với bảng công thức đang dùng của từng lớp (T034b).
 */
public record BaiDaNhap(UUID baiId, String ma, int phienBanNoiDung) {}
