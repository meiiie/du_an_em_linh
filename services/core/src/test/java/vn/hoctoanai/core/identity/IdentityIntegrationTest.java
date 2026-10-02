package vn.hoctoanai.core.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctoanai.core.TestcontainersConfiguration;
import vn.hoctoanai.core.identity.application.port.PasswordHasher;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.Role;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;

/** Trọn luồng trên PostgreSQL 18 thật: đăng nhập → /api/me → làm mới → đăng xuất → làm mới bị từ chối. */
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

    @Test
    void dangNhapLamMoiDangXuat() throws Exception {
        users.save(User.create(new Email("gv.tich-hop@demo.local"), hasher.hash("giaovien123"), "Giáo viên thử", Role.TEACHER, true, Instant.now()));

        MvcTestResult dangNhap = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"gv.tich-hop@demo.local\",\"password\":\"giaovien123\"}").exchange();
        assertThat(dangNhap).hasStatusOk();
        String body = dangNhap.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String access = JsonPath.read(body, "$.accessToken");
        String refresh = JsonPath.read(body, "$.refreshToken");

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer " + access))
            .hasStatusOk().bodyJson().extractingPath("$.role").isEqualTo("TEACHER");

        MvcTestResult lamMoi = mvc.post().uri("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refresh + "\"}").exchange();
        assertThat(lamMoi).hasStatusOk();
        String refreshMoi = JsonPath.read(lamMoi.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.refreshToken");

        assertThat(mvc.post().uri("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refreshMoi + "\"}")).hasStatus(204);
        assertThat(mvc.post().uri("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + refreshMoi + "\"}")).hasStatus(401);
        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer khong-hop-le")).hasStatus(401);
    }
}
