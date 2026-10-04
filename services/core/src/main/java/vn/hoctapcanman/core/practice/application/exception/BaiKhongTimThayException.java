package vn.hoctapcanman.core.practice.application.exception;

/**
 * Không có bài để làm (web: 404, contracts/api-core.md): bài không có, chưa phát hành ở lớp, không chấm từng bước được, hay
 * người gọi không phải học sinh của lớp. Mọi trường hợp cùng một thông điệp, không lộ bài hay lớp có tồn tại không.
 */
public class BaiKhongTimThayException extends RuntimeException {

    public static final String THONG_BAO = "Bài chưa mở hoặc không chấm được.";

    public BaiKhongTimThayException() {
        super(THONG_BAO);
    }
}
