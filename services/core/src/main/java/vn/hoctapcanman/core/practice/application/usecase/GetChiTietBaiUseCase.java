package vn.hoctapcanman.core.practice.application.usecase;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.classroom.application.dto.CaiDatChoHocSinh;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai.BuocDaLam;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai.DongDaLam;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai.ODaLam;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;
import vn.hoctapcanman.core.practice.application.service.BaiLamHienTai;
import vn.hoctapcanman.core.practice.application.service.DocKetQuaCham;
import vn.hoctapcanman.core.practice.application.service.TenBuoc;
import vn.hoctapcanman.core.practice.domain.model.StepWork;
import vn.hoctapcanman.core.practice.domain.model.Submission;

/**
 * Đề và bài làm của học sinh cho một bài ở lớp (T021, FR-006–010, {@code GET /api/hs/bai/{maBai}}). Không phải học sinh của
 * lớp, hay bài không đang phát hành ở lớp: {@link BaiKhongTimThayException}, cùng lỗi như nộp bước, không lộ bài hay lớp. Bài
 * đang phát hành mà chưa giao cho em vẫn xem được. Không có lời giải, đáp án hay dữ kiện bảo vệ (FR-006); kết quả, thông
 * báo, chỗ sai của từng bước như phản hồi nộp bước ({@link DocKetQuaCham#choHocSinh}) cho lần chấm của nội dung hiện tại.
 */
@Service
public class GetChiTietBaiUseCase {

    private final ClassMembership membership;
    private final BaiDeLam baiDeLam;
    private final BaiLamHienTai baiLam;

    public GetChiTietBaiUseCase(ClassMembership membership, BaiDeLam baiDeLam, BaiLamHienTai baiLam) {
        this.membership = membership;
        this.baiDeLam = baiDeLam;
        this.baiLam = baiLam;
    }

    public ChiTietBai execute(UUID hocSinhId, UUID lopId, String maBai) {
        if (!membership.laHocSinh(hocSinhId, lopId)) {
            throw new BaiKhongTimThayException();
        }
        BaiChoLamBai bai = baiDeLam.bai(lopId, maBai).orElseThrow(BaiKhongTimThayException::new);
        BaiLamHienTai.Xem xem = baiLam.cua(hocSinhId, lopId, bai);
        Submission bl = xem.baiLam();
        boolean nghiDoanMo = bl != null && bl.guessSuspected();
        List<BuocDaLam> cacBuoc = xem.cacBuoc().stream().map(b -> buoc(b, bai.cacBuoc(), nghiDoanMo)).toList();
        boolean coTheMoLoiGiai = membership.caiDatChoHocSinh(hocSinhId, lopId).map(CaiDatChoHocSinh::moLoiGiaiSauKhiNop).orElse(false);
        return new ChiTietBai(bai.ma(), new ChiTietBai.De(bai.deBai(), bai.deBaiLatex()), bai.kyNang(), bai.tenKyNang(), bai.mucDo(),
            bai.dangTraLoi(), bai.buocBatDau(), bai.khaiBaoKetLuan(), bai.cacBuoc().stream().map(TenBuoc::cua).toList(),
            new ChiTietBai.BaiLam(xem.trangThai(), cacBuoc), coTheMoLoiGiai);
    }

    private static BuocDaLam buoc(BaiLamHienTai.Buoc b, List<String> khung, boolean nghiDoanMo) {
        StepWork n = b.noiDung();
        List<DongDaLam> dong = n.lines().stream().map(l -> new DongDaLam(l.lineNo(), l.latex(), l.kind())).toList();
        List<ODaLam> bang = n.table() == null ? List.of()
            : n.table().cells().stream().map(o -> new ODaLam(o.row(), o.k(), o.value())).toList();
        if (b.cham() == null) {
            return new BuocDaLam(n.stepCode(), dong, bang, null, null, List.of());
        }
        KetQuaNopBuoc kq = DocKetQuaCham.choHocSinh(b.cham(), khung, nghiDoanMo);
        return new BuocDaLam(n.stepCode(), dong, bang, kq.ketQua(), kq.thongBao(), kq.oSai());
    }
}
