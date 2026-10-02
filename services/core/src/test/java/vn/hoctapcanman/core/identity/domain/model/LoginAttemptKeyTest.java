package vn.hoctapcanman.core.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginAttemptKeyTest {

    @Test
    void bamEmailChuanHoaVaIpKhongGiuGiaTriGoc() {
        LoginAttemptKey a = LoginAttemptKey.of(" HS.An@demo.local ", "203.0.113.7");
        assertThat(a).isEqualTo(LoginAttemptKey.of("hs.an@demo.local", "203.0.113.7"));
        assertThat(a.hash()).hasSize(64).doesNotContain("demo").doesNotContain("203");
        assertThat(a.toString()).doesNotContain(a.hash());
    }

    @Test
    void khacIpHoacEmailLaKhoaKhac() {
        LoginAttemptKey goc = LoginAttemptKey.of("hs.an@demo.local", "203.0.113.7");
        assertThat(LoginAttemptKey.of("hs.an@demo.local", "203.0.113.8")).isNotEqualTo(goc);
        assertThat(LoginAttemptKey.of("hs.binh@demo.local", "203.0.113.7")).isNotEqualTo(goc);
        assertThat(LoginAttemptKey.of("hs.an@demo.local", "")).isEqualTo(LoginAttemptKey.of("hs.an@demo.local", "-"));
    }
}
