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
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.exception.LoginLockedException;
import vn.hoctoanai.core.identity.domain.model.RefreshToken;
import vn.hoctoanai.core.identity.domain.model.Role;

class LoginUseCaseTest {

    private GiaLapDinhDanh gl;
    private LoginUseCase login;

    @BeforeEach
    void setUp() {
        gl = new GiaLapDinhDanh();
        login = gl.login();
        gl.themNguoiDung("hs.an@demo.local", "hocsinh123", Role.STUDENT, true);
        gl.themNguoiDung("khoa@demo.local", "hocsinh123", Role.STUDENT, false);
    }

    @Test
    void dangNhapDungCapPhien() {
        AuthResponse res = login.execute(new LoginRequest(" HS.An@demo.local ", "hocsinh123"), GiaLapDinhDanh.IP);
        assertThat(res.user().email()).isEqualTo("hs.an@demo.local");
        assertThat(res.user().role()).isEqualTo("STUDENT");
        assertThat(res.accessTokenExpiresAt()).isEqualTo(GiaLapDinhDanh.NOW.plusSeconds(15 * 60));
        assertThat(gl.tokens).containsKey(RefreshToken.hash(res.refreshToken()));
        assertThat(gl.tokens.values()).noneMatch(t -> t.tokenHash().equals(res.refreshToken()));
        assertThat(gl.sessions).hasSize(1);
        assertThat(gl.phienCua(res.refreshToken()).isRevoked()).isFalse();
    }

    @Test
    void moiLanDangNhapMoPhienRieng() {
        AuthResponse may1 = login.execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP);
        AuthResponse may2 = login.execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP);
        assertThat(gl.phienCua(may1.refreshToken()).id()).isNotEqualTo(gl.phienCua(may2.refreshToken()).id());
    }

    @Test
    void saiMatKhauEmailLaTaiKhoanKhoaCungMotThongBao() {
        for (LoginRequest sai : new LoginRequest[] {
            new LoginRequest("hs.an@demo.local", "sai"),
            new LoginRequest("khong-co@demo.local", "hocsinh123"),
            new LoginRequest("khoa@demo.local", "hocsinh123"),
            new LoginRequest("khong-phai-email", "hocsinh123")}) {
            assertThatThrownBy(() -> login.execute(sai, GiaLapDinhDanh.IP))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthenticationFailedException.THONG_BAO);
        }
        assertThat(gl.tokens).isEmpty();
        assertThat(gl.sessions).isEmpty();
    }

    private void saiNamLan(String email, String ip) {
        for (int i = 0; i < LoginUseCase.NGUONG; i++) {
            assertThatThrownBy(() -> login.execute(new LoginRequest(email, "sai"), ip))
                .isInstanceOf(AuthenticationFailedException.class);
        }
    }

    @Test
    void saiNamLanThiKhoaKeCaKhiLanSauDungMatKhau() {
        saiNamLan("hs.an@demo.local", GiaLapDinhDanh.IP);

        assertThatThrownBy(() -> login.execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP))
            .isInstanceOf(LoginLockedException.class)
            .hasMessage(LoginLockedException.THONG_BAO)
            .satisfies(e -> assertThat(((LoginLockedException) e).thuLaiSau()).isEqualTo(Duration.ofMinutes(15)));
        assertThat(gl.sessions).isEmpty();
        assertThat(gl.lanSai).hasSize(LoginUseCase.NGUONG);
    }

    @Test
    void emailKhongTonTaiCungBiKhoaNhuThuong() {
        saiNamLan("khong-co@demo.local", GiaLapDinhDanh.IP);
        assertThatThrownBy(() -> login.execute(new LoginRequest("khong-co@demo.local", "x"), GiaLapDinhDanh.IP))
            .isInstanceOf(LoginLockedException.class);
    }

    @Test
    void khoaTheoEmailVaIp() {
        saiNamLan("hs.an@demo.local", GiaLapDinhDanh.IP);
        assertThat(login.execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), "198.51.100.1").user().email())
            .isEqualTo("hs.an@demo.local");
        assertThatThrownBy(() -> login.execute(new LoginRequest(" HS.AN@demo.local ", "hocsinh123"), GiaLapDinhDanh.IP))
            .as("email chuẩn hóa trước khi băm")
            .isInstanceOf(LoginLockedException.class);
    }

    @Test
    void hetCuaSoMuoiLamPhutThiMoLai() {
        saiNamLan("hs.an@demo.local", GiaLapDinhDanh.IP);
        gl.clock = Clock.fixed(GiaLapDinhDanh.NOW.plus(Duration.ofMinutes(15)).plusSeconds(1), ZoneOffset.UTC);
        login = gl.login();
        assertThat(login.execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP).accessToken()).isNotBlank();
    }

    @Test
    void dangNhapDungDuoiNguongXoaBoDem() {
        for (int i = 0; i < LoginUseCase.NGUONG - 1; i++) {
            assertThatThrownBy(() -> login.execute(new LoginRequest("hs.an@demo.local", "sai"), GiaLapDinhDanh.IP))
                .isInstanceOf(AuthenticationFailedException.class);
        }
        login.execute(new LoginRequest("hs.an@demo.local", "hocsinh123"), GiaLapDinhDanh.IP);
        assertThat(gl.lanSai).isEmpty();
        saiNamLan("hs.an@demo.local", GiaLapDinhDanh.IP);
    }

    @Test
    void emailKhongTonTaiVanSoMatKhauDeKhongLoThoiGian() {
        assertThatThrownBy(() -> login.execute(new LoginRequest("khong-co@demo.local", "x"), GiaLapDinhDanh.IP))
            .isInstanceOf(AuthenticationFailedException.class);
        assertThat(gl.soLanSoMatKhau.get()).isEqualTo(1);
    }
}
