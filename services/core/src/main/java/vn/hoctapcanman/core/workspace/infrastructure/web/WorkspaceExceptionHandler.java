package vn.hoctapcanman.core.workspace.infrastructure.web;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@RestControllerAdvice public class WorkspaceExceptionHandler {
    @ExceptionHandler(WorkspaceException.class) public ProblemDetail handle(WorkspaceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(ex.status()),ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class) public ProblemDetail conflict(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Dữ liệu đã thay đổi hoặc trùng. Hãy tải lại và kiểm tra thông tin.");
    }
}
