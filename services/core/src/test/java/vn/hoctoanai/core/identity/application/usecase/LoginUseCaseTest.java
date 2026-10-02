package vn.hoctoanai.core.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.hoctoanai.core.identity.application.GiaLapDinhDanh;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.Role;

class LoginUseCaseTest {

    private GiaLapDinhDanh gl;
    private LoginUseCase login;

    @BeforeEach
    void setUp() {
        gl = new GiaLapDinhDanh();
        login = new LoginUseCase(gl.userRepository, gl.hasher, gl.sessionIssuer(), gl.clock);
        gl.themNguoiDung("hs.an@demo.local", "hocsinh123", Role.STUDENT, true);
        gl.themNguoiDung("khoa@demo.local", "hocsinh123", Role.STUDENT, false);
    }

    @Test
    void dangNhapDungCapPhien() {
        AuthResponse res = login.execute(new LoginRequest(" HS.An@demo.local ", "hocsinh123"));
        assertThat(res.user().email()).isEqualTo("hs.an@demo.local");
        assertThat(res.user().role()).isEqualTo("STUDENT");
        assertThat(res.accessTokenExpiresAt()).isEqualTo(GiaLapDinhDanh.NOW.plusSeconds(15 * 60));
        assertThat(gl.tokens).containsKey(RefreshToken.hash(res.refreshToken()));
        assertThat(gl.tokens.values()).noneMatch(t -> t.tokenHash().equals(res.refreshToken()));
    }

    @Test
    void saiMatKhauEmailLaTaiKhoanKhoaCungMotThongBao() {
        for (LoginRequest sai : new LoginRequest[] {
            new LoginRequest("hs.an@demo.local", "sai"),
            new LoginRequest("khong-co@demo.local", "hocsinh123"),
            new LoginRequest("khoa@demo.local", "hocsinh123"),
            new LoginRequest("khong-phai-email", "hocsinh123")}) {
            assertThatThrownBy(() -> login.execute(sai))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthenticationFailedException.THONG_BAO);
        }
        assertThat(gl.tokens).isEmpty();
    }

    @Test
    void emailKhongTonTaiVanSoMatKhauDeKhongLoThoiGian() {
        assertThatThrownBy(() -> login.execute(new LoginRequest("khong-co@demo.local", "x")))
            .isInstanceOf(AuthenticationFailedException.class);
        assertThat(gl.soLanSoMatKhau.get()).isEqualTo(1);
    }
}
