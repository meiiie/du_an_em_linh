package vn.hoctapcanman.core.practice.application.service;

import java.util.Map;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai.BuocKhung;

/**
 * Tên bước và việc của bước trên trang làm bài, chép nguyên văn {@code TEN_TRANG} và {@code LOI_BUOC} của v0
 * ({@code apps/web/lib/de-hoc-sinh.ts}). Mô tả bước trong {@code step_templates} là chữ của khung, không phải chữ cho học
 * sinh. Khung khác khung đơn điệu – cực trị chưa có tên viết cho học sinh: mã bước làm tên, việc để trống.
 */
public final class TenBuoc {

    private static final Map<String, BuocKhung> CHO_HOC_SINH = Map.of(
        "B.DH.TXD", new BuocKhung("B.DH.TXD", "Tập xác định", "Viết tập xác định của hàm số"),
        "B.DH.DAOHAM", new BuocKhung("B.DH.DAOHAM", "Đạo hàm", "Tính đạo hàm của hàm số"),
        "B.DH.NGHIEM", new BuocKhung("B.DH.NGHIEM", "Nghiệm y′", "Tìm nghiệm y′ = 0 và điểm y′ không xác định"),
        "B.DH.XETDAU", new BuocKhung("B.DH.XETDAU", "Xét dấu", "Xét dấu y′ và chiều biến thiên"),
        "B.DH.KETLUAN", new BuocKhung("B.DH.KETLUAN", "Kết luận", "Kết luận khoảng đơn điệu và cực trị"));

    private TenBuoc() {}

    public static BuocKhung cua(String maBuoc) {
        return CHO_HOC_SINH.getOrDefault(maBuoc, new BuocKhung(maBuoc, maBuoc, ""));
    }
}
