package vn.hoctoanai.core.identity.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.dto.UserDto;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.exception.LoginLockedException;
import vn.hoctoanai.core.identity.application.usecase.GetCurrentUserUseCase;
import vn.hoctoanai.core.identity.application.usecase.LoginUseCase;
import vn.hoctoanai.core.identity.application.usecase.LogoutUseCase;
import vn.hoctoanai.core.identity.application.usecase.RefreshSessionUseCase;
import vn.hoctoanai.core.identity.infrastructure.security.JwtConfig;
import vn.hoctoanai.core.identity.infrastructure.security.SecurityConfig;
import vn.hoctoanai.core.shared.infrastructure.ClockConfig;

@WebMvcTest(controllers = {AuthController.class, MeController.class})
@Import({SecurityConfig.class, JwtConfig.class, RefreshCookie.class, ClockConfig.class})
class AuthControllerTest {

    private static final UUID AN = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UserDto AN_DTO = new UserDto(AN, "hs.an@demo.local", "An", "STUDENT");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LoginUseCase login;

    @MockitoBean
    private RefreshSessionUseCase refresh;

    @MockitoBean
    private LogoutUseCase logout;

    @MockitoBean
    private GetCurrentUserUseCase currentUser;

    private static AuthResponse phien(String access, String refreshToken) {
        return new AuthResponse(access, Instant.now().plus(Duration.ofMinutes(15)), refreshToken,
            Instant.now().plus(Duration.ofDays(30)), AN_DTO);
    }

    private static String setCookie(MvcTestResult result) {
        return result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    @Test
    void dangNhapTraAccessTokenVaDatRefreshTokenVaoCookieHttpOnly() throws Exception {
        given(login.execute(any(), any())).willReturn(phien("a", "r"));
        MvcTestResult res = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"hs.an@demo.local\",\"password\":\"hocsinh123\"}").exchange();

        assertThat(res).hasStatusOk().bodyJson().extractingPath("$.user.role").isEqualTo("STUDENT");
        assertThat(res).bodyJson().extractingPath("$.accessToken").isEqualTo("a");
        assertThat(res.getResponse().getContentAsString()).doesNotContain("refreshToken").doesNotContain("\"r\"");
        assertThat(setCookie(res))
            .startsWith("hta_refresh=r;")
            .contains("Path=/api/auth", "HttpOnly", "Secure", "SameSite=Strict")
            .containsPattern("Max-Age=259\\d{4}");
    }

    @Test
    void dangNhapSaiTra401ProblemDetail() {
        given(login.execute(any(), any())).willThrow(new AuthenticationFailedException(AuthenticationFailedException.THONG_BAO));
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"hs.an@demo.local\",\"password\":\"sai\"}"))
            .hasStatus(401)
            .bodyJson()
            .extractingPath("$.detail")
            .isEqualTo(AuthenticationFailedException.THONG_BAO);
    }

    @Test
    void saiQuaNguongTra429CoRetryAfterVaTruyenIpMayKhach() {
        given(login.execute(any(), any())).willThrow(new LoginLockedException(Duration.ofMinutes(15)));
        MvcTestResult res = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"hs.an@demo.local\",\"password\":\"hocsinh123\"}")
            .with(request -> {
                request.setRemoteAddr("198.51.100.20");
                return request;
            })
            .exchange();

        assertThat(res).hasStatus(429).bodyJson().extractingPath("$.detail").isEqualTo(LoginLockedException.THONG_BAO);
        assertThat(res.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("900");
        then(login).should().execute(any(), eq("198.51.100.20"));
    }

    @Test
    void thieuTruongTra400() {
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\"}"))
            .hasStatus(400);
    }

    @Test
    void matKhauQua72ByteTra400() {
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"hs.an@demo.local\",\"password\":\"" + "ệ".repeat(25) + "\"}"))
            .hasStatus(400);
    }

    @Test
    void lamMoiDocTokenTuCookieVaXoayVongCookie() {
        given(refresh.execute(new RefreshTokenRequest("r"))).willReturn(phien("a2", "r2"));
        MvcTestResult res = mvc.post().uri("/api/auth/refresh")
            .cookie(new Cookie(RefreshCookie.TEN, "r"))
            .header(AuthController.CHONG_CSRF, "XMLHttpRequest")
            .exchange();

        assertThat(res).hasStatusOk().bodyJson().extractingPath("$.accessToken").isEqualTo("a2");
        assertThat(setCookie(res)).startsWith("hta_refresh=r2;").contains("HttpOnly", "SameSite=Strict");
    }

    @Test
    void lamMoiKhongCookieTra401VaXoaCookie() {
        MvcTestResult res = mvc.post().uri("/api/auth/refresh").header(AuthController.CHONG_CSRF, "XMLHttpRequest").exchange();

        assertThat(res).hasStatus(401).bodyJson().extractingPath("$.detail").isEqualTo(AuthenticationFailedException.PHIEN_HET_HAN);
        assertThat(setCookie(res)).startsWith("hta_refresh=;").contains("Max-Age=0", "Path=/api/auth");
    }

    @Test
    void lamMoiThatBaiCungXoaCookie() {
        given(refresh.execute(any())).willThrow(new AuthenticationFailedException(AuthenticationFailedException.PHIEN_HET_HAN));
        MvcTestResult res = mvc.post().uri("/api/auth/refresh")
            .cookie(new Cookie(RefreshCookie.TEN, "chet"))
            .header(AuthController.CHONG_CSRF, "XMLHttpRequest")
            .exchange();

        assertThat(res).hasStatus(401);
        assertThat(setCookie(res)).startsWith("hta_refresh=;").contains("Max-Age=0");
    }

    @Test
    void lamMoiVaDangXuatThieuHeaderChongCsrfTra403() {
        for (String uri : new String[] {"/api/auth/refresh", "/api/auth/logout"}) {
            assertThat(mvc.post().uri(uri).cookie(new Cookie(RefreshCookie.TEN, "r")))
                .hasStatus(403)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Thiếu header X-Requested-With.");
        }
        then(refresh).should(never()).execute(any());
        then(logout).should(never()).execute(any());
    }

    @Test
    void dangXuatThuHoiPhienCuaCookieVaXoaCookie() {
        MvcTestResult res = mvc.post().uri("/api/auth/logout")
            .cookie(new Cookie(RefreshCookie.TEN, "r"))
            .header(AuthController.CHONG_CSRF, "XMLHttpRequest")
            .exchange();

        assertThat(res).hasStatus(204);
        assertThat(setCookie(res)).startsWith("hta_refresh=;").contains("Max-Age=0", "HttpOnly");
        then(logout).should().execute(new RefreshTokenRequest("r"));
    }

    @Test
    void dangXuatKhongCookieVan204() {
        assertThat(mvc.post().uri("/api/auth/logout").header(AuthController.CHONG_CSRF, "XMLHttpRequest")).hasStatus(204);
        then(logout).should(never()).execute(any());
    }

    @Test
    void meCanToken() {
        assertThat(mvc.get().uri("/api/me")).hasStatus(401);
    }

    @Test
    void meTraChinhNguoiTrongToken() {
        given(currentUser.execute(AN)).willReturn(AN_DTO);
        assertThat(mvc.get().uri("/api/me").with(jwt().jwt(j -> j.subject(AN.toString()).claim("role", "STUDENT"))))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.id")
            .isEqualTo(AN.toString());
    }
}
