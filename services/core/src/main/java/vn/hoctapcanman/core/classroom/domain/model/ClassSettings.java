package vn.hoctapcanman.core.classroom.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Cài lớp. {@code aiProvider} chỉ là mã nhà AI; khóa do máy chủ quản (FR-019). Nhà nào được bật, và máy cục bộ có
 * dùng được không, do cấu hình máy chủ của gia sư quyết định khi cập nhật qua API (contracts/api-core.md, research R3).
 */
public record ClassSettings(
        ClassId classId,
        boolean revealSolutionAfterSubmit,
        String aiProvider,
        boolean aiAllowLocal,
        Instant updatedAt,
        @Nullable UUID updatedBy) {

    /** Nhà mặc định: chạy không cần khóa (AGENTS.md). */
    public static final String NHA_MAC_DINH = "offline";

    private static final Pattern MA_NHA = Pattern.compile("[a-z][a-z0-9_-]{0,31}");

    public ClassSettings {
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(aiProvider, "aiProvider");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (!MA_NHA.matcher(aiProvider).matches()) {
            throw new IllegalArgumentException("Mã nhà AI không hợp lệ");
        }
    }

    /** Mặc định của lớp mới: không mở lời giải sau khi nộp (FR-006), nhà offline, không dùng máy cục bộ. */
    public static ClassSettings macDinh(ClassId classId, Instant now) {
        return new ClassSettings(classId, false, NHA_MAC_DINH, false, now, null);
    }

    public ClassSettings capNhat(boolean reveal, String provider, boolean allowLocal, UUID nguoiDoi, Instant now) {
        return new ClassSettings(classId, reveal, provider, allowLocal, now, Objects.requireNonNull(nguoiDoi, "nguoiDoi"));
    }
}
