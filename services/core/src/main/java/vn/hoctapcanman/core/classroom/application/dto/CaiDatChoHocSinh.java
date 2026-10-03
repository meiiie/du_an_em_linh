package vn.hoctapcanman.core.classroom.application.dto;

import java.util.UUID;

/**
 * Phần cài lớp học sinh được biết (#111): chỉ cờ học sinh cần, không có nhà AI hay ai đổi cài. Practice dùng
 * {@code moLoiGiaiSauKhiNop} để quyết có mở lời giải sau khi nộp không (FR-006, mặc định tắt).
 */
public record CaiDatChoHocSinh(UUID lopId, boolean moLoiGiaiSauKhiNop) {}
