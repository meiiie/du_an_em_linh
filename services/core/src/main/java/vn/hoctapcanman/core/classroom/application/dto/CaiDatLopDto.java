package vn.hoctapcanman.core.classroom.application.dto;

import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;

/** Cài lớp như giáo viên thấy. {@code cacNhaDuocBat} của {@code GET /api/gv/cai-dat} do gia sư thêm ở tầng web (T058). */
public record CaiDatLopDto(boolean moLoiGiaiSauKhiNop, String nhaAi, boolean choPhepMayCucBo) {

    public static CaiDatLopDto from(ClassSettings settings) {
        return new CaiDatLopDto(settings.revealSolutionAfterSubmit(), settings.aiProvider(), settings.aiAllowLocal());
    }
}
