package vn.hoctapcanman.core.content.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.application.dto.hocsinh.DeBaiChoHocSinh;
import vn.hoctapcanman.core.content.application.port.DeBaiHocSinh;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;

/**
 * Đề bài cho học sinh từ bài đang phát hành ở lớp. Bài và trạng thái phát hành đọc trong một câu lệnh
 * ({@link ProblemRepository#findReleasedInClass}), không hai lần đọc rời: sửa nội dung bài rút phát hành trong cùng giao
 * dịch, nên học sinh không bao giờ thấy nội dung vừa sửa mà chưa kiểm. Không dùng {@code SolutionRepository}: lời giải ở bảng
 * riêng và không bao giờ đi qua cổng này (FR-006).
 */
@Service
@Transactional(readOnly = true)
public class DeBaiHocSinhService implements DeBaiHocSinh {

    private final ProblemRepository problems;

    public DeBaiHocSinhService(ProblemRepository problems) {
        this.problems = problems;
    }

    @Override
    public List<DeBaiChoHocSinh> baiDaPhatHanh(UUID lopId) {
        return problems.findReleasedInClass(lopId).stream().map(DeBaiHocSinhService::choHocSinh).toList();
    }

    @Override
    public Optional<DeBaiChoHocSinh> bai(UUID lopId, String maBai) {
        return problems.findReleasedInClass(lopId, maBai).map(DeBaiHocSinhService::choHocSinh);
    }

    private static DeBaiChoHocSinh choHocSinh(Problem p) {
        return new DeBaiChoHocSinh(p.code(), p.skillCode(), p.level4().name(), p.statementText(), p.statementLatex(),
            p.answerForm(), p.startStep());
    }
}
