package vn.hoctapcanman.core.classroom.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Ghi danh: một người với một vai trò trong một lớp. Người dùng tham chiếu bằng id (module identity), không qua kiểu
 * của module khác. Học sinh chỉ thuộc một lớp (ràng buộc ở CSDL, V3).
 */
public record Enrollment(ClassId classId, UUID userId, ClassRole role, Instant enrolledAt) {

    public Enrollment {
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(enrolledAt, "enrolledAt");
    }
}
