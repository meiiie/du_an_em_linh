package vn.hoctapcanman.mau.tot.application.dto.hocsinh;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Mẫu đúng: đề cho học sinh chỉ có kiểu Java. */
public record DeBaiChoHocSinh(String ma, String de, List<String> buoc, @Nullable String goiYDauTien) {}
