package vn.hoctoanai.mau.tot.application.usecase;

import vn.hoctoanai.mau.tot.application.dto.HocSinhDto;
import vn.hoctoanai.mau.tot.application.dto.TaoHocSinhDto;
import vn.hoctoanai.mau.tot.domain.repository.HocSinhRepository;

public class TaoHocSinhUseCase {
    private final HocSinhRepository hocSinh;

    public TaoHocSinhUseCase(HocSinhRepository hocSinh) {
        this.hocSinh = hocSinh;
    }

    public HocSinhDto thucHien(TaoHocSinhDto lenh) {
        return new HocSinhDto(lenh.ten());
    }
}
