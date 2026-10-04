package vn.hoctapcanman.core.practice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Giao một bài cho một học sinh ở một lớp (giao cho cả lớp = một dòng mỗi học sinh). Chỉ giao được bài đã phát hành cho
 * chính lớp đó, cho học sinh của lớp (FR-031; CSDL kiểm lại lúc ghi). Giao lại cùng (lớp, bài, học sinh) thì cập nhật dòng
 * cũ: tên bộ, hạn, người giao, lúc giao.
 */
public record Assignment(
        UUID id,
        UUID classId,
        UUID problemId,
        UUID studentId,
        AssignmentStatus status,
        @Nullable String setName,
        @Nullable Instant dueAt,
        @Nullable UUID assignedBy,
        Instant assignedAt) {

    public Assignment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(assignedAt, "assignedAt");
        if (setName != null && (setName.isBlank() || setName.length() > 120)) {
            throw new IllegalArgumentException("Tên bộ bài phải có chữ, tối đa 120 ký tự");
        }
    }

    public static Assignment of(UUID classId, UUID problemId, UUID studentId, @Nullable String setName, @Nullable Instant dueAt,
            UUID assignedBy, Instant now) {
        return new Assignment(UUID.randomUUID(), classId, problemId, studentId, AssignmentStatus.DA_GIAO, setName, dueAt, assignedBy,
            now);
    }
}
