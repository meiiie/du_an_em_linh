package vn.hoctoanai.core.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Người dùng. Mật khẩu chỉ ở dạng băm. {@code synthetic} đánh dấu tài khoản tổng hợp (ADR 006): bản demo không có
 * dữ liệu học sinh thật.
 */
public record User(
        UserId id,
        Email email,
        String passwordHash,
        String displayName,
        Role role,
        boolean enabled,
        boolean synthetic,
        Instant createdAt,
        Instant updatedAt) {

    private static final int TEN_DAI_TOI_DA = 120;

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        displayName = displayName.strip();
        if (displayName.isEmpty() || displayName.length() > TEN_DAI_TOI_DA) {
            throw new IllegalArgumentException("Tên hiển thị không hợp lệ");
        }
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("Thiếu mật khẩu đã băm");
        }
    }

    public static User create(Email email, String passwordHash, String displayName, Role role, boolean synthetic, Instant now) {
        return new User(UserId.newId(), email, passwordHash, displayName, role, true, synthetic, now, now);
    }

    /** Không in email hay mật khẩu băm vào log. */
    @Override
    public String toString() {
        return "User[id=" + id.value() + ", role=" + role + "]";
    }
}
