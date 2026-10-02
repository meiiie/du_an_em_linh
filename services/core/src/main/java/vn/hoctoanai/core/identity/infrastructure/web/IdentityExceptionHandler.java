package vn.hoctoanai.core.identity.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;

/**
 * Lỗi xác thực → 401 problem+json, một thông điệp, không chi tiết nội bộ. Làm mới thất bại thì xóa luôn cookie refresh
 * token để trình duyệt không gửi lại token chết. Thiếu header chống CSRF → 403.
 */
@RestControllerAdvice
public class IdentityExceptionHandler {

    private final RefreshCookie cookie;

    public IdentityExceptionHandler(RefreshCookie cookie) {
        this.cookie = cookie;
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ProblemDetail> authenticationFailed(AuthenticationFailedException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problem.setTitle("Chưa đăng nhập được");
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        if (request.getRequestURI().equals(RefreshCookie.DUONG_DAN + "/refresh")) {
            response.header(HttpHeaders.SET_COOKIE, cookie.xoa().toString());
        }
        return response.body(problem);
    }

    @ExceptionHandler(ThieuHeaderChongCsrfException.class)
    ProblemDetail thieuHeaderChongCsrf(ThieuHeaderChongCsrfException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problem.setTitle("Yêu cầu bị chặn");
        return problem;
    }
}
