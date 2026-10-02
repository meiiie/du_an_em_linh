package vn.hoctoanai.core.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.hoctoanai.core.identity.application.GiaLapDinhDanh;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.domain.model.Role;

class LogoutUseCaseTest {

    private GiaLapDinhDanh gl;
    private AuthResponse phien;

    @BeforeEach
    void setUp() {
        gl = new GiaLapDinhDanh();
        gl.themNguoiDung("gv@demo.local", "giaovien123", Role.TEACHER, true);
        phien = gl.login().execute(new LoginRequest("gv@demo.local", "giaovien123"), GiaLapDinhDanh.IP);
    }

    @Test
    void dangXuatThuHoiPhienVaLamMoiSauDoBiTuChoi() {
        gl.logout().execute(new RefreshTokenRequest(phien.refreshToken()));

        assertThat(gl.phienCua(phien.refreshToken()).isRevoked()).isTrue();
        assertThatCode(() -> gl.logout().execute(new RefreshTokenRequest(phien.refreshToken()))).doesNotThrowAnyException();
        assertThatCode(() -> gl.logout().execute(new RefreshTokenRequest("la"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void dangXuatBangTokenDaXoayVongVanGietTokenMoi() {
        // Như khi đăng xuất gửi token cũ trong lúc yêu cầu làm mới bằng chính token đó vừa xong.
        AuthResponse moi = gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken()));

        gl.logout().execute(new RefreshTokenRequest(phien.refreshToken()));

        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(moi.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage(AuthenticationFailedException.PHIEN_HET_HAN);
    }

    @Test
    void dangXuatChiKetThucPhienCuaToken() {
        AuthResponse mayKhac = gl.login().execute(new LoginRequest("gv@demo.local", "giaovien123"), GiaLapDinhDanh.IP);

        gl.logout().execute(new RefreshTokenRequest(phien.refreshToken()));

        assertThat(gl.phienCua(mayKhac.refreshToken()).isRevoked()).isFalse();
        assertThat(gl.refresh().execute(new RefreshTokenRequest(mayKhac.refreshToken())).refreshToken()).isNotBlank();
    }
}
