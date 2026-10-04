package vn.hoctapcanman.core.practice.application.dto;

import org.jspecify.annotations.Nullable;

/** Một ô của bảng xét dấu: hàng, {@code k} 0-based ({@code docs/chi-so-o-bang.md}), giá trị học sinh điền. */
public record ONop(String hang, @Nullable Integer k, String giaTri) {}
