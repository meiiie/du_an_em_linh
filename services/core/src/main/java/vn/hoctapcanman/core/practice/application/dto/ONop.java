package vn.hoctapcanman.core.practice.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Một ô của bảng xét dấu: hàng, {@code k} 0-based ({@code docs/chi-so-o-bang.md}), giá trị học sinh điền. */
public record ONop(@NotBlank @Size(max = 16) String hang, @Nullable @PositiveOrZero @Max(Short.MAX_VALUE) Integer k,
        @NotNull @Size(max = 200) String giaTri) {}
