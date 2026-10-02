package vn.hoctoanai.core.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.hoctoanai.core.identity.application.GiaLapDinhDanh;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.domain.model.AuthSession;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.Role;
import vn.hoctoanai.core.identity.domain.model.User;

class RefreshSessionUseCaseTest {

    private GiaLapDinhDanh gl;
    private User an;
    private AuthResponse phien;

    @BeforeEach
    void setUp() {
        gl = new GiaLapDinhDanh();
        an = gl.themNguoiDung("hs.an@demo.local", "hocsinh123", Role.STUDENT, true);
        phien = gl.login().execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP);
    }

    @Test
    void xoayVongThuHoiTokenCuCapTokenMoiTrongCungPhien() {
        AuthResponse moi = gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken()));
        assertThat(moi.refreshToken()).isNotEqualTo(phien.refreshToken());
        assertThat(gl.tokens.get(RefreshToken.hash(phien.refreshToken())).isRevoked()).isTrue();
        assertThat(gl.tokens.get(RefreshToken.hash(moi.refreshToken())).isActive(GiaLapDinhDanh.NOW)).isTrue();
        assertThat(gl.phienCua(moi.refreshToken())).isEqualTo(gl.phienCua(phien.refreshToken()));
        assertThat(gl.sessions).hasSize(1);
    }

    @Test
    void dungLaiTokenDaThuHoiThuHoiMoiPhienCuaNguoiDo() {
        AuthResponse mayKhac = gl.login().execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP);
        AuthResponse moi = gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken()));

        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage(AuthenticationFailedException.PHIEN_HET_HAN);

        assertThat(gl.sessions.values()).hasSize(2).allMatch(AuthSession::isRevoked);
        for (String token : new String[] {moi.refreshToken(), mayKhac.refreshToken()}) {
            assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(token)))
                .isInstanceOf(AuthenticationFailedException.class);
        }
    }

    @Test
    void thuaCuocDuaVoiYeuCauDongThoiThuHoiMoiPhien() {
        AuthResponse moi = gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken()));
        gl.thuaCuocDua = true;
        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(moi.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage(AuthenticationFailedException.PHIEN_HET_HAN);
        assertThat(gl.sessions.values()).allMatch(AuthSession::isRevoked);
    }

    @Test
    void phienDaThuHoiTuChoiMaKhongThuHoiPhienKhac() {
        AuthResponse mayKhac = gl.login().execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP);
        gl.logout().execute(new RefreshTokenRequest(phien.refreshToken()));

        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class);
        assertThat(gl.phienCua(mayKhac.refreshToken()).isRevoked()).isFalse();
    }

    @Test
    void taiKhoanBiKhoaKhongLamMoiDuoc() {
        gl.khoa(an);
        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class);
        assertThat(gl.tokens.get(RefreshToken.hash(phien.refreshToken())).isRevoked()).isFalse();
    }

    @Test
    void tokenHetHanHoacLaBiTuChoi() {
        gl.clock = Clock.fixed(GiaLapDinhDanh.NOW.plus(Duration.ofDays(31)), ZoneOffset.UTC);
        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> gl.refresh().execute(new RefreshTokenRequest("khong-ton-tai")))
            .isInstanceOf(AuthenticationFailedException.class);
    }
}
