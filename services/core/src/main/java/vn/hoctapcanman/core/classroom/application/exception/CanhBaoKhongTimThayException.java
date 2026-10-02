package vn.hoctapcanman.core.classroom.application.exception;

/**
 * Không có cảnh báo, hoặc cảnh báo thuộc lớp người dùng không dạy (web: 404). Hai trường hợp cùng một thông điệp để
 * không lộ cảnh báo của lớp khác có tồn tại hay không.
 */
public class CanhBaoKhongTimThayException extends RuntimeException {

    public static final String THONG_BAO = "Không tìm thấy cảnh báo.";

    public CanhBaoKhongTimThayException() {
        super(THONG_BAO);
    }
}
