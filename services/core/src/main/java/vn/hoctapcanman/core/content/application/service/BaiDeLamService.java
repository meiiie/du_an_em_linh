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
import vn.hoctapcanman.core.content.application.port.KhungChamTheoBai;
import vn.hoctapcanman.core.content.application.dto.hocsinh.MoTaBuoc;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ReleasedProblem;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

/**
 * Bài cho học sinh làm, từ bài đang phát hành ở lớp: bài, phiên bản nội dung và trạng thái phát hành đọc trong một câu lệnh
 * ({@link ProblemRepository#findReleasedForWork}), nên đề và bài làm gắn đúng phiên bản của nội dung đã kiểm; khung bước và
 * tên kỹ năng lấy theo chủ đề của kỹ năng chính. Khung theo bài/phiên bản ưu tiên khung chủ đề; bài không được hỗ trợ có
 * khung rỗng. Dữ kiện chấm chỉ đi qua {@code duKienCham}, không nằm trong metadata học sinh. Không dùng {@code SolutionRepository} (FR-006).
 */
@Service
@Transactional(readOnly = true)
public class BaiDeLamService implements BaiDeLam {

    private final ProblemRepository problems;
    private final TopicCatalogRepository catalog;

    private final KhungChamTheoBai frames;

    public BaiDeLamService(ProblemRepository problems, TopicCatalogRepository catalog, KhungChamTheoBai frames) {
        this.frames = frames;
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

    @Override public Map<String, Object> duKienCham(UUID id, int version) {
        return frames.doc(id, version).map(f -> f.baiToan()).orElse(Map.of());
    }

    /** Khung bước và tên kỹ năng theo chủ đề, đọc mỗi chủ đề một lần cho một lần gọi. */
    private final class DanhMuc {

        private final Map<String, List<String>> khung = new HashMap<>();
        private final Map<String, Map<String, String>> tenKyNang = new HashMap<>();

        BaiChoLamBai choLamBai(ReleasedProblem r) {
            Problem p = r.problem();
            var frame = frames.doc(p.id(), r.contentVersion());
            boolean chamDonDieu = p.isFiveStep() && p.functionSympy() != null && frame.map(f -> f.baiToan().isEmpty()).orElse(true);
            List<String> cacBuoc = chamDonDieu
                ? khung.computeIfAbsent(r.topicCode(), t -> catalog.findStepTemplates(t).stream().map(StepTemplate::stepCode).toList())
                : List.of();
            String ten = tenKyNang.computeIfAbsent(r.topicCode(),
                    t -> catalog.findSkills(t).stream().collect(Collectors.toMap(Skill::code, Skill::name)))
                .getOrDefault(p.skillCode(), p.skillCode());
            var metadata = frame.map(f -> f.cacBuoc()).orElseGet(() -> cacBuoc.stream().map(MoTaBuoc::cu).toList());
            return new BaiChoLamBai(p.id(), p.code(), r.contentVersion(), p.skillCode(), ten, p.level4().name(), p.statementText(),
                p.statementLatex(), frame.filter(f -> !"tra_loi_tu_do".equals(f.baiToan().get("loai"))).isPresent() ? Problem.TU_LUAN_5_BUOC : p.answerForm(),
                chamDonDieu ? p.functionSympy() : null, metadata.stream().map(MoTaBuoc::maBuoc).toList(),
                chamDonDieu ? p.conclusionClaims() : List.of(), p.startStep(), metadata);
        }
    }
}
