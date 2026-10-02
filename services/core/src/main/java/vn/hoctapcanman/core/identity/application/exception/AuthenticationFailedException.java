package vn.hoctapcanman.core.identity.application.exception;

/** Một thông điệp cho mọi lý do (sai email, sai mật khẩu, tài khoản khóa, token hỏng) để không lộ tài khoản. */
public class AuthenticationFailedException extends RuntimeException {

    public static final String THONG_BAO = "Email hoặc mật khẩu không đúng.";
    public static final String PHIEN_HET_HAN = "Phiên đăng nhập đã hết hạn. Đăng nhập lại.";

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
