package vn.hoctapcanman.core.practice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.TrangThaiBaiLam;
import vn.hoctapcanman.core.practice.application.service.BaiLamHienTai;
import vn.hoctapcanman.core.practice.domain.model.Assignment;
import vn.hoctapcanman.core.practice.domain.repository.AssignmentRepository;

class GetChiTietBaiUseCaseTest {

    private final UUID hocSinh = UUID.randomUUID();
    private final UUID lop = UUID.randomUUID();
    private final UUID problem = UUID.randomUUID();
    private final ClassMembership membership = mock(ClassMembership.class);
    private final BaiDeLam content = mock(BaiDeLam.class);
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final BaiLamHienTai baiLam = mock(BaiLamHienTai.class);

    @Test
    void traIdGiaoDungBaiCuaHocSinhTrongLop() {
        Assignment giao = Assignment.of(lop, problem, hocSinh, null, null, UUID.randomUUID(), Instant.EPOCH);
        Assignment baiKhac = Assignment.of(lop, UUID.randomUUID(), hocSinh, null, null, UUID.randomUUID(), Instant.EPOCH);
        when(assignments.forStudent(lop, hocSinh)).thenReturn(List.of(baiKhac, giao));

        var json = JsonMapper.builder().build().valueToTree(useCase().execute(hocSinh, lop, "BAI-THU"));

        assertThat(json.has("giaoBaiId")).isTrue();
        assertThat(json.path("giaoBaiId").asString()).isEqualTo(giao.id().toString());
    }

    @Test
    void baiPhatHanhChuaGiaoVanXemDuocVaIdGiaoLaNull() {
        Assignment baiKhac = Assignment.of(lop, UUID.randomUUID(), hocSinh, null, null, UUID.randomUUID(), Instant.EPOCH);
        when(assignments.forStudent(lop, hocSinh)).thenReturn(List.of(baiKhac));

        var json = JsonMapper.builder().build().valueToTree(useCase().execute(hocSinh, lop, "BAI-THU"));

        assertThat(json.has("giaoBaiId")).isTrue();
        assertThat(json.path("giaoBaiId").isNull()).isTrue();
    }

    private GetChiTietBaiUseCase useCase() {
        var bai = new BaiChoLamBai(problem, "BAI-THU", 1, "T12.DH.02", "Tính đạo hàm", "THONG_HIEU",
            "Tính đạo hàm.", "y = x^2", "TU_LUAN", null, List.of(), List.of(), null);
        when(membership.laHocSinh(hocSinh, lop)).thenReturn(true);
        when(content.bai(lop, bai.ma())).thenReturn(Optional.of(bai));
        when(baiLam.cua(hocSinh, lop, bai)).thenReturn(new BaiLamHienTai.Xem(TrangThaiBaiLam.CHUA_LAM, null, List.of()));
        return new GetChiTietBaiUseCase(membership, content, assignments, baiLam);
    }
}
