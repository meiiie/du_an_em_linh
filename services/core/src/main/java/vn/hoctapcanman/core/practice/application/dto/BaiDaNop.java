package vn.hoctapcanman.core.practice.application.dto;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bài làm vừa nộp, cho module mức hiểu (cổng {@code CapNhatMucHieu}): những gì {@code applyMastery} của v0 nhận mà chỉ
 * practice biết. Kỹ năng và mức của bài; kết quả là của lần chấm bước kết luận làm căn cứ, nên {@code DAT} luôn là xong cả
 * bài ({@code finished} của v0); bước sai, mã lỗi, độ tin cậy, cờ dấu U ({@code toanDung}, chỉ khi {@code SAI}) lấy từ lần
 * chấm đó; cờ nghi đoán mò của bài làm. Cùng {@code baiLamId} thì nội dung như nhau mãi (bài làm đã nộp không đổi).
 */
public record BaiDaNop(
        UUID baiLamId,
        UUID hocSinhId,
        UUID lopId,
        UUID problemId,
        String kyNang,
        String mucDo,
        KetQuaBai ketQua,
        @Nullable ViTriSai buocSai,
        @Nullable String maLoi,
        @Nullable Double doTinCay,
        @Nullable Boolean toanDung,
        boolean nghiDoanMo,
        Instant nopLuc) {}
