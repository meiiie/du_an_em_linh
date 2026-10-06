package vn.hoctapcanman.core.practice.infrastructure.web;

import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.hoctapcanman.core.practice.application.exception.BaiChuaNopDuocException;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;

/**
 * Lỗi của API học sinh làm bài → problem+json, {@code detail} tiếng Việt (contracts/api-core.md). Chỉ cho
 * {@link HocSinhBaiController}, và đứng trước bộ xử lý problem+json mặc định để lỗi kiểm thân cũng có chữ tiếng Việt.
 * <ul>
 *   <li>404: không phải học sinh của lớp, bài không có, chưa phát hành ở lớp, hay không chấm từng bước được; một thông điệp,
 *       không lộ bài hay lớp.</li>
 *   <li>409: chưa nộp bài được; {@code detail} nói em phải làm gì, {@code lyDo} là mã lý do.</li>
 *   <li>400: thân không đọc được, sai ràng buộc, hay bài làm không hợp lệ với khung của bài (bước ngoài khung, ô kết luận đề
 *       không hỏi, bước không có dòng hay ô nào).</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = HocSinhBaiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HocSinhBaiExceptionHandler {

    @ExceptionHandler(BaiKhongTimThayException.class)
    ProblemDetail khongCoBai(BaiKhongTimThayException e) {
        return loi(HttpStatus.NOT_FOUND, "Không có bài", e.getMessage());
    }

    @ExceptionHandler(BaiChuaNopDuocException.class)
    ProblemDetail chuaNopDuoc(BaiChuaNopDuocException e) {
        ProblemDetail problem = loi(HttpStatus.CONFLICT, "Chưa nộp được bài", e.getMessage());
        problem.setProperty("lyDo", e.lyDo().name());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail thanSaiRangBuoc(MethodArgumentNotValidException e) {
        String truong = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()).distinct().sorted()
            .collect(Collectors.joining(", "));
        return loi(HttpStatus.BAD_REQUEST, "Bài làm không hợp lệ", "Bài làm gửi lên thiếu hay sai trường: " + truong + ".");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail thanKhongDocDuoc(HttpMessageNotReadableException e) {
        return loi(HttpStatus.BAD_REQUEST, "Bài làm không hợp lệ", "Không đọc được bài làm gửi lên.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail baiLamKhongHopLe(IllegalArgumentException e) {
        return loi(HttpStatus.BAD_REQUEST, "Bài làm không hợp lệ", Objects.requireNonNullElse(e.getMessage(), "Bài làm gửi lên không hợp lệ."));
    }

    private static ProblemDetail loi(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
