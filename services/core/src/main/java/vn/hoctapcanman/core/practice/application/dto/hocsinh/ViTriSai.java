package vn.hoctapcanman.core.practice.application.dto.hocsinh;

import org.jspecify.annotations.Nullable;

/** Chỗ sai để tô (không kèm giá trị đúng): bước, dòng 0-based nếu là dòng, ô (hàng, {@code k}) nếu là ô của bảng. */
public record ViTriSai(String maBuoc, @Nullable Integer dong, @Nullable String hang, @Nullable Integer k) {}
