package vn.hoctapcanman.core.workspace.infrastructure.web;

import java.sql.SQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

@RestControllerAdvice public class WorkspaceExceptionHandler {
    @ExceptionHandler(WorkspaceException.class) public ProblemDetail handle(WorkspaceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(ex.status()),ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class) public ProblemDetail conflict(DataAccessException ex) {
        // V13: tra mã SQL ổn định, không dò chữ lỗi hoặc trả chi tiết CSDL cho học sinh.
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "P0403".equals(sql.getSQLState())) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                    "Bài đã hết hạn nộp. Em có thể xem lại phần đã làm.");
                problem.setProperty("lyDo", "HET_HAN");
                return problem;
            }
        }
        if (ex instanceof DataIntegrityViolationException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Dữ liệu đã thay đổi hoặc trùng. Hãy tải lại và kiểm tra thông tin.");
        }
        throw ex;
    }
}
