package vn.hoctapcanman.core.classroom.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatChoHocSinh;
import vn.hoctapcanman.core.classroom.application.exception.KhongThuocLopException;

/**
 * Quyền theo lớp cho mọi module (FR-032, F-08 của v0 {@code apps/web/lib/lop.ts}): giáo viên chỉ thấy lớp mình dạy và
 * học sinh của các lớp đó; học sinh chỉ dữ liệu của mình. Mọi đọc / ghi dữ liệu lớp đi qua cổng này, không lấy «lớp
 * đầu tiên của bảng». Id là UUID để module khác không phụ thuộc kiểu domain của lớp học.
 *
 * <p>Cổng chỉ xét vai trò <em>trong lớp</em> (ghi danh). Vai trò tài khoản ({@code users.role}) do {@code SecurityConfig}
 * chặn thêm theo đường dẫn ({@code /api/gv/**} cho giáo viên, {@code /api/hs/**} cho học sinh; #111). Id người gọi luôn
 * lấy từ access token, không từ thân yêu cầu.
 */
public interface ClassMembership {

    /** Các lớp giáo viên dạy, ghi danh cũ trước. */
    List<UUID> lopDay(UUID giaoVienId);

    /**
     * Lớp giáo viên đang xem: lớp được chọn nếu giáo viên dạy lớp đó; không chọn thì lớp đầu tiên giáo viên dạy. Rỗng
     * khi không dạy lớp nào, hoặc chọn một lớp không dạy (không lùi về lớp khác như v0). Chỉ dùng mặc định
     * ({@code lopChon == null}) cho màn đọc: thao tác ghi luôn nhận id lớp tường minh, để giáo viên dạy nhiều lớp không
     * đổi nhầm lớp khác lớp đang xem.
     */
    Optional<UUID> lopDangDay(UUID giaoVienId, @Nullable UUID lopChon);

    /** Lớp của học sinh (mỗi học sinh một lớp); rỗng khi chưa ghi danh. */
    Optional<UUID> lopHoc(UUID hocSinhId);

    /**
     * Cài của lớp mà học sinh đang học, chỉ phần học sinh được biết (#111); rỗng khi chưa ghi danh. Lớp chưa có dòng
     * cài thì trả mặc định: không mở lời giải (FR-006, đóng mặc định). Module khác không đọc kho cài lớp.
     */
    Optional<CaiDatChoHocSinh> caiDatChoHocSinh(UUID hocSinhId);

    boolean laGiaoVien(UUID userId, UUID lopId);

    boolean laHocSinh(UUID userId, UUID lopId);

    /** Ném {@link KhongThuocLopException} khi người dùng không phải giáo viên của lớp. */
    void kiemGiaoVien(UUID userId, UUID lopId);

    /** Giáo viên dạy học sinh này (cùng một lớp, giáo viên ở vai trò giáo viên, học sinh ở vai trò học sinh). */
    boolean giaoVienDayHocSinh(UUID giaoVienId, UUID hocSinhId);

    /**
     * Học sinh của lớp, ghi danh cũ trước, chỉ cho giáo viên của lớp: tự kiểm người gọi, ném {@link KhongThuocLopException}
     * khi không phải giáo viên của lớp (module khác không gọi được với id lớp lấy từ yêu cầu mà quên kiểm).
     */
    List<UUID> hocSinhCuaLop(UUID giaoVienId, UUID lopId);
}
