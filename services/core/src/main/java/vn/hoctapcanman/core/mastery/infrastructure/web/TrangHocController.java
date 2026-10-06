package vn.hoctapcanman.core.mastery.infrastructure.web;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctapcanman.core.mastery.application.dto.hocsinh.TrangHoc;
import vn.hoctapcanman.core.mastery.application.usecase.GetTrangHocUseCase;

/**
 * Trang «Học» của học sinh (contracts/api-core.md §Học sinh). {@code SecurityConfig} chỉ cho vai trò {@code STUDENT} vào
 * {@code /api/hs/**}; người gọi lấy từ access token, không id nào từ yêu cầu.
 */
@RestController
public class TrangHocController {

    private final GetTrangHocUseCase trangHoc;

    public TrangHocController(GetTrangHocUseCase trangHoc) {
        this.trangHoc = trangHoc;
    }

    @GetMapping("/api/hs/trang-hoc")
    public TrangHoc trangHoc(@AuthenticationPrincipal Jwt jwt) {
        return trangHoc.execute(UUID.fromString(jwt.getSubject()));
    }
}
