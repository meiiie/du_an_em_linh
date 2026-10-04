package vn.hoctapcanman.core.content.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

/**
 * Bài cho học sinh làm, từ bài đang phát hành ở lớp: bài, phiên bản nội dung và trạng thái phát hành đọc trong một câu lệnh
 * ({@link ProblemRepository#findReleasedForWork}), nên bài làm gắn đúng phiên bản của nội dung đã kiểm; khung bước lấy theo
 * chủ đề của kỹ năng chính. Bài không theo khung 5 bước hay không có hàm thì {@code ham} trống, khung rỗng. Không dùng {@code SolutionRepository} (FR-006).
 */
@Service
@Transactional(readOnly = true)
public class BaiDeLamService implements BaiDeLam {

    private final ProblemRepository problems;
    private final TopicCatalogRepository catalog;

    public BaiDeLamService(ProblemRepository problems, TopicCatalogRepository catalog) {
        this.problems = problems;
        this.catalog = catalog;
    }

    @Override
    public Optional<BaiChoLamBai> bai(UUID lopId, String maBai) {
        return problems.findReleasedForWork(lopId, maBai).map(r -> {
            Problem p = r.problem();
            boolean chamTungBuoc = p.isFiveStep() && p.functionSympy() != null;
            List<String> khung = chamTungBuoc
                ? catalog.findStepTemplates(r.topicCode()).stream().map(StepTemplate::stepCode).toList()
                : List.of();
            return new BaiChoLamBai(p.id(), p.code(), r.contentVersion(), p.skillCode(), p.level4().name(),
                chamTungBuoc ? p.functionSympy() : null, khung, chamTungBuoc ? p.conclusionClaims() : List.of(), p.startStep());
        });
    }
}
