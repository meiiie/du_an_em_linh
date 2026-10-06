package vn.hoctapcanman.core.practice.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.JacksonMixin;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBai;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ViTriSai;

/**
 * Trường hợp đồng ghi {@code ?} (contracts/api-core.md) vắng mặt khi không có, thay vì {@code null}: {@code dong}, {@code hang},
 * {@code k} của chỗ sai; {@code maLoi}, {@code buocKe} của kết quả nộp bước; {@code loai} của dòng đã lưu; {@code loiGiai}
 * của kết quả nộp bài. Đặt ở tầng web vì DTO học sinh không được dùng kiểu ngoài Java (ArchUnit
 * {@code DTO_HOC_SINH_KHONG_MANG_LOI_GIAI}). Trường hợp đồng ghi «hay null» vẫn có mặt với {@code null}.
 */
final class TruongTuyChon {

    private TruongTuyChon() {}

    @JacksonMixin(ViTriSai.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    abstract static class ChoSai {}

    @JacksonMixin(KetQuaNopBuoc.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    abstract static class NopBuoc {}

    @JacksonMixin(ChiTietBai.DongDaLam.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    abstract static class DongDaLuu {}

    @JacksonMixin(KetQuaNopBai.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    abstract static class NopBai {}
}
