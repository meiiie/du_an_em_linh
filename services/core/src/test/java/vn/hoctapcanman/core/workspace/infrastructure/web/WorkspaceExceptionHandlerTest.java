package vn.hoctapcanman.core.workspace.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;

class WorkspaceExceptionHandlerTest {

    private final WorkspaceExceptionHandler handler = new WorkspaceExceptionHandler();

    @Test
    void maHanNopTra403KhongLoSql() {
        // SQLSTATE riêng của V13 không thuộc nhóm lỗi integrity mà Spring phân loại.
        var ex = new UncategorizedSQLException("ghi bước", "INSERT INTO submissions",
            new SQLException("nội dung lỗi CSDL nội bộ", "P0403"));
        var problem = handler.conflict(ex);
        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getProperties()).containsEntry("lyDo", "HET_HAN");
        assertThat(problem.getDetail()).isEqualTo("Bài đã hết hạn nộp. Em có thể xem lại phần đã làm.");
    }

    @Test
    void rangBuocKhacVanLa409() {
        var problem = handler.conflict(new DataIntegrityViolationException("dữ liệu nội bộ",
            new SQLException("constraint", "23514")));
        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).doesNotContain("nội bộ", "constraint");
    }

    @Test
    void loiCsdlKhacKhongBiDoiThanh403Hay409() {
        var ex = new UncategorizedSQLException("truy vấn", "SELECT", new SQLException("lỗi", "08006"));
        assertThatThrownBy(() -> handler.conflict(ex)).isSameAs(ex);
    }
}
