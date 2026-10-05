package vn.hoctapcanman.core.practice.application.usecase;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.practice.application.dto.BaiDaNop;
import vn.hoctapcanman.core.practice.application.dto.KetQuaBai;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBai;
import vn.hoctapcanman.core.practice.application.dto.ThayDoiMucHieu;
import vn.hoctapcanman.core.practice.application.exception.BaiChuaNopDuocException;
import vn.hoctapcanman.core.practice.application.exception.BaiChuaNopDuocException.LyDo;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;
import vn.hoctapcanman.core.practice.application.port.CapNhatMucHieu;
import vn.hoctapcanman.core.practice.application.service.DocKetQuaCham;
import vn.hoctapcanman.core.practice.application.service.MoLoiGiai;
import vn.hoctapcanman.core.practice.application.service.YeuCauCham;
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.Submission;
import vn.hoctapcanman.core.practice.domain.model.SubmissionHistory;
import vn.hoctapcanman.core.practice.domain.repository.GradingResultRepository;
import vn.hoctapcanman.core.practice.domain.repository.SubmissionRepository;

/**
 * Học sinh nộp bài của một bài ở một lớp (T020, FR-006, FR-023). Nộp bài không chấm: kết quả cả bài là phán quyết đã ghi
 * của bước kết luận (bước cuối của khung) trên nội dung hiện tại của bài làm, và lần chấm đó thành căn cứ ghim vào bài làm.
 * <ol>
 *   <li>Không phải học sinh của lớp, bài chưa phát hành ở lớp hay không chấm từng bước được: {@link BaiKhongTimThayException},
 *       cùng một lỗi như nộp bước.</li>
 *   <li>Trong một giao dịch, khóa bài làm đang làm ở phiên bản nội dung hiện tại như mọi lần ghi phần con. Thiếu một bước từ
 *       bước bắt đầu tới bước kết luận thì {@code CHUA_LAM_DU_BUOC}: chỉ nộp bước kết luận thì máy chấm vẫn ghi một phán
 *       quyết (thiếu dòng), nhưng đó không phải bài đã làm. Đủ bước thì dựng lại yêu cầu chấm tới bước kết luận từ các bước
 *       đã lưu như {@code NopBuocUseCase} và tra phán quyết theo băm. Không có (chấm lỗi, hay đã sửa một bước sau lần chấm
 *       đó) thì {@code CHUA_CHAM_BUOC_KET_LUAN}. Cả hai trường hợp không ghi gì.</li>
 *   <li>Không khóa được bài làm đang làm: còn bài làm đang làm ở đúng phiên bản đã đọc (đề đổi trong lúc chờ khóa) hay ở
 *       phiên bản khác thì {@code DE_DA_DOI}; không thì lần nộp mới nhất ở đúng phiên bản được phát lại (gửi lại sau khi mất
 *       phản hồi, hai tab cùng nộp); chưa có thì {@code CHUA_LAM_DU_BUOC}.</li>
 *   <li>Cuối giao dịch: mọi {@link CapNhatMucHieu} (idempotent theo bài làm, nên phát lại trả đúng lần đầu).</li>
 *   <li>Sau commit: lời giải qua {@link MoLoiGiai}.</li>
 * </ol>
 * Không gọi dịch vụ toán. Chưa làm ở đây: giới hạn tần suất (web, T021).
 */
@Service
public class NopBaiUseCase {

    private final ClassMembership membership;
    private final BaiDeLam baiDeLam;
    private final SubmissionRepository submissions;
    private final GradingResultRepository grades;
    private final List<CapNhatMucHieu> mucHieu;
    private final MoLoiGiai moLoiGiai;
    private final TransactionTemplate tx;
    private final Clock clock;

    public NopBaiUseCase(ClassMembership membership, BaiDeLam baiDeLam, SubmissionRepository submissions, GradingResultRepository grades,
            List<CapNhatMucHieu> mucHieu, MoLoiGiai moLoiGiai, TransactionTemplate tx, Clock clock) {
        this.membership = membership;
        this.baiDeLam = baiDeLam;
        this.submissions = submissions;
        this.grades = grades;
        this.mucHieu = List.copyOf(mucHieu);
        this.moLoiGiai = moLoiGiai;
        this.tx = tx;
        this.clock = clock;
    }

    public KetQuaNopBai execute(UUID hocSinhId, UUID lopId, String maBai) {
        if (!membership.laHocSinh(hocSinhId, lopId)) {
            throw new BaiKhongTimThayException();
        }
        BaiChoLamBai bai = baiDeLam.bai(lopId, maBai).filter(b -> b.ham() != null).orElseThrow(BaiKhongTimThayException::new);
        DaNopVaMucHieu kq = Objects.requireNonNull(tx.execute(s -> {
            CoCanCu nop = submissions.lockOpen(hocSinhId, lopId, bai.problemId(), bai.phienBan())
                .map(dangLam -> nop(dangLam, bai))
                .orElseGet(() -> phatLai(hocSinhId, lopId, bai));
            BaiDaNop baiDaNop = baiDaNop(nop, bai);
            return new DaNopVaMucHieu(nop.daNop(), mucHieu.stream().flatMap(m -> m.sauKhiNop(baiDaNop).stream()).toList());
        }));
        return new KetQuaNopBai(ketQua(kq.daNop().ketQua()), kq.mucHieu(), moLoiGiai.cho(kq.daNop()).orElse(null));
    }

    private CoCanCu nop(Submission dangLam, BaiChoLamBai bai) {
        List<StepWork> daLuu = submissions.steps(dangLam.id());
        if (!daLamDuBuoc(bai, daLuu)) {
            throw new BaiChuaNopDuocException(LyDo.CHUA_LAM_DU_BUOC);
        }
        String bam = YeuCauCham.bam(YeuCauCham.dung(Objects.requireNonNull(bai.ham()), bai.cacBuoc(), bai.khaiBaoKetLuan(),
            bai.buocBatDau(), bai.cacBuoc().getLast(), daLuu));
        GradingResult canCu = grades.findByRequest(dangLam.id(), bam)
            .orElseThrow(() -> new BaiChuaNopDuocException(LyDo.CHUA_CHAM_BUOC_KET_LUAN));
        // Cắt về micro giây như cột timestamptz: phát lại đọc lúc nộp từ CSDL, BaiDaNop phải y hệt lần đầu.
        Submission.DaNop daNop = dangLam.submit(canCu, clock.instant().truncatedTo(ChronoUnit.MICROS));
        submissions.update(daNop.baiLam());
        return new CoCanCu(daNop, canCu);
    }

    private CoCanCu phatLai(UUID hocSinhId, UUID lopId, BaiChoLamBai bai) {
        SubmissionHistory lichSu = submissions.history(hocSinhId, bai.problemId());
        // lockOpen chỉ bỏ qua bài làm đang làm ở đúng phiên bản đã đọc khi đề đổi trong lúc chờ khóa: lần nộp cũ hơn của
        // phiên bản đó không phải kết quả của bài làm này.
        if (lichSu.dangLam(lopId, bai.phienBan())) {
            throw new BaiChuaNopDuocException(LyDo.DE_DA_DOI);
        }
        Submission.DaNop daNop = lichSu.daNopMoiNhat(lopId, bai.phienBan()).orElseThrow(() -> new BaiChuaNopDuocException(
            lichSu.dangLamDeKhac(lopId, bai.phienBan()) ? LyDo.DE_DA_DOI : LyDo.CHUA_LAM_DU_BUOC));
        return new CoCanCu(daNop, grades.findById(daNop.canCuId()).orElseThrow());
    }

    private static boolean daLamDuBuoc(BaiChoLamBai bai, List<StepWork> daLuu) {
        List<String> khung = bai.cacBuoc();
        Set<String> daLam = daLuu.stream().map(StepWork::stepCode).collect(Collectors.toSet());
        return daLam.containsAll(khung.subList(YeuCauCham.batDau(khung, bai.buocBatDau()), khung.size()));
    }

    /** Dựng chỉ từ bài làm đã nộp và căn cứ đã ghim, nên phát lại dựng ra y hệt. */
    private static BaiDaNop baiDaNop(CoCanCu nop, BaiChoLamBai bai) {
        Submission bl = nop.daNop().baiLam();
        GradingResult g = nop.canCu();
        return new BaiDaNop(bl.id(), bl.studentId(), bl.classId(), bl.problemId(), bai.kyNang(), bai.mucDo(), ketQua(nop.daNop().ketQua()),
            DocKetQuaCham.buocSai(g).orElse(null), g.errorCode(), g.confidence(), g.mathOk(), bl.guessSuspected(), nop.daNop().nopLuc());
    }

    private static KetQuaBai ketQua(GradeStatus g) {
        return switch (g) {
            case DAT -> KetQuaBai.DAT;
            case SAI -> KetQuaBai.SAI;
            case KHONG_KIEM_DUOC -> KetQuaBai.KHONG_KIEM_DUOC;
            case KHONG_CHAM_DUOC -> throw new IllegalStateException("Bài làm đã nộp luôn có phán quyết");
        };
    }

    /** Bài làm đã nộp cùng lần chấm làm căn cứ. */
    private record CoCanCu(Submission.DaNop daNop, GradingResult canCu) {}

    private record DaNopVaMucHieu(Submission.DaNop daNop, List<ThayDoiMucHieu> mucHieu) {}
}
