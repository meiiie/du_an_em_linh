package vn.hoctoanai.core.identity.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.UserDto;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.usecase.GetCurrentUserUseCase;
import vn.hoctoanai.core.identity.application.usecase.LoginUseCase;
import vn.hoctoanai.core.identity.application.usecase.LogoutUseCase;
import vn.hoctoanai.core.identity.application.usecase.RefreshSessionUseCase;
import vn.hoctoanai.core.identity.infrastructure.security.JwtConfig;
import vn.hoctoanai.core.identity.infrastructure.security.SecurityConfig;

@WebMvcTest(controllers = {AuthController.class, MeController.class})
@Import({SecurityConfig.class, JwtConfig.class})
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

    @Test
    void dangNhapTraPhien() {
        given(login.execute(any())).willReturn(new AuthResponse("a", Instant.EPOCH, "r", Instant.EPOCH, AN_DTO));
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"hs.an@demo.local\",\"password\":\"hocsinh123\"}"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.user.role")
            .isEqualTo("STUDENT");
    }

    @Test
    void dangNhapSaiTra401ProblemDetail() {
        given(login.execute(any())).willThrow(new AuthenticationFailedException(AuthenticationFailedException.THONG_BAO));
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"hs.an@demo.local\",\"password\":\"sai\"}"))
            .hasStatus(401)
            .bodyJson()
            .extractingPath("$.detail")
            .isEqualTo(AuthenticationFailedException.THONG_BAO);
    }

    @Test
    void thieuTruongTra400() {
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\"}"))
            .hasStatus(400);
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

    @Test
    void dangXuatTra204() {
        assertThat(mvc.post().uri("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"r\"}"))
            .hasStatus(204);
    }
}
