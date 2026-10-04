package vn.hoctapcanman.core.content.domain.event;

import java.util.UUID;

/**
 * Lớp vừa khóa một bảng công thức mới (bảng đang dùng của lớp đổi). Gợi ý của mọi bài phải kiểm lại với bảng này trước khi
 * tới học sinh (T034b: {@code /v1/kiem-loi-giang}, lưu theo phiên bản bảng).
 */
public record BangCongThucDaKhoa(UUID lopId, UUID bangId, int phienBan) {}
