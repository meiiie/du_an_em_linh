package vn.hoctapcanman.core.classroom.application.usecase;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;

/** Học sinh của một lớp, chỉ cho giáo viên của lớp đó (F-08: giáo viên lớp khác bị từ chối). */
@Service
public class GetHocSinhCuaLopUseCase {

    private final ClassMembership membership;

    public GetHocSinhCuaLopUseCase(ClassMembership membership) {
        this.membership = membership;
    }

    @Transactional(readOnly = true)
    public List<UUID> execute(UUID giaoVienId, UUID lopId) {
        membership.kiemGiaoVien(giaoVienId, lopId);
        return membership.hocSinhCuaLop(lopId);
    }
}
