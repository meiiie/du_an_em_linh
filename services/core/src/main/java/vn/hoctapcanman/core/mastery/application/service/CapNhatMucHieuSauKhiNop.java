package vn.hoctapcanman.core.mastery.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.CanhBaoGiaoVien;
import vn.hoctapcanman.core.content.application.port.DanhMucKyNang;
import vn.hoctapcanman.core.mastery.domain.model.BktConfig;
import vn.hoctapcanman.core.mastery.domain.model.Level4;
import vn.hoctapcanman.core.mastery.domain.model.MasteryEvent;
import vn.hoctapcanman.core.mastery.domain.model.MasteryEvent.Rule;
import vn.hoctapcanman.core.mastery.domain.model.MasteryState;
import vn.hoctapcanman.core.mastery.domain.repository.MasteryRepository;
import vn.hoctapcanman.core.practice.application.dto.BaiDaNop;
import vn.hoctapcanman.core.practice.application.dto.KetQuaBai;
import vn.hoctapcanman.core.practice.application.dto.ThayDoiMucHieu;
import vn.hoctapcanman.core.practice.application.port.CapNhatMucHieu;

/**
 * Mức hiểu sau khi nộp bài (T050): {@code applyMastery} của v0 cùng điều kiện gọi nó trong {@code nopBuoc}, chạy một lần cho
 * mỗi bài làm đã nộp, trong giao dịch nộp.
 * <ol>
 *   <li>Bài làm đã có sự kiện: phát lại thay đổi đã ghi, không tính lần hai.</li>
 *   <li>Không tính: {@code KHONG_KIEM_DUOC} (chờ giáo viên), và lỗi trình bày dấu U ({@code SAI} với {@code toan_dung}
 *       đúng). Không ghi gì, trả rỗng.</li>
 *   <li>Kỹ năng được tính: của bài; bài sai có mã lỗi đủ tin cậy thì kỹ năng của mã lỗi ({@code THEO_MA_LOI}), không thì
 *       có bước sai thì kỹ năng của bước ({@code THEO_BUOC}).</li>
 *   <li>Khóa trạng thái của kỹ năng đó, áp BKT ({@link MasteryState#apply}), ghi trạng thái và sự kiện; sai đủ ngưỡng kẹt
 *       thì ghi cảnh báo {@code KET} cho giáo viên của lớp (cổng của classroom không ghi trùng cảnh báo đang mở).</li>
 * </ol>
 * Khác v0: v0 tính ở mỗi lần nộp bước, kể cả bước sai giữa bài; v2 tính một lần khi nộp bài, với phán quyết của bước kết
 * luận (nên {@code DAT} luôn là xong cả bài).
 */
@Service
public class CapNhatMucHieuSauKhiNop implements CapNhatMucHieu {

    private final MasteryRepository repo;
    private final DanhMucKyNang danhMuc;
    private final CanhBaoGiaoVien canhBao;

    public CapNhatMucHieuSauKhiNop(MasteryRepository repo, DanhMucKyNang danhMuc, CanhBaoGiaoVien canhBao) {
        this.repo = repo;
        this.danhMuc = danhMuc;
        this.canhBao = canhBao;
    }

    @Override
    @Transactional
    public List<ThayDoiMucHieu> sauKhiNop(BaiDaNop bai) {
        Optional<MasteryEvent> daTinh = repo.eventOf(bai.baiLamId());
        if (daTinh.isPresent()) {
            return List.of(thayDoi(daTinh.get()));
        }
        if (bai.ketQua() == KetQuaBai.KHONG_KIEM_DUOC || (bai.ketQua() == KetQuaBai.SAI && Boolean.TRUE.equals(bai.toanDung()))) {
            return List.of();
        }
        BktConfig cfg = repo.config();
        boolean dung = bai.ketQua() == KetQuaBai.DAT;
        KyNangDuocTinh tinh = dung ? new KyNangDuocTinh(bai.kyNang(), Rule.THEO_KY_NANG_BAI) : kyNangKhiSai(bai, cfg);
        MasteryState truoc = repo.lock(MasteryState.initial(bai.hocSinhId(), tinh.kyNang(), cfg, bai.nopLuc()));
        MasteryState.Update u = truoc.apply(new MasteryState.Evidence(dung, Level4.valueOf(bai.mucDo()), bai.maLoi(), bai.nghiDoanMo()),
            cfg, bai.nopLuc());
        @Nullable String buocSai = bai.buocSai() == null ? null : bai.buocSai().maBuoc();
        MasteryEvent suKien = new MasteryEvent(UUID.randomUUID(), bai.hocSinhId(), tinh.kyNang(), bai.baiLamId(), u.delta(),
            bai.nghiDoanMo() ? Rule.NGHI_DOAN_MO : tinh.luat(), buocSai, bai.maLoi(), bai.doTinCay(), bai.nghiDoanMo(),
            truoc.level(), u.after().level(), cfg.version(), bai.nopLuc());
        repo.save(u.after(), suKien);
        if (u.stuckAlert()) {
            canhBao.ghiKet(bai.hocSinhId(), tinh.kyNang(), null, null, "Kẹt " + u.after().stuckCounter() + " lượt ở " + tinh.kyNang());
        }
        return List.of(thayDoi(suKien));
    }

    private KyNangDuocTinh kyNangKhiSai(BaiDaNop bai, BktConfig cfg) {
        if (coChu(bai.maLoi()) && bai.doTinCay() != null && bai.doTinCay() >= cfg.errorCodeConfidence()) {
            return danhMuc.kyNangCuaMaLoi(bai.maLoi()).map(kn -> new KyNangDuocTinh(kn, Rule.THEO_MA_LOI))
                .orElseGet(() -> new KyNangDuocTinh(bai.kyNang(), Rule.THEO_KY_NANG_BAI));
        }
        if (bai.buocSai() != null) {
            return new KyNangDuocTinh(danhMuc.kyNangCuaBuoc(bai.buocSai().maBuoc()).orElse(bai.kyNang()), Rule.THEO_BUOC);
        }
        return new KyNangDuocTinh(bai.kyNang(), Rule.THEO_KY_NANG_BAI);
    }

    /** Chuỗi khác rỗng, như điều kiện {@code if (opts.maLoi)} của JavaScript. */
    private static boolean coChu(@Nullable String s) {
        return s != null && !s.isEmpty();
    }

    private static ThayDoiMucHieu thayDoi(MasteryEvent e) {
        return new ThayDoiMucHieu(e.skillCode(), e.levelBefore().name(), e.levelAfter().name());
    }

    private record KyNangDuocTinh(String kyNang, Rule luat) {}
}
