package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.dto.KhungCham;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.practice.application.dto.DongNop;
import vn.hoctapcanman.core.practice.application.dto.NopBuocRequest;
import vn.hoctapcanman.core.practice.application.port.MayCham;
import vn.hoctapcanman.core.practice.application.service.YeuCauCham;
import vn.hoctapcanman.core.practice.application.usecase.NopBuocUseCase;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.repository.GradingResultRepository;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;

class KhungChamUseCaseUnitTest {
    @ParameterizedTest @ValueSource(strings = {"NT-TP12-NB-01", "NT-XS12-NB-01"})
    void nopBuocLayDuKienMayChuDungPhienBanVaGiuPhanQuyetKhongKiemDuoc(String code) {
        var frames = DocKhungCham.doc(new NguonNoiDung(new NoiDungProperties("../../data", false)));
        var frame = frames.get(code);
        var content = mock(BaiDeLam.class);
        var membership = mock(ClassMembership.class);
        var submissions = mock(SubmissionRepository.class);
        var grades = mock(GradingResultRepository.class);
        var engine = mock(MayCham.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        UUID student = UUID.randomUUID(), lop = UUID.randomUUID(), id = UUID.randomUUID();
        var bai = new BaiChoLamBai(id, code, 9, "T12.NT.HS", "Kỹ năng thử", "NHAN_BIET", "Đề thử.", "x", "TU_LUAN_5_BUOC",
            null, frame.cacBuoc().stream().map(s -> s.maBuoc()).toList(), List.of(), null, frame.cacBuoc());
        when(membership.laHocSinh(student, lop)).thenReturn(true);
        when(content.bai(lop, code)).thenReturn(Optional.of(bai));
        when(content.duKienCham(id, 9)).thenReturn(frame.baiToan());
        when(submissions.openOrGet(any())).thenAnswer(i -> i.getArgument(0));
        List<StepWork> stored = new ArrayList<>();
        doAnswer(i -> { stored.add(i.getArgument(1)); return null; }).when(submissions).saveStep(any(), any());
        when(submissions.steps(any())).thenAnswer(i -> List.copyOf(stored));
        when(grades.record(any())).thenAnswer(i -> i.getArgument(0));
        when(engine.cham(any())).thenAnswer(i -> {
            Map<String, ?> payload = i.getArgument(0);
            assertThat(payload.get("bai_toan")).isEqualTo(frame.baiToan());
            assertThat(payload).doesNotContainKey("ham");
            return Optional.of(KhungChamPracticeIntegrationTest.response(payload, "KHONG_KIEM_DUOC"));
        });
        var useCase = new NopBuocUseCase(membership, content, submissions, grades, engine, new TransactionTemplate(manager), Clock.systemUTC(), 4);
        var result = useCase.execute(student, lop, code, new NopBuocRequest(bai.cacBuoc().getFirst(),
            List.of(new DongNop(0, "Không kiểm được", null)), null, null));
        assertThat(result.ketQua()).isEqualTo("KHONG_KIEM_DUOC");
        assertThat(result.buocKe()).isNull();
        verify(content).duKienCham(id, 9);
        verify(content, never()).duKienCham(id, 1);
    }

    @Test void duKienBatBienVaBamKhongDoiTheoThuTuKhoa() {
        var steps = DocKhungCham.doc(new NguonNoiDung(new NoiDungProperties("../../data", false))).get("NT-TP12-NB-01").cacBuoc();
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("loai", "integral"); original.put("du_kien", new LinkedHashMap<>(Map.of("ham", "x", "can_duoi", 0, "can_tren", 2)));
        var one = new KhungCham(1, steps, original);
        var two = new KhungCham(1, steps, Map.of("du_kien", Map.of("can_tren", 2, "can_duoi", 0, "ham", "x"), "loai", "integral"));
        var json = JsonMapper.builder().build();
        assertThat(YeuCauCham.bam(Map.of("bai_toan", one.baiToan()))).isEqualTo(YeuCauCham.bam(Map.of("bai_toan", two.baiToan())));
        assertThat(json.writeValueAsString(one)).isEqualTo(json.writeValueAsString(two));
        original.clear();
        assertThat(one.baiToan()).hasSize(2);
        assertThatThrownBy(() -> one.baiToan().put("loai", "other")).isInstanceOf(UnsupportedOperationException.class);
        @SuppressWarnings("unchecked") var data = (Map<String, Object>) one.baiToan().get("du_kien");
        assertThatThrownBy(() -> data.put("ham", "2*x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
