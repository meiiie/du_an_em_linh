package vn.hoctoanai.core.identity.infrastructure.web;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctoanai.core.identity.application.dto.AccessTokenResponse;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.usecase.LoginUseCase;
import vn.hoctoanai.core.identity.application.usecase.LogoutUseCase;
import vn.hoctoanai.core.identity.application.usecase.RefreshSessionUseCase;

/**
 * Đăng nhập, làm mới, đăng xuất cho SPA (#57). Refresh token chỉ đi trong cookie HttpOnly {@link RefreshCookie}; thân
 * phản hồi có access token (trình duyệt giữ trong bộ nhớ) và người dùng. Làm mới và đăng xuất dựa vào cookie nên còn
 * bắt buộc header {@value #CHONG_CSRF}: trang lạ không gửi được header tùy biến khi không có CORS. Đây là lớp chặn CSRF
 * thứ hai, sau {@code SameSite=Strict}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String CHONG_CSRF = "X-Requested-With";

    private final LoginUseCase login;
    private final RefreshSessionUseCase refresh;
    private final LogoutUseCase logout;
    private final RefreshCookie cookie;

    public AuthController(LoginUseCase login, RefreshSessionUseCase refresh, LogoutUseCase logout, RefreshCookie cookie) {
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
        this.cookie = cookie;
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return phien(login.execute(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = RefreshCookie.TEN, required = false) @Nullable String token,
            @RequestHeader(name = CHONG_CSRF, required = false) @Nullable String chongCsrf) {
        kiemChongCsrf(chongCsrf);
        if (token == null || token.isBlank()) {
            throw new AuthenticationFailedException(AuthenticationFailedException.PHIEN_HET_HAN);
        }
        return phien(refresh.execute(new RefreshTokenRequest(token)));
    }

    /** Thu hồi phiên của cookie (nếu có) và xóa cookie; lặp lại không lỗi. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookie.TEN, required = false) @Nullable String token,
            @RequestHeader(name = CHONG_CSRF, required = false) @Nullable String chongCsrf) {
        kiemChongCsrf(chongCsrf);
        if (token != null && !token.isBlank()) {
            logout.execute(new RefreshTokenRequest(token));
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.xoa().toString()).build();
    }

    private ResponseEntity<AccessTokenResponse> phien(AuthResponse phien) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.cap(phien).toString())
                .body(AccessTokenResponse.from(phien));
    }

    private static void kiemChongCsrf(@Nullable String giaTri) {
        if (giaTri == null || giaTri.isBlank()) {
            throw new ThieuHeaderChongCsrfException();
        }
    }
}
