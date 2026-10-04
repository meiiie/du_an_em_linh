package vn.hoctapcanman.core.practice.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.dto.NopBuocRequest;
import vn.hoctapcanman.core.practice.application.dto.SuKienNop;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;
import vn.hoctapcanman.core.practice.application.port.MayCham;
import vn.hoctapcanman.core.practice.application.service.DocKetQuaCham;
import vn.hoctapcanman.core.practice.application.service.YeuCauCham;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;
import vn.hoctapcanman.core.practice.domain.model.InputEvent;
import vn.hoctapcanman.core.practice.domain.model.NghiDoanMo;
import vn.hoctapcanman.core.practice.domain.model.SignTable;
import vn.hoctapcanman.core.practice.domain.model.StepLine;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.model.TableCell;
import vn.hoctapcanman.core.practice.domain.repository.GradingResultRepository;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;

/**
 * Học sinh nộp một bước của bài ở một lớp (T020, FR-008–010), như {@code nopBuoc} của v0 nhưng bài làm sống qua nhiều lần
 * nộp:
 * <ol>
 *   <li>Người gọi phải là học sinh của lớp (id lớp tường minh) và bài phải đang phát hành ở lớp, chấm từng bước được; không
 *       thì {@link BaiKhongTimThayException}, cùng một lỗi, không lộ bài hay lớp.</li>
 *   <li>Trong một giao dịch: mở (hoặc lấy) bài làm đang làm ở phiên bản nội dung hiện tại, thay trọn nội dung bước (khóa
 *       dòng bài làm tới hết giao dịch), thêm sự kiện nhập chưa có (gửi lại sau khi mất phản hồi không ghi trùng, Codex
 *       #140), bật nghi đoán mò khi một ô bị đổi từ ngưỡng trở lên, đọc lại các bước đã lưu.</li>
 *   <li>Bước kết luận chỉ nhận ô đề hỏi; ô phải khai lấy từ bài ({@code khaiBaoKetLuan}), không từ bài làm.</li>
 *   <li>Dựng yêu cầu {@code /v1/grade} như v0 từ các bước đã lưu (không từ máy học sinh) và băm nó. Đã có lần chấm có phán
 *       quyết cho đúng yêu cầu này thì trả lần đó, không gọi lại dịch vụ toán (hai tab nộp cùng bước ghi một lần).</li>
 *   <li>Gọi dịch vụ toán ngoài giao dịch (không giữ khóa dòng trong lúc chờ tới 12 s), rồi ghi kết quả. Dịch vụ toán lỗi
 *       thì {@code KHONG_CHAM_DUOC}, không bao giờ là đạt; nộp lại sẽ chấm lại.</li>
 * </ol>
 * Chưa làm ở đây: cập nhật mức (mastery), đếm kẹt và đề xuất gửi thầy cô (tutor), giới hạn tần suất (web, T021).
 */
@Service
public class NopBuocUseCase {

    /** Số sự kiện nhập tối đa mỗi lần nộp. */
    static final int TOI_DA_SU_KIEN = 500;
    /** Loại bảng duy nhất của khung đơn điệu – cực trị (v0 luôn gửi {@code XET_DAU}). */
    static final String BANG_XET_DAU = "XET_DAU";

    private final ClassMembership membership;
    private final BaiDeLam baiDeLam;
    private final SubmissionRepository submissions;
    private final GradingResultRepository grades;
    private final MayCham mayCham;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int nguongDoanMo;

    public NopBuocUseCase(ClassMembership membership, BaiDeLam baiDeLam, SubmissionRepository submissions, GradingResultRepository grades,
            MayCham mayCham, TransactionTemplate tx, Clock clock, @Value("${app.practice.nguong-doan-mo:4}") int nguongDoanMo) {
        if (nguongDoanMo < 1) {
            throw new IllegalArgumentException("app.practice.nguong-doan-mo phải từ 1");
        }
        this.membership = membership;
        this.baiDeLam = baiDeLam;
        this.submissions = submissions;
        this.grades = grades;
        this.mayCham = mayCham;
        this.tx = tx;
        this.clock = clock;
        this.nguongDoanMo = nguongDoanMo;
    }

    public KetQuaNopBuoc execute(UUID hocSinhId, UUID lopId, String maBai, NopBuocRequest yeuCau) {
        if (!membership.laHocSinh(hocSinhId, lopId)) {
            throw new BaiKhongTimThayException();
        }
        BaiChoLamBai bai = baiDeLam.bai(lopId, maBai).filter(b -> b.ham() != null).orElseThrow(BaiKhongTimThayException::new);
        String ham = Objects.requireNonNull(bai.ham());
        if (bai.cacBuoc().indexOf(yeuCau.maBuoc()) < YeuCauCham.batDau(bai.cacBuoc(), bai.buocBatDau())) {
            throw new IllegalArgumentException("Bước không thuộc khung của bài: " + yeuCau.maBuoc());
        }
        StepWork buoc = buoc(yeuCau);
        if (yeuCau.maBuoc().equals(bai.cacBuoc().getLast())) {
            for (StepLine l : buoc.lines()) {
                String khoa = l.kind() == null ? null : YeuCauCham.KHAI_BAO.get(l.kind());
                if (khoa != null && !bai.khaiBaoKetLuan().contains(khoa)) {
                    throw new IllegalArgumentException("Đề không hỏi ô kết luận " + l.kind());
                }
            }
        }
        List<InputEvent> suKien = suKien(yeuCau.suKien());
        Instant moLuc = clock.instant();
        DaLuu daLuu = Objects.requireNonNull(tx.execute(s -> {
            Submission baiLam = submissions.openOrGet(Submission.open(lopId, hocSinhId, bai.problemId(), bai.phienBan(), moLuc));
            submissions.saveStep(baiLam.id(), buoc);
            // Dòng bài làm đang khóa (saveStep), nên hai lần gửi lại cùng lúc đọc và ghi sự kiện lần lượt.
            List<InputEvent> daCo = submissions.events(baiLam.id());
            List<InputEvent> moi = suKien.stream().distinct().filter(e -> !daCo.contains(e)).toList();
            if (!moi.isEmpty()) {
                submissions.addEvents(baiLam.id(), moi);
            }
            boolean nghi = baiLam.guessSuspected();
            if (!nghi) {
                Optional<String> lyDo = NghiDoanMo.lyDo(submissions.events(baiLam.id()), nguongDoanMo);
                if (lyDo.isPresent()) {
                    submissions.update(baiLam.suspectGuess(lyDo.get()));
                    nghi = true;
                }
            }
            return new DaLuu(baiLam.id(), submissions.steps(baiLam.id()), nghi);
        }));
        Map<String, @Nullable Object> payload = YeuCauCham.dung(ham, bai.cacBuoc(), bai.khaiBaoKetLuan(), bai.buocBatDau(), yeuCau.maBuoc(),
            daLuu.cacBuoc());
        String bam = YeuCauCham.bam(payload);
        GradingResult ketQua = grades.findByRequest(daLuu.baiLamId(), bam).orElseGet(() -> {
            Map<String, @Nullable Object> phanHoi = mayCham.cham(payload).orElse(null);
            return grades.record(DocKetQuaCham.ketQua(daLuu.baiLamId(), yeuCau.maBuoc(), bam, phanHoi, clock.instant()));
        });
        return DocKetQuaCham.choHocSinh(ketQua, bai.cacBuoc(), daLuu.nghiDoanMo());
    }

    private static StepWork buoc(NopBuocRequest yeuCau) {
        List<StepLine> dong = yeuCau.dong() == null ? List.of()
            : yeuCau.dong().stream().map(d -> new StepLine(d.dong(), d.latex(), d.loai())).toList();
        SignTable bang = yeuCau.bang() == null ? null
            : new SignTable(BANG_XET_DAU, yeuCau.bang().stream().map(o -> new TableCell(o.hang(), o.k(), o.giaTri())).toList());
        return new StepWork(yeuCau.maBuoc(), dong, bang);
    }

    private static List<InputEvent> suKien(@Nullable List<SuKienNop> suKien) {
        if (suKien == null) {
            return List.of();
        }
        if (suKien.size() > TOI_DA_SU_KIEN) {
            throw new IllegalArgumentException("Quá nhiều sự kiện nhập trong một lần nộp (tối đa " + TOI_DA_SU_KIEN + ")");
        }
        // Thời điểm cắt về micro giây như cột timestamptz, để sự kiện gửi lại so trùng được với sự kiện đã lưu.
        return suKien.stream()
            .map(e -> new InputEvent(e.maBuoc(), e.hang(), e.k(), e.giaTriCu(), e.giaTriMoi(), e.luc().truncatedTo(ChronoUnit.MICROS)))
            .toList();
    }

    /** Bài làm sau khi lưu bước: id, các bước đã lưu (thứ tự khung), cờ nghi đoán mò. */
    private record DaLuu(UUID baiLamId, List<StepWork> cacBuoc, boolean nghiDoanMo) {}
}
