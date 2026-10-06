package vn.hoctapcanman.core.practice.application.dto.hocsinh;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * {@code GET /api/hs/bai/{maBai}}: đề, khung bước, bài làm của em ở đề hiện tại, và lớp có mở lời giải sau khi nộp không
 * ({@code coTheMoLoiGiai}, cờ đọc lúc trả lời; lời giải chỉ đi trong phản hồi nộp bài). {@code khaiBaoKetLuan} là các ô bước
 * kết luận đề hỏi ({@code dong_bien}, {@code nghich_bien}, thêm {@code cuc_dai}, {@code cuc_tieu} khi đề hỏi cực trị): chỉ là
 * ô nào có, không mang giá trị nào. Bài không làm theo khung bước thì {@code cacBuoc} và {@code khaiBaoKetLuan} rỗng.
 */
public record ChiTietBai(
        String maBai,
        De de,
        String kyNang,
        String tenKyNang,
        String muc4,
        String dangTraLoi,
        @Nullable String buocBatDau,
        List<String> khaiBaoKetLuan,
        List<BuocKhung> cacBuoc,
        BaiLam baiLam,
        boolean coTheMoLoiGiai) {

    public ChiTietBai {
        khaiBaoKetLuan = List.copyOf(khaiBaoKetLuan);
        cacBuoc = List.copyOf(cacBuoc);
    }

    public record De(String text, String latex) {}

    /** Một bước của khung, theo thứ tự: tên bước và việc em làm ở bước đó, viết cho học sinh. */
    public record BuocKhung(String maBuoc, String ten, String viec) {}

    /** Bài làm em đang thấy: các bước đã lưu, theo thứ tự khung. */
    public record BaiLam(TrangThaiBaiLam trangThai, List<BuocDaLam> cacBuoc) {

        public BaiLam {
            cacBuoc = List.copyOf(cacBuoc);
        }
    }

    /**
     * Nội dung đã lưu của một bước và lần chấm của đúng nội dung hiện tại tới bước này ({@code null} khi chưa chấm, hay đã sửa
     * một bước từ đầu tới bước này sau lần chấm đó). Lần chấm có phán quyết thắng {@code KHONG_CHAM_DUOC}.
     */
    public record BuocDaLam(String maBuoc, List<DongDaLam> dong, List<ODaLam> bang, @Nullable String ketQua, @Nullable String thongBao,
            List<ViTriSai> oSai) {

        public BuocDaLam {
            dong = List.copyOf(dong);
            bang = List.copyOf(bang);
            oSai = List.copyOf(oSai);
        }
    }

    /** Một dòng em đã viết: số dòng 0-based, LaTeX, nhãn {@code loai} em gửi kèm (như {@code POST …/buoc}). */
    public record DongDaLam(int dong, String latex, @Nullable String loai) {}

    /** Một ô em đã điền ở bảng xét dấu: hàng, {@code k} 0-based, giá trị. */
    public record ODaLam(String hang, @Nullable Integer k, String giaTri) {}
}
