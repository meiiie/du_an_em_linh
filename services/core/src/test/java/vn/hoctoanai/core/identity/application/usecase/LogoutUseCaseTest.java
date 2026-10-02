package vn.hoctoanai.core.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import vn.hoctoanai.core.identity.application.GiaLapDinhDanh;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.Role;

class LogoutUseCaseTest {

    @Test
    void dangXuatThuHoiTokenVaLamMoiSauDoBiTuChoi() {
        GiaLapDinhDanh gl = new GiaLapDinhDanh();
        gl.themNguoiDung("gv@demo.local", "giaovien123", Role.TEACHER, true);
        AuthResponse phien = new LoginUseCase(gl.userRepository, gl.hasher, gl.sessionIssuer(), gl.clock)
            .execute(new LoginRequest("gv@demo.local", "giaovien123"));
        LogoutUseCase logout = new LogoutUseCase(gl.refreshTokenRepository, gl.clock);

        logout.execute(new RefreshTokenRequest(phien.refreshToken()));

        assertThat(gl.tokens.get(RefreshToken.hash(phien.refreshToken())).isRevoked()).isTrue();
        assertThatCode(() -> logout.execute(new RefreshTokenRequest(phien.refreshToken()))).doesNotThrowAnyException();
        assertThatCode(() -> logout.execute(new RefreshTokenRequest("la"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> new RefreshSessionUseCase(gl.refreshTokenRepository, gl.userRepository, gl.sessionIssuer(), gl.clock)
                .execute(new RefreshTokenRequest(phien.refreshToken())))
            .isInstanceOf(AuthenticationFailedException.class);
    }
}
