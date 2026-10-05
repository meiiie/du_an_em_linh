package vn.hoctapcanman.core.practice.application.service;

import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatChoHocSinh;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.port.LoiGiaiSauKhiNop;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;

/**
 * Cửa duy nhất đưa lời giải mẫu viết cho học sinh ra khỏi module nội dung (FR-006, ADR 003). Ngoài lớp hiện thực cổng
 * {@link LoiGiaiSauKhiNop}, đây là lớp duy nhất dùng cổng đó (ArchUnit {@code LOI_GIAI_CHI_QUA_CUA_MO}), và module khác không
 * import được gói này ({@code MODULE_CHI_GOI_NHAU_QUA_CONG}), nên chữ lời giải qua cổng luôn qua đủ ba điều kiện của
 * {@link #cho}. Luật không chặn mã trong nội dung đọc thẳng kho lời giải; đường đó dành cho màn giáo viên.
 */
@Service
public class MoLoiGiai {

    private final SubmissionRepository submissions;
    private final ClassMembership membership;
    private final LoiGiaiSauKhiNop loiGiai;
    private final TransactionTemplate anhChup;

    public MoLoiGiai(SubmissionRepository submissions, ClassMembership membership, LoiGiaiSauKhiNop loiGiai,
            PlatformTransactionManager giaoDich) {
        this.submissions = submissions;
        this.membership = membership;
        this.loiGiai = loiGiai;
        this.anhChup = new TransactionTemplate(giaoDich);
        anhChup.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        anhChup.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        anhChup.setReadOnly(true);
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
     * nộp. Ba điều đọc trong một ảnh chụp (REPEATABLE READ, chỉ đọc): kết quả đúng với một thời điểm, không ghép «chưa làm
     * lại» lúc này với «cờ bật» lúc khác (phán quyết #142, ba tác nhân). Ảnh chụp ở giao dịch riêng: gọi từ trong giao dịch
     * khác thì không nhập vào nó (nhập vào thì mức cách ly là của giao dịch ngoài), chỉ thấy dữ liệu đã commit, nên bài làm
     * nộp trong giao dịch ngoài chưa commit thì chưa mở.
     */
    public Optional<String> cho(Submission.DaNop daNop) {
        return Objects.requireNonNull(anhChup.execute(s -> trongAnhChup(daNop)));
    }

    private Optional<String> trongAnhChup(Submission.DaNop daNop) {
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
