package vn.hoctapcanman.core.practice.infrastructure.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.practice.application.dto.KetQuaNopBai;
import vn.hoctapcanman.core.practice.application.dto.NopBuocRequest;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.BaiCuaHocSinh;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ChiTietBai;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.KetQuaNopBuoc;
import vn.hoctapcanman.core.practice.application.exception.BaiKhongTimThayException;
import vn.hoctapcanman.core.practice.application.usecase.GetBaiCuaHocSinhUseCase;
import vn.hoctapcanman.core.practice.application.usecase.GetChiTietBaiUseCase;
import vn.hoctapcanman.core.practice.application.usecase.NopBaiUseCase;
import vn.hoctapcanman.core.practice.application.usecase.NopBuocUseCase;

/**
 * API học sinh làm bài (T021, contracts/api-core.md §Học sinh). {@code SecurityConfig} chỉ cho vai trò {@code STUDENT} vào
 * {@code /api/hs/**}. Người gọi lấy từ access token, lớp là lớp của em ({@code ClassMembership.lopHoc}); không id nào lấy từ
 * thân hay đường dẫn. Chưa ghi danh lớp nào thì danh sách rỗng, các đường còn lại 404 như bài không có.
 */
@RestController
@RequestMapping("/api/hs/bai")
public class HocSinhBaiController {

    private final ClassMembership membership;
    private final GetBaiCuaHocSinhUseCase danhSach;
    private final GetChiTietBaiUseCase chiTiet;
    private final NopBuocUseCase nopBuoc;
    private final NopBaiUseCase nopBai;

    public HocSinhBaiController(ClassMembership membership, GetBaiCuaHocSinhUseCase danhSach, GetChiTietBaiUseCase chiTiet,
            NopBuocUseCase nopBuoc, NopBaiUseCase nopBai) {
        this.membership = membership;
        this.danhSach = danhSach;
        this.chiTiet = chiTiet;
        this.nopBuoc = nopBuoc;
        this.nopBai = nopBai;
    }

    @GetMapping
    public List<BaiCuaHocSinh> danhSach(@AuthenticationPrincipal Jwt jwt) {
        UUID hocSinh = UUID.fromString(jwt.getSubject());
        return membership.lopHoc(hocSinh).map(lop -> danhSach.execute(hocSinh, lop)).orElse(List.of());
    }

    @GetMapping("/{maBai}")
    public ChiTietBai chiTiet(@AuthenticationPrincipal Jwt jwt, @PathVariable String maBai) {
        UUID hocSinh = UUID.fromString(jwt.getSubject());
        return chiTiet.execute(hocSinh, lopCua(hocSinh), maBai);
    }

    @PostMapping("/{maBai}/buoc")
    public KetQuaNopBuoc nopBuoc(@AuthenticationPrincipal Jwt jwt, @PathVariable String maBai, @Valid @RequestBody NopBuocRequest yeuCau) {
        UUID hocSinh = UUID.fromString(jwt.getSubject());
        return nopBuoc.execute(hocSinh, lopCua(hocSinh), maBai, yeuCau);
    }

    @PostMapping("/{maBai}/nop")
    public KetQuaNopBai nopBai(@AuthenticationPrincipal Jwt jwt, @PathVariable String maBai) {
        UUID hocSinh = UUID.fromString(jwt.getSubject());
        return nopBai.execute(hocSinh, lopCua(hocSinh), maBai);
    }

    private UUID lopCua(UUID hocSinh) {
        return membership.lopHoc(hocSinh).orElseThrow(BaiKhongTimThayException::new);
    }
}
