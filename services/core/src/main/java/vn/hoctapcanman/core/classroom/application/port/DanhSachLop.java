package vn.hoctapcanman.core.classroom.application.port;

import java.util.List;
import java.util.UUID;

/** Danh sách lớp cho module khác (#85, T012b: nạp tài liệu, bảng công thức, kiểm và phát hành bài theo từng lớp). */
public interface DanhSachLop {

    /** Id mọi lớp, lớp tạo trước đứng trước. */
    List<UUID> moiLop();
}
