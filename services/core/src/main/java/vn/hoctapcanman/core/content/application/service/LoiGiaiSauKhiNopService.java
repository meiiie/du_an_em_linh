package vn.hoctapcanman.core.content.application.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.application.port.LoiGiaiSauKhiNop;
import vn.hoctapcanman.core.content.domain.repository.SolutionRepository;

/**
 * Lời giải sau khi nộp: lời giải, phiên bản nội dung và phát hành ở lớp đọc trong một câu lệnh
 * ({@link SolutionRepository#findReleased}), nên không bao giờ trả lời giải vừa sửa (phiên bản mới, chưa kiểm) cho bài làm
 * của đề cũ; viết cho học sinh bằng {@link VietLoiGiai}.
 */
@Service
@Transactional(readOnly = true)
public class LoiGiaiSauKhiNopService implements LoiGiaiSauKhiNop {

    private final SolutionRepository solutions;

    public LoiGiaiSauKhiNopService(SolutionRepository solutions) {
        this.solutions = solutions;
    }

    @Override
    public Optional<String> vanBan(UUID lopId, UUID problemId, int phienBan) {
        return solutions.findReleased(lopId, problemId, phienBan).flatMap(VietLoiGiai::viet);
    }
}
