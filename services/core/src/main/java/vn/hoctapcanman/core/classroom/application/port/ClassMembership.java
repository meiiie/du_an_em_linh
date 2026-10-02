package vn.hoctapcanman.core.classroom.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctapcanman.core.classroom.application.exception.KhongThuocLopException;

/**
 * Quyền theo lớp cho mọi module (FR-032, F-08 của v0 {@code apps/web/lib/lop.ts}): giáo viên chỉ thấy lớp mình dạy và
 * học sinh của các lớp đó; học sinh chỉ dữ liệu của mình. Mọi đọc / ghi dữ liệu lớp đi qua cổng này, không lấy «lớp
 * đầu tiên của bảng». Id là UUID để module khác không phụ thuộc kiểu domain của lớp học.
 */
public interface ClassMembership {

    /** Các lớp giáo viên dạy, ghi danh cũ trước. */
    List<UUID> lopDay(UUID giaoVienId);

    /**
     * Lớp giáo viên đang thao tác: lớp được chọn nếu giáo viên dạy lớp đó; không chọn thì lớp đầu tiên giáo viên dạy.
     * Rỗng khi không dạy lớp nào, hoặc chọn một lớp không dạy (không lùi về lớp khác như v0).
     */
    Optional<UUID> lopDangDay(UUID giaoVienId, @Nullable UUID lopChon);

    /** Lớp của học sinh (mỗi học sinh một lớp); rỗng khi chưa ghi danh. */
    Optional<UUID> lopHoc(UUID hocSinhId);

    boolean laGiaoVien(UUID userId, UUID lopId);

    boolean laHocSinh(UUID userId, UUID lopId);

    /** Ném {@link KhongThuocLopException} khi người dùng không phải giáo viên của lớp. */
    void kiemGiaoVien(UUID userId, UUID lopId);

    /** Giáo viên dạy học sinh này (cùng một lớp, giáo viên ở vai trò giáo viên, học sinh ở vai trò học sinh). */
    boolean giaoVienDayHocSinh(UUID giaoVienId, UUID hocSinhId);

    /** Học sinh của lớp, ghi danh cũ trước. Không kiểm người gọi: dùng sau khi đã kiểm quyền. */
    List<UUID> hocSinhCuaLop(UUID lopId);
}
