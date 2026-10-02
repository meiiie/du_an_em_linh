package vn.hoctapcanman.core.classroom.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatLopDto;
import vn.hoctapcanman.core.classroom.application.dto.CapNhatCaiDatLopRequest;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.model.ClassSettings;
import vn.hoctapcanman.core.classroom.domain.repository.ClassSettingsRepository;

/** Giáo viên của lớp đổi cài lớp (FR-006, FR-019); ghi người đổi và thời điểm. Giáo viên lớp khác bị từ chối (F-08). */
@Service
public class CapNhatCaiDatLopUseCase {

    private final ClassMembership membership;
    private final ClassSettingsRepository settings;
    private final Clock clock;

    public CapNhatCaiDatLopUseCase(ClassMembership membership, ClassSettingsRepository settings, Clock clock) {
        this.membership = membership;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional
    public CaiDatLopDto execute(UUID giaoVienId, UUID lopId, CapNhatCaiDatLopRequest request) {
        membership.kiemGiaoVien(giaoVienId, lopId);
        ClassId id = new ClassId(lopId);
        Instant now = clock.instant();
        ClassSettings hienTai = settings.findByClassId(id).orElseGet(() -> ClassSettings.macDinh(id, now));
        ClassSettings moi = hienTai.capNhat(request.moLoiGiaiSauKhiNop(), request.nhaAi(), request.choPhepMayCucBo(), giaoVienId, now);
        return CaiDatLopDto.from(settings.save(moi));
    }
}
