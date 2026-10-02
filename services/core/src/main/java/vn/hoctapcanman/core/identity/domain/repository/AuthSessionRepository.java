package vn.hoctapcanman.core.identity.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.identity.domain.model.AuthSession;
import vn.hoctapcanman.core.identity.domain.model.UserId;

public interface AuthSessionRepository {

    AuthSession save(AuthSession session);

    /**
     * Đọc và khóa phiên tới hết giao dịch: làm mới và đăng xuất trên cùng phiên chạy nối tiếp, nên token vừa xoay vòng
     * không thể sống sót qua một lần đăng xuất chạy đồng thời.
     */
    Optional<AuthSession> findByIdForUpdate(UUID id);

    /** Thu hồi phiên nếu chưa thu hồi (UPDATE có điều kiện, chờ khóa của làm mới đang chạy). */
    boolean revoke(UUID id, Instant now);

    /** Thu hồi mọi phiên còn hiệu lực của người dùng (khi phát hiện refresh token bị dùng lại). */
    void revokeAllForUser(UserId userId, Instant now);

    /** Xóa phiên không còn token nào (sau khi dọn token hết hạn); trả số dòng đã xóa. */
    int deleteWithoutTokens();
}
