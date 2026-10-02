package vn.hoctoanai.core.identity.infrastructure.web;

/** Yêu cầu dựa vào cookie mà thiếu header {@value AuthController#CHONG_CSRF}: trang lạ không gửi được header này. */
public class ThieuHeaderChongCsrfException extends RuntimeException {

    public ThieuHeaderChongCsrfException() {
        super("Thiếu header " + AuthController.CHONG_CSRF + ".");
    }
}
