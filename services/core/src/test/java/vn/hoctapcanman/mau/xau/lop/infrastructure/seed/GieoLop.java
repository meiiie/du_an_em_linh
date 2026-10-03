package vn.hoctapcanman.mau.xau.lop.infrastructure.seed;

import vn.hoctapcanman.mau.xau.domain.repository.HocSinhRepository;

/** Vi phạm: module «lop» đọc thẳng repository của module khác thay vì qua application.port (#111). */
public class GieoLop {

    private final HocSinhRepository hocSinh;

    public GieoLop(HocSinhRepository hocSinh) {
        this.hocSinh = hocSinh;
    }

    public Object gieo() {
        return hocSinh.lay();
    }
}
