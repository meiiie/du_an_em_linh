package vn.hoctapcanman.core.mastery.application.dto.hocsinh;

import java.util.List;

/**
 * Trang «Học» của học sinh ({@code GET /api/hs/trang-hoc}, contracts/api-core.md): tên để chào, sổ «Kỹ năng» (mỗi kỹ năng em
 * đã có bài được tính, kỹ năng yếu trước như v0), các kỹ năng và chủ đề đã hoàn thành (FR-026). Chỉ mức bằng mã 4 mức, không
 * xác suất, không Bloom (FR-023).
 */
public record TrangHoc(String ten, List<KyNangCuaEm> soKyNang, HoanThanh hoanThanh) {

    public TrangHoc {
        soKyNang = List.copyOf(soKyNang);
    }

    /** Một dòng của sổ «Kỹ năng»: mã, tên (bảng {@code skills}), mức 4, kẹt (sai liền từ {@code so_luot_ket} lượt). */
    public record KyNangCuaEm(String kyNang, String tenKyNang, String muc4, boolean ket) {}

    /** Kỹ năng đang ở Vận dụng cao, và chủ đề có mọi kỹ năng cốt lõi ở Vận dụng cao; theo mã. */
    public record HoanThanh(List<String> kyNang, List<String> chuDe) {

        public HoanThanh {
            kyNang = List.copyOf(kyNang);
            chuDe = List.copyOf(chuDe);
        }
    }
}
