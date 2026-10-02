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
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.Role;

class RefreshSessionUseCaseTest {

    private GiaLapDinhDanh gl;
    private AuthResponse phien;

    @BeforeEach
    void setUp() {
        gl = new GiaLapDinhDanh();
        gl.themNguoiDung("hs.an@demo.local", "hocsinh123", Role.STUDENT, true);
        phien = new LoginUseCase(gl.userRepository, gl.hasher, gl.sessionIssuer(), gl.clock)
            .execute(new LoginRequest("hs.an@demo.local", "hocsinh123"));
    }

    private RefreshSessionUseCase refresh() {
        return new RefreshSessionUseCase(gl.refreshTokenRepository, gl.userRepository, gl.sessionIssuer(), gl.clock);
    }

    @Test
    void xoayVongThuHoiTokenCuCapTokenMoi() {
        AuthResponse moi = refresh().execute(new RefreshTokenRequest(phien.refreshToken()));
        assertThat(moi.refreshToken()).isNotEqualTo(phien.refreshToken());
        assertThat(gl.tokens.get(RefreshToken.hash(phien.refreshToken())).isRevoked()).isTrue();
        assertThat(gl.tokens.get(RefreshToken.hash(moi.refreshToken())).isActive(GiaLapDinhDanh.NOW)).isTrue();
    }

    @Test
    void dungLaiTokenDaThuHoiThuHoiMoiPhien() {
        AuthResponse moi = refresh().execute(new RefreshTokenRequest(phien.refreshToken()));
        assertThatThrownBy(() -> refresh().execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage(AuthenticationFailedException.PHIEN_HET_HAN);
        assertThat(gl.tokens.get(RefreshToken.hash(moi.refreshToken())).isRevoked()).isTrue();
    }

    @Test
    void thuaCuocDuaVoiYeuCauDongThoiThuHoiMoiPhien() {
        AuthResponse moi = refresh().execute(new RefreshTokenRequest(phien.refreshToken()));
        gl.thuaCuocDua = true;
        assertThatThrownBy(() -> refresh().execute(new RefreshTokenRequest(moi.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage(AuthenticationFailedException.PHIEN_HET_HAN);
        gl.thuaCuocDua = false;
        assertThat(gl.tokens.values()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void tokenHetHanHoacLaBiTuChoi() {
        gl.clock = Clock.fixed(GiaLapDinhDanh.NOW.plus(Duration.ofDays(31)), ZoneOffset.UTC);
        assertThatThrownBy(() -> refresh().execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> refresh().execute(new RefreshTokenRequest("khong-ton-tai")))
            .isInstanceOf(AuthenticationFailedException.class);
    }
}
