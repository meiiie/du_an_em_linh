package vn.hoctapcanman.core.classroom.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Giáo viên đổi cài lớp. Nhà phải nằm trong danh sách máy chủ bật, và {@code choPhepMayCucBo} chỉ được khi máy chủ
 * bật {@code app.tutor.allow-local}: hai điều này kiểm ở tầng web của gia sư (T058, research R3).
 */
public record CapNhatCaiDatLopRequest(
        boolean moLoiGiaiSauKhiNop,
        @NotNull @Pattern(regexp = "[a-z][a-z0-9_-]{0,31}") String nhaAi,
        boolean choPhepMayCucBo) {}
