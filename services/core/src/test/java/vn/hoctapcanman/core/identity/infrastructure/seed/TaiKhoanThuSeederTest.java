package vn.hoctapcanman.core.identity.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/** Profile dev tạo 4 tài khoản tổng hợp (AGENTS.md «Tài khoản thử»), chạy lại không nhân bản. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class TaiKhoanThuSeederTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private TaiKhoanThuSeeder seeder;

    @Autowired
    private MockMvcTester mvc;

    @Test
    void taoBonTaiKhoanTongHopVaDangNhapDuoc() {
        seeder.run(new DefaultApplicationArguments());
        for (String email : new String[] {"hs.an@demo.local", "hs.binh@demo.local", "hs.chi@demo.local", "gv@demo.local"}) {
            assertThat(users.findByEmail(new Email(email))).hasValueSatisfying(user -> assertThat(user.synthetic()).isTrue());
        }
        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"hs.an@demo.local\",\"password\":\"hocsinh123\"}"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.user.displayName")
            .isEqualTo("An");
    }
}
