package vn.hoctapcanman.core.mastery.application.port;

import java.util.List;
import java.util.UUID;
import vn.hoctapcanman.core.mastery.application.dto.MucHieuHocSinh;

/**
 * Mức hiểu của một lớp cho giáo viên (FR-025, FR-029; bảng tiến độ {@code GET /api/gv/tien-do} của T058): học sinh × kỹ
 * năng → mức 4 và cờ kẹt. Cổng tự kiểm người gọi qua {@code ClassMembership.hocSinhCuaLop}: không phải giáo viên của lớp thì
 * {@code KhongThuocLopException} (403) của classroom. Mức máy tính; mức ghi đè của giáo viên (FR-034) chưa có (T060b).
 */
public interface MucHieuCuaLop {

    /**
     * Mỗi (học sinh của lớp, kỹ năng) đã có ít nhất một bài được tính là một dòng; chưa có thì không có dòng (màn hiện
     * «chưa làm»). Theo thứ tự ghi danh của học sinh rồi mã kỹ năng.
     */
    List<MucHieuHocSinh> cuaLop(UUID giaoVienId, UUID lopId);
}
