package vn.hoctapcanman.core.mastery.application.dto;

import java.util.UUID;

/**
 * Mức hiểu của một học sinh ở một kỹ năng cho giáo viên: mức 4 ({@code NHAN_BIET} … {@code VAN_DUNG_CAO}; 3 mức CV 7991 chỉ
 * đổi lúc hiển thị, ADR 004), kẹt (sai liền từ {@code so_luot_ket} lượt).
 */
public record MucHieuHocSinh(UUID hocSinhId, String kyNang, String muc4, boolean ket) {}
