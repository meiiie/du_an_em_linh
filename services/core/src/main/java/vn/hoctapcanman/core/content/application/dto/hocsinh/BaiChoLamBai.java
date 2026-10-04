package vn.hoctapcanman.core.content.application.dto.hocsinh;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bài đang phát hành ở một lớp, cho module làm bài (practice) mở bài làm và dựng yêu cầu chấm {@code /v1/grade} như v0: id
 * và mã bài, phiên bản nội dung đọc cùng câu lệnh với trạng thái phát hành ({@code problems.content_version}), kỹ năng, mức
 * (4 mức), hàm SymPy của đề ({@code ham}, đã có trong đề), khung bước của chủ đề theo thứ tự, bước bắt đầu của bài khung
 * ngắn. {@code ham} trống và khung rỗng khi bài không làm theo khung 5 bước hay không có hàm: bài đó không chấm từng bước
 * được. Không có trường nào cho lời giải, đáp án hay dữ
 * kiện bảo vệ (FR-006); nằm trong gói {@code hocsinh} để luật {@code DTO_HOC_SINH_KHONG_MANG_LOI_GIAI} chặn mọi kiểu domain.
 */
public record BaiChoLamBai(
        UUID problemId,
        String ma,
        int phienBan,
        String kyNang,
        String mucDo,
        @Nullable String ham,
        List<String> cacBuoc,
        @Nullable String buocBatDau) {

    public BaiChoLamBai {
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(ma, "ma");
        Objects.requireNonNull(kyNang, "kyNang");
        Objects.requireNonNull(mucDo, "mucDo");
        cacBuoc = List.copyOf(cacBuoc);
        if (phienBan < 1) {
            throw new IllegalArgumentException("phienBan phải dương");
        }
    }
}
