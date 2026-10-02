package vn.hoctoanai.core.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctoanai.core.TestcontainersConfiguration;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.application.exception.LoginLockedException;
import vn.hoctoanai.core.identity.application.port.PasswordHasher;
import vn.hoctoanai.core.identity.application.usecase.LoginUseCase;
import vn.hoctoanai.core.identity.application.usecase.LogoutUseCase;
import vn.hoctoanai.core.identity.application.usecase.RefreshSessionUseCase;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.Role;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;
import vn.hoctoanai.core.identity.infrastructure.web.AuthController;
import vn.hoctoanai.core.identity.infrastructure.web.RefreshCookie;

/**
 * Trọn luồng trên PostgreSQL 18 thật, refresh token qua cookie: đăng nhập → /api/me → làm mới → đăng xuất → làm mới bị
 * từ chối; và đăng xuất chạy
 * đồng thời với làm mới trên cùng phiên.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class IdentityIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordHasher hasher;

    @Autowired
    private LoginUseCase login;

    @Autowired
    private RefreshSessionUseCase refresh;

    @Autowired
    private LogoutUseCase logout;

    @Test
    void dangNhapLamMoiDangXuat() throws Exception {
        users.save(User.create(new Email("gv.tich-hop@demo.local"), hasher.hash("giaovien123"), "Giáo viên thử", Role.TEACHER, true, Instant.now()));

        MvcTestResult dangNhap = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"gv.tich-hop@demo.local\",\"password\":\"giaovien123\"}").exchange();
        assertThat(dangNhap).hasStatusOk();
        String body = dangNhap.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String access = JsonPath.read(body, "$.accessToken");
        assertThat(body).doesNotContain("refreshToken");
        Cookie refresh = cookieRefresh(dangNhap);

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer " + access))
            .hasStatusOk().bodyJson().extractingPath("$.role").isEqualTo("TEACHER");

        MvcTestResult lamMoi = mvc.post().uri("/api/auth/refresh").cookie(refresh)
            .header(AuthController.CHONG_CSRF, "XMLHttpRequest").exchange();
        assertThat(lamMoi).hasStatusOk();
        Cookie refreshMoi = cookieRefresh(lamMoi);
        assertThat(refreshMoi.getValue()).isNotEqualTo(refresh.getValue());

        MvcTestResult dangXuat = mvc.post().uri("/api/auth/logout").cookie(refreshMoi)
            .header(AuthController.CHONG_CSRF, "XMLHttpRequest").exchange();
        assertThat(dangXuat).hasStatus(204);
        assertThat(dangXuat.getResponse().getHeader(HttpHeaders.SET_COOKIE)).startsWith(RefreshCookie.TEN + "=;").contains("Max-Age=0");
        assertThat(mvc.post().uri("/api/auth/refresh").cookie(refreshMoi)
            .header(AuthController.CHONG_CSRF, "XMLHttpRequest")).hasStatus(401);
        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer khong-hop-le")).hasStatus(401);
    }

    /** Cookie refresh token trong {@code Set-Cookie}: HttpOnly, SameSite=Strict, chỉ cho {@code /api/auth}. */
    private static Cookie cookieRefresh(MvcTestResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly", "SameSite=Strict", "Path=/api/auth");
        Matcher m = Pattern.compile(RefreshCookie.TEN + "=([^;]+);").matcher(setCookie);
        assertThat(m.find()).as("Set-Cookie có refresh token: %s", setCookie).isTrue();
        return new Cookie(RefreshCookie.TEN, m.group(1));
    }

    /** F-10 qua HTTP: 5 lần sai → 401; lần thứ 6, dù đúng mật khẩu → 429 kèm Retry-After. IP lấy từ remoteAddr. */
    @Test
    void saiNamLanThiTra429() {
        users.save(User.create(new Email("gv.f10@demo.local"), hasher.hash("giaovien123"), "Giáo viên F10", Role.TEACHER, true, Instant.now()));
        for (int i = 0; i < 5; i++) {
            assertThat(dangNhapTu("198.51.100.30", "gv.f10@demo.local", "sai")).hasStatus(401);
        }
        MvcTestResult khoa = dangNhapTu("198.51.100.30", "gv.f10@demo.local", "giaovien123");
        assertThat(khoa).hasStatus(429);
        // Còn lại tới khi lần sai cũ nhất ra khỏi cửa sổ 15 phút: vừa sai xong nên gần 900 giây.
        assertThat(Integer.parseInt(khoa.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isBetween(840, 900);
        assertThat(dangNhapTu("198.51.100.31", "gv.f10@demo.local", "giaovien123")).as("máy khác không bị khóa").hasStatusOk();
    }

    private MvcTestResult dangNhapTu(String ip, String email, String matKhau) {
        return mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + matKhau + "\"}")
            .with(request -> {
                request.setRemoteAddr(ip);
                return request;
            })
            .exchange();
    }

    /** Khóa tư vấn theo email + IP: 10 lần sai gửi cùng lúc chỉ thử được đúng 5 mật khẩu, 5 lần còn lại bị khóa. */
    @Test
    void saiDongThoiKhongVuotNguong() throws Exception {
        users.save(User.create(new Email("hs.f10@demo.local"), hasher.hash("hocsinh123"), "Học sinh F10", Role.STUDENT, true, Instant.now()));
        int soLuong = 10;
        ExecutorService pool = Executors.newFixedThreadPool(soLuong);
        try {
            CyclicBarrier xuatPhat = new CyclicBarrier(soLuong);
            List<Future<Class<?>>> ketQua = new ArrayList<>();
            for (int i = 0; i < soLuong; i++) {
                ketQua.add(pool.submit(() -> {
                    xuatPhat.await();
                    try {
                        login.execute(new LoginRequest("hs.f10@demo.local", "sai"), "198.51.100.40");
                        return Object.class;
                    } catch (AuthenticationFailedException | LoginLockedException e) {
                        return e.getClass();
                    }
                }));
            }
            List<Class<?>> loai = new ArrayList<>();
            for (Future<Class<?>> f : ketQua) {
                loai.add(f.get(30, TimeUnit.SECONDS));
            }
            assertThat(loai).filteredOn(AuthenticationFailedException.class::equals).hasSize(5);
            assertThat(loai).filteredOn(LoginLockedException.class::equals).hasSize(5);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Hai giao dịch thật chạy cùng lúc nhiều lần: dù bên nào thắng, sau đăng xuất không còn token nào của phiên dùng được. */
    @Test
    void dangXuatDongThoiVoiLamMoiVanKetThucPhien() throws Exception {
        users.save(User.create(new Email("hs.dong-thoi@demo.local"), hasher.hash("hocsinh123"), "Học sinh thử", Role.STUDENT, true, Instant.now()));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int lan = 0; lan < 30; lan++) {
                String token = login.execute(new LoginRequest("hs.dong-thoi@demo.local", "hocsinh123"), "203.0.113.7").refreshToken();
                CyclicBarrier xuatPhat = new CyclicBarrier(2);
                Future<Optional<String>> lamMoi = pool.submit(() -> {
                    xuatPhat.await();
                    try {
                        return Optional.of(refresh.execute(new RefreshTokenRequest(token)).refreshToken());
                    } catch (AuthenticationFailedException e) {
                        return Optional.empty();
                    }
                });
                Future<?> dangXuat = pool.submit(() -> {
                    xuatPhat.await();
                    logout.execute(new RefreshTokenRequest(token));
                    return null;
                });
                dangXuat.get(10, TimeUnit.SECONDS);
                String conLai = lamMoi.get(10, TimeUnit.SECONDS).orElse(token);
                assertThatThrownBy(() -> refresh.execute(new RefreshTokenRequest(conLai)))
                    .as("lần %d", lan)
                    .isInstanceOf(AuthenticationFailedException.class);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
