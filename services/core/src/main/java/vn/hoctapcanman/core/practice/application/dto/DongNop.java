package vn.hoctapcanman.core.practice.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Một dòng của bước kiểu dòng: số dòng 0-based, LaTeX học sinh viết, nhãn {@code loai} như v0 khi bước cần. */
public record DongNop(@PositiveOrZero @Max(Short.MAX_VALUE) int dong, @NotNull @Size(max = 2000) String latex,
        @Nullable @Size(max = 16) String loai) {}
