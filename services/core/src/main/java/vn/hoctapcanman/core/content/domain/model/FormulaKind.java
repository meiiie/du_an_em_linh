package vn.hoctapcanman.core.content.domain.model;

/** Loại dòng của bảng công thức, do job {@code kiem-dong-cong-thuc} đọc ra ({@code loai} trong contracts/math-v1.md). */
public enum FormulaKind {
    DANG_THUC,
    DINH_LI,
    /** Máy chưa đọc trọn mệnh đề: tầng 1 {@code KHONG_KIEM_DUOC}. */
    KHONG_BIET
}
