package vn.hoctapcanman.core.mastery.application.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.mastery.application.dto.MucHieuHocSinh;
import vn.hoctapcanman.core.mastery.application.port.MucHieuCuaLop;
import vn.hoctapcanman.core.mastery.domain.model.BktConfig;
import vn.hoctapcanman.core.mastery.domain.model.MasteryState;
import vn.hoctapcanman.core.mastery.domain.repository.MasteryRepository;

/** Hiện thực {@link MucHieuCuaLop}: học sinh của lớp qua {@code ClassMembership} (kiểm giáo viên), rồi trạng thái của các em. */
@Service
@Transactional(readOnly = true)
public class MucHieuCuaLopService implements MucHieuCuaLop {

    private final ClassMembership membership;
    private final MasteryRepository repo;

    public MucHieuCuaLopService(ClassMembership membership, MasteryRepository repo) {
        this.membership = membership;
        this.repo = repo;
    }

    @Override
    public List<MucHieuHocSinh> cuaLop(UUID giaoVienId, UUID lopId) {
        List<UUID> hocSinh = membership.hocSinhCuaLop(giaoVienId, lopId);
        Map<UUID, Integer> thuTu = new HashMap<>();
        hocSinh.forEach(id -> thuTu.put(id, thuTu.size()));
        BktConfig cfg = repo.config();
        return repo.statesOf(hocSinh).stream()
            .sorted(Comparator.<MasteryState>comparingInt(s -> thuTu.get(s.studentId())).thenComparing(MasteryState::skillCode))
            .map(s -> new MucHieuHocSinh(s.studentId(), s.skillCode(), s.level().name(), s.stuck(cfg)))
            .toList();
    }
}
