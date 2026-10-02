package vn.hoctapcanman.core.classroom.application.dto;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import vn.hoctapcanman.core.classroom.domain.model.Escalation;

/** Một cảnh báo cho giáo viên. Tên học sinh do tầng web ghép từ định danh (T058); ở đây chỉ có id. */
public record CanhBaoDto(
        UUID id,
        UUID hocSinhId,
        String loai,
        String kyNang,
        @Nullable String maBai,
        @Nullable String maBuoc,
        String lyDo,
        Instant luc,
        boolean daXuLy) {

    /** Không in học sinh hay lý do vào log. */
    @Override
    public String toString() {
        return "CanhBaoDto[id=" + id + ", loai=" + loai + "]";
    }

    public static CanhBaoDto from(Escalation e) {
        return new CanhBaoDto(e.id(), e.studentId(), e.kind().name(), e.skillCode(), e.problemCode(), e.stepCode(),
                e.reason(), e.createdAt(), !e.isOpen());
    }
}
