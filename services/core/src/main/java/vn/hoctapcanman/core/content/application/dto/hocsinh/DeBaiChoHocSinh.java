package vn.hoctapcanman.core.content.application.dto.hocsinh;

import org.jspecify.annotations.Nullable;

/**
 * Một đề bài cho học sinh khi đang làm: mã bài, kỹ năng, mức (4 mức, ADR 004), đề bằng chữ và LaTeX, dạng trả lời, bước bắt
 * đầu của bài khung ngắn. Không có trường nào cho lời giải, đáp án hay dữ kiện bảo vệ (FR-006).
 */
public record DeBaiChoHocSinh(
        String ma,
        String kyNang,
        String mucDo,
        String deBai,
        String deBaiLatex,
        String dangTraLoi,
        @Nullable String buocBatDau) {}
