package vn.hoctapcanman.mau.tot.application.usecase;

import vn.hoctapcanman.mau.tot.application.dto.HocSinhDto;
import vn.hoctapcanman.mau.tot.application.dto.TaoHocSinhDto;
import vn.hoctapcanman.mau.tot.domain.repository.HocSinhRepository;

public class TaoHocSinhUseCase {
    private final HocSinhRepository hocSinh;

    public TaoHocSinhUseCase(HocSinhRepository hocSinh) {
        this.hocSinh = hocSinh;
    }

    public HocSinhDto thucHien(TaoHocSinhDto lenh) {
        return new HocSinhDto(lenh.ten());
    }
}
