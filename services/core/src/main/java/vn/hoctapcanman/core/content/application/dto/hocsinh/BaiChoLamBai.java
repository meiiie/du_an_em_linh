package vn.hoctapcanman.core.content.application.dto.hocsinh;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bài đang phát hành ở một lớp, cho module làm bài (practice) hiện đề, mở bài làm và dựng yêu cầu chấm {@code /v1/grade} như
 * v0: id và mã bài, phiên bản nội dung đọc cùng câu lệnh với trạng thái phát hành và đề ({@code problems.content_version}),
 * kỹ năng (mã và tên học sinh đọc), mức (4 mức), đề bằng chữ và LaTeX, dạng trả lời, metadata bước theo bài/phiên bản
 * (hoặc khung chủ đề cũ), các ô kết luận đề hỏi và bước bắt đầu của khung ngắn. Hàm SymPy chỉ phục vụ payload B.DH cũ.
 * Dữ kiện chấm họ mới ở cổng nội bộ, không ở DTO này. Không có lời giải hay đáp án (FR-006).
 */
public record BaiChoLamBai(
        UUID problemId,
        String ma,
        int phienBan,
        String kyNang,
        String tenKyNang,
        String mucDo,
        String deBai,
        String deBaiLatex,
        String dangTraLoi,
        @Nullable String ham,
        List<String> cacBuoc,
        List<String> khaiBaoKetLuan,
        @Nullable String buocBatDau,
        List<MoTaBuoc> moTaBuoc) {

    public BaiChoLamBai {
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(ma, "ma");
        Objects.requireNonNull(kyNang, "kyNang");
        Objects.requireNonNull(tenKyNang, "tenKyNang");
        Objects.requireNonNull(mucDo, "mucDo");
        Objects.requireNonNull(deBai, "deBai");
        Objects.requireNonNull(deBaiLatex, "deBaiLatex");
        Objects.requireNonNull(dangTraLoi, "dangTraLoi");
        cacBuoc = List.copyOf(cacBuoc);
        moTaBuoc = List.copyOf(moTaBuoc);
        khaiBaoKetLuan = List.copyOf(khaiBaoKetLuan);
        if (phienBan < 1) {
            throw new IllegalArgumentException("phienBan phải dương");
        }
    }

    public BaiChoLamBai(UUID problemId, String ma, int phienBan, String kyNang, String tenKyNang, String mucDo,
            String deBai, String deBaiLatex, String dangTraLoi, @Nullable String ham, List<String> cacBuoc,
            List<String> khaiBaoKetLuan, @Nullable String buocBatDau) {
        this(problemId, ma, phienBan, kyNang, tenKyNang, mucDo, deBai, deBaiLatex, dangTraLoi, ham,
            cacBuoc, khaiBaoKetLuan, buocBatDau, cacBuoc.stream().map(MoTaBuoc::cu).toList());
    }

    public boolean chamTungBuoc() {
        return !cacBuoc.isEmpty() && (ham != null || moTaBuoc.stream().allMatch(s -> !s.kieu().equals("GIAY")));
    }

}
