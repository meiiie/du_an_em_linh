package vn.hoctapcanman.core.practice.application.dto;

import org.jspecify.annotations.Nullable;

/** Một dòng của bước kiểu dòng: số dòng 0-based, LaTeX học sinh viết, nhãn {@code loai} như v0 khi bước cần. */
public record DongNop(int dong, String latex, @Nullable String loai) {}
