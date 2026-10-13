package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.content.application.service.BaiDeLamService;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

/** Bài luyện dev trên giấy có các bước gợi mở; không bật chấm CAS cho dạng chưa được hỗ trợ. */
@Component
@Primary
@Profile("dev")
public class BaiLuyenNguyenTraiSeedAdapter implements BaiDeLam {
    private final BaiDeLamService delegate;
    private final TopicCatalogRepository catalog;

    public BaiLuyenNguyenTraiSeedAdapter(BaiDeLamService delegate, TopicCatalogRepository catalog) {
        this.delegate = delegate;
        this.catalog = catalog;
    }

    @Override public Optional<BaiChoLamBai> bai(UUID lop, String ma) {
        var result = delegate.bai(lop, ma);
        return ma.startsWith("NT-LUYEN") ? result.map(this::buocGoiMo) : result;
    }

    @Override public List<BaiChoLamBai> baiDaPhatHanh(UUID lop) {
        return delegate.baiDaPhatHanh(lop).stream().map(this::buocGoiMo).toList();
    }

    private BaiChoLamBai buocGoiMo(BaiChoLamBai b) {
        if (!b.ma().startsWith("NT-LUYEN") || !b.cacBuoc().isEmpty()) return b;
        var skill = catalog.findSkillsInTopicsOf(List.of(b.kyNang())).stream().filter(s -> s.code().equals(b.kyNang())).findFirst();
        var steps = skill.map(s -> catalog.findStepTemplates(s.topicCode()).stream()
            .filter(step -> b.kyNang().equals(step.skillCode())).map(StepTemplate::stepCode).toList()).orElse(List.of());
        return new BaiChoLamBai(b.problemId(), b.ma(), b.phienBan(), b.kyNang(), b.tenKyNang(), b.mucDo(),
            b.deBai(), b.deBaiLatex(), b.dangTraLoi(), b.ham(), steps, b.khaiBaoKetLuan(), b.buocBatDau());
    }
}
