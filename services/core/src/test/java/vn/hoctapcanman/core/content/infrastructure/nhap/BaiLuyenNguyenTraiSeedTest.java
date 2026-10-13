package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.service.BaiDeLamService;
import vn.hoctapcanman.core.content.domain.model.InputKind;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

class BaiLuyenNguyenTraiSeedTest {
    @Test void chiThemBuocGoiMoChoBaiLuyenDevKhongChamTungBuoc() {
        var delegate = mock(BaiDeLamService.class);
        var catalog = mock(TopicCatalogRepository.class);
        UUID cl = UUID.randomUUID();
        var paper = new BaiChoLamBai(UUID.randomUUID(), "NT-LUYEN10-NB-01", 1, "T10.NT.XS.L", "Xác suất", "NHAN_BIET",
            "Đề bài tổng hợp", "", "TU_LUAN", null, List.of(), List.of(), null);
        when(delegate.bai(cl, paper.ma())).thenReturn(Optional.of(paper));
        when(catalog.findSkillsInTopicsOf(List.of(paper.kyNang()))).thenReturn(List.of(new Skill(paper.kyNang(), "XS10", "Xác suất", null, 10, true)));
        when(catalog.findStepTemplates("XS10")).thenReturn(List.of(
            new StepTemplate("B.L10.PHANTICH", "XS10", 1, InputKind.DONG, paper.kyNang(), "Phân tích đề")));
        var adapter = new BaiLuyenNguyenTraiSeedAdapter(delegate, catalog);
        var result = adapter.bai(cl, paper.ma()).orElseThrow();
        assertThat(result.cacBuoc()).containsExactly("B.L10.PHANTICH");
        assertThat(result.ham()).isNull();
        assertThat(result.dangTraLoi()).isEqualTo("TU_LUAN");
        assertThat(result.deBai()).isEqualTo(paper.deBai());
        when(delegate.bai(cl, "NT-KG12-NB-01")).thenReturn(Optional.of(paper));
        assertThat(adapter.bai(cl, "NT-KG12-NB-01")).contains(paper);
    }
}
