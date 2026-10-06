package vn.hoctapcanman.core.practice.application.dto.hocsinh;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Một dòng của {@code GET /api/hs/bai}: bài được giao cho em ở lớp của em và đang phát hành ở lớp đó. {@code soBuoc} là số
 * bước em phải làm, từ bước bắt đầu tới bước kết luận (0 khi bài không làm theo khung bước); {@code soBuocDat} là số bước mà
 * lần chấm của nội dung hiện tại là {@code DAT}; {@code ketQua} là kết quả lúc nộp, chỉ khi {@link TrangThaiBaiLam#DA_NOP}.
 */
public record BaiCuaHocSinh(
        String maBai,
        String deBai,
        String deBaiLatex,
        String kyNang,
        String tenKyNang,
        String muc4,
        @Nullable Instant han,
        TrangThaiBaiLam trangThai,
        int soBuocDat,
        int soBuoc,
        @Nullable String ketQua) {}
