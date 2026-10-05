package vn.hoctapcanman.core.practice.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Mọi bài làm của một học sinh cho một bài, ở mọi lớp và mọi phiên bản nội dung. Giữ luật FR-006 về lúc lời giải của một
 * bài đã nộp mở được, và chọn lần nộp để phát lại khi học sinh gửi nộp lại.
 */
public record SubmissionHistory(UUID studentId, UUID problemId, List<Submission> attempts) {

    public SubmissionHistory {
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(problemId, "problemId");
        attempts = List.copyOf(attempts);
        if (attempts.stream().anyMatch(s -> !s.studentId().equals(studentId) || !s.problemId().equals(problemId))) {
            throw new IllegalArgumentException("Lịch sử chỉ gồm bài làm của một học sinh cho một bài");
        }
    }

    /**
     * Lời giải của {@code daNop} mở được về phía bài làm (FR-006): lịch sử có đúng bài làm đó ở trạng thái đã nộp, và học
     * sinh không còn bài làm đang làm nào ở cùng phiên bản nội dung, ở bất kỳ lớp nào (mở bài làm mới là đang làm lại, lời
     * giải đóng). Bài làm dở của phiên bản khác không chặn: cổng lời giải của nội dung chỉ mở cho phiên bản hiện tại. Không
     * xét cờ lớp hay phát hành.
     */
    public boolean choMoLoiGiai(Submission.DaNop daNop) {
        Submission nop = daNop.baiLam();
        if (!nop.studentId().equals(studentId) || !nop.problemId().equals(problemId)) {
            throw new IllegalArgumentException("Bài làm không thuộc lịch sử này");
        }
        boolean daNopThat = attempts.stream().anyMatch(s -> s.id().equals(nop.id()) && !s.isOpen());
        boolean dangLamLai = attempts.stream().anyMatch(s -> s.isOpen() && s.contentVersion() == nop.contentVersion());
        return daNopThat && !dangLamLai;
    }

    /** Lần nộp mới nhất ở lớp {@code classId} cho phiên bản nội dung {@code contentVersion}. */
    public Optional<Submission.DaNop> daNopMoiNhat(UUID classId, int contentVersion) {
        return attempts.stream()
            .filter(s -> s.classId().equals(classId) && s.contentVersion() == contentVersion)
            .flatMap(s -> s.daNop().stream())
            .max(Comparator.comparing(Submission.DaNop::nopLuc).thenComparing(d -> d.baiLam().id()));
    }

    /** Còn bài làm đang làm ở lớp {@code classId} cho một phiên bản nội dung khác {@code contentVersion}: đề đã đổi khi đang làm. */
    public boolean dangLamDeKhac(UUID classId, int contentVersion) {
        return attempts.stream().anyMatch(s -> s.isOpen() && s.classId().equals(classId) && s.contentVersion() != contentVersion);
    }
}
