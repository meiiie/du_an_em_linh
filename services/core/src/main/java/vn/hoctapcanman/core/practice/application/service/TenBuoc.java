package vn.hoctapcanman.core.practice.application.service;

import vn.hoctapcanman.core.content.application.dto.hocsinh.MoTaBuoc;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai.BuocKhung;

/** Chuyển metadata công khai theo từng bài sang phiếu học sinh. */
public final class TenBuoc {
    private TenBuoc() {}
    public static BuocKhung cua(MoTaBuoc buoc) {
        return new BuocKhung(buoc.maBuoc(), buoc.ten(), buoc.viec(), buoc.kieu());
    }
}
