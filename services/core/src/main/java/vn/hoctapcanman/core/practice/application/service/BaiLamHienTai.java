package vn.hoctapcanman.core.practice.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.TrangThaiBaiLam;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.repository.GradingResultRepository;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;

/**
 * Bài làm học sinh đang thấy của một bài ở một lớp, cho màn danh sách và màn làm bài (T021). Chỉ đọc. Không kiểm học sinh
 * thuộc lớp hay bài đang phát hành: nơi gọi kiểm trước.
 *
 * <p>Bài làm đang thấy ở phiên bản nội dung hiện tại của bài: bài làm đang làm nếu có (làm lại sau khi nộp cũng là đang
 * làm), không thì lần nộp mới nhất, không thì chưa làm. Bài làm dở của đề cũ không tính: bước đã viết là cho đề khác.
 *
 * <p>Lần chấm của một bước là lần chấm của đúng nội dung hiện tại: yêu cầu chấm tới bước đó dựng lại từ các bước đã lưu
 * ({@link YeuCauCham}, như {@code NopBuocUseCase}) và so băm. Sửa một bước từ bước bắt đầu tới bước đó sau lần chấm thì băm
 * khác, lần chấm cũ không còn là của bước. Lần chấm có phán quyết (một mỗi băm) thắng {@code KHONG_CHAM_DUOC}; chỉ có
 * {@code KHONG_CHAM_DUOC} thì lấy lần mới nhất.
 */
@Service
public class BaiLamHienTai {

    private final SubmissionRepository submissions;
    private final GradingResultRepository grades;

    public BaiLamHienTai(SubmissionRepository submissions, GradingResultRepository grades) {
        this.submissions = submissions;
        this.grades = grades;
    }

    public Xem cua(UUID hocSinhId, UUID lopId, BaiChoLamBai bai) {
        Optional<Submission> baiLam = submissions.findOpen(hocSinhId, lopId, bai.problemId(), bai.phienBan())
            .or(() -> submissions.history(hocSinhId, bai.problemId()).daNopMoiNhat(lopId, bai.phienBan()).map(Submission.DaNop::baiLam));
        if (baiLam.isEmpty()) {
            return new Xem(TrangThaiBaiLam.CHUA_LAM, null, List.of());
        }
        Submission bl = baiLam.get();
        List<StepWork> daLuu = submissions.steps(bl.id());
        return new Xem(bl.isOpen() ? TrangThaiBaiLam.DANG_LAM : TrangThaiBaiLam.DA_NOP, bl,
            chamHienTai(bai, daLuu, grades.bySubmission(bl.id())));
    }

    /** Các bước đã lưu theo thứ tự khung, mỗi bước kèm lần chấm của nội dung hiện tại tới bước đó, nếu có. */
    static List<Buoc> chamHienTai(BaiChoLamBai bai, List<StepWork> daLuu, List<GradingResult> lanCham) {
        List<String> khung = bai.cacBuoc();
        int dau = YeuCauCham.batDau(khung, bai.buocBatDau());
        List<Buoc> ra = new ArrayList<>();
        for (StepWork b : daLuu) {
            GradingResult cham = null;
            if (bai.ham() != null && khung.indexOf(b.stepCode()) >= dau) {
                String bam = YeuCauCham.bam(YeuCauCham.dung(bai.ham(), khung, bai.khaiBaoKetLuan(), bai.buocBatDau(), b.stepCode(), daLuu));
                cham = lanCham.stream()
                    .filter(g -> g.stepCode().equals(b.stepCode()) && g.requestHash().equals(bam))
                    .max(Comparator.comparing((GradingResult g) -> g.result() != GradeStatus.KHONG_CHAM_DUOC)
                        .thenComparing(GradingResult::gradedAt))
                    .orElse(null);
            }
            ra.add(new Buoc(b, cham));
        }
        return ra;
    }

    /** Bài làm đang thấy: trạng thái, bài làm ({@code null} khi chưa làm), các bước đã lưu theo thứ tự khung. */
    public record Xem(TrangThaiBaiLam trangThai, @Nullable Submission baiLam, List<Buoc> cacBuoc) {

        public Xem {
            Objects.requireNonNull(trangThai, "trangThai");
            cacBuoc = List.copyOf(cacBuoc);
            if ((trangThai == TrangThaiBaiLam.CHUA_LAM) != (baiLam == null)) {
                throw new IllegalArgumentException("Chỉ chưa làm mới không có bài làm");
            }
        }

        /** Số bước mà lần chấm của nội dung hiện tại là {@code DAT}. */
        public int soBuocDat() {
            return (int) cacBuoc.stream().filter(b -> b.cham() != null && b.cham().result() == GradeStatus.DAT).count();
        }

        /** Kết quả lúc nộp khi bài làm đã nộp; không thì {@code null}. */
        public @Nullable GradeStatus ketQuaNop() {
            return baiLam == null || baiLam.isOpen() ? null : baiLam.result();
        }
    }

    /** Nội dung đã lưu của một bước và lần chấm của đúng nội dung đó, nếu có. */
    public record Buoc(StepWork noiDung, @Nullable GradingResult cham) {}
}
