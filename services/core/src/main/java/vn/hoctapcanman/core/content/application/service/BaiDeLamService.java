package vn.hoctapcanman.core.content.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ReleasedProblem;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

/**
 * Bài cho học sinh làm, từ bài đang phát hành ở lớp: bài, phiên bản nội dung và trạng thái phát hành đọc trong một câu lệnh
 * ({@link ProblemRepository#findReleasedForWork}), nên đề và bài làm gắn đúng phiên bản của nội dung đã kiểm; khung bước và
 * tên kỹ năng lấy theo chủ đề của kỹ năng chính. Bài không theo khung 5 bước hay không có hàm thì {@code ham} trống, khung
 * rỗng. Không dùng {@code SolutionRepository} (FR-006).
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
        return problems.findReleasedForWork(lopId, maBai).map(new DanhMuc()::choLamBai);
    }

    @Override
    public List<BaiChoLamBai> baiDaPhatHanh(UUID lopId) {
        DanhMuc danhMuc = new DanhMuc();
        return problems.findReleasedForWork(lopId).stream().map(danhMuc::choLamBai).toList();
    }

    /** Khung bước và tên kỹ năng theo chủ đề, đọc mỗi chủ đề một lần cho một lần gọi. */
    private final class DanhMuc {

        private final Map<String, List<String>> khung = new HashMap<>();
        private final Map<String, Map<String, String>> tenKyNang = new HashMap<>();

        BaiChoLamBai choLamBai(ReleasedProblem r) {
            Problem p = r.problem();
            boolean chamTungBuoc = p.isFiveStep() && p.functionSympy() != null;
            List<String> cacBuoc = chamTungBuoc
                ? khung.computeIfAbsent(r.topicCode(), t -> catalog.findStepTemplates(t).stream().map(StepTemplate::stepCode).toList())
                : List.of();
            String ten = tenKyNang.computeIfAbsent(r.topicCode(),
                    t -> catalog.findSkills(t).stream().collect(Collectors.toMap(Skill::code, Skill::name)))
                .getOrDefault(p.skillCode(), p.skillCode());
            return new BaiChoLamBai(p.id(), p.code(), r.contentVersion(), p.skillCode(), ten, p.level4().name(), p.statementText(),
                p.statementLatex(), p.answerForm(), chamTungBuoc ? p.functionSympy() : null, cacBuoc,
                chamTungBuoc ? p.conclusionClaims() : List.of(), p.startStep());
        }
    }
}
