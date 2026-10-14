package vn.hoctapcanman.core.tutor.infrastructure.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import vn.hoctapcanman.core.tutor.application.exception.TutorLimitException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TutorExceptionHandler {
    @ExceptionHandler(TutorLimitException.class)
    public ResponseEntity<ProblemDetail> limited(TutorLimitException ex) {
        return ResponseEntity.status(429).header(HttpHeaders.RETRY_AFTER,Long.toString(ex.retryAfterSeconds()))
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,ex.getMessage()));
    }
}
