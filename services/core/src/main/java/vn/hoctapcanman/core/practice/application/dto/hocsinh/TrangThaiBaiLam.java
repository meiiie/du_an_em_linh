package vn.hoctapcanman.core.practice.application.dto.hocsinh;

/**
 * Bài làm em đang thấy của một bài, ở đề hiện tại: chưa có bài làm, đang làm (nộp bước được), hay đã nộp. Làm lại sau khi nộp
 * mở bài làm mới, nên trạng thái về {@link #DANG_LAM}. Bài làm của đề cũ (nội dung bài đã đổi) không tính.
 */
public enum TrangThaiBaiLam {
    CHUA_LAM,
    DANG_LAM,
    DA_NOP
}
