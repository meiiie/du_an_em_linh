package vn.hoctapcanman.core.practice.application.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatChoHocSinh;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.port.LoiGiaiSauKhiNop;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;

/**
 * Cửa duy nhất đưa lời giải mẫu tới học sinh (FR-006, ADR 003). Là nơi gọi duy nhất của cổng {@link LoiGiaiSauKhiNop} của
 * nội dung (ArchUnit {@code LOI_GIAI_CHI_QUA_CUA_MO}), và module khác không import được gói này
 * ({@code MODULE_CHI_GOI_NHAU_QUA_CONG}), nên mọi đường tới chữ lời giải đi qua đủ ba điều kiện của {@link #cho}.
 */
@Service
public class MoLoiGiai {

    private final SubmissionRepository submissions;
    private final ClassMembership membership;
    private final LoiGiaiSauKhiNop loiGiai;

    public MoLoiGiai(SubmissionRepository submissions, ClassMembership membership, LoiGiaiSauKhiNop loiGiai) {
        this.submissions = submissions;
        this.membership = membership;
        this.loiGiai = loiGiai;
    }

    /**
     * Lời giải cho bài làm đã nộp {@code daNop}, mở khi đủ cả ba, kiểm theo thứ tự này để không đọc lời giải khi chưa cần:
     * <ol>
     *   <li>bài làm đã nộp thật và học sinh không còn làm lại bài ở cùng phiên bản, ở bất kỳ lớp nào
     *       ({@code SubmissionHistory.choMoLoiGiai});</li>
     *   <li>người nộp còn là học sinh của lớp, và lớp bật «mở lời giải sau khi nộp»;</li>
     *   <li>bài còn phát hành ở lớp, đúng phiên bản của bài làm, có lời giải viết được.</li>
     * </ol>
     * Thiếu một điều thì rỗng, không lộ điều nào thiếu. Cờ đọc lúc gọi: giáo viên tắt cờ thì lời giải đóng lại cả với bài đã
     * nộp.
     */
    public Optional<String> cho(Submission.DaNop daNop) {
        Submission bl = daNop.baiLam();
        if (!submissions.history(bl.studentId(), bl.problemId()).choMoLoiGiai(daNop)) {
            return Optional.empty();
        }
        boolean lopChoMo = membership.caiDatChoHocSinh(bl.studentId(), bl.classId())
            .map(CaiDatChoHocSinh::moLoiGiaiSauKhiNop)
            .orElse(false);
        if (!lopChoMo) {
            return Optional.empty();
        }
        return loiGiai.vanBan(bl.classId(), bl.problemId(), bl.contentVersion());
    }
}
