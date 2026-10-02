package vn.hoctapcanman.core.identity.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Tra tài khoản cho module khác mà không lộ model domain của identity (#111: module chỉ gọi nhau qua
 * {@code application.port}, {@code application.dto}, {@code application.exception}). Chỉ trả điều module khác cần: id,
 * vai trò tài khoản, cờ tài khoản tổng hợp (ADR 006). Không trả email, tên hay băm mật khẩu.
 */
public interface UserDirectory {

    /** Tài khoản có email này (không phân biệt hoa thường); rỗng khi không có hoặc email không hợp lệ. */
    Optional<UserSummary> findByEmail(String email);

    Optional<UserSummary> findById(UUID id);

    /** {@code role}: {@code ADMIN}, {@code SCHOOL_ADMIN}, {@code TEACHER} hoặc {@code STUDENT}. */
    record UserSummary(UUID id, String role, boolean synthetic) {}
}
