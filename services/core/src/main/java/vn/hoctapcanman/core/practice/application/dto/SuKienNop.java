package vn.hoctapcanman.core.practice.application.dto;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Một lần học sinh sửa ô hay dòng kể từ lần nộp trước (cho nghi đoán mò): bước, ô (hàng và {@code k}, hoặc không có cả hai),
 * giá trị cũ, giá trị mới, thời điểm phía máy học sinh.
 */
public record SuKienNop(String maBuoc, @Nullable String hang, @Nullable Integer k, @Nullable String giaTriCu, String giaTriMoi,
        Instant luc) {}
