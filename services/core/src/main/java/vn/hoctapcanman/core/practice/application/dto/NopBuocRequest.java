package vn.hoctapcanman.core.practice.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Nộp một bước ({@code POST …/buoc}): mã bước, các dòng (bước kiểu dòng), các ô (bước kiểu bảng, loại bảng luôn là
 * {@code XET_DAU} như v0, không lấy từ máy học sinh), sự kiện nhập mới kể từ lần nộp trước. Nộp lại cùng bước thì thay
 * trọn nội dung bước đó. Giới hạn kích thước như các bản ghi của bài làm ({@code StepWork}, {@code SignTable},
 * {@code NopBuocUseCase}); thân sai thì 400 trước khi tới use case.
 */
public record NopBuocRequest(
        @NotBlank @Size(max = 32) String maBuoc,
        @Nullable @Size(max = 50) List<@NotNull @Valid DongNop> dong,
        @Nullable @Size(max = 200) List<@NotNull @Valid ONop> bang,
        @Nullable @Size(max = 500) List<@NotNull @Valid SuKienNop> suKien) {}
