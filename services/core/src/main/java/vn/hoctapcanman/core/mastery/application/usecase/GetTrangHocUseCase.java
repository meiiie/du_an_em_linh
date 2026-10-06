package vn.hoctapcanman.core.mastery.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.application.dto.KyNang;
import vn.hoctapcanman.core.content.application.port.DanhMucKyNang;
import vn.hoctapcanman.core.identity.application.port.UserDirectory;
import vn.hoctapcanman.core.mastery.application.dto.hocsinh.TrangHoc;
import vn.hoctapcanman.core.mastery.domain.model.BktConfig;
import vn.hoctapcanman.core.mastery.domain.model.Level4;
import vn.hoctapcanman.core.mastery.domain.model.MasteryState;
import vn.hoctapcanman.core.mastery.domain.repository.MasteryRepository;

/**
 * Trang «Học» của chính học sinh (id từ access token). Mức hiểu là của học sinh, không theo lớp, nên em chưa ghi danh lớp
 * nào vẫn thấy trang. Sổ «Kỹ năng» theo thứ tự của v0 ({@code apps/web/app/hs/page.tsx}): mastery thấp trước, cùng mastery
 * thì kẹt nhiều trước, rồi theo mã. Chủ đề hoàn thành: mọi kỹ năng cốt lõi của chủ đề ở Vận dụng cao.
 */
@Service
@Transactional(readOnly = true)
public class GetTrangHocUseCase {

    private final UserDirectory users;
    private final MasteryRepository repo;
    private final DanhMucKyNang danhMuc;

    public GetTrangHocUseCase(UserDirectory users, MasteryRepository repo, DanhMucKyNang danhMuc) {
        this.users = users;
        this.repo = repo;
        this.danhMuc = danhMuc;
    }

    public TrangHoc execute(UUID hocSinhId) {
        String ten = users.tenHienThi(hocSinhId).orElseThrow(() -> new IllegalStateException("Tài khoản của access token không còn"));
        BktConfig cfg = repo.config();
        List<MasteryState> cuaEm = repo.statesOf(List.of(hocSinhId)).stream()
            .sorted(Comparator.comparingDouble(MasteryState::mastery)
                .thenComparing(Comparator.comparingInt(MasteryState::stuckCounter).reversed())
                .thenComparing(MasteryState::skillCode))
            .toList();
        List<KyNang> danhMucCuaEm = danhMuc.cungChuDe(cuaEm.stream().map(MasteryState::skillCode).toList());
        Map<String, String> tenKyNang = danhMucCuaEm.stream().collect(Collectors.toMap(KyNang::ma, KyNang::ten));
        Set<String> vdc = cuaEm.stream().filter(s -> s.level() == Level4.VAN_DUNG_CAO).map(MasteryState::skillCode)
            .collect(Collectors.toSet());
        List<String> chuDeXong = danhMucCuaEm.stream().filter(KyNang::cotLoi)
            .collect(Collectors.groupingBy(KyNang::chuDe, Collectors.mapping(KyNang::ma, Collectors.toSet())))
            .entrySet().stream().filter(e -> vdc.containsAll(e.getValue())).map(Map.Entry::getKey).sorted().toList();
        return new TrangHoc(ten,
            cuaEm.stream().map(s -> new TrangHoc.KyNangCuaEm(s.skillCode(), tenKyNang.getOrDefault(s.skillCode(), s.skillCode()),
                s.level().name(), s.stuck(cfg))).toList(),
            new TrangHoc.HoanThanh(vdc.stream().sorted().toList(), chuDeXong));
    }
}
