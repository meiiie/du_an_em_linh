package vn.hoctapcanman.core.classroom.application.usecase;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatLopDto;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;

/** Cài lớp cho giáo viên của lớp. Lớp chưa có dòng cài đặt thì trả mặc định (không ghi khi chỉ đọc). */
@Service
public class GetCaiDatLopUseCase {

    private final ClassMembership membership;
    private final ClassSettingsRepository settings;
    private final Clock clock;

    public GetCaiDatLopUseCase(ClassMembership membership, ClassSettingsRepository settings, Clock clock) {
        this.membership = membership;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CaiDatLopDto execute(UUID giaoVienId, UUID lopId) {
        membership.kiemGiaoVien(giaoVienId, lopId);
        ClassId id = new ClassId(lopId);
        return CaiDatLopDto.from(settings.findByClassId(id).orElseGet(() -> ClassSettings.macDinh(id, clock.instant())));
    }
}
