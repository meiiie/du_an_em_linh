package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.content.application.dto.KhungCham;
import vn.hoctapcanman.core.content.application.dto.hocsinh.MoTaBuoc;
import vn.hoctapcanman.core.content.application.port.KhungChamTheoBai;
import vn.hoctapcanman.core.content.application.service.BaiDeLamService;
import vn.hoctapcanman.core.content.domain.model.*;
import vn.hoctapcanman.core.content.domain.repository.*;
import vn.hoctapcanman.core.practice.application.service.YeuCauCham;
import vn.hoctapcanman.core.practice.domain.model.StepLine;
import vn.hoctapcanman.core.practice.domain.model.StepWork;

class KhungChamWiringTest {
    private final NguonNoiDung source = new NguonNoiDung(new NoiDungProperties("../../data", false));
    private final JsonMapper json = JsonMapper.builder().build();

    @Test void ca43KhungTheoBaiCongKhaiChiMetadataVaPayloadDungDuKienMayChu() {
        Map<String, KhungCham> frames = DocKhungCham.doc(source);
        assertThat(frames).hasSize(43);
        assertThat(frames.values()).allSatisfy(f -> {
            assertThat(f.cacBuoc()).hasSize(5);
            assertThat(json.writeValueAsString(f)).doesNotContain("dap_an", "payload_dung");
        });
        var problems = mock(ProblemRepository.class);
        var catalog = mock(TopicCatalogRepository.class);
        var store = mock(KhungChamTheoBai.class);
        var service = new BaiDeLamService(problems, catalog, store);
        UUID lop = UUID.randomUUID();
        int interactive = 0;
        for (var raw : source.danhSach("dev/nguyen-trai/bai-tap.json")) {
            String ma = (String) raw.get("id");
            @SuppressWarnings("unchecked") var de = (Map<String, Object>) raw.get("de_bai");
            var p = new Problem(UUID.randomUUID(), ma, (String) raw.get("ky_nang_chinh"), List.of(), Level4.NHAN_BIET,
                null, null, null, (String) de.get("van_ban"), (String) de.get("latex"), (String) de.get("ham_so_sympy"),
                (String) raw.get("dang_cau"), null, "GIAO_VIEN_NGUYEN_TRAI", "a".repeat(64), null, Instant.EPOCH, Instant.EPOCH);
            when(problems.findReleasedForWork(lop, ma)).thenReturn(Optional.of(new ReleasedProblem(p, 7, "DH12")));
            when(store.doc(p.id(), 7)).thenReturn(Optional.ofNullable(frames.get(ma)));
            var bai = service.bai(lop, ma).orElseThrow();
            if (!frames.containsKey(ma)) {
                assertThat(bai.chamTungBuoc()).isFalse();
                continue;
            }
            interactive++;
            assertThat(bai.chamTungBuoc()).isTrue();
            assertThat(bai.moTaBuoc()).containsExactlyElementsOf(frames.get(ma).cacBuoc());
            assertThat(json.writeValueAsString(bai)).doesNotContain("baiToan", "bai_toan", "du_kien", "dap_an");
            var step = new StepWork(bai.cacBuoc().getFirst(), List.of(new StepLine(0, "x/x", null)), null);
            var payload = YeuCauCham.dung(bai, service, step.stepCode(), List.of(step));
            if (frames.get(ma).baiToan().isEmpty()) assertThat(payload).containsKey("ham").doesNotContainKey("bai_toan");
            else assertThat(payload).containsEntry("bai_toan", frames.get(ma).baiToan()).doesNotContainKey("ham");
        }
        assertThat(interactive).isEqualTo(43);
    }

    @Test void khungKhongDuocMuonTuPhienBanKhacHoacTuChuDe() {
        var problems = mock(ProblemRepository.class);
        var catalog = mock(TopicCatalogRepository.class);
        var store = mock(KhungChamTheoBai.class);
        UUID lop = UUID.randomUUID(), id = UUID.randomUUID();
        var p = new Problem(id, "BAI-THU", "T12.DH.02", List.of(), Level4.NHAN_BIET, null, null, null,
            "Tính tích phân.", "x", null, "TU_LUAN", null, "SUPHAM", "a".repeat(64), null, Instant.EPOCH, Instant.EPOCH);
        when(problems.findReleasedForWork(lop, p.code())).thenReturn(Optional.of(new ReleasedProblem(p, 2, "DH12")));
        when(store.doc(any(), anyInt())).thenReturn(Optional.empty());
        when(store.doc(id, 1)).thenReturn(Optional.of(DocKhungCham.doc(source).get("NT-TP12-NB-01")));
        var bai = new BaiDeLamService(problems, catalog, store).bai(lop, p.code()).orElseThrow();
        assertThat(bai.cacBuoc()).isEmpty();
        verify(store).doc(id, 2);
        verify(store, never()).doc(id, 1);
        verify(catalog, never()).findStepTemplates(any());
    }
}
