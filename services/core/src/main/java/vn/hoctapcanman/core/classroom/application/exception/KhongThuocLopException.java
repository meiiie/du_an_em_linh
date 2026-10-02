package vn.hoctapcanman.core.classroom.application.exception;

/** Người dùng không có vai trò cần thiết trong lớp (web: 403, contracts/api-core.md). */
public class KhongThuocLopException extends RuntimeException {

    public static final String THONG_BAO = "Bạn không phải giáo viên của lớp này.";

    public KhongThuocLopException() {
        super(THONG_BAO);
    }
}
