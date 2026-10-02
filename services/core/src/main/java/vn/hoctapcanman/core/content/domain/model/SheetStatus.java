package vn.hoctapcanman.core.content.domain.model;

/** Trạng thái của một phiên bản bảng công thức. */
public enum SheetStatus {
    /** Giáo viên đang sửa; chưa dùng cho cổng. */
    NHAP,
    /** Đã khóa sau khi mọi dòng {@code DAT} ở tầng 1 và tầng 2 (ADR 013); không sửa được nữa. */
    KHOA
}
