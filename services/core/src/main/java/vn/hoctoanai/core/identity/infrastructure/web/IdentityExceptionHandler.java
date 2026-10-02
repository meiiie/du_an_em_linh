package vn.hoctoanai.core.identity.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;

/** Lỗi xác thực → 401 problem+json, một thông điệp, không chi tiết nội bộ. */
@RestControllerAdvice
public class IdentityExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    ProblemDetail authenticationFailed(AuthenticationFailedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problem.setTitle("Chưa đăng nhập được");
        return problem;
    }
}
