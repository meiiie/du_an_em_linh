package vn.hoctapcanman.core.content.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.application.dto.KyNang;
import vn.hoctapcanman.core.content.application.port.DanhMucKyNang;
import vn.hoctapcanman.core.content.domain.model.ErrorType;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

/** Hiện thực {@link DanhMucKyNang} trên danh mục chủ đề. */
@Service
@Transactional(readOnly = true)
public class DanhMucKyNangService implements DanhMucKyNang {

    private final TopicCatalogRepository catalog;

    public DanhMucKyNangService(TopicCatalogRepository catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<String> kyNangCuaMaLoi(String maLoi) {
        return catalog.findErrorType(maLoi).map(ErrorType::skillCode);
    }

    @Override
    public Optional<String> kyNangCuaBuoc(String maBuoc) {
        return catalog.findStepTemplate(maBuoc).map(StepTemplate::skillCode);
    }

    @Override
    public List<KyNang> cungChuDe(Collection<String> maKyNang) {
        return catalog.findSkillsInTopicsOf(maKyNang).stream().map(s -> new KyNang(s.code(), s.name(), s.topicCode(), s.core())).toList();
    }
}
