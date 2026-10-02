package vn.hoctapcanman.core.classroom.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Cảnh báo cho giáo viên của lớp (FR-025). Kỹ năng, bài, bước tham chiếu bằng mã (module nội dung khác bảng). Mỗi
 * (lớp, học sinh, loại, kỹ năng, bài) chỉ có một cảnh báo mở; xử lý xong thì có thể ghi lại.
 */
public record Escalation(
        UUID id,
        ClassId classId,
        UUID studentId,
        EscalationKind kind,
        String skillCode,
        @Nullable String problemCode,
        @Nullable String stepCode,
        String reason,
        Instant createdAt,
        @Nullable Instant handledAt,
        @Nullable UUID handledBy) {

    private static final Pattern MA = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final int LY_DO_DAI_TOI_DA = 500;

    public Escalation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(skillCode, "skillCode");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(createdAt, "createdAt");
        kiemMa(skillCode, "kỹ năng");
        if (problemCode != null) {
            kiemMa(problemCode, "bài");
        }
        if (stepCode != null) {
            kiemMa(stepCode, "bước");
        }
        reason = reason.strip();
        if (reason.isEmpty() || reason.length() > LY_DO_DAI_TOI_DA) {
            throw new IllegalArgumentException("Lý do cảnh báo trống hoặc quá dài");
        }
        if (handledBy != null && handledAt == null) {
            throw new IllegalArgumentException("Cảnh báo có người xử lý thì phải có thời điểm xử lý");
        }
    }

    public static Escalation open(
            ClassId classId, UUID studentId, EscalationKind kind, String skillCode,
            @Nullable String problemCode, @Nullable String stepCode, String reason, Instant now) {
        return new Escalation(UUID.randomUUID(), classId, studentId, kind, skillCode, problemCode, stepCode, reason, now, null, null);
    }

    public boolean isOpen() {
        return handledAt == null;
    }

    private static void kiemMa(String ma, String ten) {
        if (!MA.matcher(ma).matches()) {
            throw new IllegalArgumentException("Mã " + ten + " không hợp lệ");
        }
    }
}
