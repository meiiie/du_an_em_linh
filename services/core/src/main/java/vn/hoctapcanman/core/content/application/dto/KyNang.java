package vn.hoctapcanman.core.content.application.dto;

/** Một kỹ năng của danh mục: mã, tên học sinh đọc, mã chủ đề, có phải kỹ năng cốt lõi của chủ đề (FR-026). */
public record KyNang(String ma, String ten, String chuDe, boolean cotLoi) {}
