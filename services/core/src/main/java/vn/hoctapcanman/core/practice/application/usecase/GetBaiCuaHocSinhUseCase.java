package vn.hoctapcanman.core.practice.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.BaiCuaHocSinh;
import vn.hoctapcanman.core.practice.application.service.BaiLamHienTai;
import vn.hoctapcanman.core.practice.application.service.YeuCauCham;
import vn.hoctapcanman.core.practice.domain.model.Assignment;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.repository.AssignmentRepository;

/**
 * Danh sách bài của học sinh ở lớp (T021, FR-031, {@code GET /api/hs/bai}): bài đang giao cho em ({@code DA_GIAO}) mà còn
 * phát hành ở lớp, theo lúc giao rồi mã bài. Bài đã giao mà bị rút phát hành không hiện, như không có. Không phải học sinh
 * của lớp thì rỗng. Mỗi bài kèm trạng thái bài làm ở đề hiện tại và số bước đạt ({@link BaiLamHienTai}).
 */
@Service
public class GetBaiCuaHocSinhUseCase {

    private final ClassMembership membership;
    private final BaiDeLam baiDeLam;
    private final AssignmentRepository assignments;
    private final BaiLamHienTai baiLam;

    public GetBaiCuaHocSinhUseCase(ClassMembership membership, BaiDeLam baiDeLam, AssignmentRepository assignments, BaiLamHienTai baiLam) {
        this.membership = membership;
        this.baiDeLam = baiDeLam;
        this.assignments = assignments;
        this.baiLam = baiLam;
    }

    public List<BaiCuaHocSinh> execute(UUID hocSinhId, UUID lopId) {
        if (!membership.laHocSinh(hocSinhId, lopId)) {
            return List.of();
        }
        List<Assignment> giao = assignments.forStudent(lopId, hocSinhId);
        if (giao.isEmpty()) {
            return List.of();
        }
        Map<UUID, BaiChoLamBai> phatHanh = baiDeLam.baiDaPhatHanh(lopId).stream()
            .collect(Collectors.toMap(BaiChoLamBai::problemId, Function.identity()));
        return giao.stream()
            .filter(a -> phatHanh.containsKey(a.problemId()))
            .sorted(Comparator.comparing(Assignment::assignedAt).thenComparing(a -> phatHanh.get(a.problemId()).ma()))
            .map(a -> dong(hocSinhId, lopId, a, phatHanh.get(a.problemId())))
            .toList();
    }

    private BaiCuaHocSinh dong(UUID hocSinhId, UUID lopId, Assignment giao, BaiChoLamBai bai) {
        BaiLamHienTai.Xem xem = baiLam.cua(hocSinhId, lopId, bai);
        int soBuoc = bai.cacBuoc().isEmpty() ? 0 : bai.cacBuoc().size() - YeuCauCham.batDau(bai.cacBuoc(), bai.buocBatDau());
        GradeStatus ketQua = xem.ketQuaNop();
        return new BaiCuaHocSinh(bai.ma(), bai.deBai(), bai.deBaiLatex(), bai.kyNang(), bai.tenKyNang(), bai.mucDo(), giao.dueAt(),
            xem.trangThai(), xem.soBuocDat(), soBuoc, ketQua == null ? null : ketQua.name());
    }
}
