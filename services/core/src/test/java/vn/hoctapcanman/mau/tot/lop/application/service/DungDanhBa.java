package vn.hoctapcanman.mau.tot.lop.application.service;

import java.util.UUID;
import vn.hoctapcanman.mau.tot.application.port.DanhBaHocSinh;

/** Đúng: module «lop» gọi module «học sinh» qua application.port. */
public class DungDanhBa {

    private final DanhBaHocSinh danhBa;

    public DungDanhBa(DanhBaHocSinh danhBa) {
        this.danhBa = danhBa;
    }

    public String tenHoacTrong(UUID id) {
        return danhBa.ten(id).orElse("");
    }
}
