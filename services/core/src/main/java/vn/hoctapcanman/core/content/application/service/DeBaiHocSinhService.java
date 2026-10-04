package vn.hoctapcanman.core.content.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.application.dto.hocsinh.DeBaiChoHocSinh;
import vn.hoctapcanman.core.content.application.port.DeBaiHocSinh;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ProblemRelease;
import vn.hoctapcanman.core.content.domain.repository.ProblemReleaseRepository;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;

/**
 * Đề bài cho học sinh từ bài và trạng thái phát hành của lớp. Không dùng {@code SolutionRepository}: lời giải ở bảng riêng
 * và không bao giờ đi qua cổng này (FR-006).
 */
@Service
@Transactional(readOnly = true)
public class DeBaiHocSinhService implements DeBaiHocSinh {

    private final ProblemRepository problems;
    private final ProblemReleaseRepository releases;

    public DeBaiHocSinhService(ProblemRepository problems, ProblemReleaseRepository releases) {
        this.problems = problems;
        this.releases = releases;
    }

    @Override
    public List<DeBaiChoHocSinh> baiDaPhatHanh(UUID lopId) {
        List<UUID> daPhatHanh = releases.findByClass(lopId).stream()
            .filter(ProblemRelease::visibleToStudents).map(ProblemRelease::problemId).toList();
        return problems.findAllById(daPhatHanh).stream().map(DeBaiHocSinhService::choHocSinh).toList();
    }

    @Override
    public Optional<DeBaiChoHocSinh> bai(UUID lopId, String maBai) {
        return problems.findByCode(maBai)
            .filter(p -> releases.find(lopId, p.id()).filter(ProblemRelease::visibleToStudents).isPresent())
            .map(DeBaiHocSinhService::choHocSinh);
    }

    private static DeBaiChoHocSinh choHocSinh(Problem p) {
        return new DeBaiChoHocSinh(p.code(), p.skillCode(), p.level4().name(), p.statementText(), p.statementLatex(),
            p.answerForm(), p.startStep());
    }
}
