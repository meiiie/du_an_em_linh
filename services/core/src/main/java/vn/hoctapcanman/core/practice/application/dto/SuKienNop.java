package vn.hoctapcanman.core.practice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Một lần học sinh sửa ô hay dòng kể từ lần nộp trước (cho nghi đoán mò): bước, ô (hàng và {@code k}, hoặc không có cả hai),
 * giá trị cũ, giá trị mới, thời điểm phía máy học sinh.
 */
public record SuKienNop(@NotBlank @Size(max = 32) String maBuoc, @Nullable @Size(max = 16) String hang, @Nullable @PositiveOrZero Integer k,
        @Nullable @Size(max = 200) String giaTriCu, @NotNull @Size(max = 200) String giaTriMoi, @NotNull Instant luc) {}
